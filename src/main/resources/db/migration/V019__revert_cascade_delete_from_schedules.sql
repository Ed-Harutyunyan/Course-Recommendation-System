-- Drop the foreign key constraint with CASCADE DELETE
ALTER TABLE schedules
    DROP FOREIGN KEY schedules_ibfk_1;

-- Re-add the original constraint without CASCADE DELETE
ALTER TABLE schedules
    ADD CONSTRAINT schedules_ibfk_1
        FOREIGN KEY (user_id) REFERENCES users (id);