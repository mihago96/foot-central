package org.prog3.central_api.repository.implementation;

import org.prog3.central_api.configuration.DataSource;
import org.prog3.central_api.model.ChampionShip;
import org.prog3.central_api.model.httpModels.ChampUrl;
import org.prog3.central_api.repository.SynchronisationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

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
    private final DataSource dataSource;
    private final RestTemplate restTemplate;

    private static final List<ChampUrl> CHAMP_URLS = Arrays.asList(
            new ChampUrl(ChampionShip.LA_LIGA, 8081)
    );

    @Override
    public Boolean Syncronisation() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                clearTables(connection);

                for (ChampUrl champUrl : CHAMP_URLS) {
                    String baseUrl = "http://localhost:" + champUrl.getPort();
                    syncClubs(connection, baseUrl, champUrl.getChampionShip());
                    syncPlayers(connection, baseUrl, champUrl.getChampionShip());
                    updateChampionshipMedian(connection, champUrl.getChampionShip());
                }

                connection.commit();
                return true;
            } catch (Exception e) {
                connection.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void clearTables(Connection connection) throws SQLException {
        String[] clearQueries = {
                "DELETE FROM players",
                "DELETE FROM clubs",
                "DELETE FROM championships"
        };

        for (String query : clearQueries) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.executeUpdate();
            }
        }
    }

    private void syncClubs(Connection connection, String baseUrl, ChampionShip championship) {
        String url = baseUrl + "/clubs";
        ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);
        Map[] clubs = response.getBody();

        if (clubs == null) return;

        String insertClub = """
            INSERT INTO clubs (
                id, name, acronym, year_creation, stadium,
                coach_name, coach_nationality, championship,
                scored_goals, conceded_goals, clean_sheet_number, ranking_points
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::championship_enum, 0, 0, 0, 0)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(insertClub)) {
            for (Map<String, Object> club : response.getBody()) {
                stmt.setString(1, (String) club.get("id"));
                stmt.setString(2, (String) club.get("name"));
                stmt.setString(3, (String) club.get("acronym"));
                stmt.setInt(4, (Integer) club.get("yearCreation"));
                stmt.setString(5, (String) club.get("stadium"));

                Map<String, String> coach = (Map<String, String>) club.get("coach");
                stmt.setString(6, coach.get("name"));
                stmt.setString(7, coach.get("nationality"));
                stmt.setString(8, championship.name());

                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void syncPlayers(Connection connection, String baseUrl, ChampionShip championship) {
        String urlPlayers = baseUrl + "/players";
        ResponseEntity<Map[]> playersResponse = restTemplate.getForEntity(urlPlayers, Map[].class);
        Map[] players = playersResponse.getBody();

        if (players == null) return;

        String insertPlayerSql = """
            INSERT INTO players ( 
                id, name, number, position, nationality, age,
                championship, scored_goals, playing_time_value, playing_time_unit
            ) VALUES (
                ?, ?, ?, ?::player_position_enum, ?, ?,
                ?::championship_enum, ?, ?, ?::duration_unit_enum
            )
        """;

        try (PreparedStatement stmt = connection.prepareStatement(insertPlayerSql)) {
            for (Map<String, Object> playerMap : players) {
                String playerId = (String) playerMap.get("id");
                String name = (String) playerMap.get("name");
                Number numberObj = (Number) playerMap.get("number");
                String position = (String) playerMap.get("playerPosition");
                String nationality = (String) playerMap.get("nationality");
                Number ageObj = (Number) playerMap.get("age");

                if (playerId == null || name == null || numberObj == null
                        || position == null || nationality == null || ageObj == null) {
                    continue;
                }

                int number = numberObj.intValue();
                int age = ageObj.intValue();

                String urlStats = String.format("%s/players/%s/statistics/2023", baseUrl, playerId);
                ResponseEntity<Map> statsResponse = restTemplate.getForEntity(urlStats, Map.class);
                Map<String, Object> stats = statsResponse.getBody();

                if (stats == null) continue;

                Number scoredGoalsObj = (Number) stats.get("scoreGoals");
                Map<String, Object> playingTime = (Map<String, Object>) stats.get("playingTime");

                if (scoredGoalsObj == null
                        || playingTime == null
                        || playingTime.get("value") == null
                        || playingTime.get("durationUnit") == null) {
                    continue;
                }

                int scoredGoals = scoredGoalsObj.intValue();
                double playingTimeValue = ((Number) playingTime.get("value")).doubleValue();
                String durationUnit = (String) playingTime.get("durationUnit");

                stmt.setString(1, playerId);
                stmt.setString(2, name);
                stmt.setInt(3, number);
                stmt.setString(4, position);
                stmt.setString(5, nationality);
                stmt.setInt(6, age);
                stmt.setString(7, championship.name());
                stmt.setInt(8, scoredGoals);
                stmt.setDouble(9, playingTimeValue);
                stmt.setString(10, durationUnit);

                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error syncing players for " + championship, e);
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
