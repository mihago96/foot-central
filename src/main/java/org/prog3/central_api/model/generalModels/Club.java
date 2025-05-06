package org.prog3.central_api.model.generalModels;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.prog3.central_api.model.Coach;

import java.io.Serializable;
import java.time.Year;

@EqualsAndHashCode(callSuper = true)
@Data
public class Club extends ClubMinimumInfo implements Serializable {
  private Integer yearCreation;
  private String stadium;
  private Coach coach;
}
