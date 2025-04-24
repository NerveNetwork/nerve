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
package network.nerve.converter.core.heterogeneous.docking.interfaces;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.core.api.interfaces.IBitCoinApi;
import network.nerve.converter.core.heterogeneous.register.interfaces.IHeterogeneousChainRegister;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.utils.ConverterUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static io.protostuff.ByteString.EMPTY_STRING;

/**
 * @author: Mimi
 * @date: 2020-02-17
 */
public interface IHeterogeneousChainDocking {

    /**
     * Does the current chain support contract assets
     */
    boolean isSupportContractAssetByCurrentChain();
    /**
     * Current heterogeneous chainID
     */
    Integer getChainId();
    /**
     * Current heterogeneous chainSymbol
     */
    String getChainSymbol();
    /**
     * Current signature address
     */
    String getCurrentSignAddress();
    /**
     * Current address for multiple signed contracts
     */
    String getCurrentMultySignAddress();

    /**
     * Generate heterogeneous chain addresses based on compressed public keys
     */
    String generateAddressByCompressedPublicKey(String compressedPublicKey);

    /**
     * According to the private key import address, only one address is allowed to be imported, and the newly imported address will overwrite the existing one
     */
    HeterogeneousAccount importAccountByPriKey(String priKey, String password) throws NulsException;

    /**
     * Verify if the account password is correct
     */
    boolean validateAccountPassword(String address, String password) throws NulsException;

    /**
     * Query account details
     */
    HeterogeneousAccount getAccount(String address);

    /**
     * Verify address format
     */
    boolean validateAddress(String address);

    default String genMultiSignAddress(int threshold, List<byte[]> pubECKeys, boolean mainnet) {
        return EMPTY_STRING;
    }
    /**
     * Query address balance
     *
     * @return Address balance
     */
    BigDecimal getBalance(String address);

    /**
     * Update multiple signed addresses
     *
     * @return Multiple signed addresses
     */
    void updateMultySignAddress(String multySignAddress) throws Exception;

    default void updateMultySignAddressProtocol16(String multySignAddress, byte version) throws Exception {
        updateMultySignAddress(multySignAddress);
    };

    /**
     * Confirm the transaction status of heterogeneous chains
     */
    void txConfirmedCompleted(String ethTxHash, Long blockHeight, String nerveTxHash, byte[] confirmTxRemark) throws Exception;
    default void txConfirmedCheck(String ethTxHash, Long blockHeight, String nerveTxHash, byte[] confirmTxRemark) throws Exception {

    }

    /**
     * RollBACK`Confirm the transaction status of heterogeneous chains`
     */
    void txConfirmedRollback(String txHash) throws Exception;

    /**
     * Query heterogeneous chain master asset information
     */
    HeterogeneousAssetInfo getMainAsset();

    /**
     * Query heterogeneous chain contract asset information based on contract address
     */
    HeterogeneousAssetInfo getAssetByContractAddress(String contractAddress);

    /**
     * Based on assetsIDQuery heterogeneous chain asset information
     */
    HeterogeneousAssetInfo getAssetByAssetId(int assetId);

    /**
     * Query heterogeneous chain contract asset information from heterogeneous chain networks and verify if asset data is correct
     */
    boolean validateHeterogeneousAssetInfoFromNet(String contractAddress, String symbol, int decimals) throws Exception;

    /**
     * Save heterogeneous chain contract assets
     */
    void saveHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception;
    /**
     * Rolling back heterogeneous chain contract assets
     */
    void rollbackHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception;

    /**
     * Obtain recharge transaction information
     *
     * @return Transaction information
     */
    HeterogeneousTransactionInfo getDepositTransaction(String txHash) throws Exception;

    /**
     * Obtain withdrawal transaction information
     *
     * @return Transaction information
     */
    HeterogeneousTransactionInfo getWithdrawTransaction(String txHash) throws Exception;

    /**
     * Obtain information for confirming transactions
     */
    HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception;

    /**
     * Obtain information on pending transactions for change administrators
     */
    HeterogeneousChangePendingInfo getChangeVirtualBankPendingInfo(String nerveTxHash) throws Exception;

