ALTER TABLE instructors
    ADD COLUMN image_url VARCHAR(255),
    ADD COLUMN position VARCHAR(255),
    ADD COLUMN mobile VARCHAR(50),
    ADD COLUMN email VARCHAR(255),
    ADD COLUMN bio TEXT,
    ADD COLUMN office_location VARCHAR(255);

-- Adding unique constraint to name
ALTER TABLE instructors ADD CONSTRAINT uk_instructor_name UNIQUE (name);