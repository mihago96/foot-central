package org.prog3.central_api.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
    @Bean
    //Cette annotatio veut dire a spring que c'est a lui de gérer cette objet, si je l'appelle c'est lui qui gère son instanciation
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}