    /**
     * Create or sign withdrawal transactions
     *
     * @return Heterogeneous Chain Tradinghash
     */
    String createOrSignWithdrawTx(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException;

    /**
     * Create or sign administrator change transactions
     *
     * @return Heterogeneous Chain Tradinghash
     */
    String createOrSignManagerChangesTx(String txHash, String[] addAddresses,
                                        String[] removeAddresses, int orginTxCount) throws NulsException;

    /**
     * Create or sign contract upgrade authorization transactions
     *
     * @return Heterogeneous Chain Tradinghash
     */
    String createOrSignUpgradeTx(String txHash) throws NulsException;

    /**
     * Force consistent recovery
     */
    String forceRecovery(String nerveTxHash, String[] seedManagers, String[] allManagers) throws NulsException;

    /**
     * Re analyze recharge transactions（The current node has missed the analysis of heterogeneous chain transactions）
     */
    Boolean reAnalysisDepositTx(String ethTxHash) throws Exception;

    /**
     * Re analyze transactions（The current node has missed the analysis of heterogeneous chain transactions）
     */
    Boolean reAnalysisTx(String ethTxHash) throws Exception;

    /**
     * Within heterogeneous chain networkschainId
     */
    long getHeterogeneousNetworkChainId();

    /**
     * Create or sign withdrawal transactions
     *
     * @return Heterogeneous Chain Tradinghash
     */
    default String createOrSignWithdrawTxII(String txHash, String toAddress, BigInteger value, Integer assetId, String signatureData) throws NulsException {
        return EMPTY_STRING;
    }

    /**
     * Verify administrator change transactions
     */
    default boolean validateManagerChangesTxII(String txHash, String[] addAddresses,
                                          String[] removeAddresses, int orginTxCount, String signatureData) throws Exception {
        return false;
    }

    /**
     * Create or sign administrator change transactions
     *
     * @return Heterogeneous Chain Tradinghash
     */
    default String createOrSignManagerChangesTxII(String txHash, String[] addAddresses,
                                          String[] removeAddresses, int orginTxCount, String signatureData) throws Exception {
        return EMPTY_STRING;
    }

    /**
     * Create or sign contract upgrade authorization transactions
     *
     * @return Heterogeneous Chain Tradinghash
     */
    default String createOrSignUpgradeTxII(String txHash, String upgradeContract, String signatureData) throws NulsException {
        return EMPTY_STRING;
    }

    /**
     * Signature withdrawal
     */
    default String signWithdrawII(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        return EMPTY_STRING;
    }

    /**
     * Signature administrator change
     */
    default String signManagerChangesII(String txHash, String[] addAddresses,
                                          String[] removeAddresses, int orginTxCount) throws Exception {
        return EMPTY_STRING;
    }

    /**
     * Signing contract upgrade authorization
     */
    default String signUpgradeII(String txHash, String upgradeContract) throws NulsException {
        return EMPTY_STRING;
    }

    /**
     * Verify signature withdrawal
     */
    default Boolean verifySignWithdrawII(String signAddress, String txHash, String toAddress, BigInteger value, Integer assetId, String signed) throws Exception {
        return false;
    }

    /**
     * Verify signature administrator changes
     */
    default Boolean verifySignManagerChangesII(String signAddress, String txHash, String[] addAddresses,
                                          String[] removeAddresses, int orginTxCount, String signed) throws Exception {
        return false;
    }

    /**
     * Verify signature contract upgrade authorization
     */
    default Boolean verifySignUpgradeII(String signAddress, String txHash, String upgradeContract, String signed) throws Exception {
        return false;
    }

    /**
     * The version of the current process processing interface
     */
    default int version() {
        return 0;
    }

    default boolean isEnoughNvtFeeOfWithdraw(BigDecimal nvtAmount, int hAssetId) {
        return false;
    }

    default boolean isEnoughFeeOfWithdrawByMainAssetProtocol15(AssetName assetName, BigDecimal amount, int hAssetId) {
        return false;
    }

    default boolean isMinterERC20(String erc20) throws Exception {
        return false;
    }

    default String cancelHtgTx(String nonce, String priceGWei) throws Exception {
        return EMPTY_STRING;
    }

    default String getAddressString(byte[] addressBytes) {
        return "0x" + HexUtil.encode(addressBytes);
    }

    default byte[] getAddressBytes(String addressString) {
        String cleanInput = ConverterUtil.cleanHexPrefix(addressString);
        int len = cleanInput.length();
        if (len == 0) {
            return new byte[0];
        }
        return HexUtil.decode(cleanInput);
    }

    default void initialSignatureVersion() {}

    default HeterogeneousOneClickCrossChainData parseOneClickCrossChainData(String extend) {
        return null;
    }
    default HeterogeneousAddFeeCrossChainData parseAddFeeCrossChainData(String extend) {
        return null;
    }

    default HeterogeneousChainGasInfo getHeterogeneousChainGasInfo() {
        return null;
    }

    default boolean isAvailableRPC() {
        return true;
    }
    default BigInteger currentGasPrice() {
        return null;
    }

    default HeterogeneousTransactionInfo getUnverifiedDepositTransaction(String txHash) throws Exception {
        return getDepositTransaction(txHash);
    }

    default IBitCoinApi getBitCoinApi() {
        return null;
    }

    void setRegister(IHeterogeneousChainRegister register);

    void closeChainPending();
    void closeChainConfirm();

    default BigInteger getCrossOutTxFee(AssetName assetName, boolean isToken) {
        return BigInteger.ZERO;
    }
}
