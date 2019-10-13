package ronan_hanley.dist_sys.grpc_password_service;

import sun.security.util.Password;

import java.util.Base64;

public class PasswordTest {

    public static void main(String[] args) {
        char[] password = "Test".toCharArray();
        byte[] salt = Passwords.getNextSalt();
        byte[] hash = Passwords.hash(password, salt);
        String strHash = new String(hash);
        System.out.println(new String(Base64.getEncoder().encode(hash)));
    }

}
