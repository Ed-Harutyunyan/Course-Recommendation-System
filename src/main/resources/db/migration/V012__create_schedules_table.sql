CREATE TABLE schedules (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE schedule_slots (
    schedule_id BINARY(16) NOT NULL,
    course_type VARCHAR(50) NOT NULL,
    offering_id BINARY(16) NOT NULL,
    credits INTEGER NOT NULL,
    times VARCHAR(255) NOT NULL,
    FOREIGN KEY (schedule_id) REFERENCES schedules(id),
    FOREIGN KEY (offering_id) REFERENCES course_offerings(id)
);