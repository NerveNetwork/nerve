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
package network.nerve.converter.heterogeneouschain.btc.core;

import com.neemre.btcdcli4j.core.domain.RawInput;
import com.neemre.btcdcli4j.core.domain.RawOutput;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.btc.txdata.*;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.interfaces.IBitCoinApi;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.btc.helper.BtcParseTxHelper;
import network.nerve.converter.heterogeneouschain.btc.model.BtcSignData;
import network.nerve.converter.heterogeneouschain.btc.utils.BtcUtil;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgInvokeTxHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgPendingTxHelper;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgAccountStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgMultiSignAddressHistoryStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousAccount;
import network.nerve.converter.model.bo.WithdrawalTotalFeeInfo;
import network.nerve.converter.model.bo.WithdrawalUTXO;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.ECKey;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static io.protostuff.ByteString.EMPTY_STRING;

/**
 * @author: PierreLuo
 * @date: 2024/2/29
 */
public class BitCoinApi implements IBitCoinApi, BeanInitial {
    private IHeterogeneousChainDocking btcDocking;
    private BtcWalletApi btcWalletApi;
    private BtcParseTxHelper btcParseTxHelper;
    private HtgContext htgContext;
    private HtgInvokeTxHelper htgInvokeTxHelper;
    private HtgPendingTxHelper htgPendingTxHelper;
    private HtgAccountStorageService htgAccountStorageService;
    private HtgMultiSignAddressHistoryStorageService htgMultiSignAddressHistoryStorageService;
    protected HtgCallBackManager htgCallBackManager;
    protected HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    protected HtgListener htgListener;

    private NulsLogger logger() {
        return htgContext.logger();
    }
    @Override
    public List<UTXOData> getUTXOs(String address) {
        return btcWalletApi.getAccountUTXOs(address);
    }

    @Override
    public long getFeeRate() {
        return htgContext.getEthGasPrice().longValue();
    }

    private WithdrawalUTXO getWithdrawalUTXO(String nerveTxHash) {
        WithdrawalUTXOTxData data = this.takeWithdrawalUTXOs(nerveTxHash);
        if (data == null) {
            return null;
        }
        return new WithdrawalUTXO(data.getNerveTxHash(), data.getHtgChainId(), data.getCurrentMultiSignAddress(), data.getCurrentVirtualBankTotal(), data.getFeeRate(), data.getPubs(), data.getUtxoDataList());
    }

