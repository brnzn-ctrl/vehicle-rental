-- =====================================================================
--  Vehicle Rental System  -  Apache Derby database script
--  Matches src/database/SchemaInitializer.java (13 tables).
--
--  Run with Derby's ij tool:
--      java -cp "derby.jar;derbytools.jar;derbyshared.jar" org.apache.derby.tools.ij VehicleRentalDB.sql
--  (run it from the folder where the VehicleRentalDB folder should live,
--   i.e. the project root, so the app finds the same database.)
--
--  NOTE: the app also creates all of this by itself on first run. This script
--  is for submitting / inspecting / rebuilding the database manually.
--  Default logins: admin/admin123, employee1/employee123, maria + pedro/customer123
-- =====================================================================

--CONNECT 'jdbc:derby:VehicleRentalDB;create=true';

-- ---------- OPTIONAL: wipe everything first (children before parents) ----------
-- DROP TABLE expenses;        DROP TABLE inventory_stock;  DROP TABLE payments;
-- DROP TABLE returns;         DROP TABLE rentals;          DROP TABLE reservations;
-- DROP TABLE supplies;        DROP TABLE vehicles;         DROP TABLE discount_events;
-- DROP TABLE suppliers;       DROP TABLE customers;        DROP TABLE staff;
-- DROP TABLE vehicle_categories;

-- ====================== TABLES (parents first) ======================

CREATE TABLE vehicle_categories (
  category_id   INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  category_name VARCHAR(50) NOT NULL,
  CONSTRAINT pk_vehicle_categories PRIMARY KEY (category_id),
  CONSTRAINT uq_category_name UNIQUE (category_name));

CREATE TABLE staff (
  staff_id      INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  username      VARCHAR(50)  NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  first_name    VARCHAR(50)  NOT NULL,
  last_name     VARCHAR(50)  NOT NULL,
  role          VARCHAR(20)  NOT NULL DEFAULT 'employee',
  is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_staff PRIMARY KEY (staff_id),
  CONSTRAINT uq_staff_username UNIQUE (username),
  CONSTRAINT ck_staff_role CHECK (role IN ('admin','employee')));

CREATE TABLE customers (
  customer_id    INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  username       VARCHAR(50)  NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  first_name     VARCHAR(50)  NOT NULL,
  last_name      VARCHAR(50)  NOT NULL,
  email          VARCHAR(100),
  phone          VARCHAR(20),
  address        VARCHAR(255),
  license_number VARCHAR(50),
  is_active      BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_customers PRIMARY KEY (customer_id),
  CONSTRAINT uq_customers_username UNIQUE (username),
  CONSTRAINT uq_customers_email UNIQUE (email),
  CONSTRAINT uq_customers_license UNIQUE (license_number));

CREATE TABLE suppliers (
  supplier_id    INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  supplier_name  VARCHAR(100) NOT NULL,
  contact_number VARCHAR(30),
  address        VARCHAR(255),
  CONSTRAINT pk_suppliers PRIMARY KEY (supplier_id));

CREATE TABLE discount_events (
  discount_id      INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  event_name       VARCHAR(100) NOT NULL,
  start_date       DATE NOT NULL,
  end_date         DATE NOT NULL,
  discount_percent DECIMAL(5,2) NOT NULL,
  is_active        BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_discount_events PRIMARY KEY (discount_id),
  CONSTRAINT ck_discount_percent CHECK (discount_percent >= 0 AND discount_percent <= 100),
  CONSTRAINT ck_discount_dates CHECK (end_date >= start_date));

CREATE TABLE vehicles (
  vehicle_id     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  plate_number   VARCHAR(20)  NOT NULL,
  vehicle_name   VARCHAR(100) NOT NULL,
  category_id    INTEGER NOT NULL,
  daily_rate     DECIMAL(10,2) NOT NULL,
  status         VARCHAR(20) NOT NULL DEFAULT 'available',
  image_filename VARCHAR(255),
  CONSTRAINT pk_vehicles PRIMARY KEY (vehicle_id),
  CONSTRAINT uq_vehicles_plate UNIQUE (plate_number),
  CONSTRAINT fk_vehicles_category FOREIGN KEY (category_id) REFERENCES vehicle_categories (category_id),
  CONSTRAINT ck_vehicles_rate CHECK (daily_rate > 0),
  CONSTRAINT ck_vehicles_status CHECK (status IN ('available','rented','maintenance','inactive')));

CREATE TABLE supplies (
  supply_id     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  supply_name   VARCHAR(100) NOT NULL,
  unit          VARCHAR(30)  NOT NULL,
  supplier_id   INTEGER,
  reorder_level INTEGER NOT NULL DEFAULT 5,
  CONSTRAINT pk_supplies PRIMARY KEY (supply_id),
  CONSTRAINT fk_supplies_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (supplier_id),
  CONSTRAINT ck_supplies_reorder CHECK (reorder_level >= 0));

