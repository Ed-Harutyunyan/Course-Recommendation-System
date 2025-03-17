-- V007__create_enrollments.sql
CREATE TABLE enrollments (
     user_id BINARY(16) NOT NULL,
     course_offering_id BINARY(16) NOT NULL,
     grade VARCHAR(50) NOT NULL,
     PRIMARY KEY (user_id, course_offering_id),
     CONSTRAINT fk_enrollment_user
         FOREIGN KEY (user_id)
             REFERENCES users(id),
     CONSTRAINT fk_enrollment_course_offering
         FOREIGN KEY (course_offering_id)
             REFERENCES course_offerings(id)
);

