package org.prog3.central_api.service;

import lombok.AllArgsConstructor;
import org.prog3.central_api.model.ClubRanking;
import org.prog3.central_api.model.DurationUnit;
import org.prog3.central_api.model.PlayerRanking;
import org.prog3.central_api.repository.RankingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class RankingService {
    private final RankingRepository rankingRepository;

    public List<PlayerRanking> getBestPlayers(Integer top, DurationUnit playingTimeUnit) {
        if (top == null) {
            top = 5;
        }
        return rankingRepository.getBestPlayers(top, playingTimeUnit);
    }

    public List<ClubRanking> getBestClubs(Integer top) {
        if (top == null) {
            top = 5;
        }
        return rankingRepository.getBestClubs(top);
    }
}
