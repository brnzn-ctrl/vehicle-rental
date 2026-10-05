package database;

import util.PasswordHasher;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates and repairs the Apache Derby database every time the program starts.
 *
 *  1. CREATE every table that does not exist yet (primary keys, foreign keys, UNIQUE, NOT NULL, DEFAULT, CHECK).
 *  2. UPGRADE a database made by an older version: add any column the program now needs, and rename the
 *     primary key of "returns" / "payments" when an older database used a different name. No data is deleted.
 *  3. SEED a default admin account when the staff table is empty. On a brand-new database it also adds a little
 *     sample data so every screen has something to show (turn that off with SEED_SAMPLE_DATA below).
 *
 * It is safe to run again and again: it only does what is still missing.
 *
 * Tables (all names match what the DAO classes use):
 *   staff, customers, vehicle_categories, vehicles, discount_events, reservations, rentals, returns, payments,
 *   suppliers, supplies, inventory_stock, expenses
 *
 * Relationships (1 ---- many):
 *   vehicle_categories -> vehicles        customers  -> reservations / rentals
 *   vehicles           -> reservations / rentals
 *   discount_events    -> reservations    reservations -> rentals (optional: walk-in rentals have none)
 *   staff -> rentals / returns / payments / expenses
 *   rentals -> returns, rentals -> payments
 *   suppliers -> supplies -> inventory_stock
 */
public final class SchemaInitializer {

    /** First login for a brand-new database. Change the password after your first login (Manage Staff). */
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    /** true = a brand-new database also gets sample categories, vehicles, customers, suppliers and supplies. */
    private static final boolean SEED_SAMPLE_DATA = true;

    private SchemaInitializer() { }

