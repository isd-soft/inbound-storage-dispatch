CREATE SEQUENCE stocks_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS stocks (
    id bigint DEFAULT nextval('stocks_sequence') PRIMARY KEY,
    product_id bigint REFERENCES products(id) NOT NULL,
    location_id bigint REFERENCES locations(id) NOT NULL UNIQUE,
    quantity integer DEFAULT 0 NOT NULL CHECK (quantity >= 0),
    manufacture_date date,
    expiration_date date,
    version BIGINT DEFAULT 0,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL
);
