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
package network.nerve.converter.heterogeneouschain.ht.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.ht.constant.HtConstant;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
import network.nerve.converter.heterogeneouschain.ht.model.HtWaitingTxPo;
import network.nerve.converter.heterogeneouschain.ht.storage.HtTxInvokeInfoStorageService;

import java.math.BigInteger;

/**
 * @author: Mimi
 * @date: 2020-08-26
 */
@Component
public class HtInvokeTxHelper {

    @Autowired
    private HtTxInvokeInfoStorageService htTxInvokeInfoStorageService;

    public void saveWaittingInvokeQueue(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, int currentNodeSendOrder, HtWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.WITHDRAW);
            info.setTo(toAddress);
            info.setValue(value);
            info.setAssetId(assetId);
            htTxInvokeInfoStorageService.save(nerveTxHash, info);
            HtContext.WAITING_TX_QUEUE.offer(info);
        }
    }

    public void saveWaittingInvokeQueue(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData, int currentNodeSendOrder, HtWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.CHANGE);
            info.setAddAddresses(addAddresses);
            info.setRemoveAddresses(removeAddresses);
            info.setOrginTxCount(orginTxCount);
            htTxInvokeInfoStorageService.save(nerveTxHash, info);
            HtContext.WAITING_TX_QUEUE.offer(info);
        }
    }

    public void saveWaittingInvokeQueue(String nerveTxHash, String upgradeContract, String signatureData, int currentNodeSendOrder, HtWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.UPGRADE);
            info.setUpgradeContract(upgradeContract);
            htTxInvokeInfoStorageService.save(nerveTxHash, info);
            HtContext.WAITING_TX_QUEUE.offer(info);
        }
    }

    public boolean ifSavedWaittingPo(String nerveTxHash) {
        return htTxInvokeInfoStorageService.existNerveTxHash(nerveTxHash);
    }

    public void clearRecordOfCurrentNodeSentEthTx(String nerveTxHash, HtWaitingTxPo po) throws Exception {
        this.clearEthWaitingTxPo(po);
        htTxInvokeInfoStorageService.deleteSentEthTx(nerveTxHash);
    }

    private void saveCommonData(HtWaitingTxPo info, String nerveTxHash, String signatureData, int currentNodeSendOrder) {
        if (info == null) {
            return;
        }
        info.setNerveTxHash(nerveTxHash);
        info.setCurrentNodeSendOrder(currentNodeSendOrder);
        info.setSignatures(signatureData);
        info.setWaitingEndTime(System.currentTimeMillis() + HtConstant.MINUTES_5 * (currentNodeSendOrder - 1));
        info.setMaxWaitingEndTime(System.currentTimeMillis() + HtConstant.MINUTES_5 * (info.getCurrentVirtualBanks().size() - 1));
    }

    public HtWaitingTxPo findEthWaitingTxPo(String nerveTxHash) {
        return htTxInvokeInfoStorageService.findEthWaitingTxPo(nerveTxHash);
    }

    public void clearEthWaitingTxPo(HtWaitingTxPo po) throws Exception {
        po.setWaitingEndTime(System.currentTimeMillis() + HtConstant.MINUTES_5 * (po.getCurrentNodeSendOrder() - 1));
        po.setMaxWaitingEndTime(System.currentTimeMillis() + HtConstant.MINUTES_5 * (po.getCurrentVirtualBanks().size() - 1));
        po.setValidateHeight(HtContext.getConverterCoreApi().getCurrentBlockHeightOnNerve() + 30);
        htTxInvokeInfoStorageService.save(po.getNerveTxHash(), po);
    }

    public void saveSuccessfulNerve(String nerveTxHash) throws Exception {
        htTxInvokeInfoStorageService.saveCompletedNerveTx(nerveTxHash);
    }

    public boolean isSuccessfulNerve(String nerveTxHash) throws Exception {
        return htTxInvokeInfoStorageService.ifCompletedNerveTx(nerveTxHash);
    }

    public void saveSentEthTx(String nerveTxHash) throws Exception {
        htTxInvokeInfoStorageService.saveSentEthTx(nerveTxHash);
    }

    public boolean currentNodeSentEthTx(String nerveTxHash) throws Exception {
        return htTxInvokeInfoStorageService.ifSentEthTx(nerveTxHash);
    }
}
