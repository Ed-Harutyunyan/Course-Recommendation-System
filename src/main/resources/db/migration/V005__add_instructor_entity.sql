-- V005__add_instructor_entity.sql
-- Create instructors table

CREATE TABLE instructors (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
