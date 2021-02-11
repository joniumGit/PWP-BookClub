/*
This file is only for the main schema of the database.
*/
CREATE DATABASE book_club CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE book_club;

-- jooq
CREATE USER IF NOT EXISTS 'jooq'@'%' IDENTIFIED BY 'jooq';
GRANT ALL ON book_club.* TO 'jooq'@'%';

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
    id          BIGINT UNSIGNED NOT NULL,
    username    VARCHAR(64)     NOT NULL,
    description VARCHAR(256)    NULL DEFAULT NULL,

    deleted     TINYINT(1)           DEFAULT 0,
    created_at  DATETIME             DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_users (id),
    UNIQUE INDEX idx_users_username (username),
    INDEX idx_users_deleted (deleted)
) ENGINE = InnoDB;

CREATE TABLE user_icon
(
    user_id  BIGINT UNSIGNED NOT NULL,
    icon_url TINYTEXT,

    PRIMARY KEY pk_user_icon (user_id),
    CONSTRAINT FOREIGN KEY fk_ui_user_id (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE clubs
(
    id          BIGINT UNSIGNED NOT NULL,
    handle      VARCHAR(64)     NOT NULL,
    description VARCHAR(2048)   NULL DEFAULT NULL,

    deleted     TINYINT(1)           DEFAULT 0,
    created_at  DATETIME             DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_clubs (id),
    UNIQUE INDEX idx_clubs_handle (handle),
    INDEX idx_clubs_deleted (deleted)
) ENGINE = InnoDB;

CREATE TABLE books
(
    id          BIGINT UNSIGNED NOT NULL,
    handle      VARCHAR(64)     NOT NULL,
    full_name   VARCHAR(256)    NOT NULL,
    description TEXT,
    pages       INTEGER         NULL DEFAULT NULL,

    deleted     TINYINT(1)           DEFAULT 0,
    created_at  DATETIME             DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_books (id),
    UNIQUE INDEX idx_books_handle (handle),
    INDEX idx_books_pages (pages),
    INDEX idx_books_deleted (deleted)
) ENGINE = InnoDB;

CREATE TABLE discussions
(
    id         BIGINT UNSIGNED NOT NULL,
    topic      TEXT,

    deleted    TINYINT(1)           DEFAULT 0,
    created_at DATETIME             DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY pk_discussions (id),
    INDEX idx_discussion_deleted (deleted)
) ENGINE = InnoDB;

CREATE TABLE moderated_discussion
(
    discussion_id BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY pd_md (discussion_id),
    CONSTRAINT FOREIGN KEY fk_md_discussion_id (discussion_id) REFERENCES discussions (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE comments_data
(
    id         BIGINT UNSIGNED NOT NULL,
    user_id    BIGINT UNSIGNED,
    content    TEXT,

    pending    TINYINT         NOT NULL DEFAULT 0,
    deleted    TINYINT         NOT NULL DEFAULT 0,
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

    deleted    TINYINT         NOT NULL DEFAULT 0,
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
    reading_status ENUM ('pending', 'reading', 'complete') NULL DEFAULT NULL,

    reviewed       TINYINT(1) UNSIGNED                          DEFAULT 0,
    ignored        TINYINT(1) UNSIGNED                          DEFAULT 0,
    like_status    ENUM ('liked', 'disliked')              NULL DEFAULT NULL,

    current_page   INTEGER                                 NULL DEFAULT NULL,

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
    CONSTRAINT FOREIGN KEY fk_dcl_comment_id (comment_id) REFERENCES comments_data (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE comment_comment_link
(
    parent_id BIGINT UNSIGNED NOT NULL,
    child_id  BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY pk_ccl (parent_id, child_id),
    UNIQUE INDEX idx_ccl_child_parent (child_id, parent_id),
    CONSTRAINT FOREIGN KEY fk_ccl_parent_id (parent_id) REFERENCES comments_data (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FOREIGN KEY fk_ccl_child_id (child_id) REFERENCES comments_data (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB;

-- views
CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW clubs_user_listing AS
SELECT c.id       AS club_id,
       c.handle   AS club_handle,
       u.id       AS user_id,
       u.username AS username
FROM club_user_link AS l
         JOIN clubs c ON c.id = l.club_id
         JOIN users u ON l.user_id = u.id;

CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW clubs_popular AS
SELECT c.id     AS club_id,
       c.handle AS handle,
       COUNT(*) AS member_count
FROM club_user_link l
         JOIN clubs c ON c.id = l.club_id
GROUP BY l.club_id
ORDER BY member_count
LIMIT 100;

CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW users_top_activity AS
SELECT u.id                        AS user_id,
       u.username                  AS username,
       r.cnt * 1.75 + c.cnt * 0.75 AS activity_score
FROM users AS u
         JOIN (SELECT COUNT(id) AS cnt FROM reviews GROUP BY user_id) AS r
         JOIN (SELECT COUNT(id) AS cnt FROM comments_data GROUP BY user_id) AS c
GROUP BY u.id
ORDER BY activity_score
LIMIT 100;

CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW books_top_activity AS
SELECT b.id                                                                                    AS book_id,
       b.handle                                                                                AS handle,
       ROUND(brl.star * 2, 2)                                                                  AS rating,
       ROUND(SQRT(brl.cnt) + cbl.cnt * 4 + ubl_complete.cnt + ubl_reading.cnt * 0.5 + dbl.cnt) AS activity_score
FROM books AS b
         JOIN (SELECT book_id, COUNT(*) AS cnt, AVG(stars) AS star
               FROM reviews
               WHERE deleted = 0
               GROUP BY book_id) AS brl
              ON b.id = brl.book_id
         JOIN (SELECT book_id, COUNT(*) AS cnt FROM club_book_link GROUP BY book_id) AS cbl ON b.id = cbl.book_id
         JOIN (SELECT book_id, COUNT(*) AS cnt
               FROM user_book_listing
               WHERE reading_status = 'complete'
                  OR reading_status = 'reviewed'
               GROUP BY book_id) AS ubl_complete
              ON b.id = ubl_complete.book_id
         JOIN (SELECT book_id, COUNT(*) AS cnt
               FROM user_book_listing
               WHERE reading_status = 'reading'
               GROUP BY book_id) AS ubl_reading
              ON b.id = ubl_reading.book_id
         JOIN (SELECT book_id, COUNT(*) AS cnt FROM discussion_book_link GROUP BY book_id) AS dbl ON b.id = dbl.book_id;

CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW books_top_rating AS
SELECT b.id                       AS book_id,
       b.handle                   AS handle,
       ROUND(AVG(r.stars) * 2, 2) AS rating
FROM books b
         JOIN reviews r ON r.book_id = b.id
GROUP BY b.id
ORDER BY rating
LIMIT 100;

CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW books_statistics AS
SELECT b.id                       AS book_id,
       b.handle                   AS handle,
       ROUND(AVG(r.stars) * 2, 2) AS rating,
       reader.cnt                 AS readers,
       completed.cnt              AS completed,
       wtr.cnt                    AS want_to_read,
       liked.cnt                  AS liked,
       disliked.cnt               AS disliked
FROM books b
         JOIN reviews r ON r.book_id = r.id
         JOIN (SELECT COUNT(*) AS cnt
               FROM user_book_listing
               WHERE reading_status = 'reading'
               GROUP BY book_id) AS reader
         JOIN (SELECT COUNT(*) AS cnt
               FROM user_book_listing
               WHERE reading_status = 'complete'
               GROUP BY book_id) AS completed
         JOIN (SELECT COUNT(*) AS cnt
               FROM user_book_listing
               WHERE reading_status = 'pending'
               GROUP BY book_id) AS wtr
         JOIN (SELECT COUNT(*) AS cnt
               FROM user_book_listing
               WHERE like_status = 'liked'
               GROUP BY book_id) AS liked
         JOIN (SELECT COUNT(*) AS cnt
               FROM user_book_listing
               WHERE like_status = 'disliked'
               GROUP BY book_id) AS disliked
GROUP BY b.id;

-- Updatable views
CREATE VIEW comments AS
SELECT id,
       user_id,
       content,
       deleted,
       created_at,
       updated_at
FROM comments_data
WHERE pending = 0;

-- Trigger
DELIMITER $$

CREATE TRIGGER trg_review_status
    AFTER INSERT
    ON reviews
    FOR EACH ROW
BEGIN
    UPDATE user_book_listing ubl
    SET ubl.reviewed=1
    WHERE new.user_id = ubl.user_id
      AND new.book_id = ubl.book_id;
END $$

CREATE TRIGGER trg_comment_review_insert
    BEFORE INSERT
    ON comments_data
    FOR EACH ROW
BEGIN
    IF EXISTS(SELECT 1
              FROM moderated_discussion md
              WHERE md.discussion_id =
                    (SELECT dcl.discussion_id FROM discussion_comment_link dcl WHERE dcl.comment_id = new.id)) THEN
        SET new.pending = 1;
    ELSE
        SET new.pending = 0;
    END IF;
END $$

CREATE TRIGGER trg_comment_review_update
    BEFORE UPDATE
    ON comments_data
    FOR EACH ROW
BEGIN
    IF EXISTS(SELECT 1
              FROM moderated_discussion md
              WHERE md.discussion_id =
                    (SELECT dcl.discussion_id FROM discussion_comment_link dcl WHERE dcl.comment_id = new.id)) THEN
        SET new.pending = 1;
    ELSE
        SET new.pending = 0;
    END IF;
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
END $$

DELIMITER ;

-- Prototype
CREATE TABLE friends_request
(
    from_id BIGINT UNSIGNED NOT NULL,
    to_id   BIGINT UNSIGNED NOT NULL,
    status  ENUM ('pending', 'confirmed', 'rejected'),
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

-- Grants
GRANT DELETE ON book_club.moderated_discussion TO 'bk_api_user'@'%';

GRANT SELECT ON book_club.* TO 'bk_api_user'@'%';
GRANT INSERT ON book_club.* TO 'bk_api_user'@'%';

GRANT UPDATE ON book_club.users TO 'bk_api_user'@'%';
GRANT UPDATE ON book_club.books TO 'bk_api_user'@'%';
GRANT UPDATE ON book_club.clubs TO 'bk_api_user'@'%';
GRANT UPDATE ON book_club.discussions TO 'bk_api_user'@'%';
GRANT UPDATE ON book_club.reviews TO 'bk_api_user'@'%';
GRANT UPDATE ON book_club.user_icon TO 'bk_api_user'@'%';
GRANT UPDATE ON book_club.user_book_listing TO 'bk_api_user'@'%';

GRANT UPDATE ON book_club.friends TO 'bk_api_user'@'%';
GRANT UPDATE ON book_club.friends_request TO 'bk_api_user'@'%';

FLUSH PRIVILEGES;
