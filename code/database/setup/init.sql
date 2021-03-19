/*
This file is only for the main schema of the database.
*/
CREATE DATABASE book_club CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE book_club;

-- Users
CREATE USER IF NOT EXISTS 'bk_api_user'@'%' IDENTIFIED BY 'should_be_created';
CREATE USER IF NOT EXISTS 'bk_api_maintenance'@'%' IDENTIFIED BY 'should_be_created';
CREATE USER IF NOT EXISTS 'bk_read_only'@'%' IDENTIFIED BY 'should_be_created';

-- Maintenance
GRANT UPDATE ON book_club.* TO 'bk_api_maintenance'@'%';
GRANT INSERT ON book_club.* TO 'bk_api_maintenance'@'%';
GRANT SELECT ON book_club.* TO 'bk_api_maintenance'@'%';
GRANT DELETE ON book_club.* TO 'bk_api_maintenance'@'%';

-- Read Only
GRANT SELECT ON book_club.* TO 'bk_read_only'@'%';
FLUSH PRIVILEGES;

-- Entity tables
CREATE TABLE users
(
    id            BIGINT UNSIGNED NOT NULL,

    username      VARCHAR(64)     NOT NULL,
    description   VARCHAR(256)    NULL     DEFAULT NULL,

    deleted       TINYINT(0)      NOT NULL DEFAULT 0,
    created_at    DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    password_hash VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin,
    icon_url      TINYTEXT        NULL     DEFAULT NULL,
    external_id   VARCHAR(128)    NULL     DEFAULT NULL,

    PRIMARY KEY pk_users (id),
    UNIQUE INDEX idx_users_username (username),
    INDEX idx_users_deleted (deleted),

    UNIQUE INDEX idx_users_external (external_id)
) ENGINE = InnoDB;

