package org.prog3.central_api.model.generalModels;

import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateSeasonStatus implements Serializable {
    private SeasonStatus  status;
}
