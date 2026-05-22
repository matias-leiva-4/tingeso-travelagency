-- Script de inicialización de PostgreSQL.
-- Se ejecuta UNA SOLA VEZ cuando el volumen db_data está vacío
-- (Docker lo corre desde /docker-entrypoint-initdb.d/).
--
-- Creamos dos bases de datos:
--   - travelagency_db : usada por el backend Spring Boot
--   - keycloak_db     : usada por Keycloak para guardar realms, users, sessions, etc.
--
-- La BD principal "travelagency_db" ya la crea Postgres con la variable
-- POSTGRES_DB del compose. Aquí solo necesitamos crear la de Keycloak.

CREATE DATABASE keycloak_db;
