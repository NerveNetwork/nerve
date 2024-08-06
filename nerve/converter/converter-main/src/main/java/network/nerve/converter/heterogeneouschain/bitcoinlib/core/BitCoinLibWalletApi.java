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
package network.nerve.converter.heterogeneouschain.bitcoinlib.core;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.NodeProperties;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.domain.*;
import com.neemre.btcdcli4j.core.domain.enums.ScriptTypes;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.BitCoinLibBlockHeader;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.BitCoinLibBlockInfo;
import network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.utils.ConverterUtil;
import org.apache.http.message.BasicHeader;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.script.ScriptException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.EMPTY_STRING;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class BitCoinLibWalletApi implements IBitCoinLibWalletApi, BeanInitial {
    protected HtgContext htgContext;
    private String protocol = "http";
    private String host = "192.168.5.140";
    private String port = "18332";
    private String user = "user";
    private String password = "password";
    private String auth_scheme = "Basic";

    protected BtcdClient client;
    private ReentrantLock checkLock = new ReentrantLock();
    private int rpcVersion = -1;
    private boolean reSyncBlock = false;
    private volatile boolean urlFromThirdPartyForce = false;

    public String symbol() {
        return htgContext.getConfig().getSymbol();
    }

    public NulsLogger getLog() {
        return htgContext.logger();
    }

    public boolean isReSyncBlock() {
        return reSyncBlock;
    }

    public void setReSyncBlock(boolean reSyncBlock) {
        this.reSyncBlock = reSyncBlock;
    }

    public HtgContext getHtgContext() {
        return htgContext;
    }

    public ReentrantLock getCheckLock() {
        return checkLock;
    }

    public int getRpcVersion() {
        return rpcVersion;
    }

    public void setRpcVersion(int rpcVersion) {
        this.rpcVersion = rpcVersion;
    }

    public boolean isUrlFromThirdPartyForce() {
        return urlFromThirdPartyForce;
    }

    public void setUrlFromThirdPartyForce(boolean urlFromThirdPartyForce) {
        this.urlFromThirdPartyForce = urlFromThirdPartyForce;
    }

    protected BtcdClient newInstanceBtcdClient() {
        return BitCoinLibUtil.newInstanceBtcdClient(makeCurrentProperties());
    }
    public void init(String rpcAddress) {
        takeRpcInfo(rpcAddress);
        if (client != null) {
            client.close();
            client = null;
        }
        client = this.newInstanceBtcdClient();
    }

    private void restartClient(CommunicationException e) {
        getLog().error(e);
        if (client != null) {
            client.close();
            client = null;
        }
        client = this.newInstanceBtcdClient();
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

    public void changeApi(String rpc) throws NulsException {
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
            case 0:
                return takeBalanceFromMempool(address, true);
            case 1:
                return takeBalanceFromBlockchainInfo(address);
            case 2:
                return takeBalanceFromBlockcypher(address);
            default:
                return null;
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
            restartClient(e);
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
            restartClient(e);
            throw new RuntimeException(e);
        }
    }

    public String getBestBlockHash() {
        try {
            return client.getBestBlockHash();
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            restartClient(e);
            throw new RuntimeException(e);
        }
    }

    public long getBestBlockHeight() {
        try {
            return client.getBlockCount();
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            restartClient(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public BitCoinLibBlockHeader getBitCoinLibBlockHeaderByHeight(long height) {
        BlockHeader header = this.getBlockHeaderByHeight(height);
        if (header == null) {
            return null;
        }
        return new BitCoinLibBlockHeader(
                header.getHeight(),
                header.getTime(),
                header.getHash(),
                header.getPreviousBlockHash()
        );
    }

    @Override
    public BitCoinLibBlockInfo getBitCoinLibBlockByHeight(Long height) {
        BlockInfo block = this.getBlockByHeight(height);
        if (block == null) {
            return null;
        }
        return new BitCoinLibBlockInfo(
                new BitCoinLibBlockHeader(
                        block.getHeight(),
                        block.getTime(),
                        block.getHash(),
                        block.getPreviousBlockHash()), block.getTx());
    }

    public BlockInfo getBlockByHeight(long height) {
        try {
            return (BlockInfo) client.getBlockInfo(client.getBlockHash((int) height));
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            restartClient(e);
            throw new RuntimeException(e);
        }
    }

    public BlockHeader getBlockHeaderByHeight(long height) {
        try {
            return (BlockHeader) client.getBlockHeader(client.getBlockHash((int) height));
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            restartClient(e);
            throw new RuntimeException(e);
        }
    }

    public BlockHeader getBlockHeaderByHash(String hash) {
        try {
            return (BlockHeader) client.getBlockHeader(hash);
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            restartClient(e);
            throw new RuntimeException(e);
        }
    }

    public long getFeeRate() {
        String url;
        if (htgContext.getConverterCoreApi().isNerveMainnet()) {
            url = "https://mempool.space/api/v1/fees/recommended";
        } else {
            url = "https://mempool.space/testnet/api/v1/fees/recommended";
        }
        try {
            String data = HttpClientUtil.get(String.format(url));
            htgContext.logger().info("BTC Fees recommended: {}", data);
            Map<String, Object> map = JSONUtils.json2map(data);
            Object object = map.get("fastestFee");
            if (object == null) {
                return htgContext.getEthGasPrice().longValue();
            }
            return Long.parseLong(object.toString());
        } catch (Exception e) {
            return htgContext.getEthGasPrice().longValue();
        }

    }

    public List<RawTransaction> getTxInfoList(BlockInfo blockInfo) {
        return blockInfo.getTx();
    }

    public List<UTXOData> getAccountUTXOs(String address) {
        if (htgContext.getConverterCoreApi().isNerveMainnet()) {
            String url = "http://api.v2.nabox.io/nabox-api/btc/utxo";
            Map<String, Object> param = new HashMap<>();
            param.put("language", "CHS");
            param.put("chainId", 0);
            param.put("address", address);
            param.put("coinAmount", 0);
            param.put("serviceCharge", 0);
            param.put("utxoType", 1);
            try {
                String data = HttpClientUtil.post(url, param);
                Map<String, Object> map = JSONUtils.json2map(data);
                Object code = map.get("code");
                if (code == null) {
                    getLog().error("get utxo error: empty code");
                    return Collections.EMPTY_LIST;
                }
                int codeValue = Integer.parseInt(code.toString());
                if (codeValue != 1000) {
                    getLog().error("get utxo error: " + map.get("msg"));
                    return Collections.EMPTY_LIST;
                }
                Map dataMap = (Map) map.get("data");
                List<Map> utxoList = (List<Map>) dataMap.get("utxo");
                if (utxoList == null || utxoList.isEmpty()) {
                    return Collections.EMPTY_LIST;
                }
                List<UTXOData> resultList = new ArrayList<>();
                for (Map utxo : utxoList) {
                    boolean isSpent = (boolean) utxo.get("isSpent");
                    if (isSpent) {
                        continue;
                    }
                    resultList.add(new UTXOData(
                            utxo.get("txid").toString(),
                            Integer.parseInt(utxo.get("vout").toString()),
                            new BigInteger(utxo.get("satoshi").toString())
                    ));
                }
                return resultList;
            } catch (Exception e) {
                getLog().error("get utxo error: " + e.getMessage());
                return Collections.EMPTY_LIST;
            }
        } else {
            String url = "https://mempool.space/testnet/api/address/%s/utxo";
            try {
                String data = HttpClientUtil.get(String.format(url, address));
                List<Map> list = JSONUtils.json2list(data, Map.class);
                if (list.isEmpty()) {
                    return Collections.EMPTY_LIST;
                }
                List<UTXOData> resultList = new ArrayList<>();
                for (Map utxo : list) {
                    Map statusInfo = (Map) utxo.get("status");
                    boolean confirmed = (boolean) statusInfo.get("confirmed");
                    if (!confirmed) {
                        continue;
                    }
                    resultList.add(new UTXOData(
                            utxo.get("txid").toString(),
                            Integer.parseInt(utxo.get("vout").toString()),
                            new BigInteger(utxo.get("value").toString())
                    ));
                }
                return resultList;
            } catch (Exception e) {
                getLog().error("get utxo error: " + e.getMessage());
                return Collections.EMPTY_LIST;
            }
        }
    }

    public String broadcast(org.bitcoinj.core.Transaction _tx) {
        try {
            String hash = client.sendRawTransaction(HexUtil.encode(_tx.serialize()));
            return hash;
        } catch (ScriptException ex) {
            throw new RuntimeException(ex);
        } catch (VerificationException ex) {
            throw new RuntimeException(ex);
        } catch (CommunicationException e) {
            restartClient(e);
            throw new RuntimeException(e);
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        }
    }

    public String broadcast(String _tx) {
        try {
            String hash = client.sendRawTransaction(_tx);
            return hash;
        } catch (ScriptException ex) {
            throw new RuntimeException(ex);
        } catch (VerificationException ex) {
            throw new RuntimeException(ex);
        } catch (CommunicationException e) {
            restartClient(e);
            throw new RuntimeException(e);
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        }
    }

    public TxOutInfo getTxOutInfo(String txid, int vout) {
        try {
            return client.getTxOutInfo(txid, vout, true);
        } catch (ScriptException ex) {
            throw new RuntimeException(ex);
        } catch (VerificationException ex) {
            throw new RuntimeException(ex);
        } catch (CommunicationException e) {
            restartClient(e);
            throw new RuntimeException(e);
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        }
    }

    public BtcdClient getClient() {
        return client;
    }
}
