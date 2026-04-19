package toko.aplikasipos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PasswordUtil {

    private static final String PREFIX = "sha256$";

    private PasswordUtil() {
    }

    public static String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return PREFIX + toHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Gagal memproses password.", e);
        }
    }

    public static boolean verify(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        String hashedInput = hashPassword(rawPassword);
        return storedPassword.equals(rawPassword) || storedPassword.equals(hashedInput);
    }

    public static boolean isHashed(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(PREFIX);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
