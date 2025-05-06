package org.prog3.central_api.model.generalModels;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
@EqualsAndHashCode(callSuper = true)
@Data
public class ClubStatistics extends Club implements Serializable {
    private Integer rankingPoints;
    private Integer scoreGoals;
    private Integer concededGoals;
    private Integer differenceGoals;
    private Integer cleanSheetNumber;

}
