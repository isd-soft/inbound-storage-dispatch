CREATE SEQUENCE order_lines_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS order_lines(
    id bigint DEFAULT nextval('order_lines_sequence') PRIMARY KEY,
    order_id bigint REFERENCES orders(id) NOT NULL,
    task_id bigint REFERENCES tasks(id) NOT NULL,
    product_id bigint REFERENCES products(id) NOT NULL,
    requested_quantity integer NOT NULL,
    status varchar(30) DEFAULT 'CREATED' NOT NULL,
    destination_location_id bigint REFERENCES locations(id) NOT NULL,
    );