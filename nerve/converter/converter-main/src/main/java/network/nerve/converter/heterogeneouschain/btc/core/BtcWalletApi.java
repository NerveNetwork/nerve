/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
package network.nerve.converter.heterogeneouschain.btc.core;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.NodeProperties;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.client.BtcdClientImpl;
import com.neemre.btcdcli4j.core.domain.*;
import com.neemre.btcdcli4j.core.domain.enums.ScriptTypes;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.btc.context.BtcContext;
import network.nerve.converter.heterogeneouschain.eth.utils.RandomUtil;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.utils.ConverterUtil;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.message.BasicHeader;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.EMPTY_STRING;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class BtcWalletApi implements BeanInitial {
    private ConverterConfig converterConfig;
    private BtcContext htgContext;

    private String protocol = "http";
    private String host = "192.168.50.40";
    private String port = "18332";
    private String user = "user";
    private String password = "password";
    private String auth_scheme = "Basic";

    private BtcdClient client;
    private byte[] sessionKey = null;
    private ReentrantLock checkLock = new ReentrantLock();
    private int rpcVersion = -1;
    private boolean reSyncBlock = false;
    private volatile boolean urlFromThirdPartyForce = false;

    private String symbol() {
        return htgContext.getConfig().getSymbol();
    }

    private NulsLogger getLog() {
        return htgContext.logger();
    }

    public void init(String rpcAddress) throws NulsException {
        takeRpcInfo(rpcAddress);
        client = new BtcdClientImpl(makeCurrentProperties());
    }

    Properties makeCurrentProperties() {
        Properties nodeConfig = new Properties();
        nodeConfig.put(NodeProperties.RPC_PROTOCOL.getKey(), protocol);
        nodeConfig.put(NodeProperties.RPC_HOST.getKey(), host);
        nodeConfig.put(NodeProperties.RPC_PORT.getKey(), port);
        nodeConfig.put(NodeProperties.RPC_USER.getKey(), user);
        nodeConfig.put(NodeProperties.RPC_PASSWORD.getKey(), password);
        nodeConfig.put(NodeProperties.HTTP_AUTH_SCHEME.getKey(), auth_scheme);
        return nodeConfig;
    }

    void takeRpcInfo(String rpc) {
        String[] info = rpc.split(",");
        if (info.length == 1) {
            takeURL(rpc);
        } else if (info.length == 2) {
            throw new RuntimeException("Error Bitcoin Rpc");
        } else {
            takeURL(info[0]);
            user = info[1];
            password = info[2];
        }
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
    }

    void takeURL(String url) {
        if (url.contains("https")) {
            protocol = "https";
            host = url.replace("https://", "");
            port = "443";
        } else {
            protocol = "http";
            host = url.replace("http://", "");
            if (host.contains(":")) {
                String[] split = host.split(":");
                host = split[0];
                port = split[1];
            } else {
                port = "80";
            }
        }
    }

    public void checkApi() throws NulsException {
        checkLock.lock();
        try {
            do {
                // Force updates from third-party systems rpc
                Map<Long, Map> rpcCheckMap = htgContext.getConverterCoreApi().HTG_RPC_CHECK_MAP();
                Map<String, Object> resultMap = rpcCheckMap.get(htgContext.getConfig().getChainIdOnHtgNetwork());
                if (resultMap == null) {
                    //getLog().warn("Empty resultMap! {} rpc check from third party, version: {}", symbol(), rpcVersion);
                    break;
                }
                Integer _version = (Integer) resultMap.get("rpcVersion");
                if (_version == null) {
                    //getLog().warn("Empty rpcVersion! {} rpc check from third party, version: {}", symbol(), rpcVersion);
                    break;
                }
                if (this.rpcVersion == -1) {
                    this.rpcVersion = _version.intValue();
                    getLog().info("initialization {} rpc check from third party, version: {}", symbol(), rpcVersion);
                    break;
                }
                if (this.rpcVersion == _version.intValue()) {
                    //getLog().info("Same version {} rpc check from third party, version: {}", symbol(), rpcVersion);
                    break;
                }
                if (_version.intValue() > this.rpcVersion) {
                    // find version Change, switch rpc
                    Integer _index = (Integer) resultMap.get("index");
                    if (_index == null) {
                        getLog().warn("Empty index! {} rpc check from third party, version: {}", symbol(), rpcVersion);
                        break;
                    }
                    String apiUrl = (String) resultMap.get("extend" + (_index + 1));
                    if (StringUtils.isBlank(apiUrl)) {
                        getLog().warn("Empty apiUrl! {} rpc check from third party, version: {}", symbol(), rpcVersion);
                        break;
                    }
                    this.changeApi(apiUrl);
                    getLog().info("Checked that changes are needed RPC service {} rpc check from third party, version: {}, url: {}", symbol(), _version.intValue(), host);
                    this.rpcVersion = _version.intValue();
                    this.urlFromThirdPartyForce = true;
                    this.reSyncBlock = true;
                    return;
                }
            } while (false);

            if (this.urlFromThirdPartyForce) {
                getLog().info("[{}]Mandatory emergency response API(ThirdParty)During the use period, no longer based onbank order switch API", symbol());
                return;
            }

        } catch (Exception e) {
            throw e;
        } finally {
            checkLock.unlock();
        }
    }

    public boolean isReSyncBlock() {
        return reSyncBlock;
    }

    public void setReSyncBlock(boolean reSyncBlock) {
        this.reSyncBlock = reSyncBlock;
    }

    private void changeApi(String rpc) throws NulsException {
        if (client != null) {
            client.close();
        }
        // switch api
        init(rpc);
    }


    public synchronized BigDecimal getBalance(String address) {
        try {
            TimeUnit.MILLISECONDS.sleep(500);
            boolean mainnet = htgContext.getConverterCoreApi().isNerveMainnet();
            if (!mainnet) {
                return takeBalanceFromMempool(address, false);
            } else {
                int[] requestIndexes = new int[]{0, 1, 2};
                ConverterUtil.shuffleArray(requestIndexes);
                for (int index : requestIndexes) {
                    BigDecimal balance = requestBalance(address, index);
                    if (balance != null) {
                        return balance;
                    }
                }
                return takeBalanceFromOKX(address);
            }
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    BigDecimal requestBalance(String address, int index) throws Exception {
        switch (index) {
            case 0 : return takeBalanceFromMempool(address, true);
            case 1 : return takeBalanceFromBlockchainInfo(address);
            case 2 : return takeBalanceFromBlockcypher(address);
            default:return null;
        }
    }
    BigDecimal takeBalanceFromOKX(String address) throws Exception {
        String url = "https://www.oklink.com/api/v5/explorer/address/address-summary?chainShortName=btc&address=%s";
        List<BasicHeader> headers = new ArrayList<>();
        headers.add(new BasicHeader("Ok-Access-Key", "33bed3e2-1605-467f-a6d0-53888df39b62"));
        String s = HttpClientUtil.get(String.format(url, address), headers);
        Map<String, Object> map = JSONUtils.json2map(s);
        List<Map> data = (List<Map>) map.get("data");
        if (data == null || data.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map dataMap = data.get(0);
        return new BigDecimal(dataMap.get("balance").toString()).movePointRight(htgContext.ASSET_NAME().decimals());
    }

    //public static void main(String[] args) throws Exception {
    //    BtcWalletApi api = new BtcWalletApi();
    //    api.htgContext = new BtcContext();
    //    System.out.println(api.takeBalanceFromOKX("1EzwoHtiXB4iFwedPr49iywjZn2nnekhoj"));
    //    System.out.println(api.takeBalanceFromBlockcypher("1EzwoHtiXB4iFwedPr49iywjZn2nnekhoj"));
    //    System.out.println(api.takeBalanceFromBlockchainInfo("1EzwoHtiXB4iFwedPr49iywjZn2nnekhoj"));
    //    System.out.println(api.takeBalanceFromMempool("1EzwoHtiXB4iFwedPr49iywjZn2nnekhoj", true));
    //    System.out.println(api.takeBalanceFromMempool("mpkGu7LBSLf799X91wAZhSyT6hAb4XiTLG", false));
    //}

    BigDecimal takeBalanceFromBlockcypher(String address) throws Exception {
        String url = "https://api.blockcypher.com/v1/btc/main/addrs/%s/balance";
        String s = HttpClientUtil.get(String.format(url, address));
        Map<String, Object> map = JSONUtils.json2map(s);
        if (map.get("error") != null) {
            return null;
        }
        return new BigDecimal(map.get("balance").toString());
    }

    BigDecimal takeBalanceFromBlockchainInfo(String address) throws Exception {
        String url = "https://blockchain.info/q/addressbalance/%s";
        String s = HttpClientUtil.get(String.format(url, address));
        if (s.contains("error")) {
            return null;
        }
        return new BigDecimal(s.trim());
    }

    BigDecimal takeBalanceFromMempool(String address, boolean mainnet) throws Exception {
        String url = "https://mempool.space/testnet/api/address/%s";
        if (mainnet) {
            url = "https://mempool.space/api/address/%s";
        }
        String s = HttpClientUtil.get(String.format(url, address));
        if (s.contains("Invalid")) {
            return null;
        }
        Map<String, Object> map = JSONUtils.json2map(s);
        Map statsMap = (Map) map.get("chain_stats");
        return new BigDecimal(statsMap.get("funded_txo_sum").toString()).subtract(new BigDecimal(statsMap.get("spent_txo_sum").toString()));
    }

    public RawTransaction getTransactionByHash(String txHash) {
        try {
            RawTransaction tx = (RawTransaction) client.getRawTransaction(txHash, 1);
            return tx;
        } catch (BitcoindException e) {
            getLog().error(e);
            return null;
        } catch (CommunicationException e) {
            getLog().error(e);
            return null;
        }
    }

    public String getOpReturnHex(RawTransaction tx) {
        if (tx == null) return EMPTY_STRING;
        List<RawOutput> vOut = tx.getVOut();
        if (HtgUtil.isEmptyList(vOut)) return EMPTY_STRING;
        String result = EMPTY_STRING;
        for (RawOutput output : vOut) {
            if (output.getValue().compareTo(BigDecimal.ZERO) > 0)
                continue;
            PubKeyScript script = output.getScriptPubKey();
            if (script.getType() != ScriptTypes.NULL_DATA)
                continue;
            String asm = script.getAsm();
            if (!asm.startsWith("OP_RETURN"))
                continue;
            String content = asm.replace("OP_RETURN ", "");
            result += content;
        }
        return result;
    }

    public Block getBestBlock() {
        try {
            return (Block) client.getBlock(client.getBestBlockHash(), true);
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            throw new RuntimeException(e);
        }
    }

    public String getBestBlockHash() {
        try {
            return client.getBestBlockHash();
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            throw new RuntimeException(e);
        }
    }

    public long getBestBlockHeight() {
        try {
            return client.getBlockCount();
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            throw new RuntimeException(e);
        }
    }

    public BlockInfo getBlockByHeight(long height) {
        try {
            return (BlockInfo) client.getBlockInfo(client.getBlockHash((int) height));
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            throw new RuntimeException(e);
        }
    }

    public BlockHeader getBlockHeaderByHeight(long height) {
        try {
            return (BlockHeader) client.getBlockHeader(client.getBlockHash((int) height));
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            throw new RuntimeException(e);
        }
    }

    public BlockHeader getBlockHeaderByHash(String hash) {
        try {
            return (BlockHeader) client.getBlockHeader(hash);
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            throw new RuntimeException(e);
        }
    }

    public List<RawTransaction> getTxInfoList(BlockInfo blockInfo) {
        return blockInfo.getTx();
    }

    public List<Output> getAccountUTXOs(String address) {
        //TODO pierre third party api
        return Collections.EMPTY_LIST;
    }

}
