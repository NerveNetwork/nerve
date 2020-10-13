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
package network.nerve.converter.heterogeneouschain.ethII.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.ethII.context.EthIIContext;
import network.nerve.converter.heterogeneouschain.ethII.model.EthWaitingTxPo;
import network.nerve.converter.heterogeneouschain.ethII.storage.EthTxInvokeInfoStorageService;

import java.math.BigInteger;

/**
 * @author: Mimi
 * @date: 2020-08-26
 */
@Component
public class EthIIInvokeTxHelper {

    @Autowired
    private EthTxInvokeInfoStorageService ethTxInvokeInfoStorageService;

    public void saveWaittingInvokeQueue(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, int currentNodeSendOrder, EthWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.WITHDRAW);
            info.setTo(toAddress);
            info.setValue(value);
            info.setAssetId(assetId);
            ethTxInvokeInfoStorageService.save(nerveTxHash, info);
            EthIIContext.WAITING_TX_QUEUE.offer(info);
        }
    }

    public void saveWaittingInvokeQueue(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData, int currentNodeSendOrder, EthWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.CHANGE);
            info.setAddAddresses(addAddresses);
            info.setRemoveAddresses(removeAddresses);
            info.setOrginTxCount(orginTxCount);
            ethTxInvokeInfoStorageService.save(nerveTxHash, info);
            EthIIContext.WAITING_TX_QUEUE.offer(info);
        }
    }

    public void saveWaittingInvokeQueue(String nerveTxHash, String upgradeContract, String signatureData, int currentNodeSendOrder, EthWaitingTxPo info) throws Exception {
        // 保存交易调用参数，设置等待结束时间
        boolean exist = this.ifSavedWaittingPo(nerveTxHash);
        if (!exist) {
            this.saveCommonData(info, nerveTxHash, signatureData, currentNodeSendOrder);
            info.setTxType(HeterogeneousChainTxType.UPGRADE);
            info.setUpgradeContract(upgradeContract);
            ethTxInvokeInfoStorageService.save(nerveTxHash, info);
            EthIIContext.WAITING_TX_QUEUE.offer(info);
        }
    }

    public boolean ifSavedWaittingPo(String nerveTxHash) {
        return ethTxInvokeInfoStorageService.existNerveTxHash(nerveTxHash);
    }

    public void clearRecordOfCurrentNodeSentEthTx(String nerveTxHash, EthWaitingTxPo po) throws Exception {
        this.clearEthWaitingTxPo(po);
        ethTxInvokeInfoStorageService.deleteSentEthTx(nerveTxHash);
    }

    private void saveCommonData(EthWaitingTxPo info, String nerveTxHash, String signatureData, int currentNodeSendOrder) {
        if (info == null) {
            return;
        }
        info.setNerveTxHash(nerveTxHash);
        info.setCurrentNodeSendOrder(currentNodeSendOrder);
        info.setSignatures(signatureData);
        info.setWaitingEndTime(System.currentTimeMillis() + EthConstant.MINUTES_5 * (currentNodeSendOrder - 1));
        info.setMaxWaitingEndTime(System.currentTimeMillis() + EthConstant.MINUTES_5 * (info.getCurrentVirtualBanks().size() - 1));
    }

    public EthWaitingTxPo findEthWaitingTxPo(String nerveTxHash) {
        return ethTxInvokeInfoStorageService.findEthWaitingTxPo(nerveTxHash);
    }

    public void clearEthWaitingTxPo(EthWaitingTxPo po) throws Exception {
        po.setWaitingEndTime(System.currentTimeMillis() + EthConstant.MINUTES_5 * (po.getCurrentNodeSendOrder() - 1));
        po.setMaxWaitingEndTime(System.currentTimeMillis() + EthConstant.MINUTES_5 * (po.getCurrentVirtualBanks().size() - 1));
        po.setValidateHeight(EthContext.getConverterCoreApi().getCurrentBlockHeightOnNerve() + 30);
        ethTxInvokeInfoStorageService.save(po.getNerveTxHash(), po);
    }

    public void saveSuccessfulNerve(String nerveTxHash) throws Exception {
        ethTxInvokeInfoStorageService.saveCompletedNerveTx(nerveTxHash);
    }

    public boolean isSuccessfulNerve(String nerveTxHash) throws Exception {
        return ethTxInvokeInfoStorageService.ifCompletedNerveTx(nerveTxHash);
    }

    public void saveSentEthTx(String nerveTxHash) throws Exception {
        ethTxInvokeInfoStorageService.saveSentEthTx(nerveTxHash);
    }

    public boolean currentNodeSentEthTx(String nerveTxHash) throws Exception {
        return ethTxInvokeInfoStorageService.ifSentEthTx(nerveTxHash);
    }
}
