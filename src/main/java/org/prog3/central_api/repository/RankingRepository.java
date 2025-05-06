package org.prog3.central_api.repository;

import org.prog3.central_api.model.ChampionShipRanking;
import org.prog3.central_api.model.ClubRanking;
import org.prog3.central_api.model.DurationUnit;
import org.prog3.central_api.model.PlayerRanking;

import java.util.List;

public interface RankingRepository {
    public List<PlayerRanking> getBestPlayers(Integer top, DurationUnit playingTimeUnit);
    public List<ClubRanking> getBestClubs(Integer top);
    public List<ChampionShipRanking> getBestChampionship();

}