    @Override
    public boolean isEnoughFeeOfWithdraw(String nerveTxHash, AssetName feeAssetName, BigDecimal fee) {
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        BigDecimal otherMainAssetUSD = coreApi.getUsdtPriceByAsset(feeAssetName);
        BigDecimal htgUSD = coreApi.getUsdtPriceByAsset(htgContext.ASSET_NAME());
        String otherSymbol = feeAssetName.toString();
        if(null == otherMainAssetUSD || null == htgUSD){
            logger().error("[{}][withdraw] Withdrawal fee calculation, Unable to obtain complete quotation. {}_USD: {}, {}_USD: {}, nerveTxHash: {}", htgContext.getConfig().getSymbol(), otherSymbol, otherMainAssetUSD, htgContext.getConfig().getSymbol(), htgUSD, nerveTxHash);
            throw new NulsRuntimeException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
        if (withdrawlUTXO == null) {
            logger().error("[{}][withdraw] Withdrawal fee calculation, empty withdrawlUTXO. nerveTxHash: {}", htgContext.getConfig().getSymbol(), nerveTxHash);
            throw new NulsRuntimeException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        List<String> multiSignAddressPubs = this.getMultiSignAddressPubs(withdrawlUTXO.getCurrentMultiSignAddress());
        int n = multiSignAddressPubs.size(), m = coreApi.getByzantineCount(n);
        byte[] nerveTxHashBytes = HexUtil.decode(nerveTxHash);
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        long size = BtcUtil.calcFeeMultiSignSizeP2WSH(UTXOList.size(), 1, new int[]{nerveTxHashBytes.length}, m, n);
        BigDecimal needNativeFee = BigDecimal.valueOf(size * withdrawlUTXO.getFeeRate());
        if (feeAssetName == htgContext.ASSET_NAME()) {
            int decimals = feeAssetName.decimals();
            String symbolNative = htgContext.getConfig().getSymbol();
            if (fee.compareTo(needNativeFee) < 0) {
                logger().info("[{}] Insufficient withdrawal fees, currently required by the network {}: {}, User provided {}: {}, Additional Required {}: {}",
                        symbolNative,
                        symbolNative,
                        needNativeFee.movePointLeft(decimals).toPlainString(),
                        symbolNative,
                        fee.movePointLeft(decimals).toPlainString(),
                        symbolNative,
                        needNativeFee.subtract(fee).movePointLeft(decimals).toPlainString());
                return false;
            }
            logger().info("[{}] The withdrawal fee is sufficient for the current network needs {}: {}, User provided {}: {}",
                    symbolNative,
                    symbolNative,
                    needNativeFee.movePointLeft(decimals).toPlainString(),
                    symbolNative,
                    fee.movePointLeft(decimals).toPlainString());
            return true;
        } else {
            BigDecimal needOtherFee = needNativeFee.movePointRight(feeAssetName.decimals()).multiply(htgUSD)
                    .divide(otherMainAssetUSD, feeAssetName.decimals(), RoundingMode.UP)
                    .movePointLeft(htgContext.ASSET_NAME().decimals());
            needOtherFee = new BigDecimal(needOtherFee.toBigInteger());
            String symbolNative = htgContext.getConfig().getSymbol();
            int decimalsPaid = feeAssetName.decimals();
            String symbolPaid = feeAssetName.toString();
            if (fee.compareTo(needOtherFee) < 0) {
                logger().info("[{}] Insufficient withdrawal fees, currently required by the network {}: {}, User provided {}: {}, Additional Required {}: {}",
                        symbolNative,
                        symbolPaid,
                        needOtherFee.movePointLeft(decimalsPaid).toPlainString(),
                        symbolPaid,
                        fee.movePointLeft(decimalsPaid).toPlainString(),
                        symbolPaid,
                        needOtherFee.subtract(fee).movePointLeft(decimalsPaid).toPlainString());
                return false;
            }
            logger().info("[{}] The withdrawal fee is sufficient for the current network needs{}: {}, User provided {}: {}",
                    symbolNative,
                    symbolPaid,
                    needOtherFee.movePointLeft(decimalsPaid).toPlainString(),
                    symbolPaid,
                    fee.movePointLeft(decimalsPaid).toPlainString());
            return true;
        }
    }

    @Override
    public String signWithdraw(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(txHash);
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();

        // take pubkeys of all managers
        List<ECKey> pubEcKeys = withdrawlUTXO.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());
        // calc the min number of signatures
        int n = withdrawlUTXO.getPubs().size(), m = htgContext.getConverterCoreApi().getByzantineCount(n);
        byte[] signerPub;
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            HeterogeneousAccount account = btcDocking.getAccount(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            // take pri from local
            ECKey pri = ECKey.fromPrivate(account.getPriKey());
            List<String> signatures = BtcUtil.createNativeSegwitMultiSignByOne(
                    pri, pubEcKeys,
                    value.longValue(),
                    toAddress,
                    UTXOList,
                    List.of(HexUtil.decode(txHash)),
                    m, n,
                    withdrawlUTXO.getFeeRate(),
                    htgContext.getConverterCoreApi().isNerveMainnet()
            );
            signerPub = pri.getPubKey();
            BtcSignData signData = new BtcSignData(signerPub, signatures.stream().map(s -> HexUtil.decode(s)).collect(Collectors.toList()));
            try {
                return HexUtil.encode(signData.serialize());
            } catch (IOException e) {
                throw new NulsException(ConverterErrorCode.IO_ERROR, e);
            }
        } else {
            // sign machine support
            signerPub = HexUtil.decode(htgContext.ADMIN_ADDRESS_PUBLIC_KEY());
            String signatures = htgContext.getConverterCoreApi().signBtcWithdrawByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(),
                    htgContext.HTG_CHAIN_ID(),
                    HexUtil.encode(signerPub),
                    txHash,
                    toAddress,
                    value.longValue(),
                    withdrawlUTXO);
            return signatures;
        }
    }


    @Override
    public Boolean verifySignWithdraw(String signAddress, String txHash, String toAddress, BigInteger amount, int assetId, String signature) throws NulsException {
        BtcSignData signData = new BtcSignData();
        signData.parse(HexUtil.decode(signature), 0);

        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(txHash);
        if (withdrawlUTXO == null) {
            return false;
        }
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        ECKey pub = ECKey.fromPublicOnly(signData.getPubkey());
        // take pubkeys of all managers
        List<ECKey> pubEcKeys = withdrawlUTXO.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());
        // calc the min number of signatures
        int n = withdrawlUTXO.getPubs().size(), m = htgContext.getConverterCoreApi().getByzantineCount(n);

