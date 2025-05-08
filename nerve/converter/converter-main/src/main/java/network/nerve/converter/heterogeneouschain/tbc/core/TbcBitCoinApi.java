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
package network.nerve.converter.heterogeneouschain.tbc.core;

import com.neemre.btcdcli4j.core.domain.RawTransaction;
import io.nuls.core.crypto.HexUtil;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.btc.txdata.*;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.IBitCoinLibParseTxHelper;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.AnalysisTxInfo;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.BtcSignData;
import network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgERC20Helper;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.heterogeneouschain.lib.model.HtgERC20Po;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.tbc.helper.TbcAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.tbc.model.FtInfo;
import network.nerve.converter.heterogeneouschain.tbc.model.TbcRawTransaction;
import network.nerve.converter.heterogeneouschain.tbc.utils.TbcSignatureUtil;
import network.nerve.converter.heterogeneouschain.tbc.utils.TbcUtil;
import network.nerve.converter.model.bo.UTXONeed;
import network.nerve.converter.model.bo.WithdrawalUTXO;
import network.nerve.converter.rpc.call.HeToolCall;
import network.nerve.converter.utils.ConverterUtil;
import org.bitcoinj.crypto.ECKey;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2024/2/29
 */
public class TbcBitCoinApi extends BitCoinLibApi  {
    private IHeterogeneousChainDocking docking;
    private TbcWalletApi walletApi;
    private TbcAnalysisTxHelper tbcAnalysisTxHelper;
    private IBitCoinLibParseTxHelper parseTxHelper;
    private HtgContext htgContext;
    protected HtgListener htgListener;
    protected HtgERC20Helper htgERC20Helper;

    @Override
    public int getByzantineCount(int virtualBankTotal) {
        int count = this.getCurrentMultiSignAddressPubsSize();
        return htgContext.getByzantineCount(count);
    }

