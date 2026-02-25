-- ============================================================
-- Family Tree API — DDL
-- PostgreSQL
-- ============================================================

-- Users (managed by family-tree-auth-starter)
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,
    middle_name VARCHAR(50),
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    enabled     BOOLEAN NOT NULL DEFAULT FALSE
);

-- Tokens: email-verification and password-reset (managed by auth-starter)
CREATE TABLE IF NOT EXISTS tokens (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(20)  NOT NULL,                          -- 'VERIFY' | 'RESET'
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    issued_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_tokens_expires_at ON tokens(expires_at);

-- Family trees
CREATE TABLE IF NOT EXISTS trees (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Tree membership (roles: OWNER > EDITOR > VIEWER)
CREATE TABLE IF NOT EXISTS tree_memberships (
    id         BIGSERIAL PRIMARY KEY,
    tree_id    BIGINT      NOT NULL REFERENCES trees(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (tree_id, user_id)
);

-- Invitations (email-based or link-based)
CREATE TABLE IF NOT EXISTS invitations (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    tree_id    BIGINT       NOT NULL REFERENCES trees(id) ON DELETE CASCADE,
    email      VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted   BOOLEAN NOT NULL DEFAULT FALSE
);

-- Persons in a family tree
CREATE TABLE IF NOT EXISTS persons (
    id          BIGSERIAL PRIMARY KEY,
    tree_id     BIGINT       NOT NULL REFERENCES trees(id) ON DELETE CASCADE,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    gender      VARCHAR(10)  NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    birth_date  DATE,
    death_date  DATE,
    birth_place VARCHAR(255),
    death_place VARCHAR(255),
    biography   TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_persons_tree_id ON persons(tree_id);

-- Relationships between persons (PARENT_CHILD or PARTNERSHIP)
-- For PARENT_CHILD: person1 = parent, person2 = child
-- For PARTNERSHIP:  person1 and person2 are partners (order is not significant)
CREATE TABLE IF NOT EXISTS relationships (
    id         BIGSERIAL PRIMARY KEY,
    tree_id    BIGINT      NOT NULL REFERENCES trees(id) ON DELETE CASCADE,
    person1_id BIGINT      NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    person2_id BIGINT      NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    type       VARCHAR(20) NOT NULL CHECK (type IN ('PARENT_CHILD', 'PARTNERSHIP')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (tree_id, person1_id, person2_id, type)
);

CREATE INDEX IF NOT EXISTS idx_relationships_tree_id ON relationships(tree_id);
CREATE INDEX IF NOT EXISTS idx_relationships_person1 ON relationships(person1_id);
CREATE INDEX IF NOT EXISTS idx_relationships_person2 ON relationships(person2_id);

-- Media files attached to persons or trees
CREATE TABLE IF NOT EXISTS media_files (
    id          BIGSERIAL PRIMARY KEY,
    person_id   BIGINT       REFERENCES persons(id) ON DELETE CASCADE,   -- nullable: file may belong to tree only
    tree_id     BIGINT       NOT NULL REFERENCES trees(id) ON DELETE CASCADE,
    file_name   VARCHAR(255) NOT NULL,
    file_path   VARCHAR(512) NOT NULL,
    file_type   VARCHAR(20)  NOT NULL CHECK (file_type IN ('PHOTO', 'DOCUMENT', 'VIDEO', 'AUDIO')),
    file_size   BIGINT       NOT NULL,
    description TEXT,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    uploaded_by BIGINT       NOT NULL REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_media_files_tree_id   ON media_files(tree_id);
CREATE INDEX IF NOT EXISTS idx_media_files_person_id ON media_files(person_id);

-- Avatar URL for persons (added in v2)
ALTER TABLE persons ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(512);

-- Comments on persons (threaded, soft-delete)
CREATE TABLE IF NOT EXISTS comments (
    id                BIGSERIAL PRIMARY KEY,
    person_id         BIGINT  NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    user_id           BIGINT  NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    parent_comment_id BIGINT  REFERENCES comments(id) ON DELETE SET NULL,
    content           TEXT    NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_deleted        BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_comments_person_id         ON comments(person_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_comment_id ON comments(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id           ON comments(user_id);

-- Notifications for users
CREATE TABLE IF NOT EXISTS notifications (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(30)  NOT NULL,
    content           TEXT         NOT NULL,
    link              VARCHAR(512),
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(user_id, is_read);
