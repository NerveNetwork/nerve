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
package network.nerve.converter.message.helper;

import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.dto.RechargeTxDTO;
import network.nerve.converter.model.po.HeterogeneousConfirmedChangeVBPo;
import network.nerve.converter.model.txdata.ConfirmWithdrawalTxData;
import network.nerve.converter.storage.HeterogeneousConfirmedChangeVBStorageService;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: Mimi
 * @date: 2020-04-29
 */
@Component
public class ByzantineTransactionHelper {

    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private HeterogeneousConfirmedChangeVBStorageService heterogeneousConfirmedChangeVBStorageService;

    public boolean genByzantineTransaction(Chain nerveChain, String byzantineTxhash, int txType, String nerveTxHash, List<HeterogeneousHash> hashList) throws Exception {
        boolean validation = false;
        HeterogeneousHash hash;
        switch (txType) {
            case TxType.RECHARGE:
                hash = hashList.get(0);
                validation = recharge(nerveChain, byzantineTxhash, hash.getHeterogeneousChainId(), hash.getHeterogeneousHash());
                break;
            case TxType.CONFIRM_WITHDRAWAL:
                hash = hashList.get(0);
                validation = withdraw(nerveChain, byzantineTxhash, nerveTxHash, hash.getHeterogeneousChainId(), hash.getHeterogeneousHash());
                break;
            case TxType.CONFIRM_CHANGE_VIRTUAL_BANK:
                validation = change(nerveChain, byzantineTxhash, nerveTxHash, hashList);
                break;
        }
        return validation;
    }

    private boolean recharge(Chain nerveChain, String byzantineTxhash, int hChainId, String hTxHash) throws Exception {
        nerveChain.getLogger().info("创建[充值]拜占庭交易[{}]消息, 异构链交易hash: {}", byzantineTxhash, hTxHash);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        HeterogeneousTransactionInfo depositTx = docking.getDepositTransaction(hTxHash);
        RechargeTxDTO dto = new RechargeTxDTO();
        dto.setOriginalTxHash(hTxHash);
        dto.setToAddress(depositTx.getNerveAddress());
        dto.setAmount(depositTx.getValue());
        dto.setHeterogeneousChainId(hChainId);
        dto.setHeterogeneousAssetId(depositTx.getAssetId());
        dto.setTxtime(depositTx.getTxTime());
        Transaction tx = assembleTxService.createRechargeTxWithoutSign(nerveChain, dto);
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[充值]拜占庭交易验证失败");
            return false;
        }
        assembleTxService.createRechargeTx(nerveChain, dto);
        return true;
    }

    private boolean withdraw(Chain nerveChain, String byzantineTxhash, String nerveTxHash, int hChainId, String hTxHash) throws Exception {
        nerveChain.getLogger().info("创建[确认提现]拜占庭交易[{}]消息, 异构链交易hash: {}", byzantineTxhash, hTxHash);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        HeterogeneousTransactionInfo hTx = docking.getWithdrawTransaction(hTxHash);
        ConfirmWithdrawalTxData txData = new ConfirmWithdrawalTxData();
        txData.setHeterogeneousChainId(hChainId);
        txData.setHeterogeneousHeight(hTx.getBlockHeight());
        txData.setHeterogeneousTxHash(hTxHash);
        txData.setWithdrawalTxHash(NulsHash.fromHex(nerveTxHash));
        txData.setListDistributionFee(hTx.getSigners());
        Transaction tx = assembleTxService.createConfirmWithdrawalTxWithoutSign(nerveChain, txData, hTx.getTxTime());
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[确认提现]拜占庭交易验证失败");
            return false;
        }
        assembleTxService.createConfirmWithdrawalTx(nerveChain, txData, hTx.getTxTime());
        return true;
    }

    private boolean change(Chain nerveChain, String byzantineTxhash, String nerveTxHash, List<HeterogeneousHash> hashList) throws Exception {
        nerveChain.getLogger().info("创建[确认变更]拜占庭交易[{}]消息, 异构链交易hash: {}", byzantineTxhash, JSONUtils.obj2json(hashList));
        HeterogeneousConfirmedChangeVBPo vbPo = heterogeneousConfirmedChangeVBStorageService.findByTxHash(nerveTxHash);
        if(vbPo == null) {
            vbPo = new HeterogeneousConfirmedChangeVBPo();
            vbPo.setNerveTxHash(nerveTxHash);
            vbPo.setHgCollection(new HashSet<>());
        }
        Set<HeterogeneousConfirmedVirtualBank> vbSet = vbPo.getHgCollection();
        int hChainId;
        String hTxHash;
        for(HeterogeneousHash hash : hashList) {
            hChainId = hash.getHeterogeneousChainId();
            hTxHash = hash.getHeterogeneousHash();
            IHeterogeneousChainDocking chainDocking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
            HeterogeneousConfirmedInfo confirmedTxInfo = chainDocking.getChangeVirtualBankConfirmedTxInfo(hTxHash);
            HeterogeneousConfirmedVirtualBank bank = new HeterogeneousConfirmedVirtualBank();
            bank.setEffectiveTime(confirmedTxInfo.getTxTime());
            bank.setHeterogeneousChainId(hChainId);
            bank.setHeterogeneousTxHash(hTxHash);
            bank.setHeterogeneousAddress(confirmedTxInfo.getMultySignAddress());
            vbSet.add(bank);
        }
        List<HeterogeneousConfirmedVirtualBank> hList = new ArrayList<>(vbSet);
        VirtualBankUtil.sortListByChainId(hList);
        Transaction tx = assembleTxService.createConfirmedChangeVirtualBankTxWithoutSign(nerveChain, NulsHash.fromHex(nerveTxHash), hList, hList.get(0).getEffectiveTime());
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[确认变更]拜占庭交易验证失败");
            return false;
        }
        assembleTxService.createConfirmedChangeVirtualBankTx(nerveChain, NulsHash.fromHex(nerveTxHash), hList, hList.get(0).getEffectiveTime());
        return true;
    }
}
