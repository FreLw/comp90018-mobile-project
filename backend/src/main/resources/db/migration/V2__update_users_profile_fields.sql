ALTER TABLE users
    RENAME COLUMN username TO account,
    RENAME COLUMN profile_url TO avatar_url,
    ADD COLUMN password_hash VARCHAR(255) NOT NULL AFTER account,
    ADD COLUMN bio VARCHAR(500) NULL AFTER avatar_url;

ALTER TABLE users
    DROP INDEX uk_users_username,
    ADD CONSTRAINT uk_users_account UNIQUE (account);