    // ------------------------------------------------------------------------------------------------
    // Table definitions, in the order they must be created (parents before children).
    // ------------------------------------------------------------------------------------------------
    private static final String[][] TABLES = {
        { "VEHICLE_CATEGORIES",
          "CREATE TABLE vehicle_categories ("
        + " category_id   INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " category_name VARCHAR(50) NOT NULL,"
        + " CONSTRAINT pk_vehicle_categories PRIMARY KEY (category_id),"
        + " CONSTRAINT uq_category_name UNIQUE (category_name))" },

        { "STAFF",
          "CREATE TABLE staff ("
        + " staff_id      INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " username      VARCHAR(50)  NOT NULL,"
        + " password_hash VARCHAR(255) NOT NULL,"
        + " first_name    VARCHAR(50)  NOT NULL,"
        + " last_name     VARCHAR(50)  NOT NULL,"
        + " role          VARCHAR(20)  NOT NULL DEFAULT 'employee',"
        + " is_active     BOOLEAN      NOT NULL DEFAULT TRUE,"
        + " CONSTRAINT pk_staff PRIMARY KEY (staff_id),"
        + " CONSTRAINT uq_staff_username UNIQUE (username),"
        + " CONSTRAINT ck_staff_role CHECK (role IN ('admin','employee')))" },

        { "CUSTOMERS",
          "CREATE TABLE customers ("
        + " customer_id    INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " username       VARCHAR(50)  NOT NULL,"
        + " password_hash  VARCHAR(255) NOT NULL,"
        + " first_name     VARCHAR(50)  NOT NULL,"
        + " last_name      VARCHAR(50)  NOT NULL,"
        + " email          VARCHAR(100),"
        + " phone          VARCHAR(20),"
        + " address        VARCHAR(255),"
        + " license_number VARCHAR(50),"
        + " is_active      BOOLEAN NOT NULL DEFAULT TRUE,"
        + " CONSTRAINT pk_customers PRIMARY KEY (customer_id),"
        + " CONSTRAINT uq_customers_username UNIQUE (username),"
        + " CONSTRAINT uq_customers_email UNIQUE (email),"
        + " CONSTRAINT uq_customers_license UNIQUE (license_number))" },

        { "SUPPLIERS",
          "CREATE TABLE suppliers ("
        + " supplier_id    INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " supplier_name  VARCHAR(100) NOT NULL,"
        + " contact_number VARCHAR(30),"
        + " address        VARCHAR(255),"
        + " CONSTRAINT pk_suppliers PRIMARY KEY (supplier_id))" },

        { "DISCOUNT_EVENTS",
          "CREATE TABLE discount_events ("
        + " discount_id      INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " event_name       VARCHAR(100) NOT NULL,"
        + " start_date       DATE NOT NULL,"
        + " end_date         DATE NOT NULL,"
        + " discount_percent DECIMAL(5,2) NOT NULL,"
        + " is_active        BOOLEAN NOT NULL DEFAULT TRUE,"
        + " CONSTRAINT pk_discount_events PRIMARY KEY (discount_id),"
        + " CONSTRAINT ck_discount_percent CHECK (discount_percent >= 0 AND discount_percent <= 100),"
        + " CONSTRAINT ck_discount_dates CHECK (end_date >= start_date))" },

        { "VEHICLES",
          "CREATE TABLE vehicles ("
        + " vehicle_id     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " plate_number   VARCHAR(20)  NOT NULL,"
        + " vehicle_name   VARCHAR(100) NOT NULL,"
        + " category_id    INTEGER NOT NULL,"
        + " daily_rate     DECIMAL(10,2) NOT NULL,"
        + " status         VARCHAR(20) NOT NULL DEFAULT 'available',"
        + " image_filename VARCHAR(255),"
        + " CONSTRAINT pk_vehicles PRIMARY KEY (vehicle_id),"
        + " CONSTRAINT uq_vehicles_plate UNIQUE (plate_number),"
        + " CONSTRAINT fk_vehicles_category FOREIGN KEY (category_id) REFERENCES vehicle_categories (category_id),"
        + " CONSTRAINT ck_vehicles_rate CHECK (daily_rate > 0),"
        + " CONSTRAINT ck_vehicles_status CHECK (status IN ('available','rented','maintenance','inactive')))" },

        { "SUPPLIES",
          "CREATE TABLE supplies ("
        + " supply_id     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " supply_name   VARCHAR(100) NOT NULL,"
        + " unit          VARCHAR(30)  NOT NULL,"
        + " supplier_id   INTEGER,"
        + " reorder_level INTEGER NOT NULL DEFAULT 5,"
        + " CONSTRAINT pk_supplies PRIMARY KEY (supply_id),"
        + " CONSTRAINT fk_supplies_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (supplier_id),"
        + " CONSTRAINT ck_supplies_reorder CHECK (reorder_level >= 0))" },

        { "RESERVATIONS",
          "CREATE TABLE reservations ("
        + " reservation_id   INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " customer_id      INTEGER NOT NULL,"
        + " vehicle_id       INTEGER NOT NULL,"
        + " reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
        + " start_date       DATE NOT NULL,"
        + " end_date         DATE NOT NULL,"
        + " status           VARCHAR(20) NOT NULL DEFAULT 'pending',"
        + " discount_id      INTEGER,"
        + " CONSTRAINT pk_reservations PRIMARY KEY (reservation_id),"
        + " CONSTRAINT fk_reservations_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id),"
        + " CONSTRAINT fk_reservations_vehicle  FOREIGN KEY (vehicle_id)  REFERENCES vehicles (vehicle_id),"
        + " CONSTRAINT fk_reservations_discount FOREIGN KEY (discount_id) REFERENCES discount_events (discount_id),"
        + " CONSTRAINT ck_reservations_dates CHECK (end_date >= start_date),"
        + " CONSTRAINT ck_reservations_status CHECK (status IN ('pending','approved','rejected','cancelled','converted')))" },

        { "RENTALS",
          "CREATE TABLE rentals ("
        + " rental_id                INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " reservation_id           INTEGER,"
        + " customer_id              INTEGER NOT NULL,"
        + " vehicle_id               INTEGER NOT NULL,"
        + " staff_id                 INTEGER NOT NULL,"
        + " rent_out_date            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
        + " due_date                 DATE NOT NULL,"
        + " daily_rate_snapshot      DECIMAL(10,2) NOT NULL,"
        + " discount_percent_snapshot DECIMAL(5,2) NOT NULL DEFAULT 0,"
        + " status                   VARCHAR(20) NOT NULL DEFAULT 'ongoing',"
        + " CONSTRAINT pk_rentals PRIMARY KEY (rental_id),"
        + " CONSTRAINT fk_rentals_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (reservation_id),"
        + " CONSTRAINT fk_rentals_customer    FOREIGN KEY (customer_id)    REFERENCES customers (customer_id),"
        + " CONSTRAINT fk_rentals_vehicle     FOREIGN KEY (vehicle_id)     REFERENCES vehicles (vehicle_id),"
        + " CONSTRAINT fk_rentals_staff       FOREIGN KEY (staff_id)       REFERENCES staff (staff_id),"
        + " CONSTRAINT ck_rentals_rate CHECK (daily_rate_snapshot > 0),"
        + " CONSTRAINT ck_rentals_discount CHECK (discount_percent_snapshot >= 0 AND discount_percent_snapshot <= 100),"
        + " CONSTRAINT ck_rentals_status CHECK (status IN ('ongoing','returned','cancelled')))" },

        { "RETURNS",
          "CREATE TABLE returns ("
        + " return_id       INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " rental_id       INTEGER NOT NULL,"
        + " return_date     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
        + " condition_notes VARCHAR(255),"
        + " late_days       INTEGER NOT NULL DEFAULT 0,"
        + " late_fee        DECIMAL(10,2) NOT NULL DEFAULT 0,"
        + " damage_fee      DECIMAL(10,2) NOT NULL DEFAULT 0,"
        + " received_by     INTEGER NOT NULL,"
        + " CONSTRAINT pk_returns PRIMARY KEY (return_id),"
        + " CONSTRAINT fk_returns_rental FOREIGN KEY (rental_id)   REFERENCES rentals (rental_id),"
        + " CONSTRAINT fk_returns_staff  FOREIGN KEY (received_by) REFERENCES staff (staff_id),"
        + " CONSTRAINT ck_returns_fees CHECK (late_days >= 0 AND late_fee >= 0 AND damage_fee >= 0))" },

        { "PAYMENTS",
          "CREATE TABLE payments ("
        + " payment_id     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " rental_id      INTEGER NOT NULL,"
        + " amount         DECIMAL(10,2) NOT NULL,"
        + " payment_method VARCHAR(20) NOT NULL DEFAULT 'cash',"
        + " payment_date   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
        + " processed_by   INTEGER NOT NULL,"
        + " CONSTRAINT pk_payments PRIMARY KEY (payment_id),"
        + " CONSTRAINT fk_payments_rental FOREIGN KEY (rental_id)    REFERENCES rentals (rental_id),"
        + " CONSTRAINT fk_payments_staff  FOREIGN KEY (processed_by) REFERENCES staff (staff_id),"
        + " CONSTRAINT ck_payments_amount CHECK (amount >= 0),"
        + " CONSTRAINT ck_payments_method CHECK (payment_method IN ('cash','card','gcash','bank_transfer')))" },

        { "INVENTORY_STOCK",
          "CREATE TABLE inventory_stock ("
        + " stock_id      INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " supply_id     INTEGER NOT NULL,"
        + " quantity      INTEGER NOT NULL,"
        + " unit_cost     DECIMAL(10,2) NOT NULL DEFAULT 0,"
        + " date_received TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
        + " CONSTRAINT pk_inventory_stock PRIMARY KEY (stock_id),"
        + " CONSTRAINT fk_stock_supply FOREIGN KEY (supply_id) REFERENCES supplies (supply_id),"
        + " CONSTRAINT ck_stock_qty CHECK (quantity >= 0),"
        + " CONSTRAINT ck_stock_cost CHECK (unit_cost >= 0))" },

        { "EXPENSES",
          "CREATE TABLE expenses ("
        + " expense_id       INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,"
        + " expense_category VARCHAR(100) NOT NULL,"
        + " description      VARCHAR(255),"
        + " amount           DECIMAL(10,2) NOT NULL,"
        + " expense_date     DATE NOT NULL,"
        + " recorded_by      INTEGER NOT NULL,"
        + " CONSTRAINT pk_expenses PRIMARY KEY (expense_id),"
        + " CONSTRAINT fk_expenses_staff FOREIGN KEY (recorded_by) REFERENCES staff (staff_id),"
        + " CONSTRAINT ck_expenses_amount CHECK (amount > 0))" },
    };

