package org.prog3.central_api.repository.implementation;

import org.prog3.central_api.model.ChampionShipRanking;
import org.prog3.central_api.model.ClubRanking;
import org.prog3.central_api.model.DurationUnit;
import org.prog3.central_api.model.PlayerRanking;
import org.prog3.central_api.repository.RankingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RankingServiceImplementation implements RankingRepository {

    /**
     * @param top
     * @param playingTimeUnit
     * @return
     */
    @Override
    public List<PlayerRanking> getBestPlayers(Integer top, DurationUnit playingTimeUnit) {
        return List.of();
    }

    /**
     * @param top
     * @return
     */
    @Override
    public List<ClubRanking> getBestClubs(Integer top) {
        return List.of();
    }

    /**
     * @return
     */
    @Override
    public List<ChampionShipRanking> getBestChampionship() {
        return List.of();
    }
}
