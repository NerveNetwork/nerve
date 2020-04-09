package io.nuls.api.constant;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @Author: zhoulijun
 * @Time: 2019-11-12 11:27
 * @Description: 功能描述
 */
public enum ReportDataTimeType {


    Day(){
        @Override
        public int getCalendarField() {
            return Calendar.DATE;
        }

        @Override
        public String getPreTimeValue(Date date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE,-1);
            return formatYYYYMMDD(calendar.getTime());
        }


        @Override
        public int modules() {
            return 6;
        }

        @Override
        public int dayNumber() {
            return 1;
        }
    },
    Month
    {
        @Override
        public int getCalendarField() {
            return Calendar.MONTH;
        }

        @Override
        public String getPreTimeValue(Date date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MONTH,-1);
            calendar.set(Calendar.DAY_OF_MONTH,1);
            return formatYYYYMMDD(calendar.getTime());
        }

        @Override
        public int modules() {
            return 360*24;
        }

        @Override
        public int dayNumber() {
            return 30;
        }
    },
    Week {
        @Override
        public int getCalendarField() {
            return Calendar.WEEK_OF_YEAR;
        }

        @Override
        public String getPreTimeValue(Date date) {
            Calendar calendar = Calendar.getInstance(Locale.CHINA);
            calendar.setTime(date);
            calendar.add(Calendar.WEEK_OF_YEAR,-1);
            //从星期一开始
            calendar.set(Calendar.DAY_OF_WEEK,2);
            return formatYYYYMMDD(calendar.getTime());
        }

        @Override
        public int modules() {
            return 360*24;
        }

        @Override
        public int dayNumber() {
            return 7;
        }
    },
    Year {
        @Override
        public int getCalendarField() {
            return Calendar.YEAR;
        }

        @Override
        public String getPreTimeValue(Date date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.YEAR,-1);
            calendar.set(Calendar.DAY_OF_YEAR,1);
            return formatYYYYMMDD(calendar.getTime());
        }

        @Override
        public int modules() {
            return 360*24;
        }

        @Override
        public int dayNumber() {
            return 365;
        }


    };

    public abstract int getCalendarField();

    public static final String YYYYMMDD = "yyyyMMdd";

    public abstract String getPreTimeValue(Date date);

    public Date[] getPreTimeRange(Date date) {
        String preTime = getPreTimeValue(date);
        try {
            return getTimeRange(new SimpleDateFormat(YYYYMMDD).parse(preTime));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public Date[] getTimeRange(Date date) {
        String dayStr = formatYYYYMMDD(date);
        try {
            Date start = new SimpleDateFormat(YYYYMMDD).parse(dayStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            calendar.add(getCalendarField(),1);
            calendar.add(Calendar.SECOND,-1);
            Date end = calendar.getTime();
            return new Date[]{start,end};
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date[0];
    }

    public Date[] getTimeRangeForDay(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE,-this.dayNumber());
        Date start = Day.getTimeRange(calendar.getTime())[0];
        Date end = Day.getPreTimeRange(date)[1];
        return new Date[]{start,end};
    }


    /**
     * 计算触发时间的模数
     * @return
     */
    public abstract int modules();

    public abstract int dayNumber();

    public static void main(String[] args) {
        Date[] range = ReportDataTimeType.Week.getPreTimeRange(new Date());
        System.out.println(new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(range[0]));
        System.out.println(new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(range[1]));
        range = ReportDataTimeType.Day.getPreTimeRange(new Date());
        System.out.println(new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(range[0]));
        System.out.println(new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(range[1]));
        range = ReportDataTimeType.Month.getPreTimeRange(new Date());
        System.out.println(new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(range[0]));
        System.out.println(new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(range[1]));
        Calendar calendar = Calendar.getInstance();
        calendar.add(ReportDataTimeType.Year.getCalendarField(),-1);
        range = ReportDataTimeType.Year.getTimeRange(calendar.getTime());
        System.out.println(new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(range[0]));
        System.out.println(new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(range[1]));


        range = ReportDataTimeType.Week.getPreTimeRange(new Date());
        System.out.println(new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(range[0]));
        System.out.println(new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(range[1]));

        System.out.println();
    }

    public static final String FORMAT_YYYYMMDD = "%1$tY%1$tm%1td";
    public static final String FORMAT_YYYYMMDDHHMM = "%1$tY%1$tm%1td%1$tH%1$tM";
    public static final String FORMAT_YYYYMMDDHHMMSS = "%1$tY%1$tm%1td%1$tH%1$tM%1$tS";

    public static String formatYYYYMMDD(Date date) {
        return String.format("%1$tY%1$tm%1td", date);
    }

    public static String formatYYYYMMDDHHMM(Date date) {
        return String.format("%1$tY%1$tm%1td%1$tH%1$tM", date);
    }

    public static String formatYYYYMMDDHHMMSS(Date date) {
        return String.format("%1$tY%1$tm%1td%1$tH%1$tM%1$tS", date);
    }

}
