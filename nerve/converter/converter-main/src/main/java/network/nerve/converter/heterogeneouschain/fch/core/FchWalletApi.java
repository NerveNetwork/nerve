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
import fchClass.Cash;
import fchClass.OpReturn;
import fchClass.P2SH;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.heterogeneouschain.fch.context.FchContext;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import txTools.FchTool;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class FchWalletApi implements BeanInitial {
    private ConverterConfig converterConfig;
    private FchContext htgContext;

    private String rpc = "https://cid.cash/APIP";
    private String via = "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD";
    private byte[] sessionKey = HexUtil.decode("47a75483f8800d0c36f6e11c7502b7b6f7522713d800790d665b89736f776cbc");
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
        this.rpc = rpcAddress;
    }

    public void checkApi() throws NulsException {
        checkLock.lock();
        try {
            do {
                // Force updates from third-party systemsrpc
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
                    // findversionChange, switchrpc
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
                    getLog().info("Checked that changes are neededRPCservice {} rpc check from third party, version: {}, url: {}", symbol(), _version.intValue(), apiUrl);
                    this.changeApi(apiUrl);
                    this.rpcVersion = _version.intValue();
                    this.urlFromThirdPartyForce = true;
                    this.reSyncBlock = true;
                    return;
                }
            } while (false);

            if (this.urlFromThirdPartyForce) {
                getLog().info("[{}]Mandatory emergency responseAPI(ThirdParty)During the use period, no longer based onbank orderswitchAPI", symbol());
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
        Map data = (Map) apipClient.getResponseBody().getData();
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
        Map data = (Map) apipClient.getResponseBody().getData();
        if (data == null) {
            return BigDecimal.ZERO;
        }
        Object balance = data.get("fch/doge");
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(balance.toString());
    }

    private Object checkApiBalance(ResponseBody responseBody) {
        long balance = responseBody.getBalance();
        if (balance < 1000000000l) {
            // Last 1000,000 requests
            String warn = String.format("[%s]Access paid API The number of times is about to run out, and the remaining amount is insufficient 10, please use %s towards FUmo2eez6VK2sfGWjek9i9aK5y1mdHSnqv Transfer", symbol(), via);
            getLog().warn(warn);
        }
        if (getLog().isDebugEnabled())
            getLog().debug("[{}]PaidAPIRemaining visits of: {}, Remaining amount: {}", symbol(), balance / 1000, new BigDecimal(balance).movePointLeft(8).stripTrailingZeros().toPlainString());
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
        Object data = apipClient.getResponseBody().getData();
        if (data == null) {
            return null;
        }
        return ApipDataGetter.getCashList(data);
    }

    public List<Cash> getUTXOsByIds(String[] cashIds) {
        ApipClient client = BlockchainAPIs.cashByIdsPost(rpc, cashIds, via, sessionKey);
        Object data = checkApiBalance(client.getResponseBody());
        if (data == null) {
            return null;
        }
        return ApipDataGetter.getCashList(data);
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

}
