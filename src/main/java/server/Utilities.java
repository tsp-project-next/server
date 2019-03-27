package server;

import java.util.Random;

public class Utilities {

    /**
     * generateCode
     * @return a 4 digit random code for user_ids and lobby codes
     */
    public static String generateCode() {
        // Creates an alphanumeric string of length 4
        String alphaNumeric = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"; //SALTCHARS
        StringBuilder codeBuilder = new StringBuilder(); //salt
        Random rand = new Random();
        while (codeBuilder.length() < 4) {
            int index = (int) (rand.nextFloat() * alphaNumeric.length());
            codeBuilder.append(alphaNumeric.charAt(index));
        }
        String code = codeBuilder.toString();

        return code;
    }

}
