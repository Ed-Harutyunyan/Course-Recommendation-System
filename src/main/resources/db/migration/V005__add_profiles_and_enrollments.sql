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

-- 3. Create enrollments table with extra attributes (e.g., passed and grade)
CREATE TABLE enrollments (
    student_id BINARY(16) NOT NULL,
    course_id BINARY(16) NOT NULL,
    grade VARCHAR(50) NOT NULL,
    semester VARCHAR(50) NOT NULL,
    PRIMARY KEY (student_id, course_id),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

