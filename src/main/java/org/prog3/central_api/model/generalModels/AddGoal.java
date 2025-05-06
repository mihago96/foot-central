package org.prog3.central_api.model.generalModels;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class AddGoal implements Serializable {
    private String clubId;
    @JsonProperty("scorerIdentifier")
    private String scorerIdentifier;
    private Integer minuteOfGoal; //TODO: Create the methode which check if the value is between 1 and 90

}
