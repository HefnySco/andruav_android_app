package com.andruav.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by M.Hefny on 04-Jan-15.
 */
public class Ciphering {

    public static byte[] encrypt(final byte[] raw, final byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES"); //Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

        return cipher.doFinal(clear);
    }

    public static byte[] decrypt(final byte[] raw, final byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES"); // Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);

        return cipher.doFinal(encrypted);
    }

}