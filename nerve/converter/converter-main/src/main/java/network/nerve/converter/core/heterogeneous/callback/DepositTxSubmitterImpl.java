/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import network.nerve.converter.btc.model.BtcTxInfo;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IDepositTxSubmitter;
import network.nerve.converter.core.heterogeneous.callback.management.CallBackBeanManager;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousHash;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.bo.NerveAssetInfo;
import network.nerve.converter.model.dto.AddFeeCrossChainTxDTO;
import network.nerve.converter.model.dto.RechargeTxDTO;
import network.nerve.converter.model.dto.RechargeTxOfBtcSysDTO;
import network.nerve.converter.model.txdata.OneClickCrossChainUnconfirmedTxData;
import network.nerve.converter.model.txdata.RechargeUnconfirmedTxData;
import network.nerve.converter.rpc.call.TransactionCall;

import java.math.BigInteger;

/**
 * @author: Mimi
 * @date: 2020-02-18
 */
public class DepositTxSubmitterImpl implements IDepositTxSubmitter {

    private Chain nerveChain;
    /**
     * Heterogeneous chainchainId
     */
    private int hChainId;
    private AssembleTxService assembleTxService;
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;
    private ConverterCoreApi converterCoreApi;
    private HeterogeneousDockingManager heterogeneousDockingManager;

    public DepositTxSubmitterImpl(Chain nerveChain, int hChainId, CallBackBeanManager callBackBeanManager) {
        this.nerveChain = nerveChain;
        this.hChainId = hChainId;
        this.assembleTxService = callBackBeanManager.getAssembleTxService();
        this.ledgerAssetRegisterHelper = callBackBeanManager.getLedgerAssetRegisterHelper();
        this.converterCoreApi = callBackBeanManager.getConverterCoreApi();
        this.heterogeneousDockingManager = callBackBeanManager.getHeterogeneousDockingManager();
    }

    private IHeterogeneousChainDocking getDocking() {
        try {
            return heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        } catch (NulsException e) {
            throw new NulsRuntimeException(e);
        }
    }
    /**
     * @param txHash          transactionhash
     * @param blockHeight     Transaction confirmation height
     * @param from            Transfer address
     * @param to              Transfer address
     * @param value           Transfer amount
     * @param txTime          Transaction time
     * @param decimals        Decimal places of assets
     * @param ifContractAsset Whether it is a contract asset
     * @param contractAddress Contract address
     * @param assetId         assetID
     * @param nerveAddress    NerveRecharge address
     * @param extend
     */
    @Override
    public String txSubmit(String txHash, Long blockHeight, String from, String to, BigInteger value, Long txTime,
                           Integer decimals, Boolean ifContractAsset, String contractAddress, Integer assetId, String nerveAddress, String extend) throws Exception {
        // assembleNerveRecharge transaction
        RechargeTxDTO dto = new RechargeTxDTO();
        dto.setOriginalTxHash(txHash);
        dto.setHeterogeneousFromAddress(from);
        dto.setToAddress(nerveAddress);
        dto.setAmount(value);
        dto.setHeterogeneousChainId(hChainId);
        dto.setHeterogeneousAssetId(assetId);
        dto.setTxtime(txTime);
        dto.setExtend(extend);
        Transaction tx = assembleTxService.createRechargeTx(nerveChain, dto);
        if(tx == null) {
            return null;
        }
        return tx.getHash().toHex();
    }

