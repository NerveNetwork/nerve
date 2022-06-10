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
     * 异构链chainId
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
     * @param extend
     */
    @Override
    public String txSubmit(String txHash, Long blockHeight, String from, String to, BigInteger value, Long txTime,
                           Integer decimals, Boolean ifContractAsset, String contractAddress, Integer assetId, String nerveAddress, String extend) throws Exception {
        // 组装Nerve充值交易
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
        // 同时充值两种资产的组装交易
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
        // 记录主资产充值数据
        NerveAssetInfo mainAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, 1);
        dto.setMainAssetChainId(mainAsset.getAssetChainId());
        dto.setMainAssetId(mainAsset.getAssetId());
        dto.setMainAssetAmount(mainAssetValue);
        // 记录token充值数据
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
        // 记录主资产充值数据
        NerveAssetInfo mainAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, 1);
        dto.setMainAssetChainId(mainAsset.getAssetChainId());
        dto.setMainAssetId(mainAsset.getAssetId());
        dto.setMainAssetAmount(mainAssetValue);
        dto.setNerveTxHash(nerveTxHash);
        dto.setSubExtend(subExtend);
        // 记录token充值数据
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
        // 充值待确认交易组装并发出
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
        // tip: 若只存在token充值 或者 main充值，则走旧流程即可
        // 充值pending交易增加同时转入eth和erc20的数据
        // 充值待确认交易组装并发出
        RechargeUnconfirmedTxData txData = new RechargeUnconfirmedTxData();
        txData.setOriginalTxHash(new HeterogeneousHash(hChainId, txHash));
        txData.setHeterogeneousHeight(blockHeight);
        txData.setHeterogeneousFromAddress(from);
        txData.setNerveToAddress(AddressTool.getAddress(nerveAddress));
        // 同时有token充值和主资产充值，原txdata字段存放token资产，扩展字段存放主资产
        // 记录token充值数据
        NerveAssetInfo tokenAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, erc20AssetId);
        txData.setAssetChainId(tokenAsset.getAssetChainId());
        txData.setAssetId(tokenAsset.getAssetId());
        txData.setAmount(erc20Value);
        // 记录主资产充值数据
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
        // 记录主资产充值数据
        NerveAssetInfo mainAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, 1);
        txData.setMainAssetAmount(mainAssetValue);
        txData.setMainAssetChainId(mainAsset.getAssetChainId());
        txData.setMainAssetId(mainAsset.getAssetId());
        // 记录token充值数据
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
        nerveChain.getLogger().info("验证充值交易: {}", hTxHash);
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
                // 支持同时转入token和main
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
}