    /**
     * Columns an OLDER database may be missing. Each row: table, column, definition used with ALTER TABLE ADD COLUMN.
     * (Every definition is nullable or has a DEFAULT, so Derby accepts it on a table that already holds rows.)
     */
    private static final String[][] COLUMNS_TO_ENSURE = {
        { "STAFF",            "IS_ACTIVE",                 "BOOLEAN NOT NULL DEFAULT TRUE" },
        { "STAFF",            "ROLE",                      "VARCHAR(20) NOT NULL DEFAULT 'employee'" },
        { "CUSTOMERS",        "IS_ACTIVE",                 "BOOLEAN NOT NULL DEFAULT TRUE" },
        { "CUSTOMERS",        "ADDRESS",                   "VARCHAR(255)" },
        { "CUSTOMERS",        "LICENSE_NUMBER",            "VARCHAR(50)" },
        { "VEHICLES",         "IMAGE_FILENAME",            "VARCHAR(255)" },
        { "DISCOUNT_EVENTS",  "IS_ACTIVE",                 "BOOLEAN NOT NULL DEFAULT TRUE" },
        { "RESERVATIONS",     "RESERVATION_DATE",          "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" },
        { "RESERVATIONS",     "DISCOUNT_ID",               "INTEGER" },
        { "RENTALS",          "RESERVATION_ID",            "INTEGER" },
        { "RENTALS",          "DISCOUNT_PERCENT_SNAPSHOT", "DECIMAL(5,2) NOT NULL DEFAULT 0" },
        { "RETURNS",          "RETURN_DATE",               "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" },
        { "RETURNS",          "CONDITION_NOTES",           "VARCHAR(255)" },
        { "RETURNS",          "LATE_DAYS",                 "INTEGER NOT NULL DEFAULT 0" },
        { "RETURNS",          "LATE_FEE",                  "DECIMAL(10,2) NOT NULL DEFAULT 0" },
        { "RETURNS",          "DAMAGE_FEE",                "DECIMAL(10,2) NOT NULL DEFAULT 0" },
        { "PAYMENTS",         "PAYMENT_DATE",              "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" },
        { "PAYMENTS",         "PAYMENT_METHOD",            "VARCHAR(20) NOT NULL DEFAULT 'cash'" },
        { "INVENTORY_STOCK",  "DATE_RECEIVED",             "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" },
        { "INVENTORY_STOCK",  "UNIT_COST",                 "DECIMAL(10,2) NOT NULL DEFAULT 0" },
    };

