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

package network.nerve.converter.core.business.impl;

import io.nuls.core.basic.Result;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IDepositTxSubmitter;
import network.nerve.converter.core.heterogeneous.callback.management.HeterogeneousCallBackManager;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.message.CancelHtgTxMessage;
import network.nerve.converter.message.CheckRetryParseMessage;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.rpc.call.NetWorkCall;
import network.nerve.converter.storage.PersistentCacheStroageService;
import network.nerve.converter.storage.TxStorageService;
import network.nerve.converter.utils.VirtualBankUtil;

import static network.nerve.converter.constant.ConverterDBConstant.*;

/**
 * @author: Loki
 * @date: 2020/3/18
 */
@Component
public class HeterogeneousServiceImpl implements HeterogeneousService {

    @Autowired
    private HeterogeneousCallBackManager heterogeneousCallBackManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private PersistentCacheStroageService persistentCacheStroageService;
    @Autowired
    private TxStorageService txStorageService;

    /**
     * 判断是否需要组装当前网络的主资产补贴异构链交易手续费
     * 异构链是合约类型,并且提现资产不是异构链主资产,才收取当前网络主资产作为手续费补贴
     * @param heterogeneousChainId
     * @param heterogeneousAssetId
     * @return
     */
    @Override
    public boolean isAssembleCurrentAssetFee(int heterogeneousChainId, int heterogeneousAssetId) throws NulsException {
        IHeterogeneousChainDocking heterogeneousDocking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
        return heterogeneousDocking.isSupportContractAssetByCurrentChain()
                && heterogeneousAssetId != ConverterConstant.ALL_MAIN_ASSET_ID;
    }

    @Override
    public boolean saveExeHeterogeneousChangeBankStatus(Chain chain, Boolean status) {
        chain.getHeterogeneousChangeBankExecuting().set(status);
        return persistentCacheStroageService.saveCacheState(chain, EXE_HETEROGENEOUS_CHANGE_BANK_KEY, status ? 1 : 0);
    }

    @Override
    public boolean saveExeDisqualifyBankProposalStatus(Chain chain, Boolean status) {
        chain.getExeDisqualifyBankProposal().set(status);
        return persistentCacheStroageService.saveCacheState(chain, EXE_DISQUALIFY_BANK_PROPOSAL_KEY, status ? 1 : 0);
    }

    @Override
    public boolean saveResetVirtualBankStatus(Chain chain, Boolean status) {
        chain.getResetVirtualBank().set(status);
        return persistentCacheStroageService.saveCacheState(chain, RESET_VIRTUALBANK_KEY, status ? 1 : 0);
    }

    @Override
    public void checkRetryParse(Chain chain, int heterogeneousChainId, String heterogeneousTxHash) throws NulsException {
        /**
         * 1.调组件
         * 2.发消息
         */
        if (!VirtualBankUtil.isCurrentDirector(chain)) {
            chain.getLogger().error("当前非虚拟银行成员节点, 不处理checkRetryParse");
            throw new NulsException(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK);
        }
        IDepositTxSubmitter submitter = heterogeneousCallBackManager.createOrGetDepositTxSubmitter(chain.getChainId(), heterogeneousChainId);
        Result result = submitter.validateDepositTx(heterogeneousTxHash);
        if(result.isFailed()){
            chain.getLogger().error("重新解析异构交易, validateDepositTx 验证失败, {}", result.getErrorCode().getCode());
            return;
        }
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
        try {
            boolean rs = docking.reAnalysisDepositTx(heterogeneousTxHash);
            if(rs) {
                txStorageService.saveHeterogeneousHash(chain, heterogeneousTxHash);
                CheckRetryParseMessage message = new CheckRetryParseMessage(heterogeneousChainId, heterogeneousTxHash);
                NetWorkCall.broadcast(chain, message, ConverterCmdConstant.CHECK_RETRY_PARSE_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new NulsException(e);
        }

    }

    @Override
    public void checkRetryHtgTx(Chain chain, int heterogeneousChainId, String heterogeneousTxHash) throws NulsException {
        /**
         * 1.调组件
         */
        if (!VirtualBankUtil.isCurrentDirector(chain)) {
            chain.getLogger().error("当前非虚拟银行成员节点, 不处理checkRetryHtgTx");
            throw new NulsException(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK);
        }
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
        try {
            boolean rs = docking.reAnalysisTx(heterogeneousTxHash);
            if (!rs) {
                throw new NulsException(CommonCodeConstanst.DATA_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new NulsException(e);
        }

    }

    @Override
    public void cancelHtgTx(Chain chain, int heterogeneousChainId, String address, String nonce, String priceGwei) throws NulsException {

        if (!VirtualBankUtil.isCurrentDirector(chain)) {
            chain.getLogger().error("当前非虚拟银行成员节点, 不处理cancelHtgTx");
            throw new NulsException(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK);
        }
        if (heterogeneousChainId <= 0
                || StringUtils.isBlank(address)
                || StringUtils.isBlank(nonce)
                || StringUtils.isBlank(priceGwei)
        ) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        try {
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            if (address.equalsIgnoreCase(docking.getCurrentSignAddress())) {
                String hash = docking.cancelHtgTx(nonce, priceGwei);
                chain.getLogger().info("[cancelHtgTx 消息处理完成] 异构chainId: {}, 异构address:{}, 取消操作的hash:{}", heterogeneousChainId, address, hash);
            } else {
                CancelHtgTxMessage message = new CancelHtgTxMessage(heterogeneousChainId, address, nonce, priceGwei);
                NetWorkCall.broadcast(chain, message, ConverterCmdConstant.CANCEL_HTG_TX_MESSAGE);
                chain.getLogger().info("[cancelHtgTx 消息转发完成] 异构chainId: {}, 异构address:{}", heterogeneousChainId, address);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
