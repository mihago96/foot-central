-- Insertion des championnats
INSERT INTO championships (name, difference_goals_median) VALUES
                                                              ('PREMIER_LEAGUE', 1.5),
                                                              ('LA_LIGA', 1.3),
                                                              ('BUNDESLIGA', 1.4),
                                                              ('SERIA', 1.1),
                                                              ('LIGUE_1', 0.9);

-- Insertion des joueurs
INSERT INTO players (id, name, number, position, nationality, age, championship, scored_goals, playing_time_value, playing_time_unit) VALUES
                                                                                                                                          ('PLAYER1', 'Erling Haaland', 9, 'STRIKER', 'Norwegian', 23, 'PREMIER_LEAGUE', 28, 30, 'HOUR'),
                                                                                                                                          ('PLAYER2', 'Vinicius Jr', 7, 'STRIKER', 'Brazilian', 23, 'LA_LIGA', 15, 28, 'HOUR'),
                                                                                                                                          ('PLAYER3', 'Joshua Kimmich', 6, 'MIDFIELDER', 'German', 28, 'BUNDESLIGA', 5, 29, 'HOUR'),
                                                                                                                                          ('PLAYER4', 'Victor Osimhen', 9, 'STRIKER', 'Nigerian', 25, 'SERIA', 21, 27, 'HOUR'),
                                                                                                                                          ('PLAYER5', 'Gianluigi Donnarumma', 99, 'GOAL_KEEPER', 'Italian', 24, 'LIGUE_1', 0, 31, 'HOUR'),
                                                                                                                                          ('PLAYER6', 'Kevin De Bruyne', 17, 'MIDFIELDER', 'Belgian', 32, 'PREMIER_LEAGUE', 7, 25, 'HOUR'),
                                                                                                                                          ('PLAYER7', 'Antoine Griezmann', 8, 'STRIKER', 'French', 32, 'LA_LIGA', 12, 26, 'HOUR');

-- Insertion des clubs
INSERT INTO clubs (id, name, acronym, year_creation, stadium, coach_name, coach_nationality, championship, scored_goals, conceded_goals, clean_sheet_number, ranking_points) VALUES
                                                                                                                                                                                 ('CLUB1', 'Manchester City', 'MCI', 1880, 'Etihad Stadium', 'Pep Guardiola', 'Spanish', 'PREMIER_LEAGUE', 68, 18, 15, 82),
                                                                                                                                                                                 ('CLUB2', 'FC Barcelona', 'FCB', 1899, 'Camp Nou', 'Xavi Hernandez', 'Spanish', 'LA_LIGA', 59, 20, 12, 78),
                                                                                                                                                                                 ('CLUB3', 'Borussia Dortmund', 'BVB', 1909, 'Signal Iduna Park', 'Edin Terzić', 'German', 'BUNDESLIGA', 52, 25, 10, 70),
                                                                                                                                                                                 ('CLUB4', 'AC Milan', 'ACM', 1899, 'San Siro', 'Stefano Pioli', 'Italian', 'SERIA', 47, 28, 9, 68),
                                                                                                                                                                                 ('CLUB5', 'Olympique de Marseille', 'OM', 1899, 'Orange Vélodrome', 'Marcelino', 'Spanish', 'LIGUE_1', 43, 22, 11, 65);