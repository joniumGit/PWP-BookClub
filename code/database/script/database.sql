/*
This file is only for the main schema of the database. All other modifications will be run after this with update.sql
*/
DROP DATABASE IF EXISTS test;
CREATE DATABASE test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE test;

CREATE TABLE test
(
    id         INT       NOT NULL AUTO_INCREMENT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    content    TEXT,
    PRIMARY KEY (id),
    INDEX idx_test_created_at (created_at)
) CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci,
  ENGINE = InnoDB;

CREATE TABLE lining_test
(
    id              INT NOT NULL,
    additional_text TEXT,
    PRIMARY KEY (id),
    CONSTRAINT FOREIGN KEY fg_lt_id (id) REFERENCES test (id) ON UPDATE RESTRICT ON DELETE CASCADE
) CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci,
  ENGINE = InnoDB;

INSERT INTO test (content)
VALUES ('Hello World'),
       ('Value 2');

INSERT INTO lining_test (id, additional_text)
SELECT id, 'Additional'
FROM test;

UPDATE lining_test
SET additional_text='Test'
WHERE id = (SELECT id FROM test ORDER BY id DESC LIMIT 1);

SELECT *
FROM test
         JOIN lining_test lt on test.id = lt.id;
