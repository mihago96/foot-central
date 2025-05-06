package org.prog3.central_api.model.generalModels;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClubPlayer extends Player implements Serializable {
    private Club club;
}