CREATE TABLE clubs
(
    id          BIGINT UNSIGNED NOT NULL,
    owner_id    BIGINT UNSIGNED,

    handle      VARCHAR(64)     NOT NULL,
    description VARCHAR(2048)   NULL     DEFAULT NULL,

    deleted     TINYINT(0)      NOT NULL DEFAULT 0,
    created_at  DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_clubs (id),
    UNIQUE INDEX idx_clubs_handle (handle),
    INDEX idx_clubs_deleted (deleted),
    CONSTRAINT FOREIGN KEY fg_club_owner (owner_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE books
(
    id          BIGINT UNSIGNED NOT NULL,

    handle      VARCHAR(64)     NOT NULL,
    full_name   VARCHAR(256)    NOT NULL,
    description TEXT,
    pages       INTEGER         NULL     DEFAULT NULL,

    deleted     TINYINT(0)      NOT NULL DEFAULT 0,
    created_at  DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_books (id),
    UNIQUE INDEX idx_books_handle (handle),
    INDEX idx_books_pages (pages),
    INDEX idx_books_deleted (deleted)
) ENGINE = InnoDB;

CREATE TABLE discussions
(
    id         BIGINT UNSIGNED NOT NULL,
    owner_id   BIGINT UNSIGNED,

    topic      TEXT,

    deleted    TINYINT(0)      NOT NULL DEFAULT 0,
    created_at DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_discussions (id),
    INDEX idx_discussion_deleted (deleted),
    CONSTRAINT FOREIGN KEY fg_discussion_owner (owner_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE comments
(
    id         BIGINT UNSIGNED NOT NULL,

    user_id    BIGINT UNSIGNED,
    content    TEXT,

    pending    TINYINT(0)      NOT NULL DEFAULT 1,
    deleted    TINYINT(0)      NOT NULL DEFAULT 0,
    created_at DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_comments (id),
    INDEX idx_comments_user_id (user_id),
    INDEX idx_comments_deleted (deleted) COMMENT 'For statistics and administration',
    INDEX idx_comments_pending (pending) COMMENT 'For statistics and administration',
    INDEX idx_comments_updated (updated_at) COMMENT 'For latest  comments and edits',
    CONSTRAINT FOREIGN KEY fk_comments_user_id (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE reviews
(
    id         BIGINT UNSIGNED NOT NULL,

    user_id    BIGINT UNSIGNED,
    book_id    BIGINT UNSIGNED,

    stars      TINYINT         NOT NULL DEFAULT 3,
    title      VARCHAR(128)    NOT NULL,
    content    TEXT,

    deleted    TINYINT(0)      NOT NULL DEFAULT 0,
    created_at DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_reviews (id),
    UNIQUE INDEX reviews_user_id (user_id, book_id),
    INDEX reviews_book_id (book_id),
    CONSTRAINT FOREIGN KEY fk_reviews_user_id (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT FOREIGN KEY fk_reviews_book_id (book_id) REFERENCES books (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE user_book_listing
(
    user_id        BIGINT UNSIGNED                         NOT NULL,
    book_id        BIGINT UNSIGNED                         NOT NULL,
    reading_status ENUM ('pending', 'reading', 'complete') NULL     DEFAULT NULL,

    reviewed       TINYINT(0)                              NOT NULL DEFAULT 0,
    ignored        TINYINT(0)                              NOT NULL DEFAULT 0,
    like_status    ENUM ('liked', 'disliked')              NULL     DEFAULT NULL,

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
    club_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY pk_cul (club_id, user_id),
    INDEX idx_cul_user_id (user_id),
    CONSTRAINT FOREIGN KEY fk_cul_club_id (club_id) REFERENCES clubs (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FOREIGN KEY fk_cul_user_id (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE club_discussion_link
(
    club_id       BIGINT UNSIGNED NOT NULL,
    discussion_id BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY pk_cdl (club_id, discussion_id),
    INDEX idx_cdl_reverse (discussion_id),
    CONSTRAINT FOREIGN KEY fk_cdl_club (club_id) REFERENCES clubs (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FOREIGN KEY fk_cdl_disc (discussion_id) REFERENCES discussions (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE club_book_link
(
    club_id BIGINT UNSIGNED NOT NULL,
    book_id BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY pk_cbl (club_id, book_id),
    INDEX idx_cbl_reverse (book_id),
    CONSTRAINT FOREIGN KEY fk_cbl_club_id (club_id) REFERENCES clubs (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FOREIGN KEY fk_cbl_book_id (book_id) REFERENCES books (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE discussion_book_link
(
    discussion_id BIGINT UNSIGNED NOT NULL,
    book_id       BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY pk_dbl (discussion_id, book_id),
    INDEX idx_dbl_reverse (book_id),
    CONSTRAINT FOREIGN KEY fk_dbl_discussion_id (discussion_id) REFERENCES discussions (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FOREIGN KEY fk_dbl_book_id (book_id) REFERENCES books (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE discussion_comment_link
(
    discussion_id BIGINT UNSIGNED NOT NULL,
    comment_id    BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY pk_dcl (comment_id),
    INDEX pk_dcl_discussion (discussion_id),
    CONSTRAINT FOREIGN KEY fk_dcl_discussion_id (discussion_id) REFERENCES discussions (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FOREIGN KEY fk_dcl_comment_id (comment_id) REFERENCES comments (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE comment_comment_link
(
    parent_id BIGINT UNSIGNED NOT NULL,
    child_id  BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY pk_ccl (parent_id, child_id),
    UNIQUE INDEX idx_ccl_child_parent (child_id, parent_id),
    CONSTRAINT FOREIGN KEY fk_ccl_parent_id (parent_id) REFERENCES comments (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FOREIGN KEY fk_ccl_child_id (child_id) REFERENCES comments (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB;

-- views
CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW books_rating AS
SELECT b.handle                   AS handle,
       ROUND(AVG(r.stars) * 2, 2) AS rating
FROM reviews r
         JOIN books b ON r.book_id = b.id
GROUP BY b.id
ORDER BY rating DESC;

CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW books_statistics AS
SELECT b.handle                 AS handle,
       IFNULL(r.stars, 0)       AS rating,
       IFNULL(reader.cnt, 0)    AS readers,
       IFNULL(completed.cnt, 0) AS completed,
       IFNULL(wtr.cnt, 0)       AS want_to_read,
       IFNULL(liked.cnt, 0)     AS liked,
       IFNULL(disliked.cnt, 0)  AS disliked
FROM books b
         LEFT JOIN (SELECT book_id AS id, ROUND(AVG(r.stars) * 2, 2) AS stars
                    FROM reviews r
                    GROUP BY r.book_id) AS r ON b.id = r.id
         LEFT JOIN (SELECT book_id AS id, COUNT(*) AS cnt
                    FROM user_book_listing
                    WHERE reading_status = 'reading'
                    GROUP BY book_id) AS reader ON b.id = reader.id
         LEFT JOIN (SELECT book_id AS id, COUNT(*) AS cnt
                    FROM user_book_listing
                    WHERE reading_status = 'complete'
                    GROUP BY book_id) AS completed ON b.id = completed.id
         LEFT JOIN (SELECT book_id AS id, COUNT(*) AS cnt
                    FROM user_book_listing
                    WHERE reading_status = 'pending'
                    GROUP BY book_id) AS wtr ON b.id = wtr.id
         LEFT JOIN (SELECT book_id AS id, COUNT(*) AS cnt
                    FROM user_book_listing
                    WHERE like_status = 'liked'
                    GROUP BY book_id) AS liked ON b.id = liked.id
         LEFT JOIN (SELECT book_id AS id, COUNT(*) AS cnt
                    FROM user_book_listing
                    WHERE like_status = 'disliked'
                    GROUP BY book_id) AS disliked ON b.id = disliked.id
ORDER BY b.handle;

CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW clubs_user_listing AS
SELECT c.handle   AS club_handle,
       u.username AS username
FROM club_user_link AS l
         JOIN clubs c ON c.id = l.club_id
         JOIN users u ON l.user_id = u.id
ORDER BY c.handle;

-- Trigger
DELIMITER $$

CREATE TRIGGER trg_review_status
    AFTER INSERT
    ON reviews
    FOR EACH ROW
BEGIN
    UPDATE user_book_listing ubl
    SET ubl.reviewed = 1
    WHERE new.user_id = ubl.user_id
      AND new.book_id = ubl.book_id;
END $$

CREATE TRIGGER trg_ccl_parent_id
    BEFORE INSERT
    ON comment_comment_link
    FOR EACH ROW
BEGIN
    IF EXISTS(SELECT 1 FROM comment_comment_link ccl WHERE ccl.child_id = new.parent_id)
    THEN
        SIGNAL SQLSTATE '51000' SET MESSAGE_TEXT = 'a child cannot be a parent';
    END IF;
    IF new.child_id = new.parent_id
    THEN
        SIGNAL SQLSTATE '51000' SET MESSAGE_TEXT = 'cannot comment on self';
    END IF;
END $$

DELIMITER ;

-- Prototype
CREATE TABLE friends_request
(
    from_id BIGINT UNSIGNED                           NOT NULL,
    to_id   BIGINT UNSIGNED                           NOT NULL,
    status  ENUM ('pending', 'confirmed', 'rejected') NOT NULL DEFAULT 'pending',
    PRIMARY KEY pk_fr (to_id, from_id),
    UNIQUE INDEX idx_fr_reverse (from_id, to_id),
    CONSTRAINT FOREIGN KEY fk_fr_from_id (from_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FOREIGN KEY fk_fr_to_id (to_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE friends
(
    from_id BIGINT UNSIGNED NOT NULL,
    to_id   BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY pk_friends (from_id, to_id),
    UNIQUE INDEX idx_friends_reverse (to_id, from_id),
    CONSTRAINT FOREIGN KEY fk_friends_from_id (from_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FOREIGN KEY fk_friends_to_id (to_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

DELIMITER $$
CREATE TRIGGER trg_friend_request
    AFTER UPDATE
    ON friends_request
    FOR EACH ROW
BEGIN
    IF new.status = 'confirmed' THEN
        INSERT IGNORE INTO friends (from_id, to_id) VALUES (new.from_id, new.to_id);
    END IF;
END $$
CREATE TRIGGER trg_friend_delete
    AFTER DELETE
    ON friends
    FOR EACH ROW
BEGIN
    DELETE
    FROM friends_request
    WHERE ((from_id = old.from_id AND to_id = old.to_id)
        OR (from_id = old.to_id AND to_id = old.from_id));
END $$
DELIMITER ;

-- Not important proto
CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW clubs_popular AS
SELECT c.handle AS handle,
       COUNT(*) AS member_count
FROM club_user_link l
         JOIN clubs c ON c.id = l.club_id
GROUP BY l.club_id
ORDER BY member_count DESC
LIMIT 100;

CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW users_top_activity AS
SELECT u.username                                       AS username,
       ROUND(IFNULL(r.cnt, 0) + SQRT(IFNULL(c.cnt, 0))) AS activity_score
FROM users AS u
         LEFT JOIN (SELECT user_id AS id, COUNT(id) AS cnt
                    FROM reviews
                    GROUP BY user_id) AS r ON u.id = r.id
         LEFT JOIN (SELECT user_id AS id, COUNT(id) AS cnt
                    FROM comments
                    WHERE pending = 0
                    GROUP BY user_id) AS c ON u.id = c.id
ORDER BY activity_score DESC
LIMIT 100;

CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW books_top_activity AS
SELECT b.handle                                AS handle,
       ROUND(IFNULL(brl.star, 0) * 2, 2)       AS rating,
       ROUND(IFNULL(cbl.cnt, 0) * 4
           + IFNULL(ubl_complete.cnt, 0)
           + SQRT(IFNULL(brl.cnt, 0))
           + SQRT(IFNULL(ubl_reading.cnt, 0))) AS activity_score
FROM books AS b
         LEFT JOIN (SELECT book_id, COUNT(*) AS cnt, AVG(stars) AS star
                    FROM reviews
                    WHERE deleted = 0
                    GROUP BY book_id) AS brl
                   ON b.id = brl.book_id
         LEFT JOIN (SELECT book_id, COUNT(*) AS cnt FROM club_book_link GROUP BY book_id) AS cbl ON b.id = cbl.book_id
         LEFT JOIN (SELECT book_id, COUNT(*) AS cnt
                    FROM user_book_listing
                    WHERE reading_status = 'complete'
                       OR reading_status = 'reviewed'
                    GROUP BY book_id) AS ubl_complete
                   ON b.id = ubl_complete.book_id
         LEFT JOIN (SELECT book_id, COUNT(*) AS cnt
                    FROM user_book_listing
                    WHERE reading_status = 'reading'
                    GROUP BY book_id) AS ubl_reading
                   ON b.id = ubl_reading.book_id
ORDER BY activity_score DESC
LIMIT 100;

-- Grants
GRANT SELECT ON book_club.* TO 'bk_api_user'@'%';
GRANT INSERT ON book_club.* TO 'bk_api_user'@'%';
GRANT UPDATE ON book_club.* TO 'bk_api_user'@'%';

GRANT DELETE ON book_club.friends TO 'bk_api_user'@'%';

FLUSH PRIVILEGES;
