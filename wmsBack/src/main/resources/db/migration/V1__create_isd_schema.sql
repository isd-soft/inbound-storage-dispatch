CREATE SEQUENCE users_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS users (
    id bigint DEFAULT nextval('users_sequence') PRIMARY KEY,
    username varchar(50) UNIQUE,
    email varchar(100) NOT NULL UNIQUE,
    password varchar(255) NOT NULL,
    user_role varchar(30) NOT NULL
    );

CREATE SEQUENCE categories_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS categories (
    id bigint DEFAULT nextval('categories_sequence') PRIMARY KEY,
    name varchar(50) NOT NULL UNIQUE
    );

CREATE SEQUENCE products_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS products (
    id bigint DEFAULT nextval('products_sequence') PRIMARY KEY,
    name varchar(100) NOT NULL,
    description varchar(255),
    category_id bigint REFERENCES categories(id)
    );

CREATE SEQUENCE locations_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS locations (
    id bigint DEFAULT nextval('locations_sequence') PRIMARY KEY,
    location_code varchar(50) NOT NULL UNIQUE,
    zone varchar(50),
    description varchar(255),
    available boolean DEFAULT true NOT NULL
    );

CREATE SEQUENCE stocks_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS stocks (
    id bigint DEFAULT nextval('stocks_sequence') PRIMARY KEY,
    SKU varchar(100) NOT NULL,
    product_id bigint REFERENCES products(id),
    location_id bigint REFERENCES locations(id),
    quantity integer DEFAULT 0 NOT NULL CHECK (quantity >= 0),
    manufacture_date date,
    expiration_date date,
    version BIGINT DEFAULT 0
    );

CREATE SEQUENCE replenishment_tasks_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS replenishment_tasks (
    id bigint DEFAULT nextval('replenishment_tasks_sequence') PRIMARY KEY,
    product_id bigint REFERENCES products(id),
    operator_id bigint REFERENCES users(id),
    requested_quantity integer NOT NULL,
    status varchar(30) NOT NULL,
    source_location_id bigint REFERENCES locations(id),
    destination_location_id bigint REFERENCES locations(id),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL
    );

CREATE SEQUENCE inventory_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS inventory_history (
    id bigint DEFAULT nextval('inventory_sequence') PRIMARY KEY,
    product_id bigint REFERENCES products(id),
    SKU varchar(100) NOT NULL,
    altered_quantity integer NOT NULL,
    quantity_after_change integer NOT NULL,
    source_location_id bigint REFERENCES locations(id),
    destination_location_id bigint REFERENCES locations(id),
    operation_type varchar(50) NOT NULL,
    timestamp timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id bigint REFERENCES users(id)
    );

CREATE SEQUENCE picking_orders_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS picking_orders (
    id bigint DEFAULT nextval('picking_orders_sequence') PRIMARY KEY,
    order_number varchar(50) NOT NULL UNIQUE,
    timestamp timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status varchar(30) NOT NULL
    );

CREATE SEQUENCE picking_order_items_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS picking_order_items (
    id bigint DEFAULT nextval('picking_order_items_sequence') PRIMARY KEY,
    picking_order_id bigint REFERENCES picking_orders(id),
    product_id bigint REFERENCES products(id),
    required_quantity integer NOT NULL,
    confirmed_quantity integer DEFAULT 0 NOT NULL
    );

CREATE SEQUENCE picking_order_tasks_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS picking_order_tasks (
    id bigint DEFAULT nextval('picking_order_tasks_sequence') PRIMARY KEY,
    picking_order_id bigint REFERENCES picking_orders(id),
    operator_id bigint REFERENCES users(id),
    location_id bigint REFERENCES locations(id),
    task_type varchar(50),
    status varchar(30) NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL
    );