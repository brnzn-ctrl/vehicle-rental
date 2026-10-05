package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted password hashing (PBKDF2 with HMAC-SHA256).
 *
 * Stored format:  pbkdf2$<iterations>$<salt, Base64>$<hash, Base64>
 * Every password gets its own random salt, so two users with the same password get different hashes.
 *
 * Old accounts created before this class existed still hold a plain unsalted SHA-256 hash (see PasswordUtil).
 * matches() still accepts those, and isLegacy() lets a DAO re-save them in the new format right after a
 * successful login, so existing accounts (including the default admin) upgrade themselves.
 */
public final class PasswordHasher {
    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 65_536;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Turned off only if the database column could not be widened (see SchemaUpgrade). */
    private static volatile boolean saltedEnabled = true;

    private PasswordHasher() { }

    public static void setSaltedEnabled(boolean enabled) { saltedEnabled = enabled; }
    public static boolean isSaltedEnabled() { return saltedEnabled; }

    /** Hash a new password with a fresh random salt. */
    public static String hash(String plain) {
        if (!saltedEnabled) return PasswordUtil.hash(plain);   // fallback: old unsalted format (column too narrow)
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] h = derive(plain.toCharArray(), salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(h);
    }

    /** True when plain matches the stored value (new salted format OR the old unsalted SHA-256 format). */
    public static boolean matches(String plain, String stored) {
        if (plain == null || stored == null) return false;
        if (!isLegacy(stored)) {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) return false;
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                byte[] actual = derive(plain.toCharArray(), salt, iterations);
                return MessageDigest.isEqual(expected, actual);   // constant-time comparison
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return MessageDigest.isEqual(PasswordUtil.hash(plain).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                     stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** True for the old unsalted format (anything that is not "pbkdf2$..."). */
    public static boolean isLegacy(String stored) {
        return stored != null && !stored.startsWith(PREFIX + "$");
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is not available: " + e.getMessage(), e);
        }
    }
}
