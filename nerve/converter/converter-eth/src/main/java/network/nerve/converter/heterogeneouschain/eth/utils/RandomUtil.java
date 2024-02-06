package network.nerve.converter.heterogeneouschain.eth.utils;

/**
 * Created by ln on 2018-06-17.
 */
public class RandomUtil {
    /**
     * Generate a long integer random number
     *
     * @return long
     */
    public static long randomLong() {
        return (long) (Math.random() * Long.MAX_VALUE);
    }
}
