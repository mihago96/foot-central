package org.prog3.central_api.model;


import lombok.Data;

import java.io.Serializable;
@Data
public class Coach implements Serializable {
    private String name;
    private String nationality;
}
