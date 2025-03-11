-- V005__create_courses.sql
CREATE TABLE courses (
                         id BINARY(16) NOT NULL,
                         code VARCHAR(255) NOT NULL,
                         title VARCHAR(255) NOT NULL,
                         description TEXT NOT NULL,
                         credits INT NOT NULL,
                         PRIMARY KEY (id),
                         UNIQUE KEY uk_course_code (code)
);

-- Create join table for prerequisites (ManyToMany relationship)
CREATE TABLE course_prerequisites (
                                      course_id BINARY(16) NOT NULL,
                                      prerequisite_id BINARY(16) NOT NULL,
                                      PRIMARY KEY (course_id, prerequisite_id),
                                      CONSTRAINT fk_course FOREIGN KEY (course_id) REFERENCES courses(id),
                                      CONSTRAINT fk_prerequisite FOREIGN KEY (prerequisite_id) REFERENCES courses(id)
);

-- Create collection table for course clusters
CREATE TABLE course_clusters (
                                 course_id BINARY(16) NOT NULL,
                                 cluster INT NOT NULL,
                                 PRIMARY KEY (course_id, cluster),
                                 CONSTRAINT fk_course_cluster FOREIGN KEY (course_id) REFERENCES courses(id)
);
