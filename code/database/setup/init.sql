/*
This file is only for the main schema of the database.
*/
CREATE DATABASE book_club CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE book_club;

-- Users
CREATE USER IF NOT EXISTS 'bk_api_user'@'%' IDENTIFIED BY 'should_be_created';
CREATE USER IF NOT EXISTS 'bk_api_maintenance'@'%' IDENTIFIED BY 'should_be_created';
CREATE USER IF NOT EXISTS 'bk_read_only'@'%' IDENTIFIED BY 'should_be_created';

-- User
GRANT SELECT ON book_club.* TO 'bk_api_user'@'%';
GRANT INSERT ON book_club.* TO 'bk_api_user'@'%';
GRANT UPDATE ON book_club.* TO 'bk_api_user'@'%';

-- Maintenance
GRANT UPDATE ON book_club.* TO 'bk_api_maintenance'@'%';
GRANT INSERT ON book_club.* TO 'bk_api_maintenance'@'%';
GRANT SELECT ON book_club.* TO 'bk_api_maintenance'@'%';
GRANT DELETE ON book_club.* TO 'bk_api_maintenance'@'%';

-- Read Only
GRANT SELECT ON book_club.* TO 'bk_read_only'@'%';

-- Flush
FLUSH PRIVILEGES;

-- Entity tables
CREATE TABLE users
(
    id            INTEGER      NOT NULL AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin,
    description   VARCHAR(256) NULL     DEFAULT NULL,

    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_users (id),
    UNIQUE INDEX idx_users_username (username),
    INDEX idx_users_deleted (deleted)
) ENGINE = InnoDB;

