package org.prog3.central_api.repository.implementation;

import lombok.AllArgsConstructor;
import org.prog3.central_api.configuration.DataSource;
import org.prog3.central_api.model.*;
import org.prog3.central_api.repository.RankingRepository;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@AllArgsConstructor
public class RankingRepositoryImplementation implements RankingRepository {
    private final DataSource dataSource;

    @Override
    public List<PlayerRanking> getBestPlayers(Integer top, DurationUnit playingTimeUnit) {
        // Définir les valeurs par défaut
        DurationUnit finalTimeUnit = playingTimeUnit != null ? playingTimeUnit : DurationUnit.MINUTE;
        int finalTop = top != null ? top : 5;

        String query = """
        WITH ranked_players AS (
            SELECT 
                id,
                name,
                number,
                position::player_position_enum as position,
                nationality,
                age,
                championship::championship_enum as championship,
                scored_goals,
                CASE ?::duration_unit_enum
                    WHEN 'SECOND' THEN total_playing_time_seconds
                    WHEN 'MINUTE' THEN total_playing_time_seconds / 60.0
                    WHEN 'HOUR' THEN total_playing_time_seconds / 3600.0
                    ELSE total_playing_time_seconds / 60.0 -- Fallback to minutes
                END as playing_time_value,
                COALESCE(?::duration_unit_enum, 'MINUTE') as playing_time_unit,
                ROW_NUMBER() OVER (
                    ORDER BY scored_goals DESC, total_playing_time_seconds DESC
                ) as ranking
            FROM players
        )
        SELECT *
        FROM ranked_players
        WHERE ranking <= ?
        ORDER BY ranking;
    """;

        List<PlayerRanking> rankings = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            // Utiliser la valeur finale garantie non-null
            stmt.setString(1, finalTimeUnit.name());
            stmt.setString(2, finalTimeUnit.name());
            stmt.setInt(3, finalTop);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PlayerRanking ranking = mapResultSetToPlayerRanking(rs);
                    rankings.add(ranking);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while fetching best players", e);
        }

        return rankings;
    }

    private PlayerRanking mapResultSetToPlayerRanking(ResultSet rs) throws SQLException {
        PlayerRanking ranking = new PlayerRanking();
        ranking.setRanking(rs.getInt("ranking"));
        ranking.setId(rs.getString("id"));
        ranking.setName(rs.getString("name"));
        ranking.setNumber(rs.getInt("number"));
        ranking.setPosition(PlayerPosition.valueOf(rs.getString("position")));
        ranking.setNationality(rs.getString("nationality"));
        ranking.setAge(rs.getInt("age"));
        ranking.setChampionShip(ChampionShip.valueOf(rs.getString("championship")));
        ranking.setScoreGoals(rs.getInt("scored_goals"));

        PlayingTime playingTime = new PlayingTime();
        playingTime.setValue((int) rs.getDouble("playing_time_value"));
        playingTime.setDurationUnit(DurationUnit.valueOf(rs.getString("playing_time_unit")));
        ranking.setPlayingTime(playingTime);

        return ranking;
    }
    @Override
    public List<ClubRanking> getBestClubs(Integer top) {
        String query = """
            WITH ranked_clubs AS (
                SELECT 
                    id,
                    name,
                    acronym,
                    year_creation,
                    stadium,
                    coach_name,
                    coach_nationality,
                    championship::championship_enum as championship,
                    scored_goals,
                    conceded_goals,
                    clean_sheet_number,
                    ranking_points,
                    (scored_goals - conceded_goals) as difference_goals,
                    ROW_NUMBER() OVER (
                        ORDER BY ranking_points DESC,
                        scored_goals DESC,
                        clean_sheet_number DESC
                    ) as rank
                FROM clubs
            )
            SELECT *
            FROM ranked_clubs
            WHERE rank <= ?
            ORDER BY rank;
        """;
    
        List<ClubRanking> rankings = new ArrayList<>();
    
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setInt(1, top);
    
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ClubRanking ranking = new ClubRanking();
                    ranking.setRank(rs.getInt("rank"));
                    
                    // Construction de l'objet Club
                    Club club = new Club();
                    club.setId(rs.getString("id"));
                    club.setName(rs.getString("name"));
                    club.setAcronym(rs.getString("acronym"));
                    club.setYearCreation(rs.getInt("year_creation"));
                    club.setStadium(rs.getString("stadium"));
                    
                    // Construction de l'objet Coach
                    Coach coach = new Coach();
                    coach.setName(rs.getString("coach_name"));
                    coach.setNationality(rs.getString("coach_nationality"));
                    club.setCoach(coach);
                    
                    club.setChampionShip(ChampionShip.valueOf(rs.getString("championship")));
                    
                    ranking.setClub(club);
                    ranking.setRankingPoints(rs.getInt("ranking_points"));
                    ranking.setScoreGoals(rs.getInt("scored_goals"));
                    ranking.setConcededGoals(rs.getInt("conceded_goals"));
                    ranking.setDifferenceGoals(rs.getInt("difference_goals"));
                    ranking.setCleanSheetNumber(rs.getInt("clean_sheet_number"));
                    
                    rankings.add(ranking);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while fetching best clubs", e);
        }
    
        return rankings;
    }

    /**
     * @return
     */
    @Override
    public List<ChampionShipRanking> getBestChampionship() {
        return List.of();
    }
}