    @Override
    public String signWithdraw(String txHash, String toAddress, BigInteger value, Integer assetId) throws Exception {
        /**
         Collect 60% of the signatures of the multi-signature public key.
         Assuming there are 15 administrators, 10 of which form a multi-signature.
         We need to collect 6 of the 10 signatures, and the other 5 are invalid.
         Determine whether the node is among the 10. Otherwise, do not sign (throw an exception).
         */
        Set<String> pubSet = (Set<String>) htgContext.dynamicCache().get("pubSet");
        if (!pubSet.contains(htgContext.ADMIN_ADDRESS_PUBLIC_KEY())) {
            throw new RuntimeException("Not the public key in the multi-signature address");
        }
        WithdrawalUTXOTxData withdrawlUTXO = this.takeWithdrawalUTXOs(txHash);
        boolean isContractAsset = assetId > 1;
        value = htgContext.getConverterCoreApi().checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
        String txraw;
        List<BigInteger> list;
        if (!isContractAsset) {
            Map map = HeToolCall.buildTbcWithdrawTbc(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    withdrawlUTXO,
                    htgContext.MULTY_SIGN_ADDRESS(),
                    toAddress,
                    new BigDecimal(value).movePointLeft(htgContext.ASSET_NAME().decimals()).toPlainString(),
                    txHash
            );
            txraw = map.get("txraw").toString();
            List amounts = (List) map.get("amounts");
            list = amounts.stream().map(a -> new BigInteger(a.toString())).toList();
        } else {
            HtgERC20Po token = htgERC20Helper.getERC20ByAssetId(assetId);
            String contractId = token.getAddress();
            FtData ftData = htgMultiSignAddressHistoryStorageService.getFtData(contractId);
            if (ftData == null) {
                ftData = this.getFtDataFromApi(contractId);
                htgMultiSignAddressHistoryStorageService.saveFtData(ftData);
            }
            List<String> preTXs = new ArrayList<>();
            List<String> prepreTxDatas = new ArrayList<>();
            this.fillPrePreInfo(withdrawlUTXO.getFtUtxoDataList(), preTXs, prepreTxDatas);
            String contractTx = walletApi.fetchTXraw(withdrawlUTXO.getUtxoDataList().get(0).getTxid());
            Map map = HeToolCall.buildTbcWithdrawFt(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    withdrawlUTXO,
                    htgContext.MULTY_SIGN_ADDRESS(),
                    (String) htgContext.dynamicCache().get("combineHash"),
                    toAddress,
                    ftData,
                    new BigDecimal(value).movePointLeft(token.getDecimals()).toPlainString(),
                    preTXs,
                    prepreTxDatas,
                    contractTx,
                    txHash
            );
            txraw = map.get("txraw").toString();
            List amounts = (List) map.get("amounts");
            list = amounts.stream().map(a -> new BigInteger(a.toString())).toList();
        }
        // calc the min number of signatures
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            // Obtain administrator account
            HtgAccount account = (HtgAccount) docking.getAccount(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());

            List<String> signed;
            if (isContractAsset) {
                signed = TbcSignatureUtil.signMultiFT(
                        htgContext.MULTY_SIGN_ADDRESS(),
                        txraw,
                        priKey,
                        list);
            } else {
                signed = TbcSignatureUtil.signMultiTBC(
                        htgContext.MULTY_SIGN_ADDRESS(),
                        txraw,
                        priKey,
                        list);
            }
            BtcSignData signData = new BtcSignData(
                    HexUtil.decode(htgContext.ADMIN_ADDRESS_PUBLIC_KEY()),
                    signed.stream().map(s -> HexUtil.decode(s)).toList()
            );
            String signatures = HexUtil.encode(signData.serialize());
            return signatures;
        } else {
            // sign machine support
            String signatures = htgContext.getConverterCoreApi().signTbcWithdrawByMachine(
                    htgContext.getConfig().getChainIdOnHtgNetwork(),
                    htgContext.HTG_CHAIN_ID(),
                    isContractAsset,
                    htgContext.ADMIN_ADDRESS_PUBLIC_KEY(),
                    htgContext.MULTY_SIGN_ADDRESS(),
                    txraw,
                    list);
            return signatures;
        }
    }

    private void fillPrePreInfo(List<FtUTXOData> ftUtxoDataList, List<String> preTXs, List<String> prepreTxDatas) throws Exception {
        for (int i = 0; i < ftUtxoDataList.size(); i++) {
            FtUTXOData ftUTXOData = ftUtxoDataList.get(i);
            int outputIndex = ftUTXOData.getOutputIndex();
            String preTX = walletApi.fetchTXraw(ftUTXOData.getTxId());
            preTXs.add(preTX);
            prepreTxDatas.add(HeToolCall.fetchFtPrePreTxData(preTX, outputIndex, walletApi.fetchFtPrePreTx(preTX, outputIndex)));
        }
    }

    private FtData getFtDataFromApi(String contractId) {
        FtInfo tbc20Info = walletApi.getTbc20Info(contractId);
        FtData ftData = new FtData();
        ftData.setName(tbc20Info.getFtName());
        ftData.setSymbol(tbc20Info.getFtSymbol());
        ftData.setDecimal(tbc20Info.getFtDecimal());
        ftData.setTotalSupply(tbc20Info.getFtSupply());
        ftData.setCodeScript(tbc20Info.getFtCodeScript());
        ftData.setTapeScript(tbc20Info.getFtTapeScript());
        ftData.setContractTxid(tbc20Info.getFtContractId());
        return ftData;
    }


    @Override
    public Boolean verifySignWithdraw(String signAddress, String txHash, String toAddress, BigInteger amount, int assetId, String signature) throws Exception {
        BtcSignData signData = new BtcSignData();
        signData.parse(HexUtil.decode(signature), 0);

        WithdrawalUTXOTxData withdrawlUTXO = this.takeWithdrawalUTXOs(txHash);
        if (withdrawlUTXO == null) {
            return false;
        }
        ECKey pub = ECKey.fromPublicOnly(signData.getPubkey());
        boolean isContractAsset = assetId > 1;
        amount = htgContext.getConverterCoreApi().checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, amount);

        if (!isContractAsset) {
            Map map = HeToolCall.buildTbcWithdrawTbc(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    withdrawlUTXO,
                    htgContext.MULTY_SIGN_ADDRESS(),
                    toAddress,
                    new BigDecimal(amount).movePointLeft(htgContext.ASSET_NAME().decimals()).toPlainString(),
                    txHash
            );
            String txraw = map.get("txraw").toString();
            List amounts = (List) map.get("amounts");
            List<BigInteger> list = amounts.stream().map(a -> new BigInteger(a.toString())).toList();
            return TbcSignatureUtil.verifySignMultiTBC(
                    pub,
                    signData.getSignatures().stream().map(s -> HexUtil.encode(s)).toList(),
                    htgContext.MULTY_SIGN_ADDRESS(),
                    txraw,
                    list
            );
        } else {
            HtgERC20Po token = htgERC20Helper.getERC20ByAssetId(assetId);
            String contractId = token.getAddress();
            FtData ftData = htgMultiSignAddressHistoryStorageService.getFtData(contractId);
            if (ftData == null) {
                ftData = this.getFtDataFromApi(contractId);
                htgMultiSignAddressHistoryStorageService.saveFtData(ftData);
            }
            List<String> preTXs = new ArrayList<>();
            List<String> prepreTxDatas = new ArrayList<>();
            this.fillPrePreInfo(withdrawlUTXO.getFtUtxoDataList(), preTXs, prepreTxDatas);
            String contractTx = walletApi.fetchTXraw(withdrawlUTXO.getUtxoDataList().get(0).getTxid());
            Map map = HeToolCall.buildTbcWithdrawFt(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    withdrawlUTXO,
                    htgContext.MULTY_SIGN_ADDRESS(),
                    (String) htgContext.dynamicCache().get("combineHash"),
                    toAddress,
                    ftData,
                    new BigDecimal(amount).movePointLeft(token.getDecimals()).toPlainString(),
                    preTXs,
                    prepreTxDatas,
                    contractTx,
                    txHash
            );
            String txraw = map.get("txraw").toString();
            List amounts = (List) map.get("amounts");
            List<BigInteger> list = amounts.stream().map(a -> new BigInteger(a.toString())).toList();
            return TbcSignatureUtil.verifySignMultiFT(
                    pub,
                    signData.getSignatures().stream().map(s -> HexUtil.encode(s)).toList(),
                    htgContext.MULTY_SIGN_ADDRESS(),
                    txraw,
                    list
            );
        }
    }

    @Override
    protected WithdrawalUTXO getWithdrawalUTXO(String nerveTxHash) {
        WithdrawalUTXOTxData data = this.takeWithdrawalUTXOs(nerveTxHash);
        if (data == null) {
            return null;
        }
        return new WithdrawalUTXO(data.getNerveTxHash(), data.getHtgChainId(), data.getCurrentMultiSignAddress(), data.getCurrentVirtualBankTotal(), data.getFeeRate(), data.getPubs(), data.getUtxoDataList());
    }

    @Override
    protected void loadERC20(HtgUnconfirmedTxPo po, WithdrawalUTXO data) {
        int assetId = po.getAssetId();
        boolean isContractAsset = assetId > 1;
        String contractAddressERC20;
        if (isContractAsset) {
            contractAddressERC20 = htgERC20Helper.getContractAddressByAssetId(assetId);
            htgERC20Helper.loadERC20(contractAddressERC20, po);
        } else {
            contractAddressERC20 = HtgConstant.ZERO_ADDRESS;
            po.setDecimals(htgContext.getConfig().getDecimals());
            po.setAssetId(htgContext.HTG_ASSET_ID());
        }
        po.setIfContractAsset(isContractAsset);
        if (isContractAsset) {
            po.setContractAddress(contractAddressERC20);
        }
    }

    @Override
    protected String _createMultiSignWithdrawTx(WithdrawalUTXO _withdrawlUTXO, String signatureData, String toAddress, BigInteger amount, String txHash, int assetId) throws Exception {
        Map<String, List<String>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        int inputCount = 0;
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            if (inputCount == 0) {
                inputCount = signDataObj.getSignatures().size();
            }
            signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()));
        }

        WithdrawalUTXOTxData withdrawlUTXO = this.takeWithdrawalUTXOs(txHash);
        boolean isContractAsset = assetId > 1;
        List<ECKey> pubEcKeys = withdrawlUTXO.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).toList();
        List<ECKey> sortedPubKeys = new ArrayList<>(pubEcKeys);
        Collections.sort(sortedPubKeys, ECKey.PUBKEY_COMPARATOR);
        List<String> pubs = sortedPubKeys.stream().map(sk -> sk.getPublicKeyAsHex()).toList();

        int byzantineCount = htgContext.getByzantineCount(pubs.size());
        List<List<String>> sigs = new ArrayList<>();
        for (int i = 0; i < inputCount; i++) {
            List<String> sigsPerInput = new ArrayList<>();
            for (String pub : pubs) {
                List<String> sigsByPub = signatures.get(pub);
                if (sigsByPub == null) continue;
                sigsPerInput.add(sigsByPub.get(i));
                if (sigsPerInput.size() == byzantineCount) {
                    break;
                }
            }
            sigs.add(sigsPerInput);
        }

        if (!isContractAsset) {
            Map map = HeToolCall.buildTbcWithdrawTbc(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    withdrawlUTXO,
                    htgContext.MULTY_SIGN_ADDRESS(),
                    toAddress,
                    new BigDecimal(amount).movePointLeft(htgContext.ASSET_NAME().decimals()).toPlainString(),
                    txHash
            );
            String txraw = map.get("txraw").toString();
            return HeToolCall.finishTbcWithdrawTbc(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    txraw,
                    sigs,
                    pubs
            );
        } else {
            HtgERC20Po token = htgERC20Helper.getERC20ByAssetId(assetId);
            String contractId = token.getAddress();
            FtData ftData = htgMultiSignAddressHistoryStorageService.getFtData(contractId);
            if (ftData == null) {
                ftData = this.getFtDataFromApi(contractId);
                htgMultiSignAddressHistoryStorageService.saveFtData(ftData);
            }
            List<String> preTXs = new ArrayList<>();
            List<String> prepreTxDatas = new ArrayList<>();
            this.fillPrePreInfo(withdrawlUTXO.getFtUtxoDataList(), preTXs, prepreTxDatas);
            String contractTx = walletApi.fetchTXraw(withdrawlUTXO.getUtxoDataList().get(0).getTxid());
            Map map = HeToolCall.buildTbcWithdrawFt(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    withdrawlUTXO,
                    htgContext.MULTY_SIGN_ADDRESS(),
                    (String) htgContext.dynamicCache().get("combineHash"),
                    toAddress,
                    ftData,
                    new BigDecimal(amount).movePointLeft(token.getDecimals()).toPlainString(),
                    preTXs,
                    prepreTxDatas,
                    contractTx,
                    txHash
            );
            String txraw = map.get("txraw").toString();
            return HeToolCall.finishTbcWithdrawFt(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    txraw,
                    sigs,
                    pubs
            );
        }
    }

    @Override
    protected IBitCoinLibWalletApi walletApi() {
        return walletApi;
    }

    @Override
    protected long _calcFeeMultiSignSize(int inputNum, int outputNum, int[] opReturnBytesLen, int m, int n) {
        return BitCoinLibUtil.TBC_FEE;
    }

    @Override
    protected long _calcFeeMultiSignSizeWithSplitGranularity(long fromTotal, long transfer, long feeRate, Long splitGranularity, int inputNum, int[] opReturnBytesLen, int m, int n) {
        return BitCoinLibUtil.TBC_FEE;
    }

    @Override
    protected WithdrawalFeeLog _takeWithdrawalFeeLogFromTxParse(String htgTxHash, boolean nerveInner) throws Exception {
        RawTransaction txInfo = walletApi.getTransactionByHash(htgTxHash);
        if (txInfo.getConfirmations() == null || txInfo.getConfirmations().intValue() == 0) {
            return null;
        }
        // load preTx and FT transfer info
        List list = tbcAnalysisTxHelper.fetchVinInfoOfMultiSign(List.of(txInfo));
        TbcRawTransaction tbcTxInfo = (TbcRawTransaction) list.get(0);
        AnalysisTxInfo analysisTxInfo = tbcAnalysisTxHelper.analysisTxTypeInfo(tbcTxInfo, txInfo.getBlockTime(), txInfo.getBlockHash());
        HeterogeneousChainTxType txType = analysisTxInfo.getTxType();
        if (txType == null) {
            return null;
        }
        WithdrawalFeeLog feeLog = null;
        if (txType == HeterogeneousChainTxType.DEPOSIT) {
            BtcUnconfirmedTxPo po = (BtcUnconfirmedTxPo) parseTxHelper.parseDepositTransaction(tbcTxInfo, null, true);
            if (po.getNerveAddress().equals(ConverterContext.BITCOIN_SYS_WITHDRAWAL_FEE_ADDRESS)) {
                // Record chain fee entry
                feeLog = new WithdrawalFeeLog(
                        po.getBlockHeight(), po.getBlockHash(), htgTxHash, htgContext.HTG_CHAIN_ID(), po.getValue().longValue(), true);
                feeLog.setTxTime(txInfo.getBlockTime());
            }
        } else if (txType == HeterogeneousChainTxType.WITHDRAW) {
            // All transactions with nerve multi-signature addresses in from must record handling fee expenditures.
            long fee = tbcAnalysisTxHelper.calcTxFee(tbcTxInfo, walletApi);
            feeLog = new WithdrawalFeeLog(
                    Long.valueOf(walletApi.getBlockHeaderByHash(txInfo.getBlockHash()).getHeight()), txInfo.getBlockHash(), htgTxHash, htgContext.HTG_CHAIN_ID(), fee, false);
            feeLog.setTxTime(txInfo.getBlockTime());
        }
        if (feeLog != null && htgContext.getConverterCoreApi().isProtocol36()) {
            feeLog.setNerveInner(nerveInner);
        }
        return feeLog;
    }

    @Override
    protected Object[] _makeChangeTxBaseInfo (WithdrawalUTXO withdrawlUTXO) {
        return new Object[0];
    }

    @Override
    public Boolean verifySignManagerChanges(String signAddress, String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signature) throws Exception {
        return true;
    }

    @Override
    public boolean validateManagerChangesTx(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signatureData) throws Exception {
        return true;
    }

    @Override
    protected String _createOrSignManagerChangesTx(WithdrawalUTXO withdrawlUTXO, String signatureData, String nerveTxHash, Object[] baseInfo) throws Exception {
        return nerveTxHash;
    }

    @Override
    public List<UTXOData> getUTXOs(String address) {
        return walletApi.getAccountUTXOs(address);
    }

    @Override
    public UTXONeed getNeedUTXO(String address, BigInteger value, Integer assetId) throws Exception {
        value = htgContext.getConverterCoreApi().checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
        List<UTXOData> utxos = walletApi.getAccountUTXOs(address);
        utxos.sort(ConverterUtil.BITCOIN_SYS_COMPARATOR);
        List<UTXOData> needUtxos = new ArrayList<>();
        BigInteger spend = value;
        boolean isContract = assetId > 1;
        BigInteger total = BigInteger.ZERO;

        if (!isContract) {
            spend = spend.add(TbcUtil.TBC_FEE);
            for (UTXOData utxo : utxos) {
                htgContext.logger().info("utxo data: {}", utxo);
                // check utxo locked
                boolean lockedUTXO = docking.getBitCoinApi().isLockedUTXO(utxo.getTxid(), utxo.getVout());
                if (lockedUTXO) {
                    continue;
                }
                total = total.add(utxo.getAmount());
                needUtxos.add(utxo);
                if (total.compareTo(spend) >= 0) {
                    break;
                }
            }
            if (total.compareTo(spend) < 0) {
                htgContext.logger().info("[{}] not enough utxos to withdraw", value);
                throw new RuntimeException(String.format("[{}] not enough utxos to withdraw", value));
            }
            String scriptAsm = TbcUtil.getMultisigLockScript(address);
            byte[] decodeASM = TbcUtil.decodeASM(scriptAsm);
            UTXONeed need = new UTXONeed(needUtxos);
            need.setScript(HexUtil.encode(decodeASM));
            return need;
        } else {
            UTXOData needTBC = null;
            BigInteger tbcValue = TbcUtil.PAY_FEE;
            for (UTXOData utxo : utxos) {
                boolean lockedUTXO = docking.getBitCoinApi().isLockedUTXO(utxo.getTxid(), utxo.getVout());
                if (lockedUTXO) {
                    continue;
                }
                if (htgContext.getConverterCoreApi().isProtocol41()) {
                    if (utxo.getVout() != 0) {
                        // For tbc to ft transactions, utxo's vout must be equal to 0 (for security reasons)
                        continue;
                    }
                }
                if (utxo.getAmount().compareTo(tbcValue) >= 0) {
                    needTBC = utxo;
                    break;
                }
            }
            if (needTBC == null) {
                htgContext.logger().info("[{}] not enough utxos to withdraw FT", tbcValue);
                throw new RuntimeException(String.format("[{}] not enough utxos to withdraw FT", tbcValue));
            }
            HtgERC20Po token = htgERC20Helper.getERC20ByAssetId(assetId);
            String contractId = token.getAddress();
            FtData ftData = htgMultiSignAddressHistoryStorageService.getFtData(contractId);
            if (ftData == null) {
                ftData = this.getFtDataFromApi(contractId);
                htgMultiSignAddressHistoryStorageService.saveFtData(ftData);
            }
            String multiAddressCombineHash = (String) htgContext.dynamicCache().get("combineHash");
            String ftTransferCode = HexUtil.encode(TbcUtil.buildFTtransferCode(ftData.getCodeScript(), multiAddressCombineHash));
            List<FtUTXOData> ftUTXODataList = walletApi.getAccountTbc20UTXOs(contractId, address);
            List<FtUTXOData> checkedList = ftUTXODataList.stream()
                    .filter(
                            utxo -> !docking.getBitCoinApi().isLockedUTXO(utxo.getTxId(), utxo.getOutputIndex())
                    ).toList();
            List<FtUTXOData> ftUTXOData = TbcUtil.fetchFtUtxosOfMultiSig(checkedList, value);
            List<UTXOData> utxoList = new ArrayList<>();
            utxoList.add(needTBC);
            return new UTXONeed(utxoList, ftTransferCode, contractId, ftUTXOData);
        }
    }

    @Override
    public String signManagerChanges(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount) throws Exception {
        return nerveTxHash;
    }


    @Override
    public long getWithdrawalFeeSize(long fromTotal, long transfer, long feeRate, int inputNum) {
        return BitCoinLibUtil.TBC_FEE;
    }

    @Override
    public long getChangeFeeSize(int utxoSize) {
        return BitCoinLibUtil.TBC_FEE;
    }

}

