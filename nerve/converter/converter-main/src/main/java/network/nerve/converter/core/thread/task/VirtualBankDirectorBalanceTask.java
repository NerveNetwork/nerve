/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.nerve.converter.core.thread.task;

import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.dto.HeterogeneousAddressDTO;
import network.nerve.converter.model.dto.VirtualBankDirectorDTO;
import network.nerve.converter.model.po.WechatMsg;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.utils.VirtualBankUtil;
import org.web3j.crypto.Hash;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.nerve.converter.constant.ConverterConstant.*;
import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.EMPTY_STRING;

/**
 * @author: Mimi
 * @date: 2021-01-12
 */
public class VirtualBankDirectorBalanceTask implements Runnable {

    private static String msgUrl = "https://wx.niels.wang";
    private static LinkedBlockingDeque<WechatMsg> wechatMsgQueue = new LinkedBlockingDeque<>();
    private static Map<String, WechatMsg> feeNotEnoughMap = new ConcurrentHashMap<>();
    private static Set<String> wechatMsgHashes = new HashSet<>();
    private static Set<String> wechatMsgFilterHashes = ConcurrentHashMap.newKeySet();
    private static Map<String, Long> warningTimeMap = new HashMap<>();
    private static final String pk = "989f28d4ac90899ba94dc50efd765f99b27393820212170a9f4f7cd869f2b691";
    static {
        wechatMsgFilterHashes.add("237e27455327f3cb9e5f40cb628b05a53f135d176d19ef8ed36c03f6f9bed788");
        wechatMsgFilterHashes.add("6ba5bb4bedd95e5441e779c7ce300759d51a74f75b14db54a310c7ad496f8960");
        wechatMsgFilterHashes.add("f63ed63dd5d6004d027d5504b1a8ebfd1f4d80af7f09acabf008617e28cfe5cf");
        wechatMsgFilterHashes.add("f8663f40ce662c5e2e663a50d01cb30727c49e5a8a9a1ef94605b8c185396bc6");
        wechatMsgFilterHashes.add("892ddf244497f7ef09ba542f4ba5dddc58035febad7d12c2eb588d6f7dcc1f05");
        wechatMsgFilterHashes.add("e12f01d1205588bc5622571d3620e3a11b87febfbc7c50876699c89dbbd6dfd3");
    }
    private Chain chain;
    private HeterogeneousDockingManager heterogeneousDockingManager;
    private ConverterCoreApi converterCoreApi;
    private HeterogeneousAssetHelper heterogeneousAssetHelper;
    private long flag;
    private int logPrint;


    public VirtualBankDirectorBalanceTask(Chain chain) {
        this.chain = chain;
        this.heterogeneousDockingManager = SpringLiteContext.getBean(HeterogeneousDockingManager.class);
        this.converterCoreApi = SpringLiteContext.getBean(ConverterCoreApi.class);
        this.heterogeneousAssetHelper = SpringLiteContext.getBean(HeterogeneousAssetHelper.class);
        this.flag = -1;
    }

    public static void addFileterHashes(List<String> hashes) {
        wechatMsgFilterHashes.addAll(hashes);
    }

    public static void removeFileterHashes(List<String> hashes) {
        wechatMsgFilterHashes.removeAll(hashes);
    }

    public static Collection<String> getFileterHashes() {
        return wechatMsgFilterHashes;
    }

    public static void putWechatMsg(String msg) {
        Long now = System.currentTimeMillis();
        boolean feeNotEnough = msg.contains("提现手续费不足");
        String hash = Hash.sha3(msg);
        // 清理过期数据
        List<String> removes = new ArrayList<>();
        Set<Map.Entry<String, Long>> entries = warningTimeMap.entrySet();
        for (Map.Entry<String, Long> entry : entries) {
            Long value = entry.getValue();
            if (now - value > MINUTES_60) {
                removes.add(entry.getKey());
            }
        }
        for (String key : removes) {
            warningTimeMap.remove(key);
        }
        // 手续费不足的消息提醒规则
        if (feeNotEnough) {
            // 过滤不提醒的交易
            String txHash = getHashFromMsg(msg);
            if (wechatMsgFilterHashes.contains(txHash)) {
                return;
            }
            // 首次提醒后，半小时提醒一次
            Long time = warningTimeMap.get(hash);
            if (time == null || now - time > MINUTES_30) {
                warningTimeMap.put(hash, now);
            } else {
                return;
            }
            feeNotEnoughMap.put(txHash, new WechatMsg(hash, msg));
            return;
        }
        if (wechatMsgHashes.add(hash)) {
            wechatMsgQueue.offer(new WechatMsg(hash, msg));
        }
    }

