package ronan_hanley.dist_sys.grpc_password_service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Random;

public class Passwords {
    private Random random;
    private int iterations;
    private int keyLength;
    private int saltLength;

    public Passwords(int iterations, int keyLength, int saltLength) {
        this.iterations = iterations;
        this.keyLength = keyLength;
        this.saltLength = saltLength;

        random = new SecureRandom();
    }

    /**
     * Returns a random salt to be used to hash a password.
     *
     * @return a random salt
     */
    public byte[] getNextSalt() {
        byte[] salt = new byte[saltLength];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Returns a salted and hashed password using the provided hash.<br>
     *
     * @param password the password to be hashed
     * @param salt a salt, ideally obtained with the getNextSalt method
     * @return the hashed password with a pinch of salt
     */
    public byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Returns true if the given password and salt match the hashed value, false otherwise.<br>
     *
     * @param password     the password to check
     * @param salt         the salt used to hash the password
     * @param expectedHash the expected hashed value of the password
     * @return true if the given password and salt match the hashed value, false otherwise
     */
    public boolean isExpectedPassword(char[] password, byte[] salt, byte[] expectedHash) {
        byte[] pwdHash = hash(password, salt);
        return Arrays.equals(pwdHash, expectedHash);
    }
}