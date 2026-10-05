package util;

import java.sql.SQLException;

/** Unchecked wrapper around SQLException that carries a message a normal user can understand. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String action, SQLException cause) {
        super(friendly(action, cause), cause);
    }

    private static String friendly(String action, SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        switch (state) {
            case "23505": return "Duplicate record: a record with the same unique value (e.g. username, plate number, email or license) already exists.";
            case "23503": return "This record is linked to other records (foreign key), so it cannot be added/changed/deleted until the related records are handled.";
            case "23513": return "One of the values is not allowed (it violates a database check rule). Please review the fields.";
            case "23502": return "A required field was left empty.";
            case "22001": return "One of the values is too long for its field.";
            case "XJ040": case "XJ041": case "XSDB6": case "08001": case "08004": case "40000":
                return "Cannot connect to the database. Make sure no other copy of the program is running, then try again.";
            default: return "Database error while trying to " + action + ": " + e.getMessage();
        }
    }
}
