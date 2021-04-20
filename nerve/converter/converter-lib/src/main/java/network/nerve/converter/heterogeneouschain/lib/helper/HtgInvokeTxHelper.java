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
package network.nerve.converter.heterogeneouschain.lib.helper;

import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxInvokeInfoStorageService;

import java.math.BigInteger;

/**
 * @author: Mimi
 * @date: 2020-08-26
 */
public class HtgInvokeTxHelper implements BeanInitial {

    private HtgTxInvokeInfoStorageService htTxInvokeInfoStorageService;
    private HtgContext htgContext;

    //public HtgInvokeTxHelper(BeanMap beanMap) {
    //    this.htTxInvokeInfoStorageService = (HtgTxInvokeInfoStorageService) beanMap.get("htTxInvokeInfoStorageService");
    //    this.htgContext = (HtgContext) beanMap.get("htgContext");
    //}

    public void saveWaittingInvokeQueue(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, int currentNodeSendOrder, HtgWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.WITHDRAW);
            info.setTo(toAddress);
            info.setValue(value);
            info.setAssetId(assetId);
            htTxInvokeInfoStorageService.save(nerveTxHash, info);
            htgContext.WAITING_TX_QUEUE().offer(info);
        }
    }

    public void saveWaittingInvokeQueue(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData, int currentNodeSendOrder, HtgWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.CHANGE);
            info.setAddAddresses(addAddresses);
            info.setRemoveAddresses(removeAddresses);
            info.setOrginTxCount(orginTxCount);
            htTxInvokeInfoStorageService.save(nerveTxHash, info);
            htgContext.WAITING_TX_QUEUE().offer(info);
        }
    }

    public void saveWaittingInvokeQueue(String nerveTxHash, String upgradeContract, String signatureData, int currentNodeSendOrder, HtgWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.UPGRADE);
            info.setUpgradeContract(upgradeContract);
            htTxInvokeInfoStorageService.save(nerveTxHash, info);
            htgContext.WAITING_TX_QUEUE().offer(info);
        }
    }

    public boolean ifSavedWaittingPo(String nerveTxHash) {
        return htTxInvokeInfoStorageService.existNerveTxHash(nerveTxHash);
    }

    public void clearRecordOfCurrentNodeSentEthTx(String nerveTxHash, HtgWaitingTxPo po) throws Exception {
        this.clearEthWaitingTxPo(po);
        htTxInvokeInfoStorageService.deleteSentEthTx(nerveTxHash);
    }

    private void saveCommonData(HtgWaitingTxPo info, String nerveTxHash, String signatureData, int currentNodeSendOrder) {
        if (info == null) {
            return;
        }
        info.setNerveTxHash(nerveTxHash);
        info.setCurrentNodeSendOrder(currentNodeSendOrder);
        info.setSignatures(signatureData);
        info.setWaitingEndTime(System.currentTimeMillis() + HtgConstant.MINUTES_5 * (currentNodeSendOrder - 1));
        info.setMaxWaitingEndTime(System.currentTimeMillis() + HtgConstant.MINUTES_5 * (info.getCurrentVirtualBanks().size() - 1));
    }

    public HtgWaitingTxPo findEthWaitingTxPo(String nerveTxHash) {
        return htTxInvokeInfoStorageService.findEthWaitingTxPo(nerveTxHash);
    }

    public void clearEthWaitingTxPo(HtgWaitingTxPo po) throws Exception {
        po.setWaitingEndTime(System.currentTimeMillis() + HtgConstant.MINUTES_5 * (po.getCurrentNodeSendOrder() - 1));
        po.setMaxWaitingEndTime(System.currentTimeMillis() + HtgConstant.MINUTES_5 * (po.getCurrentVirtualBanks().size() - 1));
        po.setValidateHeight(htgContext.getConverterCoreApi().getCurrentBlockHeightOnNerve() + 30);
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