CREATE TABLE reservations (
  reservation_id   INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  customer_id      INTEGER NOT NULL,
  vehicle_id       INTEGER NOT NULL,
  reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  start_date       DATE NOT NULL,
  end_date         DATE NOT NULL,
  status           VARCHAR(20) NOT NULL DEFAULT 'pending',
  discount_id      INTEGER,
  CONSTRAINT pk_reservations PRIMARY KEY (reservation_id),
  CONSTRAINT fk_reservations_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id),
  CONSTRAINT fk_reservations_vehicle  FOREIGN KEY (vehicle_id)  REFERENCES vehicles (vehicle_id),
  CONSTRAINT fk_reservations_discount FOREIGN KEY (discount_id) REFERENCES discount_events (discount_id),
  CONSTRAINT ck_reservations_dates CHECK (end_date >= start_date),
  CONSTRAINT ck_reservations_status CHECK (status IN ('pending','approved','rejected','cancelled','converted')));

CREATE TABLE rentals (
  rental_id                 INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  reservation_id            INTEGER,
  customer_id               INTEGER NOT NULL,
  vehicle_id                INTEGER NOT NULL,
  staff_id                  INTEGER NOT NULL,
  rent_out_date             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  due_date                  DATE NOT NULL,
  daily_rate_snapshot       DECIMAL(10,2) NOT NULL,
  discount_percent_snapshot DECIMAL(5,2) NOT NULL DEFAULT 0,
  status                    VARCHAR(20) NOT NULL DEFAULT 'ongoing',
  CONSTRAINT pk_rentals PRIMARY KEY (rental_id),
  CONSTRAINT fk_rentals_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (reservation_id),
  CONSTRAINT fk_rentals_customer    FOREIGN KEY (customer_id)    REFERENCES customers (customer_id),
  CONSTRAINT fk_rentals_vehicle     FOREIGN KEY (vehicle_id)     REFERENCES vehicles (vehicle_id),
  CONSTRAINT fk_rentals_staff       FOREIGN KEY (staff_id)       REFERENCES staff (staff_id),
  CONSTRAINT ck_rentals_rate CHECK (daily_rate_snapshot > 0),
  CONSTRAINT ck_rentals_discount CHECK (discount_percent_snapshot >= 0 AND discount_percent_snapshot <= 100),
  CONSTRAINT ck_rentals_status CHECK (status IN ('ongoing','returned','cancelled')));

CREATE TABLE returns (
  return_id       INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  rental_id       INTEGER NOT NULL,
  return_date     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  condition_notes VARCHAR(255),
  late_days       INTEGER NOT NULL DEFAULT 0,
  late_fee        DECIMAL(10,2) NOT NULL DEFAULT 0,
  damage_fee      DECIMAL(10,2) NOT NULL DEFAULT 0,
  received_by     INTEGER NOT NULL,
  CONSTRAINT pk_returns PRIMARY KEY (return_id),
  CONSTRAINT fk_returns_rental FOREIGN KEY (rental_id)   REFERENCES rentals (rental_id),
  CONSTRAINT fk_returns_staff  FOREIGN KEY (received_by) REFERENCES staff (staff_id),
  CONSTRAINT ck_returns_fees CHECK (late_days >= 0 AND late_fee >= 0 AND damage_fee >= 0));

CREATE TABLE payments (
  payment_id     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  rental_id      INTEGER NOT NULL,
  amount         DECIMAL(10,2) NOT NULL,
  payment_method VARCHAR(20) NOT NULL DEFAULT 'cash',
  payment_date   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_by   INTEGER NOT NULL,
  CONSTRAINT pk_payments PRIMARY KEY (payment_id),
  CONSTRAINT fk_payments_rental FOREIGN KEY (rental_id)    REFERENCES rentals (rental_id),
  CONSTRAINT fk_payments_staff  FOREIGN KEY (processed_by) REFERENCES staff (staff_id),
  CONSTRAINT ck_payments_amount CHECK (amount >= 0),
  CONSTRAINT ck_payments_method CHECK (payment_method IN ('cash','card','gcash','bank_transfer')));

CREATE TABLE inventory_stock (
  stock_id      INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  supply_id     INTEGER NOT NULL,
  quantity      INTEGER NOT NULL,
  unit_cost     DECIMAL(10,2) NOT NULL DEFAULT 0,
  date_received TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_inventory_stock PRIMARY KEY (stock_id),
  CONSTRAINT fk_stock_supply FOREIGN KEY (supply_id) REFERENCES supplies (supply_id),
  CONSTRAINT ck_stock_qty CHECK (quantity >= 0),
  CONSTRAINT ck_stock_cost CHECK (unit_cost >= 0));

