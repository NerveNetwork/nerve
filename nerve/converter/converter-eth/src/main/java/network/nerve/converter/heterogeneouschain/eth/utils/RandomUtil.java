package network.nerve.converter.heterogeneouschain.eth.utils;

/**
 * Created by ln on 2018-06-17.
 */
public class RandomUtil {
    /**
     * 生成一个长整形随机数
     *
     * @return long
     */
    public static long randomLong() {
        return (long) (Math.random() * Long.MAX_VALUE);
    }
}
