package edu.rogachova.common;

import java.math.BigInteger;
import java.security.MessageDigest;

public class Crypt
{
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-224");
            byte[] messageDigest = md.digest((password).getBytes());

            BigInteger num = new BigInteger(1, messageDigest);
            String hashedPassword = num.toString(16);

            // Добавить вначале 0, чтобы сделать 32-битным
            while (hashedPassword.length() < 32) {
                hashedPassword = "0" + hashedPassword;
            }
            return hashedPassword;
        }catch (Exception e){
            e.printStackTrace();
        }
        return password;
    }
}
