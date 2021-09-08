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
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.txdata.HeterogeneousContractAssetRegCompleteTxData;

import java.util.HashSet;
import java.util.Set;

import static network.nerve.converter.utils.ConverterUtil.addressToLowerCase;

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
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;

    public void validate(int chainId, Transaction tx) throws NulsException {
        if (tx == null || tx.getTxData() == null) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Chain chain = chainManager.getChain(chainId);
        NulsLogger logger = chain.getLogger();
        try {
            long s = System.currentTimeMillis();
            Set<String> contractAssetRegSet = new HashSet<>();
            Set<String> bindNewSet = new HashSet<>();
            Set<String> bindRemoveSet = new HashSet<>();
            Set<String> bindOverrideSet = new HashSet<>();
            Set<String> unregisterSet = new HashSet<>();
            // 异构合约资产注册 OR NERVE资产绑定异构合约资产: 新绑定 / 覆盖绑定 / 取消绑定 OR 异构合约资产取消注册
            HeterogeneousContractAssetRegCompleteTxData txData = new HeterogeneousContractAssetRegCompleteTxData();
            txData.parse(tx.getTxData(), 0);
            String contractAddress = addressToLowerCase(txData.getContractAddress());
            String errorCode = ledgerAssetRegisterHelper.checkHeterogeneousContractAssetReg(chain, tx, contractAddress, txData.getDecimals(), txData.getSymbol(), txData.getChainId(), contractAssetRegSet, bindNewSet, bindRemoveSet, bindOverrideSet, unregisterSet, true);
            if (StringUtils.isNotBlank(errorCode)) {
                throw new NulsException(ErrorCode.init(errorCode));
            }
            if(logger.isDebugEnabled()) {
                long e = System.currentTimeMillis();
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
