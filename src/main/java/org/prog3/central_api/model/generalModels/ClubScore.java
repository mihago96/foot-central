package org.prog3.central_api.model.generalModels;


import lombok.Data;

import java.util.List;

@Data
public class ClubScore {
    private Integer score;
    private List<Scorer> scorers;
}
