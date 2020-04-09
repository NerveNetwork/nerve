package io.nuls.network;

import io.nuls.network.model.dto.NetTimeUrl;
import io.nuls.network.utils.LoggerUtil;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class TimeManagerTest implements Runnable {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void run() {

    }

    /**
     * 时间服务器地址
     */
    private List<String> timeSeverUrlList = new ArrayList<>();
    String urlStr = "africa.pool.ntp.org,antarctica.pool.ntp.org,asia.pool.ntp.org,europe.pool.ntp.org,north-america.pool.ntp.org,oceania.pool.ntp.org,south-america.pool.ntp.org,ntp.aliyun.com,time.windows.com,time.apple.com,time1.cloud.tencent.com,time.asia.apple.com,time.euro.apple.com,time.cloudflare.com,time.google.com";

    public static void main(String[] args) {
        TimeManagerTest test = new TimeManagerTest();
        String[] urls = test.urlStr.split(",");
        for (String url : urls) {
            test.timeSeverUrlList.add(url);
        }
        test.syncWebTime();
    }

    public void syncWebTime() {
        for (int i = 0; i < timeSeverUrlList.size(); i++) {
            long localStartTime = System.currentTimeMillis();
            long ntpTime = getWebTime(timeSeverUrlList.get(i));
            if (ntpTime == 0) {
                continue;
            }
            long localEndTime = System.currentTimeMillis();
            long time = (ntpTime + (localEndTime - localStartTime) / 2) - localEndTime;
            NetTimeUrl netTimeUrl = new NetTimeUrl(timeSeverUrlList.get(i), ntpTime);

            System.out.println("---" + netTimeUrl.getUrl() + "----" + sdf.format(netTimeUrl.getTime()) + "----");
        }


    }

    private long getWebTime(String address) {
        try {
            NTPUDPClient client = new NTPUDPClient();
            client.open();
            client.setDefaultTimeout(500);
            client.setSoTimeout(500);
            InetAddress inetAddress = InetAddress.getByName(address);
            //Log.debug("start ask time....");
            TimeInfo timeInfo = client.getTime(inetAddress);
            //Log.debug("done!");
            return timeInfo.getMessage().getTransmitTimeStamp().getTime();
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.warn("address={} sync time fail", address);
            return 0L;
        }
    }
}
