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

import com.neemre.btcdcli4j.core.client.BtcdClient;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.BitCoinLibBlockHeader;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.BitCoinLibBlockInfo;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: PierreLuo
 * @date: 2024/7/9
 */
public interface IBitCoinLibWalletApi {
    void init(String rpcAddress) throws NulsException;
    String broadcast(String txHex);
    BigDecimal getBalance(String address);
    long getFeeRate();
    BitCoinLibBlockHeader getBitCoinLibBlockHeaderByHeight(long height);
    BitCoinLibBlockInfo getBitCoinLibBlockByHeight(Long height);
    long getBestBlockHeight();
    NulsLogger getLog();
    boolean isReSyncBlock();
    void setReSyncBlock(boolean reSyncBlock);
    HtgContext getHtgContext();
    ReentrantLock getCheckLock();
    int getRpcVersion();
    void setRpcVersion(int rpcVersion);
    boolean isUrlFromThirdPartyForce();
    void setUrlFromThirdPartyForce(boolean urlFromThirdPartyForce);
    void changeApi(String rpc) throws NulsException;
    String symbol();
    BtcdClient getClient();
    default void priceMaintain() {};
    default void additionalCheck(Map<String, Object> resultMap) {};
    default void checkApi() throws NulsException {
        this.getCheckLock().lock();
        try {
            do {
                // Force updates from third-party systems rpc
                Map<Long, Map> rpcCheckMap = this.getHtgContext().getConverterCoreApi().HTG_RPC_CHECK_MAP();
                Map<String, Object> resultMap = rpcCheckMap.get(this.getHtgContext().getConfig().getChainIdOnHtgNetwork());
                if (resultMap == null) {
                    //getLog().warn("Empty resultMap! {} rpc check from third party, version: {}", symbol(), rpcVersion);
                    break;
                }
                Integer _version = (Integer) resultMap.get("rpcVersion");
                if (_version == null) {
                    //getLog().warn("Empty rpcVersion! {} rpc check from third party, version: {}", symbol(), rpcVersion);
                    break;
                }
                if (this.getRpcVersion() == -1) {
                    this.setRpcVersion(_version.intValue());
                    getLog().info("initialization {} rpc check from third party, version: {}", symbol(), getRpcVersion());
                    break;
                }
                if (this.getRpcVersion() == _version.intValue()) {
                    //getLog().info("Same version {} rpc check from third party, version: {}", symbol(), rpcVersion);
                    break;
                }
                if (_version.intValue() > this.getRpcVersion()) {
                    this.additionalCheck(resultMap);
                    // find version Change, switch rpc
                    Integer _index = (Integer) resultMap.get("index");
                    if (_index == null) {
                        getLog().warn("Empty index! {} rpc check from third party, version: {}", symbol(), getRpcVersion());
                        break;
                    }
                    String apiUrl = (String) resultMap.get("extend" + (_index + 1));
                    if (StringUtils.isBlank(apiUrl)) {
                        getLog().warn("Empty apiUrl! {} rpc check from third party, version: {}", symbol(), getRpcVersion());
                        break;
                    }
                    this.changeApi(apiUrl);
                    getLog().info("Checked that changes are needed RPC service {} rpc check from third party, version: {}, url: {}", symbol(), _version.intValue(), apiUrl);
                    this.setRpcVersion(_version.intValue());
                    this.setUrlFromThirdPartyForce(true);
                    this.setReSyncBlock(true);
                    return;
                }
            } while (false);

            if (this.isUrlFromThirdPartyForce()) {
                getLog().info("[{}]Mandatory emergency response API(ThirdParty)During the use period, no longer based onbank order switch API", symbol());
                return;
            }

        } catch (Exception e) {
            throw e;
        } finally {
            this.getCheckLock().unlock();
        }
    }

}
