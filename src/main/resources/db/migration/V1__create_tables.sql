CREATE TABLE products (
    product_id   VARCHAR(50)    PRIMARY KEY,
    name         VARCHAR(255)   NOT NULL,
    product_type VARCHAR(20)    NOT NULL,
    price        DECIMAL(10, 2) NOT NULL,
    stock_qty    INTEGER,
    active       BOOLEAN        DEFAULT true,
    metadata     TEXT
);

CREATE TABLE orders (
    order_id       VARCHAR(36)    PRIMARY KEY,
    customer_id    VARCHAR(100)   NOT NULL,
    status         VARCHAR(30)    NOT NULL,
    failure_reason VARCHAR(100),
    total_amount   DECIMAL(10, 2) NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL
);

CREATE TABLE order_items (
    id           BIGSERIAL       PRIMARY KEY,
    order_id     VARCHAR(36)     NOT NULL REFERENCES orders(order_id),
    product_id   VARCHAR(50)     NOT NULL,
    product_type VARCHAR(20)     NOT NULL,
    quantity     INTEGER         NOT NULL,
    unit_price   DECIMAL(10, 2)  NOT NULL,
    metadata     TEXT
);
