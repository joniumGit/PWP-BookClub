/*
This file is only for the main schema of the database. All other modifications will be run after this with update.sql
*/
SET autocommit = 0;
CREATE DATABASE book_club CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE book_club;

# DB users
CREATE USER IF NOT EXISTS 'bk_read_only'@'localhost' IDENTIFIED BY 'should_be_created';
GRANT SELECT ON book_club.* TO 'bk_read_only'@'localhost';
GRANT CREATE VIEW ON book_club.* TO 'bk_read_only'@'localhost';
FLUSH PRIVILEGES;

CREATE USER IF NOT EXISTS 'bk_api_user'@'%' IDENTIFIED BY 'should_be_created';
GRANT SELECT ON book_club.* TO 'bk_api_user'@'%';
GRANT INSERT ON book_club.* TO 'bk_api_user'@'%';
GRANT UPDATE ON book_club.* TO 'bk_api_user'@'%';
GRANT DELETE ON book_club.* TO 'bk_api_user'@'%';
FLUSH PRIVILEGES;


# Entity tables representing the main types
# of entities present in the database

CREATE TABLE users
(
    id          BIGINT UNSIGNED NOT NULL,
    username    VARCHAR(64)     NOT NULL,
    description VARCHAR(256)    NULL DEFAULT NULL,
    created_at  DATETIME             DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY pk_users (id),
    UNIQUE INDEX idx_users_username (username)
) ENGINE = InnoDB;

