CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50),
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    enabled BOOLEAN DEFAULT FALSE
);

CREATE TABLE tokens (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL, -- 'VERIFY' или 'RESET'
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE trees (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE tree_memberships (
    id BIGSERIAL PRIMARY KEY,
    tree_id BIGINT NOT NULL REFERENCES trees(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(tree_id, user_id)
);

CREATE TABLE invitations (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    tree_id BIGINT NOT NULL REFERENCES trees(id),
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    accepted BOOLEAN DEFAULT FALSE
);

CREATE TABLE persons (
    id BIGSERIAL PRIMARY KEY,
    tree_id BIGINT NOT NULL REFERENCES trees(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE')),
    birth_date DATE,
    death_date DATE,
    birth_place VARCHAR(255),
    death_place VARCHAR(255),
    biography TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE relationships (
    id BIGSERIAL PRIMARY KEY,
    tree_id BIGINT NOT NULL REFERENCES trees(id) ON DELETE CASCADE,
    person1_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    person2_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('PARENT_CHILD', 'PARTNERSHIP')),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (tree_id, person1_id, person2_id, type)
);

CREATE TABLE media_files (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT REFERENCES persons(id) ON DELETE CASCADE,
    tree_id BIGINT NOT NULL REFERENCES trees(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL, -- путь в MinIO/S3
    file_type VARCHAR(20) NOT NULL, -- PHOTO, DOCUMENT, VIDEO, AUDIO
    file_size BIGINT NOT NULL,
    description TEXT,
    uploaded_at TIMESTAMP DEFAULT NOW(),
    uploaded_by BIGINT NOT NULL REFERENCES users(id)
);

CREATE INDEX idx_tokens_expires_at ON tokens(expires_at);