    @Override
    public String depositIITxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger erc20Value, Long txTime, Integer erc20Decimals, String erc20ContractAddress, Integer erc20AssetId, String nerveAddress, BigInteger mainAssetValue, String extend) throws Exception {
        // Assembly transaction of simultaneously recharging two types of assets
        RechargeTxDTO dto = new RechargeTxDTO();
        dto.setOriginalTxHash(txHash);
        dto.setHeterogeneousFromAddress(from);
        dto.setToAddress(nerveAddress);
        dto.setAmount(erc20Value);
        dto.setHeterogeneousChainId(hChainId);
        dto.setHeterogeneousAssetId(erc20AssetId);
        dto.setTxtime(txTime);
        dto.setDepositII(true);
        dto.setMainAmount(mainAssetValue);
        dto.setExtend(extend);
        Transaction tx = assembleTxService.createRechargeTx(nerveChain, dto);
        if(tx == null) {
            return null;
        }
        return tx.getHash().toHex();
    }

    @Override
    public String oneClickCrossChainTxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger erc20Value, Long txTime,
                                             Integer erc20Decimals, String erc20ContractAddress, Integer erc20AssetId, String nerveAddress, BigInteger mainAssetValue,
                                             BigInteger mainAssetFeeAmount, int desChainId, String desToAddress, BigInteger tipping, String tippingAddress, String desExtend) throws Exception {
        OneClickCrossChainUnconfirmedTxData dto = new OneClickCrossChainUnconfirmedTxData();
        dto.setMainAssetFeeAmount(mainAssetFeeAmount);
        dto.setDesChainId(desChainId);
        dto.setDesToAddress(desToAddress);
        dto.setDesExtend(desExtend);
        dto.setOriginalTxHash(new HeterogeneousHash(hChainId, txHash));
        dto.setHeterogeneousHeight(blockHeight);
        dto.setHeterogeneousFromAddress(from);
        dto.setNerveToAddress(AddressTool.getAddress(nerveAddress));
        dto.setTipping(tipping);
        dto.setTippingAddress(tippingAddress);
        // Record main asset recharge data
        NerveAssetInfo mainAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, 1);
        dto.setMainAssetChainId(mainAsset.getAssetChainId());
        dto.setMainAssetId(mainAsset.getAssetId());
        dto.setMainAssetAmount(mainAssetValue);
        // recordtokenRecharge data
        dto.setErc20Amount(erc20Value);
        if (erc20AssetId > 1) {
            NerveAssetInfo tokenAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, erc20AssetId);
            dto.setErc20AssetChainId(tokenAsset.getAssetChainId());
            dto.setErc20AssetId(tokenAsset.getAssetId());
            dto.setTippingChainId(tokenAsset.getAssetChainId());
            dto.setTippingAssetId(tokenAsset.getAssetId());
        } else {
            dto.setTippingChainId(mainAsset.getAssetChainId());
            dto.setTippingAssetId(mainAsset.getAssetId());
        }
        Transaction tx = assembleTxService.createOneClickCrossChainTx(nerveChain, dto, txTime);
        if(tx == null) {
            return null;
        }
        return tx.getHash().toHex();
    }

    @Override
    public String addFeeCrossChainTxSubmit(String txHash, Long blockHeight, String from, String to, Long txTime, String nerveAddress, BigInteger mainAssetValue, String nerveTxHash, String subExtend) throws Exception {
        AddFeeCrossChainTxDTO dto = new AddFeeCrossChainTxDTO();
        dto.setOriginalTxHash(new HeterogeneousHash(hChainId, txHash));
        dto.setHeterogeneousHeight(blockHeight);
        dto.setHeterogeneousFromAddress(from);
        dto.setNerveToAddress(AddressTool.getAddress(nerveAddress));
        // Record main asset recharge data
        NerveAssetInfo mainAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, 1);
        dto.setMainAssetChainId(mainAsset.getAssetChainId());
        dto.setMainAssetId(mainAsset.getAssetId());
        dto.setMainAssetAmount(mainAssetValue);
        dto.setNerveTxHash(nerveTxHash);
        dto.setSubExtend(subExtend);
        // recordtokenRecharge data
        Transaction tx = assembleTxService.createAddFeeCrossChainTx(nerveChain, dto, txTime);
        if(tx == null) {
            return null;
        }
        return tx.getHash().toHex();
    }

    @Override
    public String pendingTxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger value, Long txTime,
                                  Integer decimals, Boolean ifContractAsset, String contractAddress, Integer assetId, String nerveAddress) throws Exception {
        if (!converterCoreApi.isSupportNewMechanismOfWithdrawalFee()) {
            return null;
        }
        // Recharge pending confirmation transaction assembly and shipment
        RechargeUnconfirmedTxData txData = new RechargeUnconfirmedTxData();
        txData.setOriginalTxHash(new HeterogeneousHash(hChainId, txHash));
        txData.setHeterogeneousHeight(blockHeight);
        txData.setHeterogeneousFromAddress(from);
        txData.setNerveToAddress(AddressTool.getAddress(nerveAddress));
        NerveAssetInfo nerveAssetInfo = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, assetId);
        txData.setAssetChainId(nerveAssetInfo.getAssetChainId());
        txData.setAssetId(nerveAssetInfo.getAssetId());
        txData.setAmount(value);
        Transaction tx = assembleTxService.rechargeUnconfirmedTx(nerveChain, txData, txTime);
        if(tx == null) {
            return null;
        }
        return tx.getHash().toHex();
    }

    @Override
    public String pendingDepositIITxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger erc20Value, Long txTime,
                                           Integer erc20Decimals, String erc20ContractAddress, Integer erc20AssetId, String nerveAddress, BigInteger mainAssetValue) throws Exception {
        // tip: If it only existstokenRecharge perhaps mainRecharge, then follow the old process
        // RechargependingTransaction increase and simultaneous transfer inethanderc20Data for
        // Recharge pending confirmation transaction assembly and shipment
        RechargeUnconfirmedTxData txData = new RechargeUnconfirmedTxData();
        txData.setOriginalTxHash(new HeterogeneousHash(hChainId, txHash));
        txData.setHeterogeneousHeight(blockHeight);
        txData.setHeterogeneousFromAddress(from);
        txData.setNerveToAddress(AddressTool.getAddress(nerveAddress));
        // At the same time, there aretokenRecharge and main asset recharge, originaltxdataField storagetokenAssets, extended fields store main assets
        // recordtokenRecharge data
        NerveAssetInfo tokenAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, erc20AssetId);
        txData.setAssetChainId(tokenAsset.getAssetChainId());
        txData.setAssetId(tokenAsset.getAssetId());
        txData.setAmount(erc20Value);
        // Record main asset recharge data
        NerveAssetInfo mainAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, 1);
        txData.setMainAssetAmount(mainAssetValue);
        txData.setMainAssetChainId(mainAsset.getAssetChainId());
        txData.setMainAssetId(mainAsset.getAssetId());
        Transaction tx = assembleTxService.rechargeUnconfirmedTx(nerveChain, txData, txTime);
        if(tx == null) {
            return null;
        }
        return tx.getHash().toHex();
    }

    @Override
    public String pendingOneClickCrossChainTxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger erc20Value,
                                                    Long txTime, Integer erc20Decimals, String erc20ContractAddress, Integer erc20AssetId,
                                                    String nerveAddress, BigInteger mainAssetValue, BigInteger mainAssetFeeAmount,
                                                    int desChainId, String desToAddress, BigInteger tipping, String tippingAddress, String desExtend) throws Exception {
        OneClickCrossChainUnconfirmedTxData txData = new OneClickCrossChainUnconfirmedTxData();
        txData.setMainAssetFeeAmount(mainAssetFeeAmount);
        txData.setDesChainId(desChainId);
        txData.setDesToAddress(desToAddress);
        txData.setDesExtend(desExtend);
        txData.setOriginalTxHash(new HeterogeneousHash(hChainId, txHash));
        txData.setHeterogeneousHeight(blockHeight);
        txData.setHeterogeneousFromAddress(from);
        txData.setNerveToAddress(AddressTool.getAddress(nerveAddress));
        txData.setTipping(tipping);
        txData.setTippingAddress(tippingAddress);
        // Record main asset recharge data
        NerveAssetInfo mainAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, 1);
        txData.setMainAssetAmount(mainAssetValue);
        txData.setMainAssetChainId(mainAsset.getAssetChainId());
        txData.setMainAssetId(mainAsset.getAssetId());
        // recordtokenRecharge data
        txData.setErc20Amount(erc20Value);
        if (erc20AssetId > 1) {
            NerveAssetInfo tokenAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, erc20AssetId);
            txData.setErc20AssetChainId(tokenAsset.getAssetChainId());
            txData.setErc20AssetId(tokenAsset.getAssetId());
            txData.setTippingChainId(tokenAsset.getAssetChainId());
            txData.setTippingAssetId(tokenAsset.getAssetId());
        } else {
            txData.setTippingChainId(mainAsset.getAssetChainId());
            txData.setTippingAssetId(mainAsset.getAssetId());
        }
        Transaction tx = assembleTxService.oneClickCrossChainUnconfirmedTx(nerveChain, txData, txTime);
        if(tx == null) {
            return null;
        }
        return tx.getHash().toHex();
    }

    @Override
    public Result validateDepositTx(String hTxHash) {
        nerveChain.getLogger().info("Verify recharge transactions: {}", hTxHash);
        if (hChainId < 200) {
            return validateDepositTxOfEvmSys(hTxHash);
        } else if (hChainId < 300) {
            return validateDepositTxOfBtcSys(hTxHash);
        } else {
            return Result.getFailed(ConverterErrorCode.DATA_ERROR);
        }
    }

    private Result validateDepositTxOfEvmSys(String hTxHash) {
        try {
            HeterogeneousTransactionInfo depositTx = getDocking().getDepositTransaction(hTxHash);
            if (depositTx == null) {
                return Result.getFailed(ConverterErrorCode.DATA_PARSE_ERROR);
            }
            RechargeTxDTO dto = new RechargeTxDTO();
            dto.setOriginalTxHash(hTxHash);
            dto.setHeterogeneousFromAddress(depositTx.getFrom());
            dto.setToAddress(depositTx.getNerveAddress());
            dto.setAmount(depositTx.getValue());
            dto.setHeterogeneousChainId(hChainId);
            dto.setHeterogeneousAssetId(depositTx.getAssetId());
            dto.setTxtime(depositTx.getTxTime());
            dto.setExtend(depositTx.getDepositIIExtend());
            if (depositTx.isDepositIIMainAndToken()) {
                // Support simultaneous transfer intokenandmain
                dto.setDepositII(true);
                dto.setMainAmount(depositTx.getDepositIIMainAssetValue());
            }
            Transaction tx = assembleTxService.createRechargeTxWithoutSign(nerveChain, dto);
            Transaction confirmedTx = TransactionCall.getConfirmedTx(nerveChain, tx.getHash());
            if (confirmedTx != null) {
                return Result.getFailed(ConverterErrorCode.TX_DUPLICATION);
            }
            return Result.getSuccess(ConverterErrorCode.SUCCESS);
        } catch (Exception e) {
            nerveChain.getLogger().error(e);
            if (e instanceof NulsException) {
                return Result.getFailed(((NulsException) e).getErrorCode());
            }
            return Result.getFailed(ConverterErrorCode.DATA_ERROR).setMsg(e.getMessage());
        }
    }

    private Result validateDepositTxOfBtcSys(String hTxHash) {
        try {
            HeterogeneousTransactionInfo depositTx = getDocking().getDepositTransaction(hTxHash);
            if (depositTx == null) {
                return Result.getFailed(ConverterErrorCode.DATA_PARSE_ERROR);
            }
            BtcTxInfo txInfo = (BtcTxInfo) depositTx;
            RechargeTxOfBtcSysDTO dto = new RechargeTxOfBtcSysDTO();
            dto.setHtgTxHash(txInfo.getTxHash());
            dto.setHtgFrom(txInfo.getFrom());
            dto.setHtgChainId(hChainId);
            dto.setHtgAssetId(txInfo.getAssetId());
            dto.setHtgTxTime(txInfo.getTxTime());
            dto.setHtgBlockHeight(txInfo.getBlockHeight());
            dto.setTo(txInfo.getNerveAddress());
            dto.setAmount(txInfo.getValue());
            dto.setFee(txInfo.getFee());
            dto.setFeeTo(txInfo.getNerveFeeTo());
            dto.setExtend(txInfo.getExtend0());
            Transaction tx = assembleTxService.createRechargeTxOfBtcSysWithoutSign(nerveChain, dto);
            Transaction confirmedTx = TransactionCall.getConfirmedTx(nerveChain, tx.getHash());
            if (confirmedTx != null) {
                return Result.getFailed(ConverterErrorCode.TX_DUPLICATION);
            }
            return Result.getSuccess(ConverterErrorCode.SUCCESS);
        } catch (Exception e) {
            nerveChain.getLogger().error(e);
            if (e instanceof NulsException) {
                return Result.getFailed(((NulsException) e).getErrorCode());
            }
            return Result.getFailed(ConverterErrorCode.DATA_ERROR).setMsg(e.getMessage());
        }
    }

    @Override
    public String depositTxSubmitOfBtcSys(String txHash, Long blockHeight, String from, String to, BigInteger value, Long txTime, BigInteger fee, String feeTo, String extend) throws Exception {
        // Assembly transaction
        RechargeTxOfBtcSysDTO dto = new RechargeTxOfBtcSysDTO();
        dto.setHtgTxHash(txHash);
        dto.setHtgFrom(from);
        dto.setHtgChainId(hChainId);
        dto.setHtgAssetId(1);
        dto.setHtgTxTime(txTime);
        dto.setHtgBlockHeight(blockHeight);
        dto.setTo(to);
        dto.setAmount(value);
        dto.setFee(fee);
        dto.setFeeTo(feeTo);
        dto.setExtend(extend);
        Transaction tx = assembleTxService.createRechargeTxOfBtcSys(nerveChain, dto);
        if(tx == null) {
            return null;
        }
        return tx.getHash().toHex();
    }
}
