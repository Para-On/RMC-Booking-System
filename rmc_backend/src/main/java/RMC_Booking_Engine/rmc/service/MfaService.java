package RMC_Booking_Engine.rmc.service;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class MfaService {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSecret() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public String buildOtpAuthUrl(String email, String secret) {
        String issuer = urlEncode("RMC Booking");
        String label = urlEncode(email);
        return "otpauth://totp/" + issuer + ":" + label
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=" + CODE_DIGITS + "&period=" + TIME_STEP_SECONDS;
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        String normalized = code.trim();
        if (!normalized.matches("\\d{6}")) {
            return false;
        }
        long counter = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (long window = counter - 1; window <= counter + 1; window++) {
            if (generateCode(secret, window).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String generateCode(String secret, long counter) {
        try {
            byte[] key = decodeBase32(secret);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to generate TOTP code", ex);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                result.append(BASE32_ALPHABET[index]);
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(BASE32_ALPHABET[index]);
        }
        return result.toString();
    }

    private static byte[] decodeBase32(String value) {
        String normalized = value.trim().replace("=", "").toUpperCase();
        byte[] output = new byte[normalized.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char character : normalized.toCharArray()) {
            int charValue = base32Value(character);
            buffer = (buffer << 5) | charValue;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        if (index == output.length) {
            return output;
        }
        byte[] trimmed = new byte[index];
        System.arraycopy(output, 0, trimmed, 0, index);
        return trimmed;
    }

    private static int base32Value(char character) {
        if (character >= 'A' && character <= 'Z') {
            return character - 'A';
        }
        if (character >= '2' && character <= '7') {
            return character - '2' + 26;
        }
        throw new IllegalArgumentException("Invalid base32 character: " + character);
    }
}
