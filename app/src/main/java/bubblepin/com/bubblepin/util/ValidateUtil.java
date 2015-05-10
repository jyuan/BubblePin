package bubblepin.com.bubblepin.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidateUtil {

    /**
     * check whether a given email is a valid email string
     *
     * @param email input email string
     * @return true if input is a valid email, false if not
     */
    public static boolean isValidEmail(String email) {
        String patternString = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /**
     * check whether a given password is a valid password string
     * the password should begin with letter, can only contain digit,
     * letter and underline, with the length [6, 16]
     *
     * @param password input password string
     * @return true if input is a valid password, false if not
     */
    public static boolean isPasswordValid(String password) {
        String patternString = "^[a-zA-Z]\\w{5,17}$";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    // copy from online
    /**
     * by passing a password string, encrypt the password string with MD5 algorithm
     *
     * @param password input password string
     * @return encrypted password string
     * @throws NoSuchAlgorithmException
     */
    public static String getEncryptPassword(String password) throws NoSuchAlgorithmException {
        if (password == null || password.length() == 0) {
            return "";
        }
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(password.getBytes());
        byte[] hashs = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte hash : hashs) {
            if ((0xff & hash) < 0x10) {
                sb.append("0" + Integer.toHexString((0xff & hash)));
            } else {
                sb.append(Integer.toHexString((0xff & hash)));
            }
        }
        return sb.toString();
    }
}
