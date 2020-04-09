package io.nuls.api.utils;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-11 11:55
 * @Description: 功能描述
 */
public final class DateUtil {

    /**
     * 一天的毫秒数
     */
    public static final Long ONE_DAY_MILLISECOND = 3600 * 24 * 1000L;

    public static Long getDayStartTimestamp(Long timestamp){
        return timestamp - timestamp % (ONE_DAY_MILLISECOND);
    }

    public static Long getDayStartTimestampBySecond(Long timestamp){
        timestamp = timestamp * 1000L;
        return timestamp - timestamp % (ONE_DAY_MILLISECOND);
    }

    public static Long getDayStartTimestamp(){
        return getDayStartTimestamp(System.currentTimeMillis());
    }

    /**
     * 将秒的时间戳转换成毫秒时间戳
     * @param secondTimestamp
     * @return
     */
    public static Long timeStampSecondToMillisecond(long secondTimestamp){
        return secondTimestamp * 1000L;
    }



}
