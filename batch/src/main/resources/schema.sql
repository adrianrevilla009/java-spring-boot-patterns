CREATE TABLE processed (
    id       BIGINT PRIMARY KEY,
    customer VARCHAR(50) NOT NULL,
    total    DECIMAL(10, 2) NOT NULL
);
