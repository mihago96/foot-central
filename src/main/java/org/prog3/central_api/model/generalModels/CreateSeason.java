package org.prog3.central_api.model.generalModels;


import lombok.Data;

import java.io.Serializable;

/**
 * For example, if season.year = 2024, alias would be "S2024-2025".
 * If season.year = 2025, alias wold be "S2025-2026".
 */
@Data
public class CreateSeason implements Serializable {
        private Integer year;
        private String alias;

}
