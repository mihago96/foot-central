package org.prog3.central_api.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class Club implements Serializable {
    private String id;
    private String name;
    private String acronym;
    private Integer yearCreation;
    private String stadium;
    private Coach coach;
}
