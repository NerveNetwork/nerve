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
package network.nerve.converter.core.api;

import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousWithdrawTxInfo;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.HeterogeneousAssetConverterStorageService;
import network.nerve.converter.utils.ConverterUtil;

/**
 * @author: Mimi
 * @date: 2020-05-08
 */
@Component
public class ConverterCoreApi implements IConverterCoreApi {

    private Chain nerveChain;
    @Autowired
    private VirtualBankService virtualBankService;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService;

    private NulsLogger logger() {
        return nerveChain.getLogger();
    }

    public void setNerveChain(Chain nerveChain) {
        this.nerveChain = nerveChain;
    }

    @Override
    public long getCurrentBlockHeightOnNerve() {
        return nerveChain.getLatestBasicBlock().getHeight();
    }

    @Override
    public boolean isVirtualBankByCurrentNode() {
        try {
            return virtualBankService.getCurrentDirector(nerveChain.getChainId()) != null;
        } catch (NulsException e) {
            logger().error("查询节点状态失败", e);
            return false;
        }
    }

    @Override
    public boolean isSeedVirtualBankByCurrentNode() {
        try {
            VirtualBankDirector currentDirector = virtualBankService.getCurrentDirector(nerveChain.getChainId());
            if(null != currentDirector){
                return currentDirector.getSeedNode();
            }
            return false;
        } catch (NulsException e) {
            logger().error("查询节点状态失败", e);
            return false;
        }
    }

    @Override
    public int getVirtualBankOrder() {
        try {
            VirtualBankDirector director = virtualBankService.getCurrentDirector(nerveChain.getChainId());
            if(director == null) {
                return 0;
            }
            return director.getOrder();
        } catch (NulsException e) {
            logger().error("查询节点状态失败", e);
            return 0;
        }
    }

    @Override
    public int getVirtualBankSize() {
        return nerveChain.getMapVirtualBank().size();
    }

    @Override
    public Transaction getNerveTx(String hash) {
        try {
            if(!ConverterUtil.isHexStr(hash)) {
                return null;
            }
            return TransactionCall.getConfirmedTx(nerveChain, NulsHash.fromHex(hash));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isRunning() {
        return SyncStatusEnum.RUNNING == nerveChain.getLatestBasicBlock().getSyncStatusEnum();
    }

    @Override
    public HeterogeneousWithdrawTxInfo getWithdrawTxInfo(String nerveTxHash) throws NulsException {
        Transaction tx = TransactionCall.getConfirmedTx(nerveChain, NulsHash.fromHex(nerveTxHash));
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        CoinTo withdrawCoinTo = null;
        for (CoinTo coinTo : coinData.getTo()) {
            if (coinTo.getAssetsId() != nerveChain.getConfig().getAssetId()) {
                withdrawCoinTo = coinTo;
                break;
            }
        }
        if(null == withdrawCoinTo){
            nerveChain.getLogger().error("Withdraw transaction cointo data error, no withdrawCoinTo. hash:{}", nerveTxHash);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        HeterogeneousAssetInfo heterogeneousAssetInfo =
                heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(withdrawCoinTo.getAssetsId());
        if(null == heterogeneousAssetInfo) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
        }

        WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
        HeterogeneousWithdrawTxInfo info = new HeterogeneousWithdrawTxInfo();
        info.setHeterogeneousChainId(heterogeneousAssetInfo.getChainId());
        info.setAssetId(heterogeneousAssetInfo.getAssetId());
        info.setNerveTxHash(nerveTxHash);
        info.setToAddress(txData.getHeterogeneousAddress());
        info.setValue(withdrawCoinTo.getAmount());
        return info;
    }
}
