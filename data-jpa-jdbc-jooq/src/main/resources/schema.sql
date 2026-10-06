CREATE TABLE orders (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer VARCHAR(50) NOT NULL,
    total    DECIMAL(10, 2) NOT NULL
);
INSERT INTO orders (customer, total) VALUES ('ada', 10.00), ('ada', 25.50), ('bob', 7.25);
