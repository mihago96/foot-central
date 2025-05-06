package org.prog3.central_api.model.generalModels;


import lombok.Data;

import java.io.Serializable;

@Data
public class PlayerStatsitic implements Serializable {
    private Integer scoreGoals;
    private PlayingTime playingTime;
}
