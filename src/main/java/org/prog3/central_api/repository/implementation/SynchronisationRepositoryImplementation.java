package org.prog3.central_api.repository.implementation;
import org.prog3.central_api.configuration.AppConfig;
import org.prog3.central_api.configuration.DataSource;
import org.prog3.central_api.model.ChampionShip;
import org.prog3.central_api.model.httpModels.ChampUrl;
import org.prog3.central_api.repository.SynchronisationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Repository
@AllArgsConstructor
public class SynchronisationRepositoryImplementation implements SynchronisationRepository {

    private final AppConfig appConfig;
    private final DataSource dataSource;
    private final RestTemplate restTemplate;


    @Override
    public Boolean Syncronisation() {
        try {
            // Configuration des URLs des APIs par championnat
            List<ChampUrl> champUrls = Arrays.asList(
                new ChampUrl(ChampionShip.LA_LIGA, 8081)
//                new ChampUrl(ChampionShip.BUNDESLIGA, 8082),
//                new ChampUrl(ChampionShip.LIGUE_1, 8083),
//                new ChampUrl(ChampionShip.PREMIER_LEAGUE, 8084),
//                new ChampUrl(ChampionShip.SERIA, 8085)
            );

            // Pour chaque championnat
            for (ChampUrl champUrl : champUrls) {
                String baseUrl = "http://localhost:" + champUrl.getPort();
                
                // Récupération des joueurs
                //Le mieux aurais été de créer les classes mais il est 11h et la deadline est demain matin donc if faut rendre un livrable fonctionnel
                Object[] players = restTemplate.getForObject(baseUrl + "/players", Object[].class);
                Object[] statistics = restTemplate.getForObject(baseUrl + "//players/player-1/statistics/2023", Object[].class);
                // Récupération des clubs
                Object[] clubs = restTemplate.getForObject(baseUrl + "/clubs", Object[].class);

                // Mise à jour de la base de données
                updatePlayersData(players, champUrl.getChampionShip());
                updateClubsData(clubs, champUrl.getChampionShip());
            }

            // Calcul et mise à jour des médianes des différences de buts
            updateChampionshipMedians();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updatePlayersData(Object[] players, ChampionShip championship) throws SQLException {
        String sql = """
            INSERT INTO players (id, name, number, position, nationality, age, championship, 
                               scored_goals, playing_time_value, playing_time_unit)
            VALUES (?, ?, ?, ?::player_position_enum, ?, ?, ?::championship_enum, ?, ?, ?::duration_unit_enum)
            ON CONFLICT (id) DO UPDATE SET
                scored_goals = EXCLUDED.scored_goals,
                playing_time_value = EXCLUDED.playing_time_value,
                playing_time_unit = EXCLUDED.playing_time_unit
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            for (Object playerObj : players) {
                // Conversion et mapping des données du joueur
                // À adapter selon la structure exacte de votre réponse API
                // Ceci est un exemple
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> player = (java.util.Map<String, Object>) playerObj;
                
                pstmt.setString(1, (String) player.get("id"));
                pstmt.setString(2, (String) player.get("name"));
                pstmt.setInt(3, (Integer) player.get("number"));
                pstmt.setString(4, (String) player.get("playerPosition"));
                pstmt.setString(5, (String) player.get("nationality"));
                pstmt.setInt(6, (Integer) player.get("age"));
                pstmt.setString(7, championship.name());
                pstmt.setInt(8, (Integer) player.get("scoredGoals"));
                pstmt.setDouble(9, (Double) player.get("playingTimeValue"));
                pstmt.setString(10, (String) player.get("playingTimeUnit"));
                
                pstmt.executeUpdate();
            }
        }
    }

    private void updateClubsData(Object[] clubs, ChampionShip championship) throws SQLException {
        String sql = """
            INSERT INTO clubs (id, name, acronym, year_creation, stadium, coach_name, 
                             coach_nationality, championship, scored_goals, conceded_goals, 
                             clean_sheet_number, ranking_points)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::championship_enum, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                scored_goals = EXCLUDED.scored_goals,
                conceded_goals = EXCLUDED.conceded_goals,
                clean_sheet_number = EXCLUDED.clean_sheet_number,
                ranking_points = EXCLUDED.ranking_points
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            for (Object clubObj : clubs) {
                // Conversion et mapping des données du club
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> club = (java.util.Map<String, Object>) clubObj;
                
                pstmt.setString(1, (String) club.get("id"));
                pstmt.setString(2, (String) club.get("name"));
                pstmt.setString(3, (String) club.get("acronym"));
                pstmt.setInt(4, (Integer) club.get("yearCreation"));
                pstmt.setString(5, (String) club.get("stadium"));
                
                @SuppressWarnings("unchecked")
                java.util.Map<String, String> coach = (java.util.Map<String, String>) club.get("coach");
                pstmt.setString(6, coach.get("name"));
                pstmt.setString(7, coach.get("nationality"));
                
                pstmt.setString(8, championship.name());
                pstmt.setInt(9, (Integer) club.get("scoredGoals"));
                pstmt.setInt(10, (Integer) club.get("concededGoals"));
                pstmt.setInt(11, (Integer) club.get("cleanSheetNumber"));
                pstmt.setInt(12, (Integer) club.get("rankingPoints"));
                
                pstmt.executeUpdate();
            }
        }
    }

    private void updateChampionshipMedians() throws SQLException {
        String sql = """
            WITH club_differences AS (
                SELECT championship, (scored_goals - conceded_goals) as diff
                FROM clubs
            ),
            championship_medians AS (
                SELECT 
                    championship,
                    percentile_cont(0.5) WITHIN GROUP (ORDER BY diff) as median
                FROM club_differences
                GROUP BY championship
            )
            INSERT INTO championships (name, difference_goals_median)
            SELECT championship, median
            FROM championship_medians
            ON CONFLICT (name) DO UPDATE
            SET difference_goals_median = EXCLUDED.difference_goals_median
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        }
    }
}
