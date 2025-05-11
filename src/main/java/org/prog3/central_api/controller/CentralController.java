package org.prog3.central_api.controller;

import lombok.AllArgsConstructor;
import org.prog3.central_api.model.ChampionShipRanking;
import org.prog3.central_api.model.ClubRanking;
import org.prog3.central_api.model.DurationUnit;
import org.prog3.central_api.model.PlayerRanking;
import org.prog3.central_api.repository.implementation.RankingRepositoryImplementation;
import org.prog3.central_api.service.RankingService;
import org.prog3.central_api.service.SynchronisationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class CentralController {

    private final SynchronisationService synchronisationService;
    private final RankingService service;

    /**
     * Déclenche la synchronisation des données depuis les APIs externes
     * @return Réponse vide avec statut 204 (No Content) en cas de succès
     */
    @PostMapping("/synchronisation")
    public ResponseEntity<Void> synchronize() {
        synchronisationService.synchronisation();
        return ResponseEntity.ok().build();
    }

    /**
     * Récupère les meilleurs joueurs de tous les championnats
     * @param top Nombre de joueurs à retourner (défaut: 5)
     * @param playingTimeUnit Unité de temps de jeu pour le filtrage (optionnel)
     * @return Liste des meilleurs joueurs classés
     */
    @GetMapping("/bestPlayers")
    public ResponseEntity<List<PlayerRanking>> getBestPlayers(
            @RequestParam(defaultValue = "5") Integer top,
            @RequestParam(required = false) DurationUnit playingTimeUnit) {
        return ResponseEntity.ok(service.getBestPlayers(top, playingTimeUnit));
    }

    /**
     * Récupère les meilleurs clubs de tous les championnats
     * @param top Nombre de clubs à retourner (défaut: 5)
     * @return Liste des meilleurs clubs classés
     */
    @GetMapping("/bestClubs")
    public ResponseEntity<List<ClubRanking>> getBestClubs(
            @RequestParam(defaultValue = "5") Integer top) {
        return ResponseEntity.ok(service.getBestClubs(top));
    }

    /**
     * Calcule le classement des championnats basé sur la médiane des différences de buts
     * @return Liste des championnats classés du meilleur au moins bon
     */
    @GetMapping("/championShipRanking")
    public ResponseEntity<List<ChampionShipRanking>> getChampionShipRanking() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}