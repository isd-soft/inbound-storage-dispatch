CREATE SEQUENCE users_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS users (
    id bigint DEFAULT nextval('users_sequence') PRIMARY KEY,
    username varchar(50) UNIQUE,
    email varchar(100) NOT NULL UNIQUE,
    password varchar(255) NOT NULL,
    user_role varchar(30) NOT NULL
);
