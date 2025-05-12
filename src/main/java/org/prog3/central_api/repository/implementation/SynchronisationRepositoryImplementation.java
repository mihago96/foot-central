package org.prog3.central_api.repository.implementation;

import org.prog3.central_api.configuration.DataSource;
import org.prog3.central_api.model.ChampionShip;
import org.prog3.central_api.model.httpModels.ChampUrl;
import org.prog3.central_api.repository.SynchronisationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Repository
@AllArgsConstructor
public class SynchronisationRepositoryImplementation implements SynchronisationRepository {
    private static final Logger logger = LoggerFactory.getLogger(SynchronisationRepositoryImplementation.class);
    
    private final DataSource dataSource;
    private final RestTemplate restTemplate;

    private static final List<ChampUrl> CHAMP_URLS = Arrays.asList(
            new ChampUrl(ChampionShip.LA_LIGA, 8081),
           new ChampUrl(ChampionShip.BUNDESLIGA, 8082)
//            new ChampUrl(ChampionShip.LIGUE_1, 8083),
//            new ChampUrl(ChampionShip.PREMIER_LEAGUE, 8084),
//            new ChampUrl(ChampionShip.SERIA, 8085)
    );

    @Override
    public Boolean Syncronisation() {
        logger.info("Starting synchronization process");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                logger.info("Clearing existing data from tables");
                clearTables(connection);

                for (ChampUrl champUrl : CHAMP_URLS) {
                    String baseUrl = "http://localhost:" + champUrl.getPort();
                    logger.info("Processing championship: {} at URL: {}", champUrl.getChampionShip(), baseUrl);
                    
                    // 1. Récupérer toutes les saisons
                    logger.info("Synchronizing seasons for {}", champUrl.getChampionShip());
                    syncSeasons(connection, baseUrl, champUrl.getChampionShip());
                    
                    // 2. Récupérer tous les clubs
                    logger.info("Synchronizing clubs for {}", champUrl.getChampionShip());
                    syncClubs(connection, baseUrl, champUrl.getChampionShip());
                    
                    // 3. Récupérer tous les joueurs et leurs statistiques
                    logger.info("Synchronizing players for {}", champUrl.getChampionShip());
                    syncPlayers(connection, baseUrl, champUrl.getChampionShip());
                    
                    // 4. Calculer et mettre à jour la médiane des différences de buts
                    logger.info("Updating championship median for {}", champUrl.getChampionShip());
                    updateChampionshipMedian(connection, champUrl.getChampionShip());
                    
                    logger.info("Completed synchronization for {}", champUrl.getChampionShip());
                }

                connection.commit();
                logger.info("Synchronization completed successfully");
                return true;
            } catch (Exception e) {
                connection.rollback();
                logger.error("Synchronization failed with error: {}", e.getMessage(), e);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Database connection error: {}", e.getMessage(), e);
            return false;
        }
    }

    private void clearTables(Connection connection) throws SQLException {
        String[] clearQueries = {
                "DELETE FROM players",
                "DELETE FROM clubs",
                "DELETE FROM championships",
                "DELETE FROM seasons"
        };

        for (String query : clearQueries) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                int rowsAffected = stmt.executeUpdate();
                logger.debug("Cleared table with query: {}. Rows affected: {}", query, rowsAffected);
            }
        }
    }

    private void syncSeasons(Connection connection, String baseUrl, ChampionShip championship) {
        String url = baseUrl + "/seasons";
        logger.debug("Fetching seasons from URL: {}", url);
        
        try {
            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);
            Map[] seasons = response.getBody();

            if (seasons == null) {
                logger.warn("No seasons data returned from {}", url);
                return;
            }

            logger.info("Retrieved {} seasons for {}", seasons.length, championship);

            String insertSeason = """
                INSERT INTO seasons (year, championship, status)
                VALUES (?, ?::championship_enum, ?)
                ON CONFLICT (year) DO UPDATE
                SET championship = EXCLUDED.championship,
                    status = EXCLUDED.status
            """;

            try (PreparedStatement stmt = connection.prepareStatement(insertSeason)) {
                int insertCount = 0;
                for (Map<String, Object> season : seasons) {
                    Integer year = Integer.parseInt(season.get("year").toString());
                    String status = (String) season.get("status");
                    
                    stmt.setInt(1, year);
                    stmt.setString(2, championship.name());
                    stmt.setString(3, status);
                    stmt.executeUpdate();
                    insertCount++;
                    
                    logger.debug("Inserted/updated season: year={}, championship={}, status={}", 
                            year, championship, status);
                }
                logger.info("Synchronized {} seasons for {}", insertCount, championship);
            } catch (SQLException e) {
                logger.error("Error inserting seasons for {}: {}", championship, e.getMessage(), e);
                throw new RuntimeException("Error syncing seasons for " + championship, e);
            }
        } catch (Exception e) {
            logger.error("Failed to fetch seasons from {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Error fetching seasons for " + championship, e);
        }
    }

    private void syncClubs(Connection connection, String baseUrl, ChampionShip championship) {
        String urlClubs = baseUrl + "/clubs";
        logger.debug("Fetching clubs from URL: {}", urlClubs);
        
        try {
            ResponseEntity<Map[]> response = restTemplate.getForEntity(urlClubs, Map[].class);
            Map[] clubs = response.getBody();
    
            if (clubs == null) {
                logger.warn("No clubs data returned from {}", urlClubs);
                return;
            }
            
            logger.info("Retrieved {} clubs for {}", clubs.length, championship);
    
            // Récupérer les saisons pour ce championnat
            List<Integer> seasonYears = getSeasonYears(connection, championship);
            
            if (seasonYears.isEmpty()) {
                logger.warn("No seasons found for championship {}, skipping club synchronization", championship);
                return;
            }
    
            String insertClub = """
                INSERT INTO clubs (
                    id, name, acronym, year_creation, stadium,
                    coach_name, coach_nationality, championship,
                    scored_goals, conceded_goals, clean_sheet_number, ranking_points,
                    season_year
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::championship_enum, ?, ?, ?, ?, ?)
            """;
    
            try (PreparedStatement stmt = connection.prepareStatement(insertClub)) {
                int clubCount = 0;
                int statCount = 0;
                int skippedClubs = 0;
                int skippedStats = 0;
                
                for (Map<String, Object> club : clubs) {
                    String clubId = (String) club.get("id");
                    String name = (String) club.get("name");
                    String acronym = (String) club.get("acronym");
                    Number yearCreationObj = (Number) club.get("yearCreation");
                    String stadium = (String) club.get("stadium");
                    Map<String, String> coach = (Map<String, String>) club.get("coach");
                    
                    // Vérification détaillée des champs manquants
                    StringBuilder missingFields = new StringBuilder();
                    if (clubId == null) missingFields.append("id, ");
                    if (name == null) missingFields.append("name, ");
                    if (acronym == null) missingFields.append("acronym, ");
                    if (yearCreationObj == null) missingFields.append("yearCreation, ");
                    if (stadium == null) missingFields.append("stadium, ");
                    if (coach == null) missingFields.append("coach, ");
                    else {
                        if (coach.get("name") == null) missingFields.append("coach.name, ");
                        if (coach.get("nationality") == null) missingFields.append("coach.nationality, ");
                    }
                    
                    if (missingFields.length() > 0) {
                        missingFields.setLength(missingFields.length() - 2); // Enlever la dernière virgule et espace
                        logger.warn("Skipping club with missing fields: {}. Club data: {}", 
                                missingFields.toString(), club);
                        skippedClubs++;
                        continue;
                    }
                    
                    int yearCreation = yearCreationObj.intValue();
                    String coachName = coach.get("name");
                    String coachNationality = coach.get("nationality");
                    
                    logger.debug("Processing club: id={}, name={}, acronym={}", clubId, name, acronym);
                    
                    for (Integer seasonYear : seasonYears) {
                        String urlStats = String.format("%s/clubs/statistics/%d", baseUrl, seasonYear);
                        logger.info("Fetching club statistics for season {} from URL: {}", seasonYear, urlStats);
                        
                        try {
                            logger.debug("Sending HTTP GET request to: {}", urlStats);
                            long startTime = System.currentTimeMillis();
                            ResponseEntity<Map[]> statsResponse = restTemplate.getForEntity(urlStats, Map[].class);
                            long endTime = System.currentTimeMillis();
                            
                            logger.debug("Received response from {} in {}ms with status code: {}", 
                                    urlStats, (endTime - startTime), statsResponse.getStatusCode());
                            
                            Map[] clubsStats = statsResponse.getBody();
                            
                            if (clubsStats == null) {
                                logger.warn("No club statistics returned for season {} (empty body)", seasonYear);
                                skippedStats++;
                                continue;
                            }
                            
                            logger.debug("Retrieved statistics for {} clubs in season {}", clubsStats.length, seasonYear);
                            
                            // Trouver les statistiques du club actuel
                            Map<String, Object> clubStat = null;
                            logger.debug("Searching for statistics of club {} (name: {}) among {} entries", 
                                    clubId, name, clubsStats.length);
                            
                            // Afficher tous les IDs des clubs dans la réponse pour le débogage
                            if (logger.isDebugEnabled()) {
                                StringBuilder clubIds = new StringBuilder("Club IDs in response: ");
                                for (Map<String, Object> stat : clubsStats) {
                                    clubIds.append(stat.get("id")).append(", ");
                                }
                                logger.debug(clubIds.toString());
                            }
                            
                            // Recherche directe par ID du club (les statistiques contiennent directement l'ID du club)
                            for (Map<String, Object> stat : clubsStats) {
                                String statClubId = (String) stat.get("id");
                                if (statClubId != null && statClubId.equals(clubId)) {
                                    clubStat = stat;
                                    logger.debug("Found statistics for club {} (name: {})", clubId, name);
                                    break;
                                }
                            }
                            
                            if (clubStat == null) {
                                logger.warn("No statistics found for club {} (name: {}) in season {} - club not present in response", 
                                        clubId, name, seasonYear);
                                skippedStats++;
                                continue;
                            }
                            
                            // Log the actual statistics data
                            logger.debug("Club {} (name: {}) statistics for season {}: {}", 
                                    clubId, name, seasonYear, clubStat);
                            
                            // Vérification détaillée des statistiques manquantes
                            StringBuilder missingStats = new StringBuilder();
                            Number rankingPointsObj = (Number) clubStat.get("rankingPoints");
                            Number scoredGoalsObj = (Number) clubStat.get("scoreGoals"); // Changé de "scoredGoals" à "scoreGoals"
                            Number concededGoalsObj = (Number) clubStat.get("concededGoals");
                            Number cleanSheetNumberObj = (Number) clubStat.get("cleanSheetNumber");
                            
                            if (rankingPointsObj == null) missingStats.append("rankingPoints, ");
                            if (scoredGoalsObj == null) missingStats.append("scoreGoals, "); // Changé de "scoredGoals" à "scoreGoals"
                            if (concededGoalsObj == null) missingStats.append("concededGoals, ");
                            if (cleanSheetNumberObj == null) missingStats.append("cleanSheetNumber, ");
                            
                            if (missingStats.length() > 0) {
                                missingStats.setLength(missingStats.length() - 2); // Enlever la dernière virgule et espace
                                logger.warn("Incomplete statistics for club {} (name: {}) in season {}: missing {}. Stats data: {}", 
                                        clubId, name, seasonYear, missingStats.toString(), clubStat);
                                skippedStats++;
                                continue;
                            }
                            
                            int rankingPoints = rankingPointsObj.intValue();
                            int scoredGoals = scoredGoalsObj.intValue();
                            int concededGoals = concededGoalsObj.intValue();
                            int cleanSheetNumber = cleanSheetNumberObj.intValue();
                            
                            String clubSeasonId = clubId + "_" + seasonYear;
                            stmt.setString(1, clubSeasonId);
                            stmt.setString(2, name);
                            stmt.setString(3, acronym);
                            stmt.setInt(4, yearCreation);
                            stmt.setString(5, stadium);
                            stmt.setString(6, coachName);
                            stmt.setString(7, coachNationality);
                            stmt.setString(8, championship.name());
                            stmt.setInt(9, scoredGoals);
                            stmt.setInt(10, concededGoals);
                            stmt.setInt(11, cleanSheetNumber);
                            stmt.setInt(12, rankingPoints);
                            stmt.setInt(13, seasonYear);
                            
                            stmt.executeUpdate();
                            statCount++;
                            
                            logger.debug("Inserted club statistics: id={}, season={}, points={}, scoredGoals={}, concededGoals={}, cleanSheets={}", 
                                    clubSeasonId, seasonYear, rankingPoints, scoredGoals, concededGoals, cleanSheetNumber);
                        } catch (Exception e) {
                            logger.error("Error fetching statistics for club {} (name: {}) in season {}: {} - {}", 
                                    clubId, name, seasonYear, e.getClass().getName(), e.getMessage(), e);
                            logger.debug("Stack trace for error fetching club statistics:", e);
                            skippedStats++;
                        }
                    }
                    clubCount++;
                }
                logger.info("Synchronized {} clubs with {} season statistics for {}. Skipped {} clubs and {} statistics.", 
                        clubCount, statCount, championship, skippedClubs, skippedStats);
            } catch (SQLException e) {
                logger.error("Database error while syncing clubs for {}: {}", championship, e.getMessage(), e);
                throw new RuntimeException("Error syncing clubs for " + championship, e);
            }
        } catch (Exception e) {
            logger.error("Failed to fetch clubs from {}: {}", urlClubs, e.getMessage(), e);
            throw new RuntimeException("Error fetching clubs for " + championship, e);
        }
    }

    private List<Integer> getSeasonYears(Connection connection, ChampionShip championship) {
        List<Integer> years = new java.util.ArrayList<>();
        String query = "SELECT year FROM seasons WHERE championship = ?::championship_enum";
        
        logger.debug("Fetching season years for championship: {}", championship);
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, championship.name());
            var rs = stmt.executeQuery();
            
            while (rs.next()) {
                years.add(rs.getInt("year"));
            }
            
            logger.debug("Found {} seasons for championship {}: {}", years.size(), championship, years);
        } catch (SQLException e) {
            logger.error("Error retrieving season years for {}: {}", championship, e.getMessage(), e);
            throw new RuntimeException("Error getting season years for " + championship, e);
        }
        
        return years;
    }

    private void syncPlayers(Connection connection, String baseUrl, ChampionShip championship) {
        String urlPlayers = baseUrl + "/players";
        logger.debug("Fetching players from URL: {}", urlPlayers);
        
        try {
            ResponseEntity<Map[]> playersResponse = restTemplate.getForEntity(urlPlayers, Map[].class);
            Map[] players = playersResponse.getBody();
        
            if (players == null) {
                logger.warn("No players data returned from {}", urlPlayers);
                return;
            }

            logger.info("Retrieved {} players for {}", players.length, championship);
        
            // Récupérer les saisons pour ce championnat
            List<Integer> seasonYears = getSeasonYears(connection, championship);
            
            if (seasonYears.isEmpty()) {
                logger.warn("No seasons found for championship {}, skipping player synchronization", championship);
                return;
            }

            String insertPlayerSql = """
                INSERT INTO players ( 
                    id, name, number, position, nationality, age,
                    championship, scored_goals, playing_time_value, playing_time_unit,
                    season_year
                ) VALUES (
                    ?, ?, ?, ?::player_position_enum, ?, ?,
                    ?::championship_enum, ?, ?, ?::duration_unit_enum,
                    ?
                )
            """;
        
            try (PreparedStatement stmt = connection.prepareStatement(insertPlayerSql)) {
                int playerCount = 0;
                int statCount = 0;
                int skippedPlayers = 0;
                int skippedStats = 0;
                
                for (Map<String, Object> playerMap : players) {
                    String playerId = (String) playerMap.get("id");
                    String name = (String) playerMap.get("name");
                    Number numberObj = (Number) playerMap.get("number");
                    String position = (String) playerMap.get("playerPosition");
                    String nationality = (String) playerMap.get("nationality");
                    Number ageObj = (Number) playerMap.get("age");
            
                    // Vérification détaillée des champs manquants
                    StringBuilder missingFields = new StringBuilder();
                    if (playerId == null) missingFields.append("id, ");
                    if (name == null) missingFields.append("name, ");
                    if (numberObj == null) missingFields.append("number, ");
                    if (position == null) missingFields.append("position, ");
                    if (nationality == null) missingFields.append("nationality, ");
                    if (ageObj == null) missingFields.append("age, ");
                    
                    if (missingFields.length() > 0) {
                        missingFields.setLength(missingFields.length() - 2); // Enlever la dernière virgule et espace
                        logger.warn("Skipping player with missing fields: {}. Player data: {}", 
                                missingFields.toString(), playerMap);
                        skippedPlayers++;
                        continue;
                    }

                    int number = numberObj.intValue();
                    int age = ageObj.intValue();
                    
                    logger.debug("Processing player: id={}, name={}, number={}, position={}", 
                            playerId, name, number, position);
            
                    for (Integer seasonYear : seasonYears) {
                        String urlStats = String.format("%s/players/%s/statistics/%d", baseUrl, playerId, seasonYear);
                        logger.debug("Fetching statistics for player {} in season {} from URL: {}", 
                                playerId, seasonYear, urlStats);
                        
                        try {
                            ResponseEntity<Map> statsResponse = restTemplate.getForEntity(urlStats, Map.class);
                            Map<String, Object> stats = statsResponse.getBody();
            
                            if (stats == null) {
                                logger.warn("No statistics returned for player {} in season {}", playerId, seasonYear);
                                skippedStats++;
                                continue;
                            }

                            Number scoredGoalsObj = (Number) stats.get("scoreGoals");
                            Map<String, Object> playingTime = (Map<String, Object>) stats.get("playingTime");

                            // Vérification détaillée des statistiques manquantes
                            StringBuilder missingStats = new StringBuilder();
                            if (scoredGoalsObj == null) missingStats.append("scoreGoals, ");
                            if (playingTime == null) {
                                missingStats.append("playingTime, ");
                            } else {
                                if (playingTime.get("value") == null) missingStats.append("playingTime.value, ");
                                if (playingTime.get("durationUnit") == null) missingStats.append("playingTime.durationUnit, ");
                            }
                            
                            if (missingStats.length() > 0) {
                                missingStats.setLength(missingStats.length() - 2); // Enlever la dernière virgule et espace
                                logger.warn("Incomplete statistics for player {} (name: {}) in season {}: missing {}. Stats data: {}", 
                                        playerId, name, seasonYear, missingStats.toString(), stats);
                                skippedStats++;
                                continue;
                            }

                            int scoredGoals = scoredGoalsObj.intValue();
                            double playingTimeValue = ((Number) playingTime.get("value")).doubleValue();
                            String durationUnit = (String) playingTime.get("durationUnit");

                            String playerSeasonId = playerId + "_" + seasonYear;
                            stmt.setString(1, playerSeasonId);
                            stmt.setString(2, name);
                            stmt.setInt(3, number);
                            stmt.setString(4, position);
                            stmt.setString(5, nationality);
                            stmt.setInt(6, age);
                            stmt.setString(7, championship.name());
                            stmt.setInt(8, scoredGoals);
                            stmt.setDouble(9, playingTimeValue);
                            stmt.setString(10, durationUnit);
                            stmt.setInt(11, seasonYear);

                            stmt.executeUpdate();
                            statCount++;
                            
                            logger.debug("Inserted player statistics: id={}, season={}, goals={}, playingTime={}{}", 
                                    playerSeasonId, seasonYear, scoredGoals, playingTimeValue, durationUnit);
                        } catch (Exception e) {
                            logger.error("Error getting statistics for player {} (name: {}) in season {}: {}", 
                                    playerId, name, seasonYear, e.getMessage(), e);
                            skippedStats++;
                        }
                    }
                    playerCount++;
                }
                logger.info("Synchronized {} players with {} season statistics for {}. Skipped {} players and {} statistics.", 
                        playerCount, statCount, championship, skippedPlayers, skippedStats);
            } catch (SQLException e) {
                logger.error("Database error while syncing players for {}: {}", championship, e.getMessage(), e);
                throw new RuntimeException("Error syncing players for " + championship, e);
            }
        } catch (Exception e) {
            logger.error("Failed to fetch players from {}: {}", urlPlayers, e.getMessage(), e);
            throw new RuntimeException("Error fetching players for " + championship, e);
        }
    }

    private void updateChampionshipMedian(Connection connection, ChampionShip championship) throws SQLException {
        String calculateMedian = """
            WITH diff_goals AS (
                SELECT (scored_goals - conceded_goals) as diff
                FROM clubs
                WHERE championship = ?::championship_enum
            ),
            sorted_rows AS (
                SELECT diff, ROW_NUMBER() OVER (ORDER BY diff) as row_num,
                COUNT(*) OVER () as total_count
                FROM diff_goals
            )
            SELECT AVG(diff::numeric) as median
            FROM sorted_rows
            WHERE row_num IN ((total_count + 1)/2, (total_count + 2)/2);
        """;

        try (PreparedStatement stmt = connection.prepareStatement(calculateMedian)) {
            stmt.setString(1, championship.name());
            var rs = stmt.executeQuery();

            if (rs.next()) {
                double median = rs.getDouble("median");

                String upsertMedian = """
                    INSERT INTO championships (name, difference_goals_median)
                    VALUES (?::championship_enum, ?)
                    ON CONFLICT (name) DO UPDATE
                    SET difference_goals_median = EXCLUDED.difference_goals_median;
                """;

                try (PreparedStatement upsertStmt = connection.prepareStatement(upsertMedian)) {
                    upsertStmt.setString(1, championship.name());
                    upsertStmt.setDouble(2, median);
                    upsertStmt.executeUpdate();
                }
            }
        }
    }
}
