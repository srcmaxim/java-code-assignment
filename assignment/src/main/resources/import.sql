INSERT INTO store(id, name, quantityProductsInStock) VALUES (1, 'TONSTAD', 10);
INSERT INTO store(id, name, quantityProductsInStock) VALUES (2, 'KALLAX', 5);
INSERT INTO store(id, name, quantityProductsInStock) VALUES (3, 'BESTÅ', 3);
ALTER SEQUENCE store_seq RESTART WITH 4;

INSERT INTO product(id, name, stock) VALUES (1, 'TONSTAD', 10);
INSERT INTO product(id, name, stock) VALUES (2, 'KALLAX', 5);
INSERT INTO product(id, name, stock) VALUES (3, 'BESTÅ', 3);
ALTER SEQUENCE product_seq RESTART WITH 4;

INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt, archivedAt) 
VALUES (1, 'MWH.001', 'ZWOLLE-001', 100, 10, '2024-07-01', null);
INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt, archivedAt)
VALUES (2, 'MWH.012', 'AMSTERDAM-001', 50, 5, '2023-07-01', null);
INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt, archivedAt)
VALUES (3, 'MWH.023', 'TILBURG-001', 30, 27, '2021-02-01', null);
ALTER SEQUENCE warehouse_seq RESTART WITH 4;

-- Broken DB invariant location:(ZWOLLE-001 maxCapacity: 40) < warehouse:(ZWOLLE-001 capacity: 100)
-- Updated location: ZWOLLE-001 maxCapacity: 40=>100
INSERT INTO location(id, identification, maxNumberOfWarehouses, maxCapacity)
VALUES (1, 'ZWOLLE-001', 1, 100);
INSERT INTO location(id, identification, maxNumberOfWarehouses, maxCapacity)
VALUES (2, 'ZWOLLE-002', 2, 50);
INSERT INTO location(id, identification, maxNumberOfWarehouses, maxCapacity)
VALUES (3, 'AMSTERDAM-001', 5, 100);
INSERT INTO location(id, identification, maxNumberOfWarehouses, maxCapacity)
VALUES (4, 'AMSTERDAM-002', 3, 75);
INSERT INTO location(id, identification, maxNumberOfWarehouses, maxCapacity)
VALUES (5, 'TILBURG-001', 1, 40);
INSERT INTO location(id, identification, maxNumberOfWarehouses, maxCapacity)
VALUES (6, 'HELMOND-001', 1, 45);
INSERT INTO location(id, identification, maxNumberOfWarehouses, maxCapacity)
VALUES (7, 'EINDHOVEN-001', 2, 70);
INSERT INTO location(id, identification, maxNumberOfWarehouses, maxCapacity)
VALUES (8, 'VETSBY-001', 1, 90);
ALTER SEQUENCE warehouse_seq RESTART WITH 9;
