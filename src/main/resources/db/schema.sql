-- Création des types énumérés
CREATE TYPE championship_enum AS ENUM (
    'PREMIER_LEAGUE',
    'LA_LIGA',
    'BUNDESLIGA',
    'SERIA',
    'LIGUE_1'
    );

CREATE TYPE duration_unit_enum AS ENUM (
    'SECOND',
    'MINUTE',
    'HOUR'
    );

CREATE TYPE player_position_enum AS ENUM (
    'STRIKER',
    'MIDFIELDER',
    'DEFENSE',
    'GOAL_KEEPER'
    );

-- Table des championnats avec médiane pré-calculée
CREATE TABLE championships (
                               name championship_enum PRIMARY KEY,
                               difference_goals_median NUMERIC
);

-- Table des joueurs
CREATE TABLE players (
                         id VARCHAR PRIMARY KEY,
                         name VARCHAR NOT NULL,
                         number INTEGER,
                         position player_position_enum,
                         nationality VARCHAR,
                         age INTEGER,
                         championship championship_enum,
                         scored_goals INTEGER,
                         playing_time_value NUMERIC,
                         playing_time_unit duration_unit_enum,
                         total_playing_time_seconds NUMERIC GENERATED ALWAYS AS (
                             CASE playing_time_unit
                                 WHEN 'HOUR' THEN playing_time_value * 3600
                                 WHEN 'MINUTE' THEN playing_time_value * 60
                                 ELSE playing_time_value
                                 END
                             ) STORED
);

-- Index pour optimiser les requêtes de classement
CREATE INDEX idx_players_ranking ON players (scored_goals DESC, total_playing_time_seconds DESC);

-- Table des clubs
CREATE TABLE clubs (
                       id VARCHAR PRIMARY KEY,
                       name VARCHAR NOT NULL,
                       acronym VARCHAR(3),
                       year_creation INTEGER,
                       stadium VARCHAR,
                       coach_name VARCHAR,
                       coach_nationality VARCHAR,
                       championship championship_enum,
                       scored_goals INTEGER,
                       conceded_goals INTEGER,
                       clean_sheet_number INTEGER,
                       ranking_points INTEGER
);

-- Index pour le classement des clubs
CREATE INDEX idx_clubs_ranking ON clubs (ranking_points DESC);

-- Ajout d'une table pour les saisons
CREATE TABLE seasons (
    year INTEGER PRIMARY KEY,
    championship championship_enum,
    status VARCHAR
);

-- Modification de la table players pour inclure l'année de saison
ALTER TABLE players ADD COLUMN season_year INTEGER REFERENCES seasons(year);

-- Modification de la table clubs pour inclure l'année de saison
ALTER TABLE clubs ADD COLUMN season_year INTEGER REFERENCES seasons(year);