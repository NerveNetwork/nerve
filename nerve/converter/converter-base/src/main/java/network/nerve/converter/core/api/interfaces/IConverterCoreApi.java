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
package network.nerve.converter.core.api.interfaces;

import io.nuls.base.data.Transaction;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.model.bo.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Mimi
 * @date: 2020-05-08
 */
public interface IConverterCoreApi {
    /**
     * obtainNerveThe current block height of the network
     */
    long getCurrentBlockHeightOnNerve();

    /**
     * Is the current node a virtual bank
     */
    boolean isVirtualBankByCurrentNode();

    /**
     * Is the current node a seed virtual bank
     */
    boolean isSeedVirtualBankByCurrentNode();

    /**
     * Obtain the order in which the current node joins the virtual bank
     */
    int getVirtualBankOrder();

    /**
     * Obtain the current number of virtual bank members
     */
    int getVirtualBankSize();

    /**
     * obtainNervetransaction
     */
    Transaction getNerveTx(String hash);

    /**
     * Is the current node in a running state
     */
    boolean isRunning();

    /**
     * Obtain heterogeneous chain withdrawal data
     */
    HeterogeneousWithdrawTxInfo getWithdrawTxInfo(String nerveTxHash) throws NulsException;

    /**
     * Obtain a list of virtual banks for a specified heterogeneous chain
     */
    Map<String, Integer>  currentVirtualBanks(int hChainId);
    Map<String, Integer>  currentVirtualBanksBalanceOrder(int hChainId);

    /**
     * Retrieve Byzantine signatures again
     */
    List<HeterogeneousSign> regainSignatures(int nerveChainId, String nerveTxHash, int hChainId);

    /**
     * Whether heterogeneous chain assets are bound to existing onesNERVEasset
     */
    boolean isBoundHeterogeneousAsset(int hChainId, int hAssetId);

    /**
     * Is the withdrawal transaction heterogeneous chain confirmed(Determine by obtaining withdrawal confirmation transaction data)
     * @param nerveChainId
     * @param hash
     * @return
     */
    boolean isWithdrawalComfired(String nerveTxHash);

    /**
     * Obtain withdrawal transactions provided byNVTHandling fees, including additional handling fees for users
     */
    WithdrawalTotalFeeInfo getFeeOfWithdrawTransaction(String nerveTxHash) throws NulsException;

    /**
     * Obtain the specified asset'sUSDTprice
     */
    BigDecimal getUsdtPriceByAsset(AssetName assetName);

    /**
     * Does it support a new withdrawal fee mechanism
     */
    boolean isSupportNewMechanismOfWithdrawalFee();
    /**
     * Does it support transfer and partial destructionERC20
     */
    boolean isSupportProtocol12ERC20OfTransferBurn();

    /**
     * Verify if it isnerveaddress
     */
    boolean validNerveAddress(String address);

    /**
     * Does it support rechargingERC20New verification method forI
     */
    boolean isSupportProtocol13NewValidationOfERC20();

    /**
     * Does it support protocols14 v1.14.0
     */
    boolean isProtocol14();

    /**
     * Does it support wave field cross chain v1.15.0
     */
    boolean isSupportProtocol15TrxCrossChain();

    /**
     * Does it support protocols16 v1.16.0
     */
    boolean isProtocol16();

    /**
     * Does it support protocols21 v1.21.0
     */
    boolean isProtocol21();

    /**
     * Does it support protocols22 v1.22.0
     */
    boolean isProtocol22();

    /**
     * Does it support protocols23 v1.23.0
     */
    boolean isProtocol23();
    /**
     * Does it support protocols24 v1.24.0
     */
    boolean isProtocol24();
    /**
     * Does it support protocols26 v1.26.0
     */
    boolean isProtocol26();
    /**
     * Does it support protocols27 v1.27.0
     */
    boolean isProtocol27();
    /**
     * Does it support protocols31 v1.31.0
     */
    boolean isProtocol31();
    boolean isProtocol33();
    boolean isProtocol34();
    boolean isProtocol35();
    boolean isProtocol36();
    boolean isProtocol37();
    boolean isProtocol38();

    /**
     * Add task
     */
    void addHtgConfirmTxHandler(Runnable runnable);
    void addHtgRpcAvailableHandler(Runnable runnable);
    void addHtgWaitingTxInvokeDataHandler(Runnable runnable);
    List<Runnable> getHtgConfirmTxHandlers();
    List<Runnable> getHtgRpcAvailableHandlers();
    List<Runnable> getHtgWaitingTxInvokeDataHandlers();

