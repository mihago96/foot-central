package org.prog3.central_api.service;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.prog3.central_api.model.ChampionShip;
import org.prog3.central_api.model.httpModels.ChampUrl;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class SynchronizationService {
    private final RestTemplate restTemplate;

    public void syncData() {

        List<ChampionShip> championships = List.of(
                ChampionShip.PREMIER_LEAGUE,
                ChampionShip.LA_LIGA,
                ChampionShip.BUNDESLIGA,
                ChampionShip.SERIA,
                ChampionShip.LIGUE_1
        );

        championships.forEach(champ -> {
            String baseUrl = championshipUrls.get(champ);

            // Récupérer les statistiques des clubs
            String clubsStatsUrl = baseUrl + "/clubs/statistics/2024-2025";
            ResponseEntity<ClubStatistics[]> clubsResponse = restTemplate.getForEntity(
                    clubsStatsUrl,
                    ClubStatistics[].class
            );
            List<ClubStatistics> clubsStats = Arrays.asList(clubsResponse.getBody());

            // Récupérer les joueurs et leurs statistiques
            String playersUrl = baseUrl + "/players";
            ResponseEntity<Player[]> playersResponse = restTemplate.getForEntity(
                    playersUrl,
                    Player[].class
            );
            List<Player> players = Arrays.asList(playersResponse.getBody());

            // Pour chaque joueur, récupérer les statistiques individuelles
            players.forEach(player -> {
                String playerStatsUrl = baseUrl + "/players/" + player.getId() + "/statistics/2024-2025";
                ResponseEntity<PlayerStatistics> statsResponse = restTemplate.getForEntity(
                        playerStatsUrl,
                        PlayerStatistics.class
                );
                PlayerStatistics stats = statsResponse.getBody();
                // Mapper vers PlayerRanking
            });

            // Stocker les données dans l'API centrale
            CentralDataStorage.update(clubsStats, playersStats, champ);
        });
    }
}