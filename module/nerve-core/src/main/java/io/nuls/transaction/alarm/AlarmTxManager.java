package io.nuls.transaction.alarm;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.DateUtils;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.transaction.manager.ChainManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class AlarmTxManager implements InitializingBean, Runnable {

    private static final BlockingQueue<Block> queue = new LinkedBlockingQueue<>();
    private static final List<Integer> typeList = new ArrayList<>();
    private static final BigDecimal biaozhun = BigDecimal.valueOf(1000);
    private static final String scanBaseUrl = "https://scan.nerve.network/transaction/info?hash=";
    private static final String pk = "989f28d4ac90899ba94dc50efd765f99b27393820212170a9f4f7cd869f2b691";
    public static String msgUrl = "https://wx.niels.wang";

    @Autowired
    private ChainManager chainManager;

    public static void offer(BlockHeader blockHeader, List<Transaction> txList) {
        Block block = new Block();
        block.setHeader(blockHeader);
        block.setTxs(txList);
        queue.offer(block);
    }

    @Override
    public void afterPropertiesSet() throws NulsException {
        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(2);
        pool.scheduleWithFixedDelay(new TokenPriceManager(chainManager), 0, 10, TimeUnit.MINUTES);

        typeList.add(TxType.TRANSFER);
        typeList.add(TxType.CROSS_CHAIN);
        typeList.add(TxType.RECHARGE);
        typeList.add(TxType.WITHDRAWAL);
        typeList.add(TxType.SWAP_TRADE);
        typeList.add(TxType.SWAP_TRADE_STABLE_COIN);

        pool.scheduleWithFixedDelay(this, 0, 10000, TimeUnit.DAYS);

    }

    @Override
    public void run() {
        try {
            while (true) {
                Block block = queue.take();
                if (block.getTxs().isEmpty()) {
                    continue;
                }
                try {
                    executeAlarm(block);
                } catch (Exception e) {
                    Log.error(e);
                }
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }

    private void executeAlarm(Block block) {
        for (Transaction tx : block.getTxs()) {
            if (!typeList.contains(tx.getType())) {
                continue;
            }
            //金额判断
            CoinData coinData = null;
            try {
                coinData = tx.getCoinDataInstance();
            } catch (NulsException e) {
                Log.error(e);
            }
            for (CoinFrom from : coinData.getFrom()) {
                double price = TokenPriceManager.getPrice(from.getAssetsChainId(), from.getAssetsId());
                if (price <= 0) {
                    continue;
                }
                int decimals = TokenPriceManager.getDecimals(from.getAssetsChainId(), from.getAssetsId());
                if (decimals < 0) {
                    continue;
                }
                BigDecimal count = new BigDecimal(from.getAmount(), decimals);
                if (count.multiply(BigDecimal.valueOf(price)).compareTo(biaozhun) > 0) {
                    alarm(block.getHeader(), tx, AddressTool.getStringAddressByBytes(from.getAddress()), TokenPriceManager.getSymbol(from.getAssetsChainId(), from.getAssetsId()), count);
                    break;
                }
            }
        }
    }

    private void alarm(BlockHeader header, Transaction tx, String from, String symbol, BigDecimal amount) {
        StringBuilder ss = new StringBuilder();
        ss.append("【Nerve大额提醒】");
        ss.append(DateUtils.timeStamp2Str(header.getTime() * 1000));
        ss.append(" , [");
        ss.append(getTxTypeStr(tx.getType()));
        ss.append("],地址-");
        ss.append(from);
        ss.append(", ");
        ss.append(symbol);
        ss.append(":");
        ss.append(DoubleUtils.getRoundStr(amount.doubleValue(), 4, true));
        ss.append(" ， ");
        ss.append(scanBaseUrl);
        ss.append(tx.getHash().toHex());
//        System.out.println(ss.toString());
        sendMessage2Wechat(ss.toString());
    }

    private void sendMessage2Wechat(String msg) {
        ECKey ecKey = ECKey.fromPrivate(HexUtil.decode(pk));
        String signMsg = HexUtil.encode(ecKey.sign(Sha256Hash.hash(msg.getBytes(Charset.forName("UTF-8")))));
        Map map = new HashMap();
        map.put("msg", msg);
        map.put("sig", signMsg);
        post(msgUrl, map);
    }

    private void post(String url, Map msgMap) {
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("token", "ASDF304IXK2WCQVBM21WN4F35OU6QV0");
        headerMap.put("Content-Type", "application/json");
        headerMap.put("abc", "1");
        sendPost(url, "UTF-8", msgMap, headerMap);
    }

    private static String sendPost(String uri, String charset, Map<String, Object> bodyMap, Map<String, String> headerMap) {
        String result = null;
        PrintWriter out = null;
        InputStream in = null;
        try {
            URL url = new URL(uri);
            HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
            urlcon.setDoInput(true);
            urlcon.setDoOutput(true);
            urlcon.setUseCaches(false);
            urlcon.setRequestMethod("POST");
            if (!headerMap.isEmpty()) {
                for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                    urlcon.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            // 获取连接
            urlcon.connect();
            out = new PrintWriter(urlcon.getOutputStream());
            //请求体里的内容转成json用输出流发送到目标地址
            out.print(JSONUtils.obj2json(bodyMap));
            out.flush();
            in = urlcon.getInputStream();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(in, charset));
            StringBuffer bs = new StringBuffer();
            String line = null;
            while ((line = buffer.readLine()) != null) {
                bs.append(line);
            }
            result = bs.toString();
            System.out.println(result);
        } catch (Exception e) {
            System.out.println("[请求异常][地址：" + uri + "][错误信息：" + e.getMessage() + "]");
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
                if (null != out) {
                    out.close();
                }
            } catch (Exception e2) {
                System.out.println("[关闭流异常][错误信息：" + e2.getMessage() + "]");
            }
        }
        return result;
    }


    private String getTxTypeStr(int type) {
        switch (type) {
            case TxType.TRANSFER:
                return "转账交易";
            case TxType.CROSS_CHAIN:
                return "跨链交易";
            case TxType.RECHARGE:
                return "充值交易";
            case TxType.WITHDRAWAL:
                return "提现交易";
            case TxType.SWAP_TRADE:
                return "Swap交易";
            case TxType.SWAP_TRADE_STABLE_COIN:
                return "StableSwap交易";
        }
        return "";
    }

    public static void main(String[] args) {
        AlarmTxManager manager = new AlarmTxManager();
        String msg = "sdfsdfsdf lin test";

        ECKey ecKey = ECKey.fromPrivate(HexUtil.decode(pk));
        String signMsg = HexUtil.encode(ecKey.sign(Sha256Hash.hash(msg.getBytes(Charset.forName("UTF-8")))));
        Map map = new HashMap();
        map.put("msg", msg);
        map.put("sig", signMsg);
        map.put("to","6198556336");
        manager. post(msgUrl, map);
    }
}
