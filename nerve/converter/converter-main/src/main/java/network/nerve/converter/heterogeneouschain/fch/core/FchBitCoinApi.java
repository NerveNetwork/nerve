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
package network.nerve.converter.heterogeneouschain.fch.core;

import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.btc.txdata.*;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.interfaces.IBitCoinApi;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.fch.helper.FchParseTxHelper;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

import static io.protostuff.ByteString.EMPTY_STRING;

/**
 * @author: PierreLuo
 * @date: 2024/2/29
 */
public class FchBitCoinApi implements IBitCoinApi, BeanInitial {
    private IHeterogeneousChainDocking docking;
    private FchWalletApi fchWalletApi;
    private FchParseTxHelper fchParseTxHelper;
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
        return Collections.EMPTY_LIST;
    }

    @Override
    public long getFeeRate() {
        return 1;
    }


    @Override
    public boolean isEnoughFeeOfWithdraw(String nerveTxHash, AssetName feeAssetName, BigDecimal fee) {
        return true;
    }

    @Override
    public String signWithdraw(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        return txHash;
    }


    @Override
    public Boolean verifySignWithdraw(String signAddress, String txHash, String toAddress, BigInteger amount, int assetId, String signature) throws NulsException {
        return true;
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
        return EMPTY_STRING;
    }

    @Override
    public List<String> getMultiSignAddressPubs(String address) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean checkEnoughAvailablePubs(String address) {
        return true;
    }


    @Override
    public String signManagerChanges(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount) throws NulsException {
        return nerveTxHash;

    }

    @Override
    public Boolean verifySignManagerChanges(String signAddress, String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signature) throws NulsException {
        return true;
    }

    @Override
    public boolean validateManagerChangesTx(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signatureData) throws NulsException {
        return true;
    }

    @Override
    public String createOrSignManagerChangesTx(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signatureData, boolean checkOrder) throws NulsException {
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
        long size = 550;
        return size + 350L * (utxoSize - 1);
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
        return Collections.EMPTY_MAP;
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

