package rcmobile.andruavmiddlelibrary.factory.util;
import android.os.Build;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Created by M.Hefny on 29-Sep-14.
 * @link "http://stackoverflow.com/questions/5980658/how-to-sha1-hash-a-string-in-android"
 */



public class SHA1 {
        private static String convertToHex(byte[] data) {
            StringBuilder buf = new StringBuilder();
            for (byte b : data) {
                int halfbyte = (b >>> 4) & 0x0F;
                int two_halfs = 0;
                do {
                    buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                    halfbyte = b & 0x0F;
                } while (two_halfs++ < 1);
            }
            return buf.toString();
        }

        public static String Process(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            {
                md.update(text.getBytes(StandardCharsets.ISO_8859_1), 0, text.length());
            }
            else
            {
                md.update(text.getBytes(), 0, text.length());
            }
            byte[] sha1hash = md.digest();
            return convertToHex(sha1hash);
        }



}
