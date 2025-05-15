-- Drop the existing foreign key constraint
ALTER TABLE schedules
    DROP FOREIGN KEY schedules_ibfk_1;

ALTER TABLE schedules
    ADD CONSTRAINT schedules_ibfk_1
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE;