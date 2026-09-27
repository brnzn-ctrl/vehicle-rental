public class ExpenseDAO extends AbstractDAO<Expense> {

    private static final String BASE_SELECT =
        "SELECT e.expense_id, e.expense_category, e.description, e.amount, e.expense_date, " +
        "       e.recorded_by, st.first_name, st.last_name " +
        "FROM expenses e INNER JOIN staff st ON e.recorded_by = st.staff_id";

    @Override protected String selectAllSql()  { return BASE_SELECT + " ORDER BY e.expense_date DESC"; }
    @Override protected String selectByIdSql() { return BASE_SELECT + " WHERE e.expense_id=?"; }
    @Override protected String deleteSql()     { return "DELETE FROM expenses WHERE expense_id=?"; }

    @Override protected String insertSql() {
        return "INSERT INTO expenses(expense_category, description, amount, expense_date, recorded_by) VALUES (?,?,?,?,?)";
    }
    @Override protected String updateSql() {
        return "UPDATE expenses SET expense_category=?, description=?, amount=?, expense_date=?, recorded_by=? WHERE expense_id=?";
    }

    @Override protected RowMapper<Expense> mapper() {
        return rs -> {
            Expense e = new Expense();
            e.id = rs.getInt("expense_id");
            e.category = rs.getString("expense_category");
            e.description = rs.getString("description");
            e.amount = rs.getBigDecimal("amount");
            e.expenseDate = rs.getDate("expense_date").toLocalDate();
            e.recordedBy = rs.getInt("recorded_by");
            e.recordedByName = rs.getString("first_name") + " " + rs.getString("last_name");
            return e;
        };
    }

    @Override protected ParamBinder<Expense> insertBinder() {
        return (ps, e) -> {
            ps.setString(1, e.category);
            ps.setString(2, e.description);
            ps.setBigDecimal(3, e.amount);
            ps.setDate(4, java.sql.Date.valueOf(e.expenseDate));
            ps.setInt(5, e.recordedBy);
        };
    }

    @Override protected ParamBinder<Expense> updateBinder() {
        return (ps, e) -> {
            ps.setString(1, e.category);
            ps.setString(2, e.description);
            ps.setBigDecimal(3, e.amount);
            ps.setDate(4, java.sql.Date.valueOf(e.expenseDate));
            ps.setInt(5, e.recordedBy);
            ps.setInt(6, e.id);
        };
    }
}
