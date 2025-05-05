package org.prog3.central_api.configuration;


import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class DataSource {
    private final static int DEFAULT_PORT = 5432; // Port par défaut pour PostgreSQL
    private final String host;
    private final String user;
    private final String password;
    private final String database;
    private final String jdbcUrl;

    // Constructeur
    public DataSource() {
        this.host = System.getenv("HOST");
        this.user = System.getenv("USER");
        this.password = System.getenv("PASSWORD");
        this.database = System.getenv("NAME");

        // Validation des paramètres
        if (host == null || user == null || password == null || database == null) {
            throw new IllegalStateException("Les variables d'environnement HOST, USER, PASSWORD et NAME doivent être définies.");
        }

        // Construction de l'URL JDBC
        this.jdbcUrl = "jdbc:postgresql://" + host + ":" + DEFAULT_PORT + "/" + database;
    }

    // Méthode pour obtenir une connexion
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(jdbcUrl, user, password);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la connexion à la base de données : " + e.getMessage(), e);
        }
    }
}

