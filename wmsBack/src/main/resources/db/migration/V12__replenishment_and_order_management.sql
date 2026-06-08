ALTER TABLE stocks ADD COLUMN IF NOT EXISTS reserved_quantity INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL;

DROP TABLE IF EXISTS picking_order_tasks CASCADE;
DROP TABLE IF EXISTS picking_order_items CASCADE;
DROP TABLE IF EXISTS picking_orders CASCADE;
DROP TABLE IF EXISTS replenishment_tasks CASCADE;


CREATE SEQUENCE IF NOT EXISTS tasks_sequence START 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS orders_sequence START 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS replenishments_sequence START 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS processes_sequence START 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS order_lines_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS tasks (
    id bigint DEFAULT nextval('tasks_sequence') PRIMARY KEY,
    supervisor_id bigint REFERENCES users(id) NOT NULL,
    task_type varchar(20) NOT NULL,
    requested_quantity integer NOT NULL,
    status varchar(30) NOT NULL,
    completed_at timestamptz,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS replenishments (
    id bigint DEFAULT nextval('replenishments_sequence') PRIMARY KEY,
    product_id bigint REFERENCES products(id) NOT NULL,
    task_id bigint REFERENCES tasks(id) UNIQUE NOT NULL,
    requested_quantity integer NOT NULL,
    status varchar(30) NOT NULL,
    destination_location_id bigint REFERENCES locations(id) NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS orders (
    id bigint DEFAULT nextval('orders_sequence') PRIMARY KEY,
    logic_id varchar(50) NOT NULL UNIQUE,
    status varchar(30) NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS order_lines (
    id bigint DEFAULT nextval('order_lines_sequence') PRIMARY KEY,
    order_id bigint REFERENCES orders(id) NOT NULL,
    task_id bigint REFERENCES tasks(id) UNIQUE NOT NULL,
    product_id bigint REFERENCES products(id) NOT NULL,
    requested_quantity integer NOT NULL,
    status varchar(30) NOT NULL,
    destination_location_id bigint REFERENCES locations(id) NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS processes (
    id bigint DEFAULT nextval('processes_sequence') PRIMARY KEY,
    operator_id bigint REFERENCES users(id),
    task_id bigint REFERENCES tasks(id) NOT NULL,
    quantity integer DEFAULT 0 NOT NULL,
    stock_id bigint REFERENCES stocks(id) NOT NULL,
    status varchar(30) NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL
    );