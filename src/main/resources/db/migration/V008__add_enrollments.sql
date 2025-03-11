-- V007__create_enrollments.sql
CREATE TABLE enrollments (
    user_id BINARY(16) NOT NULL,
    course_id BINARY(16) NOT NULL,
    grade VARCHAR(50) NOT NULL,
    semester VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, course_id),
    CONSTRAINT fk_enrollment_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES courses(id)
);
