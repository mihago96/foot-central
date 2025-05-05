package org.prog3.central_api.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class PlayerRanking implements Serializable {
    private Integer ranking;
    private String id;
    private String name;
    private Integer number;//numéro de maillot
    private PlayerPosition position;
    private String nationality;
    private Integer age;
    private ChampionShip championShip;
    private Integer scoreGoals;
    private PlayingTime playingTime;
}
