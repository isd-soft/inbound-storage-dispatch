CREATE SEQUENCE locations_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS locations (
    id bigint DEFAULT nextval('locations_sequence') PRIMARY KEY,
    location_code varchar(50) NOT NULL UNIQUE,
    zone varchar(50),
    description varchar(255),
    available boolean DEFAULT true NOT NULL,
    is_active boolean DEFAULT TRUE NOT NULL
);
