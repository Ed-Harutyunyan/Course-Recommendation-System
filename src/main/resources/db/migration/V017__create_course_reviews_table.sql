CREATE TABLE course_reviews
(
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    course_id BINARY(16) NOT NULL,
    content    TEXT      NOT NULL,
    rating     INT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    created_at TIMESTAMP NOT NULL,

    INDEX idx_course_reviews_course_id (course_id),
    INDEX idx_course_reviews_user_id (user_id)
);