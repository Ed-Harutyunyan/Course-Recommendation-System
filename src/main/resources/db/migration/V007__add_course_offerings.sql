-- V006__create_course_offerings.sql
CREATE TABLE course_offerings (
                                  id BINARY(16) NOT NULL,
                                  course_id BINARY(16) NOT NULL,
                                  section VARCHAR(255) NOT NULL,
                                  session VARCHAR(255) NOT NULL,
                                  campus VARCHAR(255) NOT NULL,
                                  instructor_id BINARY(16) NOT NULL,
                                  times VARCHAR(255) NOT NULL,
                                  taken_seats VARCHAR(255),
                                  spaces_waiting VARCHAR(255),
                                  delivery_method VARCHAR(255),
                                  dist_learning VARCHAR(255),
                                  location VARCHAR(255) NOT NULL,
                                  year VARCHAR(255) NOT NULL,
                                  semester VARCHAR(255) NOT NULL,
                                  PRIMARY KEY (id),
                                  CONSTRAINT fk_course_offering_course FOREIGN KEY (course_id) REFERENCES courses(id),
                                  CONSTRAINT fk_course_offering_instructor FOREIGN KEY (instructor_id) REFERENCES instructors(id)
);
