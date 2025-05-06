package org.prog3.central_api.model.generalModels;


import lombok.Data;

@Data
public class MatchScore {
    private ClubScore home;
    private ClubScore away;
}