        return BtcUtil.verifyNativeSegwitMultiSign(
                pub,
                signData.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()),
                pubEcKeys,
                amount.longValue(),
                toAddress,
                UTXOList,
                List.of(HexUtil.decode(txHash)),
                m, n,
                withdrawlUTXO.getFeeRate(),
                htgContext.getConverterCoreApi().isNerveMainnet());
    }

    private void beforeSend(HtgWaitingTxPo po) {
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        Map<String, Integer> seedPacker = coreApi.getSeedPacker();
        Set<String> allPackers = coreApi.getAllPackers();
        Map<String, Integer> currentVirtualBanks = new HashMap<>();
        int i = 0;
        for (String packer : allPackers) {
            Integer order = seedPacker.get(packer);
            if (order != null) {
                currentVirtualBanks.put(packer, order);
            } else {
                currentVirtualBanks.put(packer, ++i + 100);
            }
        }
        po.setCurrentVirtualBanks(currentVirtualBanks);
    }

    @Override
    public String createOrSignWithdrawTx(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {

        try {
            if (!htgContext.isAvailableRPC()) {
                logger().error("[{}]networkRPCUnavailable, pause this task", htgContext.getConfig().getSymbol());
                throw new NulsException(ConverterErrorCode.HTG_RPC_UNAVAILABLE);
            }
            IConverterCoreApi coreApi = htgContext.getConverterCoreApi();

            String adminPub = htgContext.ADMIN_ADDRESS_PUBLIC_KEY();
            String adminAddress = AddressTool.getStringAddressByBytes(AddressTool.getAddress(HexUtil.decode(adminPub), htgContext.NERVE_CHAINID()));
            Integer order = coreApi.getSeedPackerOrder(adminAddress);
            if (order == null) {
                return EMPTY_STRING;
            }
            logger().info("Preparing to send withdrawal {} Transactions,nerveTxHash: {}, toAddress: {}, value: {}, assetId: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, toAddress, value, assetId, signatureData);
            // Transaction preparation
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            this.beforeSend(waitingPo);
            // Save transaction call parameters and set waiting time to end
            htgInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, toAddress, value, assetId, signatureData, order, waitingPo);
            // When checking the order, do not send transactions that are not in the first place
            if (checkOrder && order != 0) {
                logger().info("Non primary non transaction, order: {}", order);
                return EMPTY_STRING;
            }
            // Business validation
            HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            boolean isContractAsset = assetId > 1;
            String contractAddressERC20  = HtgConstant.ZERO_ADDRESS;
            po.setDecimals(htgContext.getConfig().getDecimals());
            po.setAssetId(htgContext.HTG_ASSET_ID());

            // Convert the address to lowercase
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            // If the accuracy of cross chain assets is different, then the conversion accuracy
            value = coreApi.checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
            // calculateGasPrice
            // Check withdrawal fees
            WithdrawalTotalFeeInfo feeInfo = coreApi.getFeeOfWithdrawTransaction(nerveTxHash);
            if (feeInfo.isNvtAsset()) feeInfo.setHtgMainAssetName(AssetName.NVT);
            // When using other main assets of non withdrawal networks as transaction fees
            // withdrawal fee check enough
            BigDecimal feeAmount = new BigDecimal(coreApi.checkDecimalsSubtractedToNerveForWithdrawal(feeInfo.getHtgMainAssetName().chainId(), 1, feeInfo.getFee()));
            if (!this.isEnoughFeeOfWithdraw(nerveTxHash, feeInfo.getHtgMainAssetName(), feeAmount)) {
                throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
            }

            Map<String, List<String>> signatures = new HashMap<>();
            String[] signDatas = signatureData.split(",");
            for (String signData : signDatas) {
                BtcSignData signDataObj = new BtcSignData();
                signDataObj.parse(HexUtil.decode(signData.trim()), 0);
                signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()));
            }

            WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
            List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
            // take pubkeys of all managers
            List<ECKey> pubEcKeys = withdrawlUTXO.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());
            // calc the min number of signatures
            int n = withdrawlUTXO.getPubs().size(), m = htgContext.getConverterCoreApi().getByzantineCount(n);

            Transaction tx = BtcUtil.createNativeSegwitMultiSignTx(
                    signatures, pubEcKeys,
                    value.longValue(),
                    toAddress,
                    UTXOList,
                    List.of(HexUtil.decode(nerveTxHash)),
                    m, n,
                    withdrawlUTXO.getFeeRate(),
                    htgContext.getConverterCoreApi().isNerveMainnet()
            );
            String htgTxHash = btcWalletApi.broadcast(tx);
            if (StringUtils.isNotBlank(htgTxHash)) {
                // Record withdrawal transactions that have been transferred to HTG Network transmission
                htgPendingTxHelper.commitNervePendingWithdrawTx(nerveTxHash, htgTxHash);
            }
            // save UNCONFIRMED_TX_QUEUE
            this.saveUnconfirmedTxQueue(nerveTxHash, po, withdrawlUTXO.getCurrentMultiSignAddress(), htgTxHash, HeterogeneousChainTxType.WITHDRAW);
            return htgTxHash;
        } catch (Exception e) {
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            logger().error(e);
            if (e.getCause() != null) logger().error(e.getCause());
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    @Override
    public List<String> getMultiSignAddressPubs(String address) {
        return htgMultiSignAddressHistoryStorageService.getMultiSignAddressPubs(address);
    }

    @Override
    public boolean checkEnoughAvailablePubs(String address) {
        /*if (true) {
            Set<String> availablePubs = htgMultiSignAddressHistoryStorageService.getMultiSignAddrAvailablePubs(address);
            return true;
        }
        // whether the available public key of the current bitSys'chain is greater than total * (2/3) + 1
        Set<String> availablePubs = htgMultiSignAddressHistoryStorageService.getMultiSignAddrAvailablePubs(address);
        if (availablePubs == null) {
            return false;
        }*/

        List<String> addressPubs = htgMultiSignAddressHistoryStorageService.getMultiSignAddressPubs(address);
        List<String> allPackerPubs = htgContext.getConverterCoreApi().getAllPackerPubs();
        List<String> availablePubs = new ArrayList<>();
        for (String pub : addressPubs) {
            if (allPackerPubs.contains(pub)) {
                availablePubs.add(pub);
            }
        }
        htgContext.logger().info("{} availablePubs: {}", address, availablePubs);
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        int byzantineCount = coreApi.getByzantineCount(addressPubs.size());
        return availablePubs.size() > byzantineCount + 1;
    }

    private Object[] makeChangeTxBaseInfo (WithdrawalUTXO withdrawlUTXO) {
        List<String> multiSignAddressPubs = this.getMultiSignAddressPubs(withdrawlUTXO.getCurrentMultiSignAddress());
        return BtcUtil.makeChangeTxBaseInfo(htgContext, withdrawlUTXO, multiSignAddressPubs);
    }

    private void changeBaseCheck(String nerveTxHash, String[] addPubs, String[] removePubs) throws NulsException {
        // Business validation
        if (addPubs == null) {
            addPubs = new String[0];
        }
        if (removePubs == null) {
            removePubs = new String[0];
        }
        // Transaction preparation
        Set<String> addSet = new HashSet<>();
        for (int a = 0, addSize = addPubs.length; a < addSize; a++) {
            String add = addPubs[a];
            add = add.toLowerCase();
            addPubs[a] = add;
            if (!addSet.add(add)) {
                logger().error("Duplicate list of addresses to be added, nerveTxHash: {}", nerveTxHash);
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_2);
            }
        }
        Set<String> removeSet = new HashSet<>();
        for (int r = 0, removeSize = removePubs.length; r < removeSize; r++) {
            String remove = removePubs[r];
            remove = remove.toLowerCase();
            removePubs[r] = remove;
            if (!removeSet.add(remove)) {
                logger().error("Duplicate list of pending exits, nerveTxHash: {}", nerveTxHash);
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_4);
            }
        }
    }

    @Override
    public String signManagerChanges(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount) throws NulsException {
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        if (coreApi.checkChangeP35(nerveTxHash)) {
            return nerveTxHash;
        }
        // Business validation
        this.changeBaseCheck(nerveTxHash, addPubs, removePubs);
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
        if (withdrawlUTXO == null || HtgUtil.isEmptyList(withdrawlUTXO.getUtxoDataList())) {
            return nerveTxHash;
        }
        Object[] baseInfo = this.makeChangeTxBaseInfo(withdrawlUTXO);
        List<ECKey> currentPubs = (List<ECKey>) baseInfo[0];
        long amount = (long) baseInfo[1];
        String toAddress = (String) baseInfo[2];
        List<byte[]> opReturns = (List<byte[]>) baseInfo[3];
        int m = (int) baseInfo[4];
        int n = (int) baseInfo[5];

        // Check whether the remaining utxo is enough to pay the transfer fee, otherwise the transaction will not be issued.
        if (amount <= ConverterConstant.BTC_DUST_AMOUNT) {
            return nerveTxHash;
        }
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        byte[] signerPub;
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            HeterogeneousAccount account = btcDocking.getAccount(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            // take pri from local
            ECKey pri = ECKey.fromPrivate(account.getPriKey());
            /*// take pubkeys of all managers
            List<ECKey> newPubEcKeys = withdrawlUTXO.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());

            List<String> multiSignAddressPubs = this.getMultiSignAddressPubs(withdrawlUTXO.getCurrentMultiSignAddress());
            List<ECKey> oldPubEcKeys = multiSignAddressPubs.stream().map(p -> ECKey.fromPublicOnly(HexUtil.decode(p))).collect(Collectors.toList());

            String toAddress = BtcUtil.getNativeSegwitMultiSignAddress(coreApi.getByzantineCount(newPubEcKeys.size()), newPubEcKeys, coreApi.isNerveMainnet());
            // calc the min number of signatures
            int n = oldPubEcKeys.size(), m = coreApi.getByzantineCount(n);
            byte[] nerveTxHashBytes = HexUtil.decode(nerveTxHash);
            long fee = BtcUtil.calcFeeMultiSignSizeP2WSH(UTXOList.size(), 1, new int[]{nerveTxHashBytes.length}, m, n);
            long totalMoney = 0;
            for (int k = 0; k < UTXOList.size(); k++) {
                totalMoney += UTXOList.get(k).getAmount().longValue();
            }*/
            List<String> signatures = BtcUtil.createNativeSegwitMultiSignByOne(
                    pri,
                    currentPubs,
                    amount,
                    toAddress,
                    UTXOList,
                    opReturns,
                    m,
                    n,
                    withdrawlUTXO.getFeeRate(),
                    coreApi.isNerveMainnet(),
                    true
            );
            signerPub = pri.getPubKey();
            BtcSignData signData = new BtcSignData(signerPub, signatures.stream().map(s -> HexUtil.decode(s)).collect(Collectors.toList()));
            try {
                return HexUtil.encode(signData.serialize());
            } catch (IOException e) {
                throw new NulsException(ConverterErrorCode.IO_ERROR, e);
            }
        } else {
            // sign machine support
            WithdrawalUTXO data = new WithdrawalUTXO(
                    nerveTxHash,
                    htgContext.HTG_CHAIN_ID(),
                    withdrawlUTXO.getCurrentMultiSignAddress(),
                    withdrawlUTXO.getCurrenVirtualBankTotal(),
                    withdrawlUTXO.getFeeRate(),
                    currentPubs.stream().map(p -> p.getPubKey()).collect(Collectors.toList()),
                    UTXOList);
            signerPub = HexUtil.decode(htgContext.ADMIN_ADDRESS_PUBLIC_KEY());
            String signatures = htgContext.getConverterCoreApi().signBtcChangeByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(),
                    htgContext.HTG_CHAIN_ID(),
                    HexUtil.encode(signerPub),
                    nerveTxHash,
                    toAddress,
                    amount,
                    data);
            return signatures;
        }

    }

    @Override
    public Boolean verifySignManagerChanges(String signAddress, String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signature) throws NulsException {
        if (htgContext.getConverterCoreApi().checkChangeP35(nerveTxHash)) {
            return true;
        }
        // Business validation
        this.changeBaseCheck(nerveTxHash, addPubs, removePubs);
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
        if (withdrawlUTXO == null || HtgUtil.isEmptyList(withdrawlUTXO.getUtxoDataList())) {
            return true;
        }
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        Object[] baseInfo = this.makeChangeTxBaseInfo(withdrawlUTXO);
        long amount = (long) baseInfo[1];
        // Check whether the remaining utxo is enough to pay the transfer fee, otherwise the transaction will not be issued.
        if (amount <= ConverterConstant.BTC_DUST_AMOUNT) {
            return true;
        }

        BtcSignData signData = new BtcSignData();
        signData.parse(HexUtil.decode(signature), 0);

        ECKey pub = ECKey.fromPublicOnly(signData.getPubkey());

        return BtcUtil.verifyNativeSegwitMultiSign(
                pub,
                signData.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()),
                (List<ECKey>) baseInfo[0],
                (long) baseInfo[1],
                (String) baseInfo[2],
                UTXOList,
                (List<byte[]>) baseInfo[3],
                (int) baseInfo[4],
                (int) baseInfo[5],
                withdrawlUTXO.getFeeRate(),
                htgContext.getConverterCoreApi().isNerveMainnet(), true);
    }

    @Override
    public boolean validateManagerChangesTx(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signatureData) throws NulsException {
        if (htgContext.getConverterCoreApi().checkChangeP35(nerveTxHash)) {
            return true;
        }
        // Business validation
        this.changeBaseCheck(nerveTxHash, addPubs, removePubs);
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
        if (withdrawlUTXO == null || HtgUtil.isEmptyList(withdrawlUTXO.getUtxoDataList())) {
            return true;
        }
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        Object[] baseInfo = this.makeChangeTxBaseInfo(withdrawlUTXO);
        long amount = (long) baseInfo[1];
        // Check whether the remaining utxo is enough to pay the transfer fee, otherwise the transaction will not be issued.
        if (amount <= ConverterConstant.BTC_DUST_AMOUNT) {
            return true;
        }
        // Assemble and verify sufficient Byzantine signatures
        Map<String, List<String>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()));
        }
        List<ECKey> pubs = (List<ECKey>) baseInfo[0];
        int verified = BtcUtil.verifyNativeSegwitMultiSignCount(
                signatures,
                pubs,
                amount,
                (String) baseInfo[2],
                UTXOList,
                (List<byte[]>) baseInfo[3],
                (int) baseInfo[4],
                (int) baseInfo[5],
                withdrawlUTXO.getFeeRate(),
                htgContext.getConverterCoreApi().isNerveMainnet(), true);
        int byzantineCount = htgContext.getConverterCoreApi().getByzantineCount(pubs.size());
        return verified >= byzantineCount;
    }

    @Override
    public String createOrSignManagerChangesTx(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signatureData, boolean checkOrder) throws NulsException {
        try {
            WithdrawalUTXO withdrawlUTXO = null;
            boolean completeDirectly = false;
            do {
                if (htgContext.getConverterCoreApi().checkChangeP35(nerveTxHash)) {
                    completeDirectly = true;
                    break;
                }
                // Business validation
                this.changeBaseCheck(nerveTxHash, addPubs, removePubs);
                withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
                if (withdrawlUTXO == null || HtgUtil.isEmptyList(withdrawlUTXO.getUtxoDataList())) {
                    completeDirectly = true;
                    break;
                }
            } while (false);
            if (completeDirectly) {
                logger().info("Virtual banking changes have not been added or exited, and confirmation transactions have been sent directly, nerveTxHash: {}", nerveTxHash);
                try {
                    htgCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            HeterogeneousChainTxType.CHANGE,
                            nerveTxHash,
                            null, //htTxHash,
                            null, //ethTx blockHeight,
                            null, //ethTx tx time,
                            htgContext.MULTY_SIGN_ADDRESS(),
                            null,  //ethTx signers
                            null
                    );
                } catch (Exception e) {
                    if (e instanceof NulsException) {
                        throw (NulsException) e;
                    }
                    throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
                }
                return EMPTY_STRING;
            }
            IConverterCoreApi coreApi = htgContext.getConverterCoreApi();

            String adminPub = htgContext.ADMIN_ADDRESS_PUBLIC_KEY();
            String adminAddress = AddressTool.getStringAddressByBytes(AddressTool.getAddress(HexUtil.decode(adminPub), htgContext.NERVE_CHAINID()));
            Integer order = coreApi.getSeedPackerOrder(adminAddress);
            if (order == null) {
                return EMPTY_STRING;
            }
            logger().info("Preparing to send change {} Transactions, nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
            // Transaction preparation
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            this.beforeSend(waitingPo);
            // Save transaction call parameters and set waiting time to end
            htgInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, addPubs, removePubs, orginTxCount, signatureData, order, waitingPo);
            // When checking the order, do not send transactions that are not in the first place
            if (checkOrder && order != 0) {
                logger().info("Non primary non transaction, order: {}", order);
                return EMPTY_STRING;
            }
            List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
            Object[] baseInfo = this.makeChangeTxBaseInfo(withdrawlUTXO);
            long amount = (long) baseInfo[1];
            // Prepare data
            HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            po.setAddAddresses(addPubs);
            po.setRemoveAddresses(removePubs);
            po.setOrginTxCount(orginTxCount);
            // Check whether the remaining utxo is enough to pay the transfer fee, otherwise the transaction will not be issued.
            // If you have not joined or exited, send a confirmation transaction directly
            if (amount <= ConverterConstant.BTC_DUST_AMOUNT || (addPubs.length == 0 && removePubs.length == 0)) {
                logger().info("Virtual banking changes have not been added or exited, and confirmation transactions have been sent directly, nerveTxHash: {}", nerveTxHash);
                try {
                    htgCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            HeterogeneousChainTxType.CHANGE,
                            nerveTxHash,
                            null, //htTxHash,
                            null, //ethTx blockHeight,
                            null, //ethTx tx time,
                            htgContext.MULTY_SIGN_ADDRESS(),
                            null,  //ethTx signers
                            null
                    );
                } catch (Exception e) {
                    if (e instanceof NulsException) {
                        throw (NulsException) e;
                    }
                    throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
                }
                return EMPTY_STRING;
            }
            Map<String, List<String>> signatures = new HashMap<>();
            String[] signDatas = signatureData.split(",");
            for (String signData : signDatas) {
                BtcSignData signDataObj = new BtcSignData();
                signDataObj.parse(HexUtil.decode(signData.trim()), 0);
                signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()));
            }

            Transaction tx = BtcUtil.createNativeSegwitMultiSignTx(
                    signatures,
                    (List<ECKey>) baseInfo[0],
                    amount,
                    (String) baseInfo[2],
                    UTXOList,
                    (List<byte[]>) baseInfo[3],
                    (int) baseInfo[4],
                    (int) baseInfo[5],
                    withdrawlUTXO.getFeeRate(),
                    coreApi.isNerveMainnet(), true);
            String htgTxHash = btcWalletApi.broadcast(tx);
            // save UNCONFIRMED_TX_QUEUE
            this.saveUnconfirmedTxQueue(nerveTxHash, po, withdrawlUTXO.getCurrentMultiSignAddress(), htgTxHash, HeterogeneousChainTxType.CHANGE);
            return htgTxHash;
        } catch (Exception e) {
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            logger().error(e);
            if (e.getCause() != null) logger().error(e.getCause());
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    @Override
    public boolean hasRecordFeePayment(String htgTxHash) throws NulsException {
        WithdrawalFeeLog feeLog = htgMultiSignAddressHistoryStorageService.queryChainWithdrawalFeeLog(htgTxHash);
        return feeLog != null;
    }

    @Override
    public void recordFeePayment(long blockHeight, String blockHash, String htgTxHash, long fee, boolean recharge) throws Exception {
        WithdrawalFeeLog feeLog = new WithdrawalFeeLog(
                blockHeight, blockHash, htgTxHash, htgContext.HTG_CHAIN_ID(), fee, recharge
        );
        htgMultiSignAddressHistoryStorageService.saveChainWithdrawalFee(feeLog);
    }

    @Override
    public BigInteger getChainWithdrawalFee() {
        return htgMultiSignAddressHistoryStorageService.getChainWithdrawalFee();
    }

    @Override
    public WithdrawalFeeLog getWithdrawalFeeLogFromDB(String htgTxHash) throws Exception {
        return htgMultiSignAddressHistoryStorageService.queryChainWithdrawalFeeLog(htgTxHash);
    }

    @Override
    public WithdrawalFeeLog takeWithdrawalFeeLogFromTxParse(String htgTxHash) throws Exception {
        RawTransaction txInfo = btcWalletApi.getTransactionByHash(htgTxHash);
        if (txInfo.getConfirmations() == null || txInfo.getConfirmations().intValue() == 0) {
            return null;
        }
        List<RawOutput> outputList = txInfo.getVOut();
        List<RawInput> inputList = txInfo.getVIn();
        HeterogeneousChainTxType txType = null;
        OUT:
        do {
            for (RawInput input : inputList) {
                String inputAddress = BtcUtil.takeAddressWithP2WSH(input, htgContext.getConverterCoreApi().isNerveMainnet());
                if (htgListener.isListeningAddress(inputAddress)) {
                    txType = HeterogeneousChainTxType.WITHDRAW;
                    break OUT;
                }
            }
            for (RawOutput output : outputList) {
                String outputAddress = output.getScriptPubKey().getAddress();
                if (htgListener.isListeningAddress(outputAddress)) {
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    break OUT;
                }
            }
        } while (false);
        if (txType == null) {
            return null;
        }
        if (txType == HeterogeneousChainTxType.DEPOSIT) {
            BtcUnconfirmedTxPo po = (BtcUnconfirmedTxPo) btcParseTxHelper.parseDepositTransaction(txInfo, null, true);
            if (po.getNerveAddress().equals(ConverterContext.BITCOIN_SYS_WITHDRAWAL_FEE_ADDRESS)) {
                // Record chain fee entry
                WithdrawalFeeLog feeLog = new WithdrawalFeeLog(
                        po.getBlockHeight(), po.getBlockHash(), htgTxHash, htgContext.HTG_CHAIN_ID(), po.getValue().longValue(), true);
                feeLog.setTxTime(txInfo.getBlockTime());
                return feeLog;
            }
        } else if (txType == HeterogeneousChainTxType.WITHDRAW) {
            // All transactions with nerve multi-signature addresses in from must record handling fee expenditures.
            long fee = BtcUtil.calcTxFee(txInfo, btcWalletApi);
            WithdrawalFeeLog feeLog = new WithdrawalFeeLog(
                    Long.valueOf(btcWalletApi.getBlockHeaderByHash(txInfo.getBlockHash()).getHeight()), txInfo.getBlockHash(), htgTxHash, htgContext.HTG_CHAIN_ID(), fee, false);
            feeLog.setTxTime(txInfo.getBlockTime());
            return feeLog;
        }
        return null;
    }

    @Override
    public void saveWithdrawalUTXOs(WithdrawalUTXOTxData txData) throws Exception {
        htgMultiSignAddressHistoryStorageService.saveWithdrawalUTXOs(txData);
    }

    @Override
    public boolean isLockedUTXO(String txid, int vout) {
        return htgMultiSignAddressHistoryStorageService.isLockedUTXO(txid, vout);
    }

    @Override
    public String getNerveHashByLockedUTXO(String txid, int vout) {
        return htgMultiSignAddressHistoryStorageService.getNerveHashByLockedUTXO(txid, vout);
    }

    @Override
    public WithdrawalUTXOTxData checkLockedUTXO(String nerveTxHash, List<UsedUTXOData> usedUTXOs) throws Exception {
        return htgMultiSignAddressHistoryStorageService.checkLockedUTXO(nerveTxHash, usedUTXOs);
    }

    @Override
    public WithdrawalUTXOTxData takeWithdrawalUTXOs(String nerveTxhash) {
        return htgMultiSignAddressHistoryStorageService.takeWithdrawalUTXOs(nerveTxhash);
    }

    @Override
    public void saveWithdrawalUTXORebuildPO(String nerveTxHash, WithdrawalUTXORebuildPO po) throws Exception {
        htgMultiSignAddressHistoryStorageService.saveWithdrawalUTXORebuildPO(nerveTxHash, po);
    }

    @Override
    public WithdrawalUTXORebuildPO getWithdrawalUTXORebuildPO(String nerveTxHash) throws Exception {
        return htgMultiSignAddressHistoryStorageService.getWithdrawalUTXORebuildPO(nerveTxHash);
    }

    @Override
    public long getWithdrawalFeeSize(int utxoSize) {
        int n = htgContext.getConverterCoreApi().getVirtualBankSize();
        int m = htgContext.getConverterCoreApi().getByzantineCount(n);
        long size = BtcUtil.calcFeeMultiSignSizeP2WSH(utxoSize, 1, new int[]{32}, m, n);
        return size;
    }

    @Override
    public long convertMainAssetByFee(AssetName feeAssetName, BigDecimal fee) {
        if (feeAssetName == htgContext.ASSET_NAME()) {
            return fee.longValue();
        } else {
            IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
            BigDecimal otherMainAssetUSD = coreApi.getUsdtPriceByAsset(feeAssetName);
            BigDecimal htgUSD = coreApi.getUsdtPriceByAsset(htgContext.ASSET_NAME());
            String otherSymbol = feeAssetName.toString();
            if(null == otherMainAssetUSD || null == htgUSD){
                logger().error("[{}] ConvertMainAssetByFee calculation, Unable to obtain complete quotation. {}_USD: {}, {}_USD: {}", htgContext.getConfig().getSymbol(), otherSymbol, otherMainAssetUSD, htgContext.getConfig().getSymbol(), htgUSD);
                throw new NulsRuntimeException(ConverterErrorCode.DATA_NOT_FOUND);
            }
            BigDecimal mainFee = fee.movePointRight(htgContext.ASSET_NAME().decimals()).multiply(otherMainAssetUSD)
                    .divide(htgUSD, htgContext.ASSET_NAME().decimals(), RoundingMode.UP)
                    .movePointLeft(feeAssetName.decimals());
            mainFee = new BigDecimal(mainFee.toBigInteger());
            return mainFee.longValue();
        }
    }

    @Override
    public Map<String, String> getMinimumFeeOfWithdrawal(String nerveTxHash) {
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
        if (withdrawlUTXO == null) {
            logger().error("[{}][withdraw] Withdrawal fee calculation, empty withdrawlUTXO. nerveTxHash: {}", htgContext.getConfig().getSymbol(), nerveTxHash);
            throw new NulsRuntimeException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        List<String> multiSignAddressPubs = this.getMultiSignAddressPubs(withdrawlUTXO.getCurrentMultiSignAddress());
        int n = multiSignAddressPubs.size(), m = htgContext.getConverterCoreApi().getByzantineCount(n);
        byte[] nerveTxHashBytes = HexUtil.decode(nerveTxHash);
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        long size = BtcUtil.calcFeeMultiSignSizeP2WSH(UTXOList.size(), 1, new int[]{nerveTxHashBytes.length}, m, n);
        BigDecimal needNativeFee = BigDecimal.valueOf(size * withdrawlUTXO.getFeeRate());
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("minimumFee", needNativeFee.toPlainString());
        resultMap.put("utxoSize", String.valueOf(UTXOList.size()));
        resultMap.put("feeRate", String.valueOf(withdrawlUTXO.getFeeRate()));
        return resultMap;
    }

    private void saveUnconfirmedTxQueue(String nerveTxHash, HtgUnconfirmedTxPo po, String multiSignAddress, String htgTxHash, HeterogeneousChainTxType txType) throws Exception {
        htgInvokeTxHelper.saveSentEthTx(nerveTxHash);
        // Save unconfirmed transactions
        po.setTxHash(htgTxHash);
        po.setFrom(multiSignAddress);
        po.setTxType(txType);
        htgUnconfirmedTxStorageService.save(po);
        htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
        // Monitor the packaging status of this transaction
        htgListener.addListeningTx(htgTxHash);
        logger().info("NerveNetwork oriented {} Network transmission [{}] transaction, nerveTxHash: {}, details: {}", htgContext.getConfig().getSymbol(), txType, nerveTxHash, po.toString());
    }
}

