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
package network.nerve.converter.heterogeneouschain.bnb.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbWaitingTxPo;
import network.nerve.converter.heterogeneouschain.bnb.storage.BnbTxInvokeInfoStorageService;

import java.math.BigInteger;

/**
 * @author: Mimi
 * @date: 2020-08-26
 */
@Component
public class BnbInvokeTxHelper {

    @Autowired
    private BnbTxInvokeInfoStorageService bnbTxInvokeInfoStorageService;

    public void saveWaittingInvokeQueue(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, int currentNodeSendOrder, BnbWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.WITHDRAW);
            info.setTo(toAddress);
            info.setValue(value);
            info.setAssetId(assetId);
            bnbTxInvokeInfoStorageService.save(nerveTxHash, info);
            BnbContext.WAITING_TX_QUEUE.offer(info);
        }
    }

    public void saveWaittingInvokeQueue(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData, int currentNodeSendOrder, BnbWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.CHANGE);
            info.setAddAddresses(addAddresses);
            info.setRemoveAddresses(removeAddresses);
            info.setOrginTxCount(orginTxCount);
            bnbTxInvokeInfoStorageService.save(nerveTxHash, info);
            BnbContext.WAITING_TX_QUEUE.offer(info);
        }
    }

    public void saveWaittingInvokeQueue(String nerveTxHash, String upgradeContract, String signatureData, int currentNodeSendOrder, BnbWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.UPGRADE);
            info.setUpgradeContract(upgradeContract);
            bnbTxInvokeInfoStorageService.save(nerveTxHash, info);
            BnbContext.WAITING_TX_QUEUE.offer(info);
        }
    }

    public boolean ifSavedWaittingPo(String nerveTxHash) {
        return bnbTxInvokeInfoStorageService.existNerveTxHash(nerveTxHash);
    }

    public void clearRecordOfCurrentNodeSentEthTx(String nerveTxHash, BnbWaitingTxPo po) throws Exception {
        this.clearEthWaitingTxPo(po);
        bnbTxInvokeInfoStorageService.deleteSentEthTx(nerveTxHash);
    }

    private void saveCommonData(BnbWaitingTxPo info, String nerveTxHash, String signatureData, int currentNodeSendOrder) {
        if (info == null) {
            return;
        }
        info.setNerveTxHash(nerveTxHash);
        info.setCurrentNodeSendOrder(currentNodeSendOrder);
        info.setSignatures(signatureData);
        info.setWaitingEndTime(System.currentTimeMillis() + BnbConstant.MINUTES_5 * (currentNodeSendOrder - 1));
        info.setMaxWaitingEndTime(System.currentTimeMillis() + BnbConstant.MINUTES_5 * (info.getCurrentVirtualBanks().size() - 1));
    }

    public BnbWaitingTxPo findEthWaitingTxPo(String nerveTxHash) {
        return bnbTxInvokeInfoStorageService.findEthWaitingTxPo(nerveTxHash);
    }

    public void clearEthWaitingTxPo(BnbWaitingTxPo po) throws Exception {
        po.setWaitingEndTime(System.currentTimeMillis() + BnbConstant.MINUTES_5 * (po.getCurrentNodeSendOrder() - 1));
        po.setMaxWaitingEndTime(System.currentTimeMillis() + BnbConstant.MINUTES_5 * (po.getCurrentVirtualBanks().size() - 1));
        po.setValidateHeight(BnbContext.getConverterCoreApi().getCurrentBlockHeightOnNerve() + 30);
        bnbTxInvokeInfoStorageService.save(po.getNerveTxHash(), po);
    }

    public void saveSuccessfulNerve(String nerveTxHash) throws Exception {
        bnbTxInvokeInfoStorageService.saveCompletedNerveTx(nerveTxHash);
    }

    public boolean isSuccessfulNerve(String nerveTxHash) throws Exception {
        return bnbTxInvokeInfoStorageService.ifCompletedNerveTx(nerveTxHash);
    }

    public void saveSentEthTx(String nerveTxHash) throws Exception {
        bnbTxInvokeInfoStorageService.saveSentEthTx(nerveTxHash);
    }

    public boolean currentNodeSentEthTx(String nerveTxHash) throws Exception {
        return bnbTxInvokeInfoStorageService.ifSentEthTx(nerveTxHash);
    }
}
