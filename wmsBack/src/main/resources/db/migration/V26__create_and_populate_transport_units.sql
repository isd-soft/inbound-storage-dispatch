CREATE SEQUENCE transport_units_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS transport_units (
                                               id bigint DEFAULT nextval('transport_units_sequence') PRIMARY KEY,
                                               barcode VARCHAR(50) NOT NULL UNIQUE,
                                               order_id bigint REFERENCES orders(id),
                                               replenishment_id bigint REFERENCES replenishments(id),
                                               created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                               updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_transport_units_barcode ON transport_units(barcode);

INSERT INTO transport_units (barcode, order_id, replenishment_id, created_at, updated_at)
VALUES
    ('TU100001', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TU100002', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TU100003', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TU100004', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TU100005', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TU100006', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TU100007', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TU100008', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TU100009', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TU100010', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (barcode) DO NOTHING;
