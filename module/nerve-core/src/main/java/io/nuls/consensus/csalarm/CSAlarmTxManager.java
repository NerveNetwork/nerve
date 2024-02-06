package io.nuls.consensus.csalarm;

import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.round.MeetingMember;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.consensus.v1.utils.RoundUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class CSAlarmTxManager implements InitializingBean, Runnable {

    private static final String scanBaseUrl = "https://scan.nerve.network/consensus/info?hash=";
    private static final String pk = "989f28d4ac90899ba94dc50efd765f99b27393820212170a9f4f7cd869f2b691";
    public static String msgUrl = "https://wx.niels.wang";


    @Override
    public void afterPropertiesSet() throws NulsException {
        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1);
        pool.scheduleWithFixedDelay(this, 1, 10, TimeUnit.MINUTES);

    }

    @Autowired
    private ChainManager chainManager;

    @Override
    public void run() {
        try {
            Chain chain = chainManager.getChainMap().get(9);
            MeetingRound round = RoundUtils.getRoundController().getCurrentRound();
            for (MeetingMember member : round.getMemberList()) {
                Agent agent = member.getAgent();
                if (agent.getTxHash() == null) {
                    continue;
                }
                String key;
                if (StringUtils.isBlank(agent.getAlias())) {
                    key = agent.getTxHash().toHex().substring(56);
                } else {
                    key = agent.getAlias();
                }
                Double lastValue = creditMap.computeIfAbsent(key, v -> 0d);
                creditMap.put(key, agent.getRealCreditVal());
                if (agent.getRealCreditVal() + 0.005 < lastValue) {
                    sendMessage2Wechat("【Nerve节点信用】" + key + " : " + agent.getRealCreditVal() + ", " + scanBaseUrl + agent.getTxHash().toHex());
                }
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }

    private Map<String, Double> creditMap = new HashMap<>();

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
}
