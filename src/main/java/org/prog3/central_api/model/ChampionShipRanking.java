package org.prog3.central_api.model;

import lombok.Data;

@Data
public class ChampionShipRanking {
    private Integer ranking;
    private ChampionShip championShip;
    private Integer differenceGoalMedian;
}
