CREATE TABLE IF NOT EXISTS users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    hashed_pass VARCHAR(255),
    status VARCHAR (20) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(20) NOT NULL,

    CONSTRAINT chk_user_role CHECK (role IN ('SELLER', 'CUSTOMER', 'ADMIN')),

    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'BLOCKED')) -- might add unverified later
);

CREATE TABLE IF NOT EXISTS sessions (
    id UUID PRIMARY KEY, -- id generated in backend
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, -- Managed by the application
    expires_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_session_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_session_expiry
        CHECK (expires_at > created_at)
);

CREATE TABLE IF NOT EXISTS seller_users (
    seller_id BIGINT PRIMARY KEY,

    CONSTRAINT fk_seller_user
        FOREIGN KEY (seller_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS customer_users (
    customer_id BIGINT PRIMARY KEY,

    CONSTRAINT fk_customer_user
        FOREIGN KEY (customer_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS categories (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    category_id INTEGER,
    stock INTEGER NOT NULL DEFAULT 0,
    price INTEGER NOT NULL, -- Stored in cents

    CONSTRAINT fk_product_seller
        FOREIGN KEY (seller_id)
        REFERENCES seller_users(seller_id),

    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id),

    CONSTRAINT chk_product_stock CHECK (stock >= 0),

    CONSTRAINT chk_product_price CHECK (price >= 0)
);


