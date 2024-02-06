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
package network.nerve.converter.heterogeneouschain.btc.docking;

import com.neemre.btcdcli4j.core.domain.RawTransaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.FormatValidUtils;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.heterogeneouschain.btc.context.BtcContext;
import network.nerve.converter.heterogeneouschain.btc.core.BtcWalletApi;
import network.nerve.converter.heterogeneouschain.btc.helper.BtcAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.btc.helper.BtcParseTxHelper;
import network.nerve.converter.heterogeneouschain.btc.utils.BtcUtil;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgCommonHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgInvokeTxHelper;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgMultiSignAddressHistoryStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.rpc.call.SwapCall;
import org.bitcoinj.core.Base58;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.btc.utils.BtcUtil.getBtcLegacyAddress;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class BtcDocking implements IHeterogeneousChainDocking, BeanInitial {

    private HeterogeneousAssetInfo mainAsset;
    private BtcContext context;
    private BtcWalletApi walletApi;
    protected ConverterConfig converterConfig;
    protected HtgListener listener;
    protected HtgMultiSignAddressHistoryStorageService htgMultiSignAddressHistoryStorageService;
    protected HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    protected HtgInvokeTxHelper htgInvokeTxHelper;
    protected BtcParseTxHelper parseIxHelper;
    protected ReentrantLock reAnalysisLock = new ReentrantLock();
    protected HtgCommonHelper htgCommonHelper;
    protected BtcAnalysisTxHelper htgAnalysisTxHelper;
    private HeterogeneousChainGasInfo gasInfo;
    private HtgAccount account;

    private NulsLogger logger() {
        return context.logger();
    }

    @Override
    public boolean isSupportContractAssetByCurrentChain() {
        return false;
    }

    @Override
    public Integer getChainId() {
        return context.getConfig().getChainId();
    }

    @Override
    public String getChainSymbol() {
        return context.getConfig().getSymbol();
    }

    @Override
    public String getCurrentSignAddress() {
        return context.ADMIN_ADDRESS();
    }

    @Override
    public String getCurrentMultySignAddress() {
        return context.MULTY_SIGN_ADDRESS();
    }

    @Override
    public String generateAddressByCompressedPublicKey(String compressedPublicKey) {
        return getBtcLegacyAddress(HexUtil.decode(compressedPublicKey), context.getConverterCoreApi().isNerveMainnet());
    }

    @Override
    public HeterogeneousAccount importAccountByPriKey(String key, String password) throws NulsException {
        if (context.getConverterCoreApi().isLocalSign()) {
            return _importAccountByPriKey(key, password);
        } else {
            return _importAccountByPubKey(key, password);
        }
    }

    private HeterogeneousAccount _importAccountByPriKey(String priKey, String password) throws NulsException {
        if (!FormatValidUtils.validPassword(password)) {
            logger().error("password format wrong");
            throw new NulsException(ConverterErrorCode.PASSWORD_FORMAT_WRONG);
        }
        HtgAccount account = BtcUtil.createAccount(priKey, context.getConverterCoreApi().isNerveMainnet());
        account.encrypt(password);
        try {
            // Overwrite this address as the virtual bank administrator address
            context.SET_ADMIN_ADDRESS(account.getAddress());
            context.SET_ADMIN_ADDRESS_PUBLIC_KEY(account.getCompressedPublicKey());
            context.SET_ADMIN_ADDRESS_PASSWORD(password);
            this.account = account;
            logger().info("towards{}Heterogeneous component import node block address information, address: [{}]", context.getConfig().getSymbol(), account.getAddress());
            return account;
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR, e);
        }
    }

    private HeterogeneousAccount _importAccountByPubKey(String pubKey, String password) throws NulsException {
        if (!FormatValidUtils.validPassword(password)) {
            logger().error("password format wrong");
            throw new NulsException(ConverterErrorCode.PASSWORD_FORMAT_WRONG);
        }
        HtgAccount account = BtcUtil.createAccountByPubkey(pubKey, context.getConverterCoreApi().isNerveMainnet());
        try {
            // Overwrite this address as the virtual bank administrator address
            context.SET_ADMIN_ADDRESS(account.getAddress());
            context.SET_ADMIN_ADDRESS_PUBLIC_KEY(account.getCompressedPublicKey());
            context.SET_ADMIN_ADDRESS_PASSWORD(password);
            this.account = account;
            logger().info("towards{}Heterogeneous component import node block address information, address: [{}]", context.getConfig().getSymbol(), account.getAddress());
            return account;
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR, e);
        }
    }

    @Override
    public boolean validateAccountPassword(String address, String password) throws NulsException {
        return true;
    }

    @Override
    public HeterogeneousAccount getAccount(String address) {
        HtgAccount account = this.account;
        if (!account.getAddress().equals(address)) {
            return null;
        }
        return account;
    }

    @Override
    public boolean validateAddress(String address) {
        return SwapCall.isLegalBTCAddr(context.getConverterCoreApi().isNerveMainnet(), address);
    }

    @Override
    public BigDecimal getBalance(String address) {
        BigDecimal balance = walletApi.getBalance(address);
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return balance;
        }
        return balance.movePointLeft(context.ASSET_NAME().decimals());
    }

    @Override
    public void updateMultySignAddress(String multySignAddress) throws Exception {
        logger().info("{}Update multiple contract addresses, old: {}, new: {}", context.getConfig().getSymbol(), context.MULTY_SIGN_ADDRESS(), multySignAddress);
        multySignAddress = multySignAddress.toLowerCase();
        // Listening for multi signature address transactions
        listener.removeListeningAddress(context.MULTY_SIGN_ADDRESS());
        listener.addListeningAddress(multySignAddress);
        // Update multiple signed addresses
        context.SET_MULTY_SIGN_ADDRESS(multySignAddress);
        // Save the current multi signature address to the multi signature address history list
        htgMultiSignAddressHistoryStorageService.save(multySignAddress);
    }

    @Override
    public void updateMultySignAddressProtocol16(String multySignAddress, byte version) throws Exception {
        updateMultySignAddress(multySignAddress);
        context.SET_VERSION(version);
        htgMultiSignAddressHistoryStorageService.saveVersion(version);
        logger().info("Update signature version number: {}", version);
    }

    @Override
    public void txConfirmedCompleted(String htgTxHash, Long blockHeight, String nerveTxHash) throws Exception {
        logger().info("NerveNetwork confirmation{}transaction Nerver hash: {}", context.getConfig().getSymbol(), nerveTxHash);
        if (StringUtils.isBlank(htgTxHash)) {
            logger().warn("Empty htgTxHash warning");
            return;
        }
        // updatedbinpoChange the status of todeleteConfirm in the queue task`ROLLBACK_NUMER`Remove after each block to facilitate state rollback
        HtgUnconfirmedTxPo txPo = htgUnconfirmedTxStorageService.findByTxHash(htgTxHash);
        if (txPo == null) {
            txPo = new HtgUnconfirmedTxPo();
            txPo.setTxHash(htgTxHash);
        }
        txPo.setDelete(true);
        txPo.setDeletedHeight(blockHeight + HtgConstant.ROLLBACK_NUMER);
        logger().info("NerveNetwork impact[{}]{}transaction[{}]Confirm completion, nerveheight: {}, nerver hash: {}", txPo.getTxType(), context.getConfig().getSymbol(), txPo.getTxHash(), blockHeight, txPo.getNerveTxHash());
        boolean delete = txPo.isDelete();
        Long deletedHeight = txPo.getDeletedHeight();
        htgUnconfirmedTxStorageService.update(txPo, update -> {
            update.setDelete(delete);
            update.setDeletedHeight(deletedHeight);
        });
        // Persisted state successfullynerveTx
        if (StringUtils.isNotBlank(nerveTxHash)) {
            logger().debug("Persisted state successfullynerveTxHash: {}", nerveTxHash);
            htgInvokeTxHelper.saveSuccessfulNerve(nerveTxHash);
        }
    }

    @Override
    public void txConfirmedRollback(String txHash) throws Exception {
        HtgUnconfirmedTxPo txPo = htgUnconfirmedTxStorageService.findByTxHash(txHash);
        if (txPo != null) {
            txPo.setDelete(false);
            txPo.setDeletedHeight(null);
            htgUnconfirmedTxStorageService.update(txPo, update -> {
                update.setDelete(txPo.isDelete());
                update.setDeletedHeight(txPo.getDeletedHeight());
            });
        }

    }

    private HeterogeneousAssetInfo mainAsset() {
        if (mainAsset == null) {
            mainAsset = new HeterogeneousAssetInfo();
            mainAsset.setChainId(context.getConfig().getChainId());
            mainAsset.setAssetId(context.HTG_ASSET_ID());
            mainAsset.setDecimals((byte) context.getConfig().getDecimals());
            mainAsset.setSymbol(context.getConfig().getSymbol());
            mainAsset.setContractAddress(EMPTY_STRING);
        }
        return mainAsset;
    }

    @Override
    public HeterogeneousAssetInfo getMainAsset() {
        return mainAsset();
    }

    @Override
    public HeterogeneousAssetInfo getAssetByContractAddress(String contractAddress) {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public HeterogeneousAssetInfo getAssetByAssetId(int assetId) {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public boolean validateHeterogeneousAssetInfoFromNet(String contractAddress, String symbol, int decimals) throws Exception {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public void saveHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public void rollbackHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public HeterogeneousTransactionInfo getDepositTransaction(String txHash) throws Exception {
        return parseIxHelper.parseDepositTransaction(txHash, true);
    }

    @Override
    public HeterogeneousTransactionInfo getUnverifiedDepositTransaction(String txHash) throws Exception {
        return parseIxHelper.parseDepositTransaction(txHash, false);
    }

    @Override
    public HeterogeneousTransactionInfo getWithdrawTransaction(String txHash) throws Exception {
        //TODO pierre auto-generated method stub
        return null;
    }

    @Override
    public HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception {
        //TODO pierre Withdraw tx
        return null;
    }

    @Override
    public HeterogeneousChangePendingInfo getChangeVirtualBankPendingInfo(String nerveTxHash) throws Exception {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public String createOrSignWithdrawTx(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public String createOrSignManagerChangesTx(String txHash, String[] addAddresses, String[] removeAddresses, int orginTxCount) throws NulsException {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public String createOrSignUpgradeTx(String txHash) throws NulsException {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public String forceRecovery(String nerveTxHash, String[] seedManagers, String[] allManagers) throws NulsException {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public Boolean reAnalysisDepositTx(String htgTxHash) throws Exception {
        if (htgCommonHelper.constainHash(htgTxHash)) {
            logger().info("Repeated collection of recharge transactionshash: {}No more repeated parsing[0]", htgTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (htgCommonHelper.constainHash(htgTxHash)) {
                logger().info("Repeated collection of recharge transactionshash: {}No more repeated parsing[1]", htgTxHash);
                return true;
            }
            logger().info("Re analyze recharge transactions: {}", htgTxHash);
            RawTransaction tx = walletApi.getTransactionByHash(htgTxHash);
            Long txTime = tx.getBlockTime();
            Long height = Long.valueOf(walletApi.getBlockHeaderByHash(tx.getBlockHash()).getHeight());
            htgAnalysisTxHelper.analysisTx(tx, txTime, height, tx.getBlockHash());
            htgCommonHelper.addHash(htgTxHash);
            return true;
        } catch (Exception e) {
            throw e;
        } finally {
            reAnalysisLock.unlock();
        }
    }

    @Override
    public Boolean reAnalysisTx(String htgTxHash) throws Exception {
        if (htgCommonHelper.constainHash(htgTxHash)) {
            logger().info("Repeated collection of transactionshash: {}No more repeated parsing[0]", htgTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (htgCommonHelper.constainHash(htgTxHash)) {
                logger().info("Repeated collection of transactionshash: {}No more repeated parsing[1]", htgTxHash);
                return true;
            }
            logger().info("Re analyze transactions: {}", htgTxHash);
            RawTransaction txInfo = walletApi.getTransactionByHash(htgTxHash);
            Long txTime = txInfo.getBlockTime();
            htgUnconfirmedTxStorageService.deleteByTxHash(htgTxHash);
            Long height = Long.valueOf(walletApi.getBlockHeaderByHash(txInfo.getBlockHash()).getHeight());
            htgAnalysisTxHelper.analysisTx(txInfo, txTime, height, txInfo.getBlockHash());
            htgCommonHelper.addHash(htgTxHash);
            return true;
        } catch (Exception e) {
            throw e;
        } finally {
            reAnalysisLock.unlock();
        }
    }

    @Override
    public long getHeterogeneousNetworkChainId() {
        return context.getConfig().getChainIdOnHtgNetwork();
    }

    @Override
    public String createOrSignWithdrawTxII(String txHash, String toAddress, BigInteger value, Integer assetId, String signatureData) throws NulsException {
        //TODO pierre WithdrawTx
        return null;
    }

    @Override
    public boolean validateManagerChangesTxII(String txHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData) throws NulsException {
        //TODO pierre validate ChangesTx
        return false;
    }

    @Override
    public String createOrSignManagerChangesTxII(String txHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData) throws NulsException {
        //TODO pierre ChangesTx
        return null;
    }

    @Override
    public String createOrSignUpgradeTxII(String txHash, String upgradeContract, String signatureData) throws NulsException {
        return EMPTY_STRING;
    }

    @Override
    public String signWithdrawII(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        //TODO pierre auto-generated method stub
        return null;
    }

    @Override
    public String signManagerChangesII(String txHash, String[] addAddresses, String[] removeAddresses, int orginTxCount) throws NulsException {
        //TODO pierre auto-generated method stub
        return null;
    }

    @Override
    public String signUpgradeII(String txHash, String upgradeContract) throws NulsException {
        return EMPTY_STRING;
    }

    @Override
    public Boolean verifySignWithdrawII(String signAddress, String txHash, String toAddress, BigInteger value, Integer assetId, String signed) throws NulsException {
        //TODO pierre auto-generated method stub
        return false;
    }

    @Override
    public Boolean verifySignManagerChangesII(String signAddress, String txHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signed) throws NulsException {
        //TODO pierre auto-generated method stub
        return false;
    }

    @Override
    public Boolean verifySignUpgradeII(String signAddress, String txHash, String upgradeContract, String signed) throws NulsException {
        return true;
    }

    @Override
    public int version() {
        return context.VERSION();
    }

    @Override
    public boolean isEnoughNvtFeeOfWithdraw(BigDecimal nvtAmount, int hAssetId) {
        //TODO pierre auto-generated method stub
        return false;
    }

    @Override
    public boolean isEnoughFeeOfWithdrawByMainAssetProtocol15(AssetName assetName, BigDecimal amount, int hAssetId) {
        //TODO pierre auto-generated method stub
        return false;
    }

    @Override
    public boolean isMinterERC20(String erc20) throws Exception {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public String cancelHtgTx(String nonce, String priceGWei) throws Exception {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public String getAddressString(byte[] addressBytes) {
        int version = context.getConverterCoreApi().isNerveMainnet() ? 0 : 111;
        return Base58.encodeChecked(version, addressBytes);
    }

    @Override
    public byte[] getAddressBytes(String addressString) {
        byte[] addrBytes = Base58.decode(addressString);
        byte[] hash160Bytes = new byte[20];
        System.arraycopy(addrBytes, 1, hash160Bytes, 0, 20);
        return hash160Bytes;
    }

    @Override
    public void initialSignatureVersion() {
        byte version = htgMultiSignAddressHistoryStorageService.getVersion();
        if (converterConfig.isNewProcessorMode()) {
            context.SET_VERSION(HtgConstant.VERSION_MULTY_SIGN_LATEST);
        }
        if (version > 0) {
            context.SET_VERSION(version);
        }
        logger().info("[{}]Current signature version number on the network: {}", context.getConfig().getSymbol(), context.VERSION());
    }

    @Override
    public HeterogeneousOneClickCrossChainData parseOneClickCrossChainData(String extend) {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public HeterogeneousAddFeeCrossChainData parseAddFeeCrossChainData(String extend) {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public HeterogeneousChainGasInfo getHeterogeneousChainGasInfo() {
        if (gasInfo == null) {
            gasInfo = new HeterogeneousChainGasInfo();
            gasInfo.setGasLimitOfWithdraw(context.GAS_LIMIT_OF_WITHDRAW().toString());
        }
        return gasInfo;
    }

    @Override
    public boolean isAvailableRPC() {
        return context.isAvailableRPC();
    }

    @Override
    public BigInteger currentGasPrice() {
        return context.getEthGasPrice();
    }
}