CREATE TABLE expenses (
  expense_id       INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
  expense_category VARCHAR(100) NOT NULL,
  description      VARCHAR(255),
  amount           DECIMAL(10,2) NOT NULL,
  expense_date     DATE NOT NULL,
  recorded_by      INTEGER NOT NULL,
  CONSTRAINT pk_expenses PRIMARY KEY (expense_id),
  CONSTRAINT fk_expenses_staff FOREIGN KEY (recorded_by) REFERENCES staff (staff_id),
  CONSTRAINT ck_expenses_amount CHECK (amount > 0));

-- ====================== SAMPLE DATA ======================
-- Passwords are salted PBKDF2 hashes in the same format the app uses (PasswordHasher).

INSERT INTO staff (username, password_hash, first_name, last_name, role, is_active) VALUES
  ('admin',     'pbkdf2$65536$oFn+Sli/WkZEuw/v68vZ7Q==$2TU7cf6K6nKtE0Enu8Jk+Sj7wvH9ZbCVjnA4ffj6yqg=', 'System', 'Administrator', 'admin',    TRUE),
  ('employee1', 'pbkdf2$65536$hvVCIg1Y73BrjG+U5m4Bog==$Q5I8nYEKgp10SLoTNYJAnSj0xfyboymbzP3l3d7u2Jg=',   'Juan',   'Dela Cruz',     'employee', TRUE);

INSERT INTO vehicle_categories (category_name) VALUES
  ('Sedan'), ('SUV'), ('Van'), ('Pickup'), ('Motorcycle');

INSERT INTO vehicles (plate_number, vehicle_name, category_id, daily_rate, status) VALUES
  ('ABC 1234', 'Toyota Vios',     (SELECT category_id FROM vehicle_categories WHERE category_name='Sedan'),      1500.00, 'available'),
  ('DEF 2345', 'Honda City',      (SELECT category_id FROM vehicle_categories WHERE category_name='Sedan'),      1600.00, 'available'),
  ('GHI 3456', 'Toyota Fortuner', (SELECT category_id FROM vehicle_categories WHERE category_name='SUV'),        3200.00, 'available'),
  ('JKL 4567', 'Toyota Hiace',    (SELECT category_id FROM vehicle_categories WHERE category_name='Van'),        3800.00, 'available'),
  ('MNO 5678', 'Ford Ranger',     (SELECT category_id FROM vehicle_categories WHERE category_name='Pickup'),     3000.00, 'available'),
  ('PQR 6789', 'Honda Click 160', (SELECT category_id FROM vehicle_categories WHERE category_name='Motorcycle'),   700.00, 'available');

INSERT INTO customers (username, password_hash, first_name, last_name, email, phone, address, license_number, is_active) VALUES
  ('maria', 'pbkdf2$65536$F9IJvHPDzAbhojLjFu3v/w==$9rPUFtYDisPtS0LOSZNYh/eL6FpOVwtWCjXhDuRl/sg=', 'Maria', 'Santos', 'maria@example.com', '09171234567', 'Taguig City', 'N01-23-456789', TRUE),
  ('pedro', 'pbkdf2$65536$XrFOAT03keDVuEEEIvAFJg==$XF6zqrr/A52Jq7lQYKuRX6/1n1sbY+S0eb0nMnohsCo=', 'Pedro', 'Reyes',  'pedro@example.com', '09181234567', 'Makati City', 'N02-34-567890', TRUE);

INSERT INTO suppliers (supplier_name, contact_number, address) VALUES
  ('AutoCare Supplies', '09191112222', 'Pasig City'),
  ('FuelMax Trading',   '09193334444', 'Mandaluyong City');

INSERT INTO supplies (supply_name, unit, supplier_id, reorder_level) VALUES
  ('Engine Oil',    'liters',  (SELECT supplier_id FROM suppliers WHERE supplier_name='AutoCare Supplies'), 10),
  ('Car Wash Soap', 'bottles', (SELECT supplier_id FROM suppliers WHERE supplier_name='AutoCare Supplies'),  5),
  ('Wiper Blades',  'pcs',     (SELECT supplier_id FROM suppliers WHERE supplier_name='AutoCare Supplies'),  4);

INSERT INTO inventory_stock (supply_id, quantity, unit_cost) VALUES
  ((SELECT supply_id FROM supplies WHERE supply_name='Engine Oil'),    24, 450.00),
  ((SELECT supply_id FROM supplies WHERE supply_name='Car Wash Soap'), 12, 180.00),
  ((SELECT supply_id FROM supplies WHERE supply_name='Wiper Blades'),   8, 250.00);

-- ====================== QUICK CHECK ======================
SELECT v.plate_number, v.vehicle_name, c.category_name, v.daily_rate
  FROM vehicles v JOIN vehicle_categories c ON c.category_id = v.category_id
 ORDER BY v.vehicle_id;