CREATE TABLE user_icon
(
    user_id  BIGINT UNSIGNED NOT NULL,
    icon_url TINYTEXT,
    PRIMARY KEY pk_user_icon (user_id),
    CONSTRAINT FOREIGN KEY fk_ui_user_id (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE clubs
(
    id          BIGINT UNSIGNED NOT NULL,
    handle      VARCHAR(64)     NOT NULL,
    description VARCHAR(2048)   NULL DEFAULT NULL,
    created_at  DATETIME             DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY pk_clubs (id),
    UNIQUE INDEX idx_clubs_handle (handle)
) ENGINE = InnoDB;

CREATE TABLE books
(
    id          BIGINT UNSIGNED NOT NULL,
    handle      VARCHAR(64)     NOT NULL,
    full_name   VARCHAR(256)    NOT NULL,
    description TEXT,
    created_at  DATETIME             DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY pk_books (id),
    UNIQUE INDEX idx_books_handle (handle)
) ENGINE = InnoDB;

CREATE TABLE discussions
(
    id         BIGINT UNSIGNED NOT NULL,
    topic      TEXT,
    created_at DATETIME             DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY pk_discussions (id)
) ENGINE = InnoDB;

CREATE TABLE comments
(
    id         BIGINT UNSIGNED NOT NULL,
    user_id    BIGINT UNSIGNED,
    content    TEXT,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    created_at DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY pk_comments (id),
    INDEX idx_comments_user_id (user_id),
    CONSTRAINT FOREIGN KEY fk_comments_user_id (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE reviews
(
    id      BIGINT UNSIGNED NOT NULL,
    stars   TINYINT         NOT NULL DEFAULT 3,
    user_id BIGINT UNSIGNED,
    content TEXT,
    PRIMARY KEY pk_reviews (id),
    INDEX reviews_user_id (user_id),
    CONSTRAINT FOREIGN KEY fk_reviews_user_id (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE user_book_listing
(
    user_id  BIGINT UNSIGNED NOT NULL,
    book_id  BIGINT UNSIGNED NOT NULL,
    status   ENUM ('pending', 'reading', 'complete', 'reviewed'),
    PRIMARY KEY pk_ubl (user_id, book_id),
    INDEX idx_ubl_reverse (book_id, user_id),
    CONSTRAINT FOREIGN KEY fk_ubl_user_id (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FOREIGN KEY fk_ubl_book_id (book_id) REFERENCES books (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

# Link tables for linking entities
# and enforcing constraints across database

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
    CONSTRAINT FOREIGN KEY fk_cdl_club (club_id) REFERENCES clubs (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FOREIGN KEY fk_cdl_disc (discussion_id) REFERENCES discussions (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE club_book_link
(
    club_id BIGINT UNSIGNED NOT NULL,
    book_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY pk_cbl (club_id, book_id),
    INDEX idx_cbl_reverse (book_id),
    CONSTRAINT FOREIGN KEY fk_cbl_club_id (club_id) REFERENCES clubs (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FOREIGN KEY fk_cbl_book_id (book_id) REFERENCES books (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE book_review_link
(
    book_id   BIGINT UNSIGNED NOT NULL,
    review_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY pk_brl (review_id),
    INDEX idx_brl_book_review (book_id, review_id),
    CONSTRAINT FOREIGN KEY fk_brl_book_id (book_id) REFERENCES books (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FOREIGN KEY fk_brl_review_id (review_id) REFERENCES reviews (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE discussion_book_link
(
    discussion_id BIGINT UNSIGNED NOT NULL,
    book_id       BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY pk_dbl (discussion_id, book_id),
    INDEX idx_dbl_reverse (book_id),
    CONSTRAINT FOREIGN KEY fk_dbl_discussion_id (discussion_id) REFERENCES discussions (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FOREIGN KEY fk_dbl_book_id (book_id) REFERENCES books (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE discussion_comment_link
(
    discussion_id BIGINT UNSIGNED NOT NULL,
    comment_id    BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY pk_dcl (comment_id),
    INDEX pk_dcl_discussion_comments (discussion_id, comment_id),
    CONSTRAINT FOREIGN KEY fk_dcl_discussion_id (discussion_id) REFERENCES discussions (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FOREIGN KEY fk_dcl_comment_id (comment_id) REFERENCES comments (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;


CREATE TABLE comment_comment_link
(
    parent_id BIGINT UNSIGNED NOT NULL,
    child_id  BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY pk_ccl (parent_id, child_id),
    CONSTRAINT FOREIGN KEY fk_ccl_parent_id (parent_id) REFERENCES comments (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT FOREIGN KEY fk_ccl_child_id (child_id) REFERENCES comments (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

# views


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
         JOIN (SELECT COUNT(id) AS cnt FROM comments GROUP BY user_id) AS c
GROUP BY u.id
ORDER BY activity_score
LIMIT 100;

CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW books_top_activity AS
SELECT b.id                                                                                    AS book_id,
       b.handle                                                                                AS handle,
       ROUND(brl.star * 2, 2)                                                                  AS rating,
       ROUND(SQRT(brl.cnt) + cbl.cnt * 4 + ubl_complete.cnt + ubl_reading.cnt * 0.5 + dbl.cnt) AS activity_score
FROM books AS b
         JOIN (SELECT book_id, COUNT(*) AS cnt, AVG(r.stars) AS star
               FROM book_review_link
                        JOIN reviews r ON book_review_link.review_id = r.id
               GROUP BY book_id) AS brl ON b.id = brl.book_id
         JOIN (SELECT book_id, COUNT(*) AS cnt FROM club_book_link GROUP BY book_id) AS cbl ON b.id = cbl.book_id
         JOIN (SELECT book_id, COUNT(*) AS cnt
               FROM user_book_listing
               WHERE status = 'complete'
               GROUP BY book_id) AS ubl_complete
              ON b.id = ubl_complete.book_id
         JOIN (SELECT book_id, COUNT(*) AS cnt
               FROM user_book_listing
               WHERE status = 'reading'
               GROUP BY book_id) AS ubl_reading
              ON b.id = ubl_reading.book_id
         JOIN (SELECT book_id, COUNT(*) AS cnt FROM discussion_book_link GROUP BY book_id) AS dbl ON b.id = dbl.book_id;

CREATE DEFINER = 'bk_read_only'@'localhost' SQL SECURITY DEFINER VIEW books_top_rating AS
SELECT b.id                       AS book_id,
       b.handle                   AS handle,
       ROUND(AVG(r.stars) * 2, 2) AS rating
FROM books b
         JOIN book_review_link brl ON b.id = brl.book_id
         JOIN reviews r ON brl.review_id = r.id
GROUP BY b.id
ORDER BY rating
LIMIT 100;

# Prototype

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




