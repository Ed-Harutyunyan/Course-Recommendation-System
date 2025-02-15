-- V006_add_profiles_and_enrollments.sql

-- 1. Create instructor_profiles table
CREATE TABLE instructor_profiles (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_instructor_user (user_id),
    CONSTRAINT fk_instructor_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 2. Create student_profiles table
CREATE TABLE student_profiles (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_user (user_id),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 3. Create enrollments table with extra attributes (e.g., passed and grade)
CREATE TABLE enrollments (
    student_profile_id BINARY(16) NOT NULL,
    course_id BINARY(16) NOT NULL,
    grade VARCHAR(50) NOT NULL,
    semester VARCHAR(50) NOT NULL,
    PRIMARY KEY (student_profile_id, course_id),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id),
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- 4. Update courses table to use instructor_profile_id instead of instructor_id
ALTER TABLE courses
    DROP COLUMN instructor_id,
    ADD COLUMN instructor_profile_id BINARY(16) NOT NULL;

ALTER TABLE courses
    ADD CONSTRAINT fk_instructor_profile
        FOREIGN KEY (instructor_profile_id) REFERENCES instructor_profiles(id);
