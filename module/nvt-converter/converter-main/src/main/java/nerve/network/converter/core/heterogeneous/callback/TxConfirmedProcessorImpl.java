/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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
package nerve.network.converter.core.heterogeneous.callback;

import io.nuls.base.data.NulsHash;
import nerve.network.converter.core.business.AssembleTxService;
import nerve.network.converter.core.business.VirtualBankService;
import nerve.network.converter.core.heterogeneous.callback.interfaces.ITxConfirmedProcessor;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import nerve.network.converter.enums.HeterogeneousChainTxType;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.HeterogeneousAddress;
import nerve.network.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import nerve.network.converter.model.po.HeterogeneousConfirmedChangeVBPo;
import nerve.network.converter.model.txdata.ConfirmWithdrawalTxData;
import nerve.network.converter.storage.HeterogeneousConfirmedChangeVBStorageService;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;

import java.util.*;

/**
 * @author: Chino
 * @date: 2020-02-18
 */
public class TxConfirmedProcessorImpl implements ITxConfirmedProcessor {

    private Chain nerveChain;
    /**
     * 异构链chainId
     */
    private int hChainId;
    private AssembleTxService assembleTxService;
    private HeterogeneousDockingManager heterogeneousDockingManager;
    private HeterogeneousConfirmedChangeVBStorageService heterogeneousConfirmedChangeVBStorageService;
    private VirtualBankService virtualBankService;

    public TxConfirmedProcessorImpl(Chain nerveChain, int hChainId, AssembleTxService assembleTxService,
                                    HeterogeneousDockingManager heterogeneousDockingManager,
                                    HeterogeneousConfirmedChangeVBStorageService heterogeneousConfirmedChangeVBStorageService,
                                    VirtualBankService virtualBankService) {
        this.nerveChain = nerveChain;
        this.hChainId = hChainId;
        this.assembleTxService = assembleTxService;
        this.heterogeneousDockingManager = heterogeneousDockingManager;
        this.heterogeneousConfirmedChangeVBStorageService = heterogeneousConfirmedChangeVBStorageService;
        this.virtualBankService = virtualBankService;
    }

    private NulsLogger logger() {
        return nerveChain.getLogger();
    }

    /**
     * 管理员变更: 收集完所有异构链组件的管理员变更确认交易，再组装发送交易
     * 提现: 直接组装发送交易
     *
     * @param txType           交易类型 - WITHDRAW/CHANGE 提现/管理员变更
     * @param nerveTxHash      本链交易hash
     * @param txHash           异构链交易hash
     * @param blockHeight      异构链交易确认高度
     * @param txTime           异构链交易时间
     * @param multiSignAddress 最新多签地址
     * @param signers          交易签名地址列表
     */
    @Override
    public void txConfirmed(HeterogeneousChainTxType txType, String nerveTxHash, String txHash, Long blockHeight, Long txTime, String multiSignAddress, List<HeterogeneousAddress> signers) throws Exception {
        switch (txType) {
            case WITHDRAW:
                ConfirmWithdrawalTxData txData = new ConfirmWithdrawalTxData();
                txData.setHeterogeneousChainId(hChainId);
                txData.setHeterogeneousHeight(blockHeight);
                txData.setHeterogeneousTxHash(txHash);
                txData.setWithdrawalTxHash(NulsHash.fromHex(nerveTxHash));
                txData.setListDistributionFee(signers);
                assembleTxService.createConfirmWithdrawalTx(nerveChain, txData, txTime);
                break;
            case CHANGE:
                HeterogeneousConfirmedVirtualBank bank = new HeterogeneousConfirmedVirtualBank();
                bank.setEffectiveTime(txTime);
                bank.setHeterogeneousChainId(hChainId);
                bank.setHeterogeneousTxHash(txHash);
                bank.setHeterogeneousAddress(multiSignAddress);
                HeterogeneousConfirmedChangeVBPo vbPo = heterogeneousConfirmedChangeVBStorageService.findByTxHash(nerveTxHash);
                Set<HeterogeneousConfirmedVirtualBank> vbSet = vbPo.getHgCollection();
                // 检查重复添加
                if (!vbSet.add(bank)) {
                    return;
                }
                int hChainSize = heterogeneousDockingManager.getAllHeterogeneousDocking().size();
                // 收集完成，组装广播交易
                if (vbSet.size() == hChainSize) {
                    List<HeterogeneousConfirmedVirtualBank> hList = new ArrayList<>(vbSet);
                    sortListByChainId(hList);
                    assembleTxService.createConfirmedChangeVirtualBankTx(nerveChain, NulsHash.fromHex(nerveTxHash), hList, hList.get(0).getEffectiveTime());
                } else {
                    heterogeneousConfirmedChangeVBStorageService.save(vbPo);
                }
                break;
        }
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

    private static void sortListByChainId(List<HeterogeneousConfirmedVirtualBank> hList) {
        Collections.sort(hList, new Comparator<HeterogeneousConfirmedVirtualBank>() {
            @Override
            public int compare(HeterogeneousConfirmedVirtualBank o1, HeterogeneousConfirmedVirtualBank o2) {
                if (o1.getHeterogeneousChainId() > o2.getHeterogeneousChainId()) {
                    return 1;
                } else if (o1.getHeterogeneousChainId() < o2.getHeterogeneousChainId()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }
}