    private static String getHashFromMsg(String msg) {
        if (StringUtils.isBlank(msg)) {
            return EMPTY_STRING;
        }
        String[] split = msg.split("nerveTxHash:");
        if (split.length < 2) {
            return EMPTY_STRING;
        }
        if (split[1].length() < 65) {
            return EMPTY_STRING;
        }
        String hash = split[1].substring(1, 65);
        return hash;
    }

    @Override
    public void run() {
        try {
            if (!converterCoreApi.isRunning()) {
                chain.getLogger().info("[Basic information query of heterogeneous chains]Ignoring synchronous block mode");
                return;
            }
            // every other150Update each block once, calculate if the update conditions are met
            LatestBasicBlock latestBasicBlock = chain.getLatestBasicBlock();
            long latestHeight = latestBasicBlock.getHeight();
            long currentFlag = latestHeight / 150;
            do {
                if (flag == -1) {
                    flag = currentFlag;
                    break;
                }
                if (currentFlag > flag) {
                    flag = currentFlag;
                    break;
                }
                chain.getLogger().info("[Basic information query of heterogeneous chains]Not meeting the execution conditions, latestHeight: {}, currentFlag: {}, flag: {}", latestHeight, currentFlag, flag);
                return;
            } while (false);
            try {
                do {
                    if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                        chain.getLogger().info("Non virtual bank members, skipping heterogeneous chainsRPCThe task of inspection");
                        break;
                    }
                    chain.getLogger().info("Update heterogeneous chains by heightRPCViewing Information, every other150Count each block once, Current network height: {}", latestHeight);
                    String result = HttpClientUtil.get(String.format("https://assets.nabox.io/api/chainapi"));
                    if (StringUtils.isNotBlank(result)) {
                        List<Map> list = JSONUtils.json2list(result, Map.class);
                        Map<Long, Map> map = list.stream().collect(Collectors.toMap(m -> Long.valueOf(m.get("nativeId").toString()), Function.identity()));
                        ConverterContext.HTG_RPC_CHECK_MAP = map;
                    }
                } while (false);
            } catch (Exception e) {
                chain.getLogger().error(e.getMessage(), e);
            }

            Map<Integer, HeterogeneousAddressDTO> preAddressDTOMap = null;
            try {
                List<VirtualBankDirectorDTO> preList = ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST;
                if (preList != null) {
                    for (VirtualBankDirectorDTO dto : preList) {
                        if ("NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA".equals(dto.getSignAddress())) {
                            preAddressDTOMap = dto.getHeterogeneousAddresses().stream().collect(Collectors.toMap(HeterogeneousAddress::getChainId, Function.identity()));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                chain.getLogger().error(e.getMessage(), e);
            }

            try {
                do {
                    if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                        chain.getLogger().info("Non virtual bank members, skip the task of heterogeneous chain balance caching");
                        break;
                    }
                    chain.getLogger().info("Update cached virtual bank heterogeneous chain network balances by height, every other150Count each block once, Current network height: {}", latestHeight);
                    Map<String, VirtualBankDirector> mapVirtualBank = chain.getMapVirtualBank();
                    List<VirtualBankDirectorDTO> list = new ArrayList<>();
                    for (VirtualBankDirector director : mapVirtualBank.values()) {
                        VirtualBankDirectorDTO directorDTO = new VirtualBankDirectorDTO(director);
                        for (HeterogeneousAddressDTO addr : directorDTO.getHeterogeneousAddresses()) {
                            if (chain.getChainId() == 5 && addr.getChainId() == 101) {
                                continue;
                            }
                            IHeterogeneousChainDocking heterogeneousDocking = heterogeneousDockingManager.getHeterogeneousDocking(addr.getChainId());
                            String chainSymbol = heterogeneousDocking.getChainSymbol();
                            addr.setSymbol(chainSymbol);
                        }
                        list.add(directorDTO);
                    }
                    ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST = list;
                    // Parallel query of heterogeneous chain balances
                    VirtualBankUtil.virtualBankDirectorBalance(list, chain, heterogeneousDockingManager, this.logPrint, converterCoreApi);

                } while (false);
            } catch (Exception e) {
                chain.getLogger().error(e.getMessage(), e);
            }
            try {
                VirtualBankDirectorDTO nodeAA = null;
                for (VirtualBankDirectorDTO dto : ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST) {
                    if ("NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA".equals(dto.getSignAddress())) {
                        nodeAA = dto;
                        break;
                    }
                }
                if (nodeAA != null) {
                    List<HeterogeneousAddressDTO> list = nodeAA.getHeterogeneousAddresses();
                    for (HeterogeneousAddressDTO dto : list) {
                        int chainId = dto.getChainId();
                        BigDecimal balance = new BigDecimal(dto.getBalance());
                        if (balance.compareTo(BigDecimal.ZERO) == 0 && preAddressDTOMap != null) {
                            HeterogeneousAddressDTO preAddressDTO = preAddressDTOMap.get(chainId);
                            BigDecimal preBalance;
                            if (preAddressDTO != null && StringUtils.isNotBlank(preAddressDTO.getBalance()) && (preBalance = new BigDecimal(preAddressDTO.getBalance())).compareTo(BigDecimal.ZERO) > 0) {
                                balance = preBalance;
                                dto.setBalance(balance.toPlainString());
                            }
                        }
                        BigDecimal minBalance = minBalances.get(chainId);
                        if (minBalance != null && minBalance.compareTo(balance) > 0) {
                            String msg = String.format("虚拟银行节点[NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA]，[%s]网络地址[%s]余额不足[%s]", dto.getSymbol(), dto.getAddress(), minBalance.toPlainString());
                            putWechatMsg(msg);
                        }
                    }
                }
            } catch (Exception e) {
                chain.getLogger().error(e.getMessage(), e);
            }
            try {
                wechatMsgQueue.addAll(feeNotEnoughMap.values());
                feeNotEnoughMap.clear();
                chain.getLogger().info("每隔150个区块通知微信, 当前网络高度: {}", latestHeight);
                while (!wechatMsgQueue.isEmpty()) {
                    WechatMsg wechatMsg = wechatMsgQueue.peekFirst();
                    this.sendMessage2Wechat(wechatMsg.getMsg());
                    wechatMsgQueue.remove();
                    wechatMsgHashes.remove(wechatMsg.getKey());
                }
            } catch (Exception e) {
                chain.getLogger().error(e.getMessage(), e);
            }
            try {
                chain.getLogger().info("Update the registration chain of cached assets by height, every other150Count each block once, Current network height: {}", latestHeight);
                // Calculate asset registration chain
                List<Map> assetList = LedgerCall.ledgerAssetQueryAll(chain.getChainId());
                chain.getLogger().info("Number of assets to be queried: {}", assetList == null ? 0 : assetList.size());
                if (!assetList.isEmpty()) {
                    for (Map asset : assetList) {
                        Integer assetChainId = Integer.parseInt(asset.get("assetChainId").toString());
                        Integer assetId = Integer.parseInt(asset.get("assetId").toString());
                        HeterogeneousAssetInfo info = this.registerNetwork(chain, assetChainId, assetId);
                        if (info != null) {
                            ConverterContext.assetRegisterNetwork.put(assetChainId + "_" + assetId, info);
                        }
                    }
                }
            } catch (Exception e) {
                chain.getLogger().error(e.getMessage(), e);
            }

            this.logPrint++;
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    private HeterogeneousAssetInfo registerNetwork(Chain chain, int assetChainId, int assetId) {
        try {
            if (this.logPrint % 10 == 0) {
                chain.getLogger().info("Asset: {}-{}, Query registration chain", assetChainId, assetId);
            } else {
                chain.getLogger().debug("Asset: {}-{}, Query registration chain", assetChainId, assetId);
            }
            HeterogeneousAssetInfo resultAssetInfo = null;
            List<HeterogeneousAssetInfo> assetInfos = heterogeneousAssetHelper.getHeterogeneousAssetInfo(assetChainId, assetId);
            if (assetInfos == null || assetInfos.isEmpty()) {
                return null;
            }
            int resultChainId = 0;
            for (HeterogeneousAssetInfo assetInfo : assetInfos) {
                if (StringUtils.isBlank(assetInfo.getContractAddress())) {
                    resultChainId = assetInfo.getChainId();
                    resultAssetInfo = assetInfo;
                    break;
                }
            }
            if (resultChainId == 0) {
                for (HeterogeneousAssetInfo assetInfo : assetInfos) {
                    if (!converterCoreApi.checkNetworkRunning(assetInfo.getChainId())) {
                        return null;
                    }
                    if (chain.getChainId() == 5 && assetInfo.getChainId() == 101) {
                        return null;
                    }
                    IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(assetInfo.getChainId());
                    if (docking == null) {
                        return null;
                    }
                    try {
                        if (!docking.isMinterERC20(assetInfo.getContractAddress())) {
                            resultChainId = docking.getChainId();
                            resultAssetInfo = assetInfo;
                            break;
                        }
                    } catch (Exception e) {
                        //skip it
                    }
                }
            }
            if (resultChainId == 0) {
                return null;
            }
            if (this.logPrint % 10 == 0) {
                chain.getLogger().info("Asset: {}-{}, Found registration chain: {}", assetChainId, assetId, resultChainId);
            } else {
                chain.getLogger().debug("Asset: {}-{}, Found registration chain: {}", assetChainId, assetId, resultChainId);
            }
            return resultAssetInfo;
        } catch (Exception e) {
            chain.getLogger().error("Asset: {}-{}, Query registration chain error: {}", assetChainId, assetId, e.getMessage());
            return null;
        }
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
}
