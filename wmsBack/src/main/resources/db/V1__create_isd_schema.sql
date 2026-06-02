CREATE TABLE IF NOT EXISTS user_profile (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username varchar(50) UNIQUE,
    email varchar(100) UNIQUE,
    password varchar(255) NOT NULL,
    user_role varchar(30) NOT NULL
    );

CREATE TABLE IF NOT EXISTS category (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(50) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS product (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(100) NOT NULL,
    description varchar(255),
    category_id bigint REFERENCES category(id)
    );

CREATE TABLE IF NOT EXISTS location (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location_code varchar(50) NOT NULL UNIQUE,
    zone varchar(50),
    description varchar(255),
    available boolean DEFAULT true NOT NULL
    );

CREATE TABLE IF NOT EXISTS stock (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    SKU varchar(100) NOT NULL,
    product_id bigint REFERENCES product(id),
    location_id bigint REFERENCES location(id),
    quantity integer DEFAULT 0 NOT NULL CHECK (quantity >= 0)
    );

CREATE TABLE IF NOT EXISTS replenishment_task (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id bigint REFERENCES product(id),
    operator_id bigint REFERENCES user_profile(id),
    requested_quantity integer NOT NULL,
    status varchar(30) NOT NULL,
    source_location_id bigint REFERENCES location(id),
    destination_location_id bigint REFERENCES location(id)
    );

CREATE TABLE IF NOT EXISTS inventory_history (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id bigint REFERENCES product(id),
    SKU varchar(100) NOT NULL,
    altered_quantity integer NOT NULL,
    quantity_after_change integer NOT NULL,
    source_location_id bigint REFERENCES location(id),
    destination_location_id bigint REFERENCES location(id),
    operation_type varchar(50) NOT NULL,
    timestamp timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id bigint REFERENCES user_profile(id)
    );

CREATE TABLE IF NOT EXISTS picking_order (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    timestamp timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status varchar(30) NOT NULL
    );

CREATE TABLE IF NOT EXISTS picking_order_item (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    picking_order_id bigint REFERENCES picking_order(id),
    product_id bigint REFERENCES product(id),
    required_quantity integer NOT NULL,
    confirmed_quantity integer DEFAULT 0 NOT NULL
    );

CREATE TABLE IF NOT EXISTS picking_order_task (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    picking_order_id bigint REFERENCES picking_order(id),
    operator_id bigint REFERENCES user_profile(id),
    task_type varchar(50),
    status varchar(30) NOT NULL
    );