CREATE TABLE clubs
(
    id          INTEGER       NOT NULL AUTO_INCREMENT,
    owner_id    INTEGER       NULL,
    handle      VARCHAR(64)   NOT NULL,
    description VARCHAR(2048) NULL     DEFAULT NULL,

    deleted     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  DATETIME               DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_clubs (id),
    UNIQUE INDEX idx_clubs_handle (handle),
    INDEX idx_clubs_deleted (deleted),
    CONSTRAINT FOREIGN KEY fg_club_owner (owner_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE books
(
    id          INTEGER      NOT NULL AUTO_INCREMENT,
    handle      VARCHAR(64)  NOT NULL,
    full_name   VARCHAR(256) NOT NULL,
    description TEXT,
    pages       INTEGER      NULL     DEFAULT NULL,

    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME              DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_books (id),
    UNIQUE INDEX idx_books_handle (handle),
    INDEX idx_books_pages (pages),
    INDEX idx_books_deleted (deleted)
) ENGINE = InnoDB;

CREATE TABLE comments
(
    id         INTEGER         NOT NULL AUTO_INCREMENT,

    uuid       BIGINT UNSIGNED NOT NULL DEFAULT UUID_SHORT(),
    user_id    INTEGER         NULL,
    content    TEXT,

    deleted    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_comments (id),
    INDEX idx_comments_user_id (user_id),
    INDEX idx_comments_deleted (deleted),
    INDEX idx_comments_updated (updated_at),
    UNIQUE INDEX idx_comments_uuid (uuid),
    CONSTRAINT FOREIGN KEY fk_comments_user_id (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE reviews
(
    id         INTEGER      NOT NULL AUTO_INCREMENT,
    user_id    INTEGER      NULL,
    book_id    INTEGER      NOT NULL,
    stars      TINYINT      NOT NULL DEFAULT 3,
    title      VARCHAR(128) NOT NULL,
    content    TEXT,

    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME              DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_reviews (id),
    UNIQUE INDEX reviews_user_id (user_id, book_id),
    INDEX reviews_book_id (book_id),
    CONSTRAINT FOREIGN KEY fk_reviews_user_id (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT FOREIGN KEY fk_reviews_book_id (book_id) REFERENCES books (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE user_books
(
    user_id        INTEGER                                 NOT NULL,
    book_id        INTEGER                                 NOT NULL,
    reading_status ENUM ('pending', 'reading', 'complete') NULL     DEFAULT NULL,
    reviewed       BOOLEAN                                 NOT NULL DEFAULT FALSE,
    ignored        BOOLEAN                                 NOT NULL DEFAULT FALSE,
    liked          BOOLEAN                                 NULL     DEFAULT NULL,
    current_page   INTEGER                                 NULL     DEFAULT NULL,

    PRIMARY KEY pk_ubl (user_id, book_id),
    INDEX idx_ubl_reverse (book_id, user_id),
    INDEX idx_ubl_reading_status (reading_status),
    INDEX idx_ubl_like_status (reading_status),
    INDEX idx_ubl_reviewed (user_id, reviewed),
    CONSTRAINT FOREIGN KEY fk_ubl_user_id (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FOREIGN KEY fk_ubl_book_id (book_id) REFERENCES books (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- Link tables
CREATE TABLE club_user_link
(
    club_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,

    PRIMARY KEY pk_cul (club_id, user_id),
    INDEX idx_cul_user_id (user_id),
    CONSTRAINT FOREIGN KEY fk_cul_club_id (club_id) REFERENCES clubs (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FOREIGN KEY fk_cul_user_id (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE club_book_link
(
    club_id INTEGER NOT NULL,
    book_id INTEGER NOT NULL,

    PRIMARY KEY pk_cbl (club_id, book_id),
    INDEX idx_cbl_reverse (book_id),
    CONSTRAINT FOREIGN KEY fk_cbl_club_id (club_id) REFERENCES clubs (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FOREIGN KEY fk_cbl_book_id (book_id) REFERENCES books (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE review_comment_link
(
    review_id  INTEGER NOT NULL,
    comment_id INTEGER NOT NULL,

    PRIMARY KEY pk_ccl (review_id, comment_id),
    CONSTRAINT FOREIGN KEY fk_ccl_parent_id (review_id) REFERENCES reviews (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FOREIGN KEY fk_ccl_child_id (comment_id) REFERENCES comments (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- views
CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW books_statistics AS
SELECT b.handle                 AS handle,
       IFNULL(r.stars, 0)       AS rating,
       IFNULL(reader.cnt, 0)    AS readers,
       IFNULL(completed.cnt, 0) AS completed,
       IFNULL(wtr.cnt, 0)       AS pending,
       IFNULL(liked.cnt, 0)     AS liked,
       IFNULL(disliked.cnt, 0)  AS disliked
FROM books b
         LEFT JOIN (SELECT book_id AS id, ROUND(AVG(r.stars) * 2, 2) AS stars
                    FROM reviews r
                    GROUP BY r.book_id) AS r ON b.id = r.id
         LEFT JOIN (SELECT book_id AS id, COUNT(*) AS cnt
                    FROM user_books
                    WHERE reading_status = 'reading'
                    GROUP BY book_id) AS reader ON b.id = reader.id
         LEFT JOIN (SELECT book_id AS id, COUNT(*) AS cnt
                    FROM user_books
                    WHERE reading_status = 'complete'
                    GROUP BY book_id) AS completed ON b.id = completed.id
         LEFT JOIN (SELECT book_id AS id, COUNT(*) AS cnt
                    FROM user_books
                    WHERE reading_status = 'pending'
                    GROUP BY book_id) AS wtr ON b.id = wtr.id
         LEFT JOIN (SELECT book_id AS id, COUNT(*) AS cnt
                    FROM user_books
                    WHERE liked = TRUE
                    GROUP BY book_id) AS liked ON b.id = liked.id
         LEFT JOIN (SELECT book_id AS id, COUNT(*) AS cnt
                    FROM user_books
                    WHERE liked = FALSE
                    GROUP BY book_id) AS disliked ON b.id = disliked.id
ORDER BY b.handle;

-- Trigger
DELIMITER $$

CREATE TRIGGER trg_review_status
    AFTER INSERT
    ON reviews
    FOR EACH ROW
BEGIN
    UPDATE user_books ubl
    SET ubl.reviewed = 1
    WHERE new.user_id = ubl.user_id
      AND new.book_id = ubl.book_id;
END $$

DELIMITER ;
