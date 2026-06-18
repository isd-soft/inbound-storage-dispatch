ALTER TABLE stocks
ADD CONSTRAINT location_unique UNIQUE(location_id);

ALTER TABLE stocks
ALTER COLUMN location_id SET NOT NULL;

ALTER TABLE stocks
ALTER COLUMN product_id SET NOT NULL;
