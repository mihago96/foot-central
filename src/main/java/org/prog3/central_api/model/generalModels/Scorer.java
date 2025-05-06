package org.prog3.central_api.model.generalModels;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper=true)
@Data
public class Scorer extends PlayerMinimumInfo implements Serializable {
    private Integer minuteOfGoal;//Dois être entre 1 et 90
    private Boolean ownGoal;
}
