package at.technikum.swendoc.user;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2 from the JDK, so no crypto dependency is needed.
 * Stored form is {@code base64(salt):base64(digest)}.
 */
final class Passwords {

    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Passwords() {
    }

    static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(salt) + ":" + encoder.encodeToString(derive(password, salt));
    }

    static boolean matches(String password, String stored) {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] salt = decoder.decode(parts[0]);
        byte[] expected = decoder.decode(parts[1]);
        return MessageDigest.isEqual(expected, derive(password, salt));
    }

    private static byte[] derive(String password, byte[] salt) {
        try {
            var spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        }
    }
}
