-- 1. Create instructor table
CREATE TABLE instructors (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_instructor_user (user_id),
    CONSTRAINT fk_instructor_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 2. Create students table
CREATE TABLE students (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_user (user_id),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users(id)
);