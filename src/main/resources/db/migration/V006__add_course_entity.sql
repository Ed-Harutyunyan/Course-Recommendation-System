CREATE TABLE courses (
     id BINARY(16) NOT NULL,
     title VARCHAR(255) NOT NULL,
     description TEXT NOT NULL,
     section VARCHAR(255) NOT NULL,
     duration VARCHAR(255) NOT NULL,
     credits INT NOT NULL,
     campus VARCHAR(255) NOT NULL,
     instructor_id BINARY(16),
     times VARCHAR(255) NOT NULL,
     location VARCHAR(255) NOT NULL,
     PRIMARY KEY (id),
     CONSTRAINT fk_instructor FOREIGN KEY (instructor_id) REFERENCES instructors(id)
);
