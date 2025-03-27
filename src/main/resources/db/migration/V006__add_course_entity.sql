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

CREATE TABLE course_prerequisites (
    course_code VARCHAR(255) NOT NULL,
    prerequisite_code VARCHAR(255) NOT NULL,
    PRIMARY KEY (course_code, prerequisite_code),
    CONSTRAINT fk_course_prereq FOREIGN KEY (course_code) REFERENCES courses(code),
    CONSTRAINT fk_prereq_code FOREIGN KEY (prerequisite_code) REFERENCES courses(code)
);

CREATE TABLE course_themes (
    course_id BINARY(16) NOT NULL,
    theme INT NOT NULL,
    PRIMARY KEY (course_id, theme),
    CONSTRAINT fk_course_themes FOREIGN KEY (course_id) REFERENCES courses(id)
       ON DELETE CASCADE
);