    boolean skippedTransaction(String nerveTxHash);

    ConverterConfig getConverterConfig();

    boolean isPauseInHeterogeneousAsset(int hChainId, int hAssetId) throws Exception;
    boolean isPauseOutHeterogeneousAsset(int hChainId, int hAssetId) throws Exception;

    Map<Long, Map> HTG_RPC_CHECK_MAP();
    HeterogeneousAssetInfo getHeterogeneousAsset(int hChainId, int hAssetId);
    BigInteger checkDecimalsSubtractedToNerveForWithdrawal(int htgChainId, int htgAssetId, BigInteger value);
    BigInteger checkDecimalsSubtractedToNerveForWithdrawal(HeterogeneousAssetInfo assetInfo, BigInteger value);
    BigInteger checkDecimalsSubtractedToNerveForDeposit(int htgChainId, int nerveAssetChainId, int nerveAssetId, BigInteger value);
    BigInteger checkDecimalsSubtractedToNerveForDeposit(HeterogeneousAssetInfo assetInfo, BigInteger value);

    void setCurrentHeterogeneousVersionII();

    boolean checkNetworkRunning(int hChainId);

    boolean isLocalSign();

    String signWithdrawByMachine(long nativeId, String signerPubkey, String txKey, String toAddress, BigInteger value, Boolean isContractAsset, String erc20, byte version) throws NulsException;
    String signChangeByMachine(long nativeId, String signerPubkey, String txKey, String[] adds, int count, String[] removes, byte version) throws NulsException;
    String signUpgradeByMachine(long nativeId, String signerPubkey, String txKey, String upgradeContract, byte version) throws NulsException;
    String signRawTransactionByMachine(long nativeId, String signerPubkey, String from, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to, BigInteger value, String data) throws Exception;
    String signTronRawTransactionByMachine(String signerPubkey, String txStr) throws Exception;

    void addChainDBName(int hChainId, String dbName);
    Map<Integer, String> chainDBNameMap();
    void setDbMergedStatus(int hChainid);
    boolean isDbMerged(int hChainid);
    String mergedDBName();
    BigInteger getL1Fee(int htgChainId);
    boolean hasL1FeeOnChain(int htgChainId);

    boolean isNerveMainnet();

    void setFchToDogePrice(BigDecimal fchToDogePrice);

    int getByzantineCount(Integer total);

    Integer getSeedPackerOrder(String addr);

    Map<String, Integer> getSeedPacker();

    Set<String> getAllPackers();
    List<String> getAllPackerPubs();

    String getBtcFeeReceiverPub();

    String getInitialBtcPubKeyList();

    String getInitialFchPubKeyList();

    String getInitialBchPubKeyList();

    HeterogeneousAssetInfo getHeterogeneousAssetByNerveAsset(int htgChainId, int nerveAssetChainId, int nerveAssetId);

    String signBtcWithdrawByMachine(long nativeId, int htgChainId, String signerPubkey, String txKey, String toAddress, long value, WithdrawalUTXO txData, Long splitGranularity) throws NulsException;
    String signBtcChangeByMachine(long nativeId, int htgChainId, String signerPubkey, String txKey, String toAddress, long value, WithdrawalUTXO txData) throws NulsException;

    void updateMultySignAddress(int heterogeneousChainId, String multySignAddress) throws NulsException;

    boolean checkChangeP35(String nerveTxHash);

    String[] inChangeP35();
    String[] outChangeP35();

    void closeHtgChain(int htgChainId) throws Exception;

    String signFchWithdrawByMachine(long nativeId, int htgChainId, String signerPubkey, String txKey, String toAddress, long value, WithdrawalUTXO txData, Long splitGranularity) throws NulsException;
    String signFchChangeByMachine(long nativeId, int htgChainId, String signerPubkey, String txKey, String toAddress, long value, WithdrawalUTXO txData) throws NulsException;
    String signBchWithdrawByMachine(long nativeId, int htgChainId, String signerPubkey, String txKey, String toAddress, long value, WithdrawalUTXO txData, Long splitGranularity) throws NulsException;
    String signBchChangeByMachine(long nativeId, int htgChainId, String signerPubkey, String txKey, String toAddress, long value, WithdrawalUTXO txData) throws NulsException;

    NerveAssetInfo getHtgMainAsset(int htgChainId);

    String getBlockHashByHeight(long height);

    void updateSplitGranularity(int htgChainId, long splitGranularity) throws Exception;

    BigInteger getCrossOutTxFee(String txHash) throws NulsException;
}
