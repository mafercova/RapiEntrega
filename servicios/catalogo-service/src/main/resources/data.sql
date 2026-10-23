INSERT INTO restaurante SELECT 1, 'Cocina Universitaria', TRUE, 19.4326, -99.1332 WHERE NOT EXISTS (SELECT 1 FROM restaurante WHERE id=1);
INSERT INTO producto SELECT 1, 1, 'Tacos de pollo', 65.50, TRUE WHERE NOT EXISTS (SELECT 1 FROM producto WHERE id=1);
