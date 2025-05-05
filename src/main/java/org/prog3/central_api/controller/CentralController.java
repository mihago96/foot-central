package org.prog3.central_api.controller;

import org.prog3.central_api.model.ChampionShipRanking;
import org.prog3.central_api.model.ClubRanking;
import org.prog3.central_api.model.DurationUnit;
import org.prog3.central_api.model.PlayerRanking;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CentralController {

    /**
     * @Description Fetch all data from different api
     * @return a message of success if it's done
     */
    @PostMapping("/syncronisation")
    public ResponseEntity<String> syncronisation() {
        return ResponseEntity.ok("Syncronisation");
    }

    /**
     *
     * @param top Choose the top ranking number default 5
     * @param playingTimeUnit Choose the playingTime filter of the player
     * @return List of best players from all championships order by the best player
     *             first
     */
    @GetMapping("/bestPlayers")
    public ResponseEntity<List<PlayerRanking>> bestPlayer(
            @RequestParam( required = false, name = "top") Integer top,
            @RequestParam(required = false)DurationUnit playingTimeUnit
            ) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     *
     * @param top Choose the top ranking number default 5
     * @return
     */
    @GetMapping("/bestClubs")
    public ResponseEntity<List<ClubRanking>> bestClub(
            @RequestParam(required = false,name = "top") Integer top){

        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @Description The championship that has the least median of total difference goals
     *         between all clubs is the best championship,
     *
     *         and the championship that as the grater median of total difference goals
     *         is the worst.
     *
     *         The lower the median, the closer the matches and the better the level of
     *         the clubs.
     *
     *         The greater the median, the greater the gap between clubs.
     *
     * @return The ranking
     */
    @GetMapping("/championShipRanking")
    public ResponseEntity<List<ChampionShipRanking>> getChampionShipRanking(){
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
