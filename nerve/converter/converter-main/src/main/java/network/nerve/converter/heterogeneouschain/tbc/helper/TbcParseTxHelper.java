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
package network.nerve.converter.heterogeneouschain.tbc.helper;

import com.neemre.btcdcli4j.core.domain.*;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.btc.txdata.*;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.core.api.interfaces.IBitCoinApi;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.BchUtxoUtil;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.IBitCoinLibParseTxHelper;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.AnalysisTxInfo;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgERC20Helper;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.tbc.model.FtTransfer;
import network.nerve.converter.heterogeneouschain.tbc.model.FtTransferInfo;
import network.nerve.converter.heterogeneouschain.tbc.model.TbcRawTransaction;
import network.nerve.converter.heterogeneouschain.tbc.utils.TbcUtil;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.utils.ConverterUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class TbcParseTxHelper implements IBitCoinLibParseTxHelper, BeanInitial {

    private HtgERC20Helper htgERC20Helper;
    private IBitCoinApi bitCoinApi;
    private BitCoinLibWalletApi walletApi;
    private HtgListener htgListener;
    private HtgContext htgContext;
    private TbcAnalysisTxHelper tbcAnalysisTxHelper;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    @Override
    public boolean isCompletedTransaction(String nerveTxHash) {
        WithdrawalUTXOTxData utxoTxData = bitCoinApi.takeWithdrawalUTXOs(nerveTxHash);
        if (utxoTxData == null) {
            return false;
        }
        List<UTXOData> utxoDataList = utxoTxData.getUtxoDataList();
        if (HtgUtil.isEmptyList(utxoDataList)) {
            return true;
        }
        Collections.sort(utxoDataList, ConverterUtil.BITCOIN_SYS_COMPARATOR);
        UTXOData utxoData = utxoDataList.get(0);
        TxOutInfo txOutInfo = walletApi.getTxOutInfo(utxoData.getTxid(), utxoData.getVout());
        return txOutInfo == null;
    }

    @Override
    public HeterogeneousTransactionInfo parseDepositTransaction(Object txInfoObj, Long blockHeight, boolean validate) throws Exception {
        String multiAddressCombineHash = (String) htgContext.dynamicCache().get("combineHash");
        TbcRawTransaction tbcTxInfo = (TbcRawTransaction) txInfoObj;
        if (tbcTxInfo == null) {
            logger().warn("Transaction does not exist");
            return null;
        }
        RawTransaction txInfo = tbcTxInfo.getTx();
        String htgTxHash = txInfo.getTxId();
        if (HtgUtil.isEmptyList(txInfo.getVOut())) {
            return null;
        }
        List<RawOutput> outputList = txInfo.getVOut();
        if (HtgUtil.isEmptyList(outputList)) {
            return null;
        }
        BigInteger value = BigInteger.ZERO;
        String txTo = null;
        // 接收者是多签地址，资产是TBC
        for (RawOutput output : outputList) {
            String asm = output.getScriptPubKey().getAsm();
            if (asm.endsWith("OP_CHECKMULTISIG")) {
                String outputAddress = TbcUtil.convertP2MSScriptToMSAddress(asm);
                if (htgListener.isListeningAddress(outputAddress)) {
                    if (txTo == null) {
                        txTo = outputAddress;
                    }
                    value = value.add(output.getValue().movePointRight(htgContext.ASSET_NAME().decimals()).toBigInteger());
                }
            }
        }
        BtcUnconfirmedTxPo po = new BtcUnconfirmedTxPo();
        // 转账的是ft token
        String contractId = null;
        long tokenValue = 0L;
        FtTransferInfo ftTransferInfo = tbcTxInfo.getFtTransferInfo();
        if (ftTransferInfo != null) {
            List<FtTransfer> output = ftTransferInfo.getOutput();
            contractId = output.get(0).getContract_id();
            if (htgERC20Helper.isERC20(contractId, po)) {
                for (FtTransfer transfer : output) {
                    if (!transfer.getContract_id().equals(contractId)) {
                        logger().warn("Recharge duplicate token[{}], contractIds: [{}, {}]", htgTxHash, contractId, transfer.getContract_id());
                        return null;
                    }
                    String address = transfer.getAddress();
                    if (htgListener.isListeningAddress(address) || address.contains(multiAddressCombineHash)) {
                        tokenValue = transfer.getFt_balance();
                    }
                }
            }
        }
        if (value.compareTo(BigInteger.ZERO) == 0 && tokenValue == 0) {
            logger().warn("[{}] can not parse amount", htgTxHash);
            return null;
        }
        RechargeData rechargeData = null;
        boolean error = false;
        do {
            String opReturnInfo = walletApi.getOpReturnHex(txInfo);
            if (StringUtils.isBlank(opReturnInfo)) {
                logger().warn("Recharge informationDataIllegal transaction[{}], opReturnInfo: {}", htgTxHash, opReturnInfo);
                error = true;
                break;
            }
            try {
                rechargeData = new RechargeData();
                rechargeData.parse(HexUtil.decode(opReturnInfo), 0);
            } catch (Exception e) {
                logger().warn(String.format("Illegal recharge information[1] transaction[%s], opReturnInfo: %s", htgTxHash, opReturnInfo), e);
                error = true;
                break;
            }
            byte[] rechargeDataTo = rechargeData.getTo();
            if (rechargeDataTo == null) {
                logger().warn("[Abnormal recharge address] transaction[{}], [0]The recharge address is empty", htgTxHash);
                error = true;
                break;
            }
            if (!AddressTool.validAddress(htgContext.NERVE_CHAINID(), rechargeDataTo)) {
                logger().warn("[Abnormal recharge address] transaction[{}], [1]Recharge addressHexData: {}", htgTxHash, HexUtil.encode(rechargeDataTo));
                error = true;
                break;
            }
            boolean hasFeeTo = rechargeData.getFeeTo() != null;
            if (hasFeeTo && !AddressTool.validAddress(htgContext.NERVE_CHAINID(), rechargeData.getFeeTo())) {
                logger().warn("[FeeToAddress abnormality] transaction[{}], [0]addressHexData: {}", htgTxHash, HexUtil.encode(rechargeData.getFeeTo()));
                error = true;
                break;
            }
            BigInteger rechargeValue = BigInteger.valueOf(rechargeData.getValue());
            if (rechargeValue.compareTo(value) > 0) {
                logger().warn("[Abnormal recharge amount] transaction[{}], [0]Registration amount: {}, Actual amount: {}", htgTxHash, rechargeData.getValue(), value);
                error = true;
                break;
            }
            // If the actual amount is greater than the specified recharge amount and no handling fee address is filled in, the excess amount will be transferred to the team address
            if (value.compareTo(rechargeValue) > 0 && !hasFeeTo) {
                rechargeData.setFeeTo(ConverterContext.AWARD_FEE_SYSTEM_ADDRESS_PROTOCOL_1_17_0);
            }
            if (tokenValue > 0) {
                // check token info
                String tokenInfo = rechargeData.getExtend1();
                if (StringUtils.isBlank(tokenInfo) || tokenInfo.length() < 65) {
                    logger().warn("[Abnormal recharge token amount] transaction[{}], Registration token info: {}, Actual amount: {}", htgTxHash, tokenInfo, tokenValue);
                    error = true;
                    break;
                }
                String contractId_ = tokenInfo.substring(0, 64);
                if (!contractId_.equals(contractId)) {
                    logger().warn("[Abnormal recharge token info] transaction[{}], Registration token :{}, Actual token: {}", htgTxHash, contractId_, contractId);
                    error = true;
                    break;
                }
                long tokenValue_ = Long.parseLong(tokenInfo.substring(64));
                if (tokenValue_ != tokenValue) {
                    logger().warn("[Abnormal recharge token amount] transaction[{}], Registration token amount: {}, Actual amount: {}", htgTxHash, tokenValue_, tokenValue);
                    error = true;
                    break;
                }
            }
        } while (false);
        if (validate && error) {
            return null;
        }

        po.setFee(BigInteger.ZERO);
        po.setValue(value);
        // analysisfromaddress
        RawTransaction previousTx;
        Map<String, Map> preTxMap = tbcTxInfo.getPreTxMap();
        RawInput rawInput = txInfo.getVIn().get(0);
        Map preTx = (Map) preTxMap.get(rawInput.getTxId());
        if (preTx != null) {
            previousTx = JSONUtils.map2pojo(preTx, RawTransaction.class);
        } else {
            previousTx = walletApi.getTransactionByHash(rawInput.getTxId());
        }

        //RawOutput previousTxOutput = previousTx.getVOut().get(rawInput.getVOut());
        po.setFrom(TbcUtil.calcAddressFromOutput(previousTx, rawInput.getVOut()));
        po.setTo(txTo);

        // 充值TBC
        if (tokenValue == 0) {
            if (rechargeData != null) {
                po.setNerveAddress(rechargeData.getTo() != null ? AddressTool.getStringAddressByBytes(rechargeData.getTo()) : null);
                po.setValue(BigInteger.valueOf(rechargeData.getValue()));
                po.setFee(value.subtract(BigInteger.valueOf(rechargeData.getValue())));
                po.setNerveFeeTo(rechargeData.getFeeTo() != null ? AddressTool.getStringAddressByBytes(rechargeData.getFeeTo()) : null);
                po.setExtend0(rechargeData.getExtend0());
                po.setExtend1(rechargeData.getExtend1());
                po.setExtend2(rechargeData.getExtend2());
                po.setExtend3(rechargeData.getExtend3());
                po.setExtend4(rechargeData.getExtend4());
                po.setExtend5(rechargeData.getExtend5());
            }
            po.setTxType(HeterogeneousChainTxType.DEPOSIT);
            po.setTxHash(htgTxHash);
            if (blockHeight != null) {
                po.setBlockHeight(blockHeight);
            } else {
                po.setBlockHeight(Long.valueOf(walletApi.getBlockHeaderByHash(txInfo.getBlockHash()).getHeight()));
            }
            po.setBlockHash(txInfo.getBlockHash());
            po.setTxTime(txInfo.getBlockTime());
            po.setDecimals(htgContext.ASSET_NAME().decimals());
            po.setIfContractAsset(false);
            po.setAssetId(1);
        } else {
            // 充值Token，同时可能充值TBC
            if (rechargeData != null) {
                po.setNerveAddress(rechargeData.getTo() != null ? AddressTool.getStringAddressByBytes(rechargeData.getTo()) : null);
                po.setValue(new BigInteger(rechargeData.getExtend1().substring(64)));
                po.setFee(value.subtract(BigInteger.valueOf(rechargeData.getValue())));
                po.setNerveFeeTo(rechargeData.getFeeTo() != null ? AddressTool.getStringAddressByBytes(rechargeData.getFeeTo()) : null);
                po.setExtend0(rechargeData.getExtend0());
                po.setExtend1(rechargeData.getExtend1());
                po.setExtend2(rechargeData.getExtend2());
                po.setExtend3(rechargeData.getExtend3());
                po.setExtend4(rechargeData.getExtend4());
                po.setExtend5(rechargeData.getExtend5());
            }
            if (rechargeData.getValue() > 0) {
                po.setDepositIIMainAsset(BigInteger.valueOf(rechargeData.getValue()), htgContext.getConfig().getDecimals(), htgContext.HTG_ASSET_ID());
            }
            po.setDepositIIExtend(rechargeData.getExtend0());
            po.setTxType(HeterogeneousChainTxType.DEPOSIT);
            po.setTxHash(htgTxHash);
            if (blockHeight != null) {
                po.setBlockHeight(blockHeight);
            } else {
                po.setBlockHeight(Long.valueOf(walletApi.getBlockHeaderByHash(txInfo.getBlockHash()).getHeight()));
            }
            po.setBlockHash(txInfo.getBlockHash());
            po.setTxTime(txInfo.getBlockTime());
            po.setIfContractAsset(true);
        }
        return po;
    }

    @Override
    public HeterogeneousTransactionInfo parseWithdrawalTransaction(Object txInfoObj, Long blockHeight, boolean validate) throws Exception {
        String multiAddressCombineHash = (String) htgContext.dynamicCache().get("combineHash");
        TbcRawTransaction tbcTxInfo = (TbcRawTransaction) txInfoObj;
        if (tbcTxInfo == null) {
            logger().warn("Transaction does not exist");
            return null;
        }
        RawTransaction txInfo = tbcTxInfo.getTx();
        String htgTxHash = txInfo.getTxId();
        if (HtgUtil.isEmptyList(txInfo.getVOut())) {
            return null;
        }
        if (HtgUtil.isEmptyList(txInfo.getVIn())) {
            return null;
        }
        boolean correctErc20 = false;
        boolean correctMainAsset = false;
        Map<String, Map> preTxMap = tbcTxInfo.getPreTxMap();
        FtTransferInfo ftTransferInfo = tbcTxInfo.getFtTransferInfo();
        String multiAddr = null;
        List<UsedUTXOData> usedUTXOList = new ArrayList<>();
        List<RawInput> inputList = txInfo.getVIn();
        for (RawInput input : inputList) {
            SignatureScript scriptSig = input.getScriptSig();
            if (scriptSig == null || StringUtils.isBlank(scriptSig.getHex())) {
                continue;
            }
            String hex = scriptSig.getHex();
            if (!hex.startsWith("00")) {
                continue;
            }
            // 发起者是多签地址，资产是TBC
            Map preTx = preTxMap.get(input.getTxId());
            if (preTx == null) {
                continue;
            }
            List<Map> voutList = (List<Map>) preTx.get("vout");
            RawOutput preOutput = JSONUtils.map2pojo(voutList.get(input.getVOut()), RawOutput.class);
            String inputAddress = TbcUtil.convertP2MSScriptToMSAddress(preOutput.getScriptPubKey().getAsm());
            if (htgListener.isListeningAddress(inputAddress)) {
                multiAddr = inputAddress;
                usedUTXOList.add(new UsedUTXOData(input.getTxId(), input.getVOut()));
            }
        }
        // 转账的是ft token
        if (ftTransferInfo != null) {
            List<FtTransfer> ftInput = ftTransferInfo.getInput();
            for (FtTransfer transfer : ftInput) {
                String address = transfer.getAddress();
                if (htgListener.isListeningAddress(address) || address.contains(multiAddressCombineHash)) {
                    multiAddr = htgContext.MULTY_SIGN_ADDRESS();
                    usedUTXOList.add(new UsedUTXOData(transfer.getTxid(), transfer.getVout()));
                }
            }
        }

        if (!htgContext.MULTY_SIGN_ADDRESS().equals(multiAddr)) {
            return null;
        }
        List<RawOutput> outputList = txInfo.getVOut();
        BigInteger value = BigInteger.ZERO;
        BigInteger ftValue = BigInteger.ZERO;
        int tokenDecimals = 0;
        String contractId = null;
        String txTo = null;
        // 接收者是普通地址，资产是TBC
        for (RawOutput output : outputList) {
            List<String> addresses = output.getScriptPubKey().getAddresses();
            if (HtgUtil.isEmptyList(addresses)) {
                continue;
            }
            String outputAddress = addresses.get(0);
            if (StringUtils.isBlank(outputAddress)) {
                continue;
            }
            // except current nerve multi-signature address
            if (!htgListener.isListeningAddress(outputAddress)) {
                if (txTo == null) {
                    txTo = outputAddress;
                } else if (!txTo.equals(outputAddress)) {
                    // Only one receiver is allowed here
                    logger().warn("Only one receiver is allowed here");
                    return null;
                }
                value = value.add(output.getValue().movePointRight(htgContext.ASSET_NAME().decimals()).toBigInteger());
            }
        }
        if (value.compareTo(BigInteger.ZERO) > 0) {
            correctMainAsset = true;
        }
        // 检查接收者是普通地址，资产是erc20
        if (ftTransferInfo != null) {
            List<FtTransfer> output = ftTransferInfo.getOutput();
            for (FtTransfer transfer : output) {
                if (contractId == null) {
                    contractId = transfer.getContract_id();
                    tokenDecimals = transfer.getFt_decimal();
                } else if (!contractId.equals(transfer.getContract_id())) {
                    logger().warn("Only one ft token is allowed here");
                    return null;
                }
                String address = transfer.getAddress();
                if (!htgListener.isListeningAddress(address) && !address.contains(multiAddressCombineHash)) {
                    if (txTo == null) {
                        txTo = address;
                    } else if (!txTo.equals(address)) {
                        // Only one receiver is allowed here
                        logger().warn("Only one receiver is allowed here");
                        return null;
                    }
                    ftValue = ftValue.add(BigInteger.valueOf(transfer.getFt_balance()));
                }
            }
        }
        if (ftValue.compareTo(BigInteger.ZERO) > 0) {
            correctErc20 = true;
        }
        if (!correctErc20 && !correctMainAsset) {
            logger().warn("Withdrawal transactions [{}] 0 amount in withdrawal type[0]", htgTxHash);
            return null;
        }

        if (correctErc20 && correctMainAsset) {
            logger().warn("Withdrawal transactions [{}] Conflict in withdrawal type[1]", htgTxHash);
            return null;
        }
        // check withdrawal info
        String nerveTxHash = null;
        boolean error = true;
        Transaction nerveTx = null;
        do {
            String opReturnInfo = walletApi.getOpReturnHex(txInfo);
            if (StringUtils.isBlank(opReturnInfo)) {
                logger().warn("Withdrawal information Data Illegal transaction[{}], opReturnInfo: {}", htgTxHash, opReturnInfo);
                break;
            }
            byte[] opReturnInfoBytes;
            try {
                opReturnInfoBytes = HexUtil.decode(opReturnInfo);
            } catch (Exception e) {
                logger().warn(String.format("Illegal withdrawal information[1] transaction[%s], opReturnInfo: %s", htgTxHash, opReturnInfo), e);
                break;
            }
            if (opReturnInfoBytes.length != NulsHash.HASH_LENGTH) {
                logger().warn(String.format("Illegal withdrawal information[2] transaction[%s], opReturnInfo: %s", htgTxHash, opReturnInfo));
                break;
            }
            nerveTxHash = opReturnInfo;
            if ((nerveTx = htgContext.getConverterCoreApi().getNerveTx(nerveTxHash)) == null) {
                htgListener.removeListeningTx(htgTxHash);
                htgContext.logger().warn("Illegal transaction business[{}], not found NERVE Transaction, Type: WITHDRAWAL, Key: {}", htgTxHash, nerveTxHash);
                break;
            }
            if (nerveTx.getType() != TxType.WITHDRAWAL && nerveTx.getType() != TxType.CHANGE_VIRTUAL_BANK) {
                htgContext.logger().warn("Illegal transaction business[{}], not found NERVE Transaction, Type: WITHDRAWAL, Key: {}", htgTxHash, nerveTxHash);
                break;
            }
            error = false;
        } while (false);
        if (validate && error) {
            return null;
        }

        BtcUnconfirmedTxPo po = new BtcUnconfirmedTxPo();
        po.setCheckWithdrawalUsedUTXOData(new CheckWithdrawalUsedUTXOData(usedUTXOList).serialize());
        po.setFrom(multiAddr);
        po.setTo(txTo);
        po.setFee(BigInteger.ZERO);
        if (nerveTx.getType() == TxType.WITHDRAWAL) {
            po.setTxType(HeterogeneousChainTxType.WITHDRAW);
        } else {
            po.setTxType(HeterogeneousChainTxType.CHANGE);
        }
        po.setTxHash(htgTxHash);
        if (blockHeight != null) {
            po.setBlockHeight(blockHeight);
        } else {
            po.setBlockHeight(Long.valueOf(walletApi.getBlockHeaderByHash(txInfo.getBlockHash()).getHeight()));
        }
        po.setBlockHash(txInfo.getBlockHash());
        po.setTxTime(txInfo.getBlockTime());
        po.setNerveTxHash(nerveTxHash);

        if (correctMainAsset) {
            po.setValue(value);
            po.setDecimals(htgContext.getConfig().getDecimals());
            po.setAssetId(htgContext.HTG_ASSET_ID());
            po.setIfContractAsset(false);
        } else if (correctErc20) {
            po.setValue(ftValue);
            po.setDecimals(tokenDecimals);
            po.setContractAddress(contractId);
            po.setIfContractAsset(true);
        }

        if (po.isIfContractAsset()) {
            htgERC20Helper.loadERC20(po.getContractAddress(), po);
        }

        String btcFeeReceiverPub = htgContext.getConverterCoreApi().getBtcFeeReceiverPub();
        String btcFeeReceiver = TbcUtil.getBtcLegacyAddress(btcFeeReceiverPub);
        List<HeterogeneousAddress> signers = new ArrayList<>();
        signers.add(new HeterogeneousAddress(htgContext.getConfig().getChainId(), btcFeeReceiver));
        po.setSigners(signers);
        return po;
    }

    @Override
    public HeterogeneousTransactionInfo parseDepositTransaction(String txHash, boolean validate) throws Exception {
        RawTransaction txInfo = walletApi.getTransactionByHash(txHash);
        List list = tbcAnalysisTxHelper.fetchVinInfoOfMultiSign(List.of(txInfo));
        return this.parseDepositTransaction(list.get(0), null, validate);
    }

    @Override
    public HeterogeneousTransactionInfo parseWithdrawalTransaction(String txHash, boolean validate) throws Exception {
        RawTransaction txInfo = walletApi.getTransactionByHash(txHash);
        List list = tbcAnalysisTxHelper.fetchVinInfoOfMultiSign(List.of(txInfo));
        return this.parseWithdrawalTransaction(list.get(0), null, validate);
    }
}
