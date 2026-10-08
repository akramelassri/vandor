CREATE TABLE IF NOT EXISTS users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    hashed_pass VARCHAR(255),
    role VARCHAR(20) NOT NULL,

    CONSTRAINT chk_users_role CHECK (role IN ('SELLER', 'CUSTOMER', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS sessions (
    id UUID PRIMARY KEY, -- id generated in backend to not add one more sql request
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    expires_at TIMESTAMP,

    CONSTRAINT fk_session_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);