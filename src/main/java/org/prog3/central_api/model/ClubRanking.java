package org.prog3.central_api.model;

import lombok.Data;

import java.io.Serializable;
@Data
public class ClubRanking implements Serializable {
    private Integer rank;
    private Club club;
    private Integer rankingPoints;
    private Integer scoreGoals;
    private Integer concededGoals;//But encaissé
    private Integer differenceGoals;
    private Integer cleanSheetNumber;
}
