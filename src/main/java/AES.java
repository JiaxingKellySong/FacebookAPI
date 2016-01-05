

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;


public class AES {
    public static String encrypt(SecretKey key, String initVector, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(Base64.getDecoder().decode(initVector));

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception ex) {

        }
        return null;
    }

    public static String decrypt(SecretKey key, String initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(Base64.getDecoder().decode(initVector));

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));

            return new String(original);
        } catch (Exception ex) {

        }

        return null;
    }

    /**
     * For unit test only.
     * */
    public static void main(String[] args) {
        String initVector = Util.generateIV(); // 16 bytes IV
        SecretKey secretKey = Util.generateAESKey();
        System.out.println(decrypt(secretKey, initVector,
                encrypt(secretKey, initVector, "Hello World")));
        System.out.println(encrypt(secretKey, initVector, "Hello World"));

        initVector = Util.generateIV(); // 16 bytes IV
        secretKey = Util.generateAESKey();
        System.out.println(decrypt(secretKey, initVector,
                encrypt(secretKey, initVector, "Hello World")));
        System.out.println(encrypt(secretKey, initVector, "Hello World"));
    }
}