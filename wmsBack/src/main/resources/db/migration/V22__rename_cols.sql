ALTER TABLE products
RENAME COLUMN sku TO barcode;

ALTER TABLE inventory_history
    RENAME COLUMN sku TO barcode;

ALTER TABLE locations
RENAME COLUMN location_code TO barcode;

ALTER TABLE locations
ADD COLUMN name VARCHAR(100) DEFAULT 'name' NOT NULL;
