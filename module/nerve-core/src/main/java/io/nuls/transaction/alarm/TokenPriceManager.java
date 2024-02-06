package io.nuls.transaction.alarm;

import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.transaction.manager.ChainManager;
import io.nuls.transaction.rpc.call.LedgerCall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class TokenPriceManager implements Runnable {

    private static final Map<String, Double> priceMap = new HashMap<>();
    private static final Map<String, Integer> decimalsMap = new HashMap<>();
    private static final Map<String, String> symbolMap = new HashMap<>();
    private static String url = "https://assets.nabox.io/api/price/list";
    private static ChainManager chainManager;

    public TokenPriceManager(ChainManager chainManager) {
        this.chainManager = chainManager;
    }

    public static int getDecimals(int chainId, int assetId) {
        Integer val = decimalsMap.get(chainId + "-" + assetId);
        if (null == val && null != chainManager) {
            HashMap map = LedgerCall.getAssetInfo(chainManager.getChain(9), chainId, assetId);
            if (null != map) {
                val = (Integer) map.get("decimalPlace");
                String key = chainId + "-" + assetId;
                decimalsMap.put(key, val);
                symbolMap.put(key, map.get("assetSymbol") + "");
            }
        }
        if (null == val) {
            return -1;
        }
        return val.intValue();
    }

    public static String getSymbol(int chainId, int assetId) {
        String key = chainId + "-" + assetId;
        String val = symbolMap.get(key);
        if (null == val) {
            return key;
        }
        return val;
    }

    public static double getPrice(int chainId, int assetId) {
        Double val = priceMap.get(chainId + "-" + assetId);
        if (val == null) {
            return 0;
        }
        return val.doubleValue();
    }

    @Override
    public void run() {
        try {
            String jsonStr = sendGet(url);
            if (StringUtils.isBlank(jsonStr)) {
                return;
            }
            Map<String, Object> map = JSONUtils.json2map(jsonStr);
            if (null == map) {
                return;
            }
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Double val = Double.parseDouble(entry.getValue() + "");
                priceMap.put(entry.getKey(), val);
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }

    public static String sendGet(String url) {
        String result = "";
        BufferedReader in = null;
        try {
            URL realUrl = new URL(url + "?time=" + System.currentTimeMillis());
            //打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            //设置通用的请求属性
            conn.setRequestProperty("accept", "charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json, text/javascript, */*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            //建立实际的连接
            conn.connect();
            //定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            Log.error(e);
        }
        //使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Log.error(ex);//ex.printStackTrace();
            }
        }
        return result;
    }
}
