-- Run once on TiDB/MySQL before using image upload feature.
-- Stores product image as base64 text (not a file path).

ALTER TABLE items
  ADD COLUMN image_data LONGTEXT NULL
  COMMENT 'Base64-encoded image bytes';
