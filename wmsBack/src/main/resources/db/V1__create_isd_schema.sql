CREATE TABLE IF NOT EXISTS user_profile (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username varchar(50) UNIQUE,
    email varchar(50) UNIQUE,
    password varchar(50),
    user_role varchar(15)
);

CREATE TABLE IF NOT EXISTS category (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(20) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS product (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(20) NOT NULL,
    description varchar(150),
    category_id integer REFERENCES category(id)
);

CREATE TABLE IF NOT EXISTS location (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location_code varchar(20) NOT NULL UNIQUE,
    zone varchar(20),
    description varchar(150),
    available boolean DEFAULT true NOT NULL
);

CREATE TABLE IF NOT EXISTS stock (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    SKU varchar(50) NOT NULL,
    product_id integer REFERENCES product(id),
    location_id integer REFERENCES location(id),
    quantity integer DEFAULT 0 NOT NULL
);

CREATE TABLE IF NOT EXISTS replenishment_task (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id integer REFERENCES product(id),
    requested_quantity integer NOT NULL,
    status varchar(12) NOT NULL,
    source_location_id integer REFERENCES location(id),
    destination_location_id integer REFERENCES location(id)
);

CREATE TABLE IF NOT EXISTS replenishment_task_history (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id integer REFERENCES product(id),
    SKU varchar(50) NOT NULL,
    altered_quantity integer NOT NULL,
    quantity_after_change integer NOT NULL,
    source_location_id integer REFERENCES location(id),
    destination_location_id integer REFERENCES location(id),
    operation_type varchar(20) NOT NULL,
    timestamp timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id integer REFERENCES user_profile(id)
);

CREATE TABLE IF NOT EXISTS picking_order (
    order_number integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    timestamp timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status varchar(12) NOT NULL
);

CREATE TABLE IF NOT EXISTS picking_order_item (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    picking_order_number integer REFERENCES picking_order(order_number),
    product_id integer REFERENCES product(id),
    required_quantity integer NOT NULL,
    confirmed_quantity integer DEFAULT 0 NOT NULL
);

CREATE TABLE IF NOT EXISTS picking_order_task (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    picking_order_number integer REFERENCES picking_order(order_number),
    operator_id integer REFERENCES user_profile(id),
    task_type varchar(20),
    status varchar(12) NOT NULL
);
