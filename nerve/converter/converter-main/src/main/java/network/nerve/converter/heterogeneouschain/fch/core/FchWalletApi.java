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
package network.nerve.converter.heterogeneouschain.fch.core;

import apipClass.BlockInfo;
import apipClass.ResponseBody;
import apipClass.TxInfo;
import apipClient.ApipClient;
import apipClient.ApipDataGetter;
import apipClient.BlockchainAPIs;
import apipClient.FreeGetAPIs;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.domain.BlockHeader;
import fcTools.ParseTools;
import fchClass.Cash;
import fchClass.OpReturn;
import fchClass.P2SH;
import fchClass.TxMark;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.BitCoinLibBlockHeader;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.BitCoinLibBlockInfo;
import network.nerve.converter.heterogeneouschain.fch.context.FchContext;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import txTools.FchTool;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class FchWalletApi implements IBitCoinLibWalletApi, BeanInitial {
    private ConverterConfig converterConfig;
    private FchContext htgContext;

    private String rpc = "https://cid.cash/APIP";
    private String via = "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD";
    private byte[] sessionKey = HexUtil.decode("b3928a1dc649b38fb1f4b21b0afc3def668bad9f335c99db4fc0ec54cac1e655");
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

    public void init(String rpcAddress) throws NulsException {
        this.rpc = rpcAddress;
    }

    @Override
    public long getFeeRate() {
        return 1;
    }

    @Override
    public long getBestBlockHeight() {
        BlockInfo bestBlock = this.getBestBlock();
        return bestBlock.getHeight();
    }

    public void changeApi(String rpc) throws NulsException {
        // switchapi
        String[] info = rpc.split(",");
        if (info.length == 1) {
            this.rpc = rpc;
        } else if (info.length == 2) {
            this.rpc = info[0];
            this.sessionKey = HexUtil.decode(info[1]);
        } else {
            this.rpc = info[0];
            this.via = info[1];
            this.sessionKey = HexUtil.decode(info[2]);
        }
    }

    public BigDecimal getBalance(String address) {
        ApipClient apipClient = FreeGetAPIs.getFidCid(rpc, address);
        Map data = (Map) checkBestHeight(apipClient.getResponseBody());
        if (data == null) {
            return BigDecimal.ZERO;
        }
        Object balance = data.get("balance");
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(balance.toString());
    }

    public BigDecimal getPrice() {
        ApipClient apipClient = FreeGetAPIs.getPrices(rpc);
        Map data = (Map) checkBestHeight(apipClient.getResponseBody());
        if (data == null) {
            return BigDecimal.ZERO;
        }
        Object balance = data.get("fch/doge");
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(balance.toString());
    }

    ThreadLocal<Long> bestHeightLocal = new ThreadLocal<>();

    public Long getBestHeightForCurrentThread() {
        return bestHeightLocal.get();
    }

    private Object checkBestHeight(ResponseBody responseBody) {
        if (responseBody.getCode() != 0) {
            throw new RuntimeException(String.format("%s, detail: %s", responseBody.getMessage(), responseBody.getData()));
        }
        bestHeightLocal.set(responseBody.getBestHeight());
        return responseBody.getData();
    }

    private Object checkApiBalance(ResponseBody responseBody) {
        checkBestHeight(responseBody);
        long balance = responseBody.getBalance();
        if (balance < 1000000000l) {
            // Last 1000,000 requests
            String warn = String.format("[%s] Access paid API The number of times is about to run out, and the remaining amount is insufficient 10, please use %s towards FUmo2eez6VK2sfGWjek9i9aK5y1mdHSnqv Transfer", symbol(), via);
            getLog().warn(warn);
        }
        if (getLog().isDebugEnabled())
            getLog().debug("[{}] Paid API Remaining visits of: {}, Remaining amount: {}", symbol(), balance / 1000, new BigDecimal(balance).movePointLeft(8).stripTrailingZeros().toPlainString());
        return responseBody.getData();
    }

    public TxInfo getTransactionByHash(String txHash) {
        ApipClient client = BlockchainAPIs.txByIdsPost(rpc, new String[]{txHash}, via, sessionKey);
        Object data = checkApiBalance(client.getResponseBody());
        if (data == null) {
            return null;
        }
        List<TxInfo> txInfoList = ApipDataGetter.getTxInfoList(data);
        if (txInfoList == null || txInfoList.isEmpty()) {
            return null;
        }
        return txInfoList.get(0);
    }

    public String getOpReturnInfo(String txHash) {
        ApipClient client = BlockchainAPIs.opReturnByIdsPost(rpc, new String[]{txHash}, via, sessionKey);
        Object data = checkApiBalance(client.getResponseBody());
        if (data == null) {
            return null;
        }
        Map<String, OpReturn> opReturnMap = ApipDataGetter.getOpReturnMap(data);
        if (opReturnMap == null || opReturnMap.isEmpty()) {
            return null;
        }
        OpReturn opReturn = opReturnMap.get(txHash);
        return opReturn == null ? null : opReturn.getOpReturn();
    }

    @Override
    public BitCoinLibBlockHeader getBitCoinLibBlockHeaderByHeight(long height) {
        BlockInfo blockInfo = this.getBlockByHeight(height);
        if (blockInfo == null) {
            return null;
        }
        return new BitCoinLibBlockHeader(
                blockInfo.getHeight(),
                blockInfo.getTime(),
                blockInfo.getBlockId(),
                blockInfo.getPreId()
        );
    }

    @Override
    public BitCoinLibBlockInfo getBitCoinLibBlockByHeight(Long height) {
        BlockInfo blockInfo = this.getBlockByHeight(height);
        if (blockInfo == null) {
            return null;
        }
        ArrayList<TxMark> txList = blockInfo.getTxList();
        List<TxInfo> txInfoList = this.getTxInfoList(txList.stream().map(tx -> tx.getTxId()).collect(Collectors.toList()));
        return new BitCoinLibBlockInfo(
                new BitCoinLibBlockHeader(
                        blockInfo.getHeight(),
                        blockInfo.getTime(),
                        blockInfo.getBlockId(),
                        blockInfo.getPreId()), txInfoList);
    }

    @Override
    public BtcdClient getClient() {
        return null;
    }

    @Override
    public void priceMaintain() {
        BigDecimal fchToDogePrice = this.getPrice();
        if (fchToDogePrice.compareTo(BigDecimal.ZERO) > 0) {
            htgContext.getConverterCoreApi().setFchToDogePrice(fchToDogePrice);
        }
    }

    public BlockInfo getBestBlock() {
        ApipClient apipClient = FreeGetAPIs.getBestBlock(rpc);
        Object data = apipClient.getResponseBody().getData();
        if (data == null) {
            return null;
        }
        Type t = (new TypeToken<BlockInfo>() {
        }).getType();
        Gson gson = new Gson();
        return (BlockInfo) gson.fromJson(gson.toJson(data), t);
    }

    public BlockInfo getBlockByHeight(long height) {
        String heightKey = height + "";
        ApipClient client = BlockchainAPIs.blockByHeightsPost(rpc, new String[]{heightKey}, via, sessionKey);
        Object data = checkApiBalance(client.getResponseBody());
        if (data == null) {
            return null;
        }
        Map<String, BlockInfo> blockInfoMap = ApipDataGetter.getBlockInfoMap(data);
        if (blockInfoMap == null) {
            return null;
        }
        return blockInfoMap.get(heightKey);
    }

    public List<TxInfo> getTxInfoList(List<String> txHashes) {
        String[] txHashArr = new String[txHashes.size()];
        txHashes.toArray(txHashArr);
        ApipClient client = BlockchainAPIs.txByIdsPost(rpc, txHashArr, via, sessionKey);
        Object data = checkApiBalance(client.getResponseBody());
        if (data == null) {
            return null;
        }
        return ApipDataGetter.getTxInfoList(data);
    }

    public List<Cash> getAccountUTXOs(String address) {
        ApipClient apipClient = FreeGetAPIs.getCashes(rpc, address, 0);
        Object data = checkBestHeight(apipClient.getResponseBody());
        if (data == null) {
            return null;
        }
        return ApipDataGetter.getCashList(data);
    }

    public Map<String, Cash> getUTXOsByIds(String[] cashIds) {
        ApipClient client = BlockchainAPIs.cashByIdsPost(rpc, cashIds, via, sessionKey);
        Object data = checkApiBalance(client.getResponseBody());
        if (data == null) {
            return null;
        }
        return ApipDataGetter.getCashMap(data);
    }

    public Cash getUTXOByTxIdIndex(String txid, int vout) {
        String cashId = ParseTools.calcTxoId(txid, vout);
        String[] cashes = new String[]{cashId};
        ApipClient client = BlockchainAPIs.cashByIdsPost(rpc, cashes, via, sessionKey);
        Object data = checkApiBalance(client.getResponseBody());
        if (data == null) {
            return null;
        }
        Map<String, Cash> cashMap = ApipDataGetter.getCashMap(data);
        if (cashMap == null) {
            return null;
        }
        return cashMap.get(cashId);
    }



    public String createMultisigAddress(List<byte[]> pubs, int minSign) {
        if (HtgUtil.isEmptyList(pubs)) {
            return null;
        }
        if (pubs.size() < minSign) {
            return null;
        }
        P2SH p2sh = FchTool.genMultiP2sh(pubs, minSign);
        return p2sh.getFid();
    }

    public String broadcast(String txHex) {
        ApipClient apipClient = FreeGetAPIs.broadcast(rpc, txHex);
        Object data = checkBestHeight(apipClient.getResponseBody());
        if (data == null) {
            return null;
        }
        return data.toString();
    }

}