    /** Tables whose primary key MUST have this exact name (the DAO classes select it by name). */
    private static final String[][] PRIMARY_KEYS_TO_ENSURE = {
        { "RETURNS",  "RETURN_ID" },
        { "PAYMENTS", "PAYMENT_ID" },
    };

    // ------------------------------------------------------------------------------------------------
    // Entry point (called from LoginForm.launch())
    // ------------------------------------------------------------------------------------------------

    /**
     * Creates / upgrades / seeds the database.
     * @throws RuntimeException when the database cannot be opened or a table cannot be created
     */
    public static void run() {
        try (Connection c = DatabaseConnection.getConnection()) {
            boolean freshDatabase = !tableExists(c, "STAFF");

            for (String[] t : TABLES) {
                if (!tableExists(c, t[0])) execute(c, t[1]);
            }

            List<String> warnings = new ArrayList<>();
            for (String[] col : COLUMNS_TO_ENSURE) ensureColumn(c, col[0], col[1], col[2], warnings);
            for (String[] pk : PRIMARY_KEYS_TO_ENSURE) ensurePrimaryKeyName(c, pk[0], pk[1], warnings);
            for (String w : warnings) System.err.println("[SchemaInitializer] " + w);

            seed(c, freshDatabase);
        } catch (SQLException e) {
            throw new RuntimeException("Could not prepare the Derby database: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------

    private static void execute(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private static boolean tableExists(Connection c, String table) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getTables(null, null, table, new String[]{ "TABLE" })) {
            return rs.next();
        }
    }

    private static boolean columnExists(Connection c, String table, String column) throws SQLException {
        try (ResultSet rs = c.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        }
    }

    /** ALTER TABLE ... ADD COLUMN, only when the column is missing. A failure is reported, never fatal. */
    private static void ensureColumn(Connection c, String table, String column, String definition, List<String> warnings)
            throws SQLException {
        if (!tableExists(c, table) || columnExists(c, table, column)) return;
        try {
            execute(c, "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException first) {
            // second try without NOT NULL (some Derby versions refuse NOT NULL + DEFAULT on a table that has rows)
            String relaxed = definition.replace(" NOT NULL", "");
            try {
                if (relaxed.equals(definition)) throw first;
                execute(c, "ALTER TABLE " + table + " ADD COLUMN " + column + " " + relaxed);
            } catch (SQLException e) {
                warnings.add("Could not add column " + table + "." + column + ": " + e.getMessage());
            }
        }
    }

    /** If the table's single primary-key column has another name, rename it to the name the DAO expects. */
    private static void ensurePrimaryKeyName(Connection c, String table, String wanted, List<String> warnings)
            throws SQLException {
        if (!tableExists(c, table) || columnExists(c, table, wanted)) return;
        List<String> pkColumns = new ArrayList<>();
        try (ResultSet rs = c.getMetaData().getPrimaryKeys(null, null, table)) {
            while (rs.next()) pkColumns.add(rs.getString("COLUMN_NAME"));
        }
        if (pkColumns.size() != 1) {
            warnings.add(table + " has no " + wanted + " column and no single primary key to rename.");
            return;
        }
        try {
            execute(c, "RENAME COLUMN " + table + "." + pkColumns.get(0) + " TO " + wanted);
        } catch (SQLException e) {
            warnings.add("Could not rename " + table + "." + pkColumns.get(0) + " to " + wanted + ": " + e.getMessage());
        }
    }

    private static int countRows(Connection c, String table) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Seed data (PreparedStatement everywhere; one transaction so it is all-or-nothing)
    // ------------------------------------------------------------------------------------------------

    private static void seed(Connection c, boolean freshDatabase) throws SQLException {
        boolean needAdmin = countRows(c, "staff") == 0;
        boolean sample = freshDatabase && SEED_SAMPLE_DATA;
        if (!needAdmin && !sample) return;

        c.setAutoCommit(false);
        try {
            if (needAdmin) {
                insertStaff(c, DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD, "System", "Administrator", "admin");
            }
            if (sample) {
                insertStaff(c, "employee1", "employee123", "Juan", "Dela Cruz", "employee");
                seedSampleData(c);
            }
            c.commit();
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    private static void insertStaff(Connection c, String username, String password, String first, String last, String role)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO staff(username, password_hash, first_name, last_name, role, is_active) VALUES (?,?,?,?,?,TRUE)")) {
            ps.setString(1, username);
            ps.setString(2, PasswordHasher.hash(password));   // salted PBKDF2, never plain text
            ps.setString(3, first);
            ps.setString(4, last);
            ps.setString(5, role);
            ps.executeUpdate();
        }
    }

    private static void seedSampleData(Connection c) throws SQLException {
        // categories -> remember their generated ids so the vehicles can point at them
        Map<String, Integer> categoryId = new HashMap<>();
        String[] categories = { "Sedan", "SUV", "Van", "Pickup", "Motorcycle" };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO vehicle_categories(category_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            for (String name : categories) {
                ps.setString(1, name);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) categoryId.put(name, keys.getInt(1));
                }
            }
        }

        Object[][] vehicles = {
            // plate, name, category, daily rate
            { "ABC 1234", "Toyota Vios",       "Sedan",      "1500.00" },
            { "DEF 2345", "Honda City",        "Sedan",      "1600.00" },
            { "GHI 3456", "Toyota Fortuner",   "SUV",        "3200.00" },
            { "JKL 4567", "Toyota Hiace",      "Van",        "3800.00" },
            { "MNO 5678", "Ford Ranger",       "Pickup",     "3000.00" },
            { "PQR 6789", "Honda Click 160",   "Motorcycle",  "700.00" },
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO vehicles(plate_number, vehicle_name, category_id, daily_rate, status) VALUES (?,?,?,?,'available')")) {
            for (Object[] v : vehicles) {
                ps.setString(1, (String) v[0]);
                ps.setString(2, (String) v[1]);
                ps.setInt(3, categoryId.get((String) v[2]));
                ps.setBigDecimal(4, new java.math.BigDecimal((String) v[3]));
                ps.executeUpdate();
            }
        }

        // two customers (password for both: customer123)
        String[][] customers = {
            { "maria", "Maria", "Santos", "maria@example.com", "09171234567", "Taguig City", "N01-23-456789" },
            { "pedro", "Pedro", "Reyes",  "pedro@example.com", "09181234567", "Makati City", "N02-34-567890" },
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO customers(username, password_hash, first_name, last_name, email, phone, address, license_number, is_active) "
              + "VALUES (?,?,?,?,?,?,?,?,TRUE)")) {
            for (String[] cu : customers) {
                ps.setString(1, cu[0]);
                ps.setString(2, PasswordHasher.hash("customer123"));
                for (int i = 1; i <= 6; i++) ps.setString(i + 2, cu[i]);
                ps.executeUpdate();
            }
        }

        // suppliers -> supplies -> stock
        Map<String, Integer> supplierId = new HashMap<>();
        String[][] suppliers = {
            { "AutoCare Supplies", "09191112222", "Pasig City" },
            { "FuelMax Trading",   "09193334444", "Mandaluyong City" },
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO suppliers(supplier_name, contact_number, address) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            for (String[] s : suppliers) {
                ps.setString(1, s[0]); ps.setString(2, s[1]); ps.setString(3, s[2]);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) supplierId.put(s[0], keys.getInt(1));
                }
            }
        }

        Map<String, Integer> supplyId = new HashMap<>();
        Object[][] supplies = {
            // name, unit, supplier, reorder level
            { "Engine Oil",     "liters", "AutoCare Supplies", 10 },
            { "Car Wash Soap",  "bottles", "AutoCare Supplies", 5 },
            { "Wiper Blades",   "pcs",    "AutoCare Supplies", 4 },
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO supplies(supply_name, unit, supplier_id, reorder_level) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            for (Object[] s : supplies) {
                ps.setString(1, (String) s[0]);
                ps.setString(2, (String) s[1]);
                ps.setInt(3, supplierId.get((String) s[2]));
                ps.setInt(4, (Integer) s[3]);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) supplyId.put((String) s[0], keys.getInt(1));
                }
            }
        }

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO inventory_stock(supply_id, quantity, unit_cost) VALUES (?,?,?)")) {
            Object[][] stock = { { "Engine Oil", 24, "450.00" }, { "Car Wash Soap", 12, "180.00" }, { "Wiper Blades", 8, "250.00" } };
            for (Object[] s : stock) {
                ps.setInt(1, supplyId.get((String) s[0]));
                ps.setInt(2, (Integer) s[1]);
                ps.setBigDecimal(3, new java.math.BigDecimal((String) s[2]));
                ps.executeUpdate();
            }
        }
    }
}
