/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

package network.nerve.converter.core.validator;

import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.txdata.HeterogeneousContractAssetRegCompleteTxData;

/**
 * @author: Mimi
 * @date: 2020-03-23
 */
@Component
public class HeterogeneousContractAssetRegCompleteVerifier {


    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;

    public void validate(int chainId, Transaction tx) throws NulsException {
        if (tx == null || tx.getTxData() == null) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Chain chain = chainManager.getChain(chainId);
        NulsLogger logger = chain.getLogger();
        try {
            long s = System.currentTimeMillis();
            HeterogeneousContractAssetRegCompleteTxData txData = new HeterogeneousContractAssetRegCompleteTxData();
            txData.parse(tx.getTxData(), 0);
            String contractAddress = txData.getContractAddress().toLowerCase();
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getChainId());
            HeterogeneousAssetInfo assetInfo = docking.getAssetByContractAddress(contractAddress);
            // 资产已存在
            if(assetInfo != null) {
                logger.error("资产已存在");
                throw new NulsException(ConverterErrorCode.ASSET_EXIST);
            }
            // 资产信息验证
            if (!docking.validateHeterogeneousAssetInfoFromNet(contractAddress, txData.getSymbol(), txData.getDecimals())) {
                logger.error("资产信息不匹配");
                throw new NulsException(ConverterErrorCode.REG_ASSET_INFO_INCONSISTENCY);
            }
            long e = System.currentTimeMillis();
            if(logger.isDebugEnabled()) {
                logger.debug("[异构链合约资产注册信息-validate], 调用异构链[validateHeterogeneousAssetInfoFromNet]时间:{}", e - s);
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            if(e instanceof NulsException) {
                throw (NulsException) e;
            }
            throw new NulsException(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

}
