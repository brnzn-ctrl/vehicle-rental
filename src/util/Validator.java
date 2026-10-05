package util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Central input validation (required fields, numbers, e-mail, phone, dates, passwords). */
public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\d{11}$");
    public static final int MIN_PASSWORD_LENGTH = 6;

    private Validator() { }

    public static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    /** Empty e-mail is allowed (optional field); otherwise must look like name@domain.tld. */
    public static boolean isValidEmail(String s) { return isBlank(s) || EMAIL.matcher(s.trim()).matches(); }

    /** Empty phone is allowed (optional field); otherwise exactly 11 digits. */
    public static boolean isValidPhone(String s) { return isBlank(s) || PHONE.matcher(s.trim()).matches(); }

    /** Password rule used everywhere a password is created or changed. */
    public static boolean isValidPassword(String s) { return s != null && s.length() >= MIN_PASSWORD_LENGTH; }

    /** Returns the number, or null when the text is not a valid decimal that is 0 or more. */
    public static BigDecimal parseNonNegative(String s) {
        try {
            BigDecimal v = new BigDecimal(s.trim());
            return v.signum() >= 0 ? v : null;
        } catch (Exception e) { return null; }
    }

    /** Returns the number, or null when the text is not a valid decimal greater than 0. */
    public static BigDecimal parsePositive(String s) {
        try {
            BigDecimal v = new BigDecimal(s.trim());
            return v.signum() > 0 ? v : null;
        } catch (Exception e) { return null; }
    }

    /** Returns the whole number, or null when the text is not a whole number that is 0 or more. */
    public static Integer parseNonNegativeInt(String s) {
        try {
            int v = Integer.parseInt(s.trim());
            return v >= 0 ? v : null;
        } catch (Exception e) { return null; }
    }

    /** Returns the whole number, or null when the text is not a whole number greater than 0. */
    public static Integer parsePositiveInt(String s) {
        try {
            int v = Integer.parseInt(s.trim());
            return v > 0 ? v : null;
        } catch (Exception e) { return null; }
    }

    /** Returns the percent (greater than 0, up to 100), or null when invalid. */
    public static BigDecimal parsePercent(String s) {
        BigDecimal v = parsePositive(s);
        return (v != null && v.compareTo(BigDecimal.valueOf(100)) <= 0) ? v : null;
    }

    /** Returns the date, or null when the text is not a valid YYYY-MM-DD date. */
    public static LocalDate parseDate(String s) {
        try { return LocalDate.parse(s.trim()); }
        catch (DateTimeParseException | NullPointerException e) { return null; }
    }
}
