/*
This file is only for the main shcema of the database. All other modifications will be run after this with update.sql
*/
CREATE DATABASE test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE test
(
    id          INT         NOT NULL    AUTO_INCREMENT,
    created_at  TIMESTAMP   NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    text        TEXT,
    PRIMARY KEY (id),
    INDEX idx_test_created_at (created_at)
) CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci,
  ENGINE = InnoDB;

CREATE TABLE lining_test
(
    id              INT     NOT NULL,
    additional_text TEXT,
    PRIMARY KEY(id),
    CONSTRAINT FOREIGN KEY fg_lt_id (id) REFERENCES test (id) ON UPDATE RESTRICT ON DELETE CASCADE
) CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci,
  ENGINE = InnoDB;