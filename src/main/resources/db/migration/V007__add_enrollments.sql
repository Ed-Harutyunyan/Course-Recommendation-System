CREATE TABLE enrollments (
    student_id BINARY(16) NOT NULL,
    course_id BINARY(16) NOT NULL,
    grade VARCHAR(50) NOT NULL,
    semester VARCHAR(50) NOT NULL,
    PRIMARY KEY (student_id, course_id),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

