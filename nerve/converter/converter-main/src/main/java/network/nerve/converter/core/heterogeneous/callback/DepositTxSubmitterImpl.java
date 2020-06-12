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
package network.nerve.converter.core.heterogeneous.callback;

import io.nuls.base.data.Transaction;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IDepositTxSubmitter;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.dto.RechargeTxDTO;

import java.math.BigInteger;

/**
 * @author: Mimi
 * @date: 2020-02-18
 */
public class DepositTxSubmitterImpl implements IDepositTxSubmitter {

    private Chain nerveChain;
    /**
     * 异构链chainId
     */
    private int hChainId;
    private AssembleTxService assembleTxService;
    private HeterogeneousDockingManager heterogeneousDockingManager;
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;

    public DepositTxSubmitterImpl(Chain nerveChain, int hChainId, AssembleTxService assembleTxService, HeterogeneousDockingManager heterogeneousDockingManager, LedgerAssetRegisterHelper ledgerAssetRegisterHelper) {
        this.nerveChain = nerveChain;
        this.hChainId = hChainId;
        this.assembleTxService = assembleTxService;
        this.heterogeneousDockingManager = heterogeneousDockingManager;
        this.ledgerAssetRegisterHelper = ledgerAssetRegisterHelper;
    }

    /**
     * @param txHash          交易hash
     * @param blockHeight     交易确认高度
     * @param from            转出地址
     * @param to              转入地址
     * @param value           转账金额
     * @param txTime          交易时间
     * @param decimals        资产小数位数
     * @param ifContractAsset 是否为合约资产
     * @param contractAddress 合约地址
     * @param assetId         资产ID
     * @param nerveAddress    Nerve充值地址
     */
    @Override
    public String txSubmit(String txHash, Long blockHeight, String from, String to, BigInteger value, Long txTime,
                         Integer decimals, Boolean ifContractAsset, String contractAddress, Integer assetId, String nerveAddress) throws Exception {
        /*IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        // 向账本登记(覆盖登记)异构链资产
        HeterogeneousAssetInfo assetInfo = docking.getAssetByAssetId(assetId);
        ledgerAssetRegisterHelper.crossChainAssetReg(nerveChain.getChainId(), assetInfo.getChainId(), assetInfo.getAssetId(),
                assetInfo.getSymbol(), assetInfo.getDecimals(), assetInfo.getSymbol(), assetInfo.getContractAddress());*/

        // 组装Nerve充值交易
        RechargeTxDTO dto = new RechargeTxDTO();
        dto.setOriginalTxHash(txHash);
        dto.setToAddress(nerveAddress);
        dto.setAmount(value);
        dto.setHeterogeneousChainId(hChainId);
        dto.setHeterogeneousAssetId(assetId);
        dto.setTxtime(txTime);
        Transaction tx = assembleTxService.createRechargeTx(nerveChain, dto);
        if(tx == null) {
            return null;
        }
        return tx.getHash().toHex();
    }
}
