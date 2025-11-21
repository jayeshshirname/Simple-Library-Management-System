package library;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Represents a library member with secure password handling.
 * Supports password hashing with PBKDF2 and legacy SHA-256 migration.
 */
public final class Member {
    private static final String PBKDF2_ALG = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITER = 600_000; // Increased from 100k for better security
    private static final int SALT_BYTES = 32; // Increased from 16 for better security
    private static final int KEY_LEN = 256; // bits
    private static final SecureRandom RNG;
    private static final Pattern HEX64 = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    
    static {
        // Initialize SecureRandom with the strongest algorithm available
        SecureRandom strong;
        try {
            strong = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            strong = new SecureRandom();
        }
        RNG = strong;
    }

    private String id;
    private String name;
    private String email;
    private String phone;
    private String passwordHash; // stores scheme-prefixed value for new accounts
    private boolean isAdmin;
    private static final String EMAIL_PATTERN = 
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final Pattern EMAIL_VALIDATOR = Pattern.compile(EMAIL_PATTERN);

    /**
     * Default constructor for serialization/deserialization
     */
    public Member() {}

    /**
     * Creates a new member with the specified details.
     * @param id unique member ID
     * @param name member's full name
     * @param email valid email address
     * @param phone phone number
     * @param password password (will be hashed)
     * @param isAdmin whether member has admin privileges
     * @throws IllegalArgumentException if any required field is invalid
     */
    public Member(String id, String name, String email, String phone, String password, boolean isAdmin) {
        Objects.requireNonNull(id, "Member ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("Member ID cannot be empty");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (!EMAIL_VALIDATOR.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        
        this.id = id.trim();
        this.name = name.trim();
        this.email = email.trim().toLowerCase();
        this.phone = phone == null ? "" : phone.trim();
        this.isAdmin = isAdmin;
        setPassword(password);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    // kept for DAO usage; prefer not to expose externally
    public String getPasswordHash() { return passwordHash; }
    public boolean isAdmin() { return isAdmin; }

    public void setPassword(String password) {
        this.passwordHash = createPbkdf2Hash(password == null ? "" : password);
    }

    public String toCSV() {
        return CsvUtil.join(id, name, email, phone, passwordHash, String.valueOf(isAdmin));
    }

    public static Member fromCSV(String line) {
        List<String> fields = CsvUtil.parseLine(line);
        Member m = new Member();
        m.id = !fields.isEmpty() ? CsvUtil.unescape(fields.get(0)) : null;
        m.name = fields.size() > 1 ? CsvUtil.unescape(fields.get(1)) : null;
        m.email = fields.size() > 2 ? CsvUtil.unescape(fields.get(2)) : null;
        m.phone = fields.size() > 3 ? CsvUtil.unescape(fields.get(3)) : null;
        m.passwordHash = fields.size() > 4 ? CsvUtil.unescape(fields.get(4)) : null;
        m.isAdmin = fields.size() > 5 && Boolean.parseBoolean(CsvUtil.unescape(fields.get(5)));
        return m;
    }

    /**
     * Checks password. Supports legacy SHA-256 hex (no prefix) and prefixed "sha256$..." entries.
     * On successful verification of a legacy SHA-256 entry, this method will upgrade the stored hash
     * to the PBKDF2 format in-memory so callers can persist the migrated value.
     */
    public boolean checkPassword(String plain) {
        String stored = this.passwordHash;
        if (stored == null) return false;

        try {
            if (stored.startsWith("pbkdf2$")) {
                // format: pbkdf2$iterations$base64salt$base64hash
                String[] parts = stored.split("\\$", 4);
                if (parts.length != 4) return false;
                int iter = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                byte[] got = pbkdf2(plain == null ? "" : plain, salt, iter, expected.length * 8);
                return MessageDigest.isEqual(expected, got);
            } else if (stored.startsWith("sha256$")) {
                String hex = stored.substring("sha256$".length());
                String h = sha256Hex(plain);
                if (h.equalsIgnoreCase(hex)) {
                    // migrate
                    this.passwordHash = createPbkdf2Hash(plain == null ? "" : plain);
                    return true;
                }
                return false;
            } else if (HEX64.matcher(stored).matches()) {
                // legacy raw hex sha256
                String h = sha256Hex(plain);
                if (h.equalsIgnoreCase(stored)) {
                    this.passwordHash = createPbkdf2Hash(plain == null ? "" : plain);
                    return true;
                }
                return false;
            } else {
                // unknown format
                return false;
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error verifying password", e);
        } catch (IllegalArgumentException e) {
            return false; // Invalid base64 or integer format
        }
    }

    private static String createPbkdf2Hash(String password) {
        try {
            byte[] salt = new byte[SALT_BYTES];
            RNG.nextBytes(salt);
            byte[] dk = pbkdf2(password, salt, PBKDF2_ITER, KEY_LEN);
            String bSalt = Base64.getEncoder().encodeToString(salt);
            String bHash = Base64.getEncoder().encodeToString(dk);
            return String.format("pbkdf2$%d$%s$%s", PBKDF2_ITER, bSalt, bHash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error creating password hash", e);
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations, int keyLenBits) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLenBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALG);
        return skf.generateSecret(spec).getEncoded();
    }

    private static String sha256Hex(String input) {
        try {
            if (input == null) input = "";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | admin=%b", id, name, email, isAdmin);
    }
}
