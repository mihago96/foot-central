package org.prog3.central_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan({"org.prog3.central_api","org.prog3.central_api.controller"})
public class CentralApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CentralApiApplication.class, args);
    }

}
