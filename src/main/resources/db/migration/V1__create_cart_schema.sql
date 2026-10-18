-- One row per cart line. A cart has no header row: it is simply all rows with the same user_id.
-- offer_id is the supplier_product id and doubles as the API itemId.
CREATE TABLE cart_items (
    user_id     RAW(16)       NOT NULL,
    offer_id    RAW(16)       NOT NULL,
    product_id  RAW(16)       NOT NULL,
    supplier_id RAW(16)       NOT NULL,
    quantity    NUMBER(10)    NOT NULL CHECK (quantity > 0),
    unit_price  NUMBER(19,4)  NOT NULL,
    currency    CHAR(3)       NOT NULL,
    added_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_cart_items PRIMARY KEY (user_id, offer_id),
    -- one line per product per cart; also stops two concurrent adds from creating duplicate lines
    CONSTRAINT uq_cart_items_product UNIQUE (user_id, product_id)
);

-- used by the abandoned-cart purge job
CREATE INDEX idx_cart_items_updated ON cart_items (updated_at);
