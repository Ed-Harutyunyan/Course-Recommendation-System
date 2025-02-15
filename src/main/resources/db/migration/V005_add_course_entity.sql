-- V005_add_course_entity.sql

CREATE TABLE courses (
     id BINARY(36) NOT NULL,
     title VARCHAR(255) NOT NULL,
     description TEXT NOT NULL,
     section VARCHAR(255) NOT NULL,
     duration VARCHAR(255) NOT NULL,
     credits INT NOT NULL,
     campus VARCHAR(255) NOT NULL,
     instructor_id CHAR(36),
     times VARCHAR(255) NOT NULL,
     location VARCHAR(255) NOT NULL,
     PRIMARY KEY (id),
     CONSTRAINT fk_instructor FOREIGN KEY (instructor_id) REFERENCES users(id)
);
