package org.prog3.central_api.model.httpModels;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.prog3.central_api.model.ChampionShip;

@Data
@AllArgsConstructor
public class ChampUrl {
    private ChampionShip championShip;
    private Integer port;
}
