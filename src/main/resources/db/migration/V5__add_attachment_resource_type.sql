ALTER TABLE attachments
    ADD COLUMN resource_type VARCHAR(20) NOT NULL DEFAULT 'image';
