package uk.gov.pay.publicauth.util;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RandomIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * This method will generate a Base32Hex random string.
     * This works by choosing 130 bits from a cryptographically secure random bit generator,
     * and encoding them in base-32-Hex.
     * <p> 128 bits is considered to be cryptographically strong,
     * but each digit in a base 32 number can encode 5 bits, so 128 is rounded up to the next multiple of 5.
     * This encoding is compact and efficient, with 5 random bits per character. Compare this to a random UUID,
     * which only has 3.4 bits per character in standard layout, and only 122 random bits in total </p>
     *
     * @return a random number in base32 ((0-9 a-v) in string format)
     */
    public static String newId() {
        return new BigInteger(130, RANDOM).toString(32);
    }
}
