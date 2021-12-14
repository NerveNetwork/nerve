/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.utils.ConverterUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static io.protostuff.ByteString.EMPTY_STRING;

/**
 * @author: Mimi
 * @date: 2020-02-17
 */
public interface IHeterogeneousChainDocking {

    /**
     * 当前链是否支持合约资产
     */
    boolean isSupportContractAssetByCurrentChain();
    /**
     * 当前异构链ID
     */
    Integer getChainId();
    /**
     * 当前异构链Symbol
     */
    String getChainSymbol();
    /**
     * 当前签名地址
     */
    String getCurrentSignAddress();
    /**
     * 当前多签合约地址
     */
    String getCurrentMultySignAddress();

    /**
     * 根据压缩公钥生成异构链地址
     */
    String generateAddressByCompressedPublicKey(String compressedPublicKey);

    /**
     * 根据私钥导入地址，只允许导入一个地址，新导入的将覆盖已有的
     */
    HeterogeneousAccount importAccountByPriKey(String priKey, String password) throws NulsException;

    /**
     * 根据keystore导入地址，只允许导入一个地址，新导入的将覆盖已有的
     */
    HeterogeneousAccount importAccountByKeystore(String keystore, String password) throws NulsException;

    /**
     * 导出账户的keystore
     */
    String exportAccountKeystore(String address, String password) throws NulsException;

    /**
     * 验证账户密码是否正确
     */
    boolean validateAccountPassword(String address, String password) throws NulsException;

    /**
     * 查询账户详情
     */
    HeterogeneousAccount getAccount(String address);

    /**
     * 查询账户列表
     */
    List<HeterogeneousAccount> getAccountList();

    /**
     * 移除账户
     */
    void removeAccount(String address) throws Exception;

    /**
     * 验证地址格式
     */
    boolean validateAddress(String address);

    /**
     * 查询地址余额
     *
     * @return 地址余额
     */
    BigDecimal getBalance(String address);

    /**
     * 创建多签地址
     *
     * @return 多签地址
     */
    String createMultySignAddress(String[] pubKeys, int minSigns);

    /**
     * 更新多签地址
     *
     * @return 多签地址
     */
    void updateMultySignAddress(String multySignAddress) throws Exception;

    default void updateMultySignAddressProtocol16(String multySignAddress, byte version) throws Exception {
        updateMultySignAddress(multySignAddress);
    };

    /**
     * 确认异构链的交易状态
     */
    void txConfirmedCompleted(String ethTxHash, Long blockHeight, String nerveTxHash) throws Exception;

    /**
     * 回滚`确认异构链的交易状态`
     */
    void txConfirmedRollback(String txHash) throws Exception;

    /**
     * 查询异构链主资产信息
     */
    HeterogeneousAssetInfo getMainAsset();

    /**
     * 根据合约地址查询异构链合约资产信息
     */
    HeterogeneousAssetInfo getAssetByContractAddress(String contractAddress);

    /**
     * 根据资产ID查询异构链资产信息
     */
    HeterogeneousAssetInfo getAssetByAssetId(int assetId);

    /**
     * 从异构链网络上查询异构链合约资产信息，验证资产数据是否正确
     */
    boolean validateHeterogeneousAssetInfoFromNet(String contractAddress, String symbol, int decimals) throws Exception;

    /**
     * 保存异构链合约资产
     */
    void saveHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception;
    /**
     * 回滚异构链合约资产
     */
    void rollbackHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception;

    /**
     * 获取充值交易信息
     *
     * @return 交易信息
     */
    HeterogeneousTransactionInfo getDepositTransaction(String txHash) throws Exception;

    /**
     * 获取提现交易信息
     *
     * @return 交易信息
     */
    HeterogeneousTransactionInfo getWithdrawTransaction(String txHash) throws Exception;

    /**
     * 获取确认交易的信息
     */
    HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception;

    /**
     * 获取变更管理员待处理交易的信息
     */
    HeterogeneousChangePendingInfo getChangeVirtualBankPendingInfo(String nerveTxHash) throws Exception;

    /**
     * 创建或签名提现交易
     *
     * @return 异构链交易hash
     */
    String createOrSignWithdrawTx(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException;

    /**
     * 创建或签名管理员变更交易
     *
     * @return 异构链交易hash
     */
    String createOrSignManagerChangesTx(String txHash, String[] addAddresses,
                                        String[] removeAddresses, int orginTxCount) throws NulsException;

    /**
     * 创建或签名合约升级授权交易
     *
     * @return 异构链交易hash
     */
    String createOrSignUpgradeTx(String txHash) throws NulsException;

    /**
     * 强制恢复一致
     */
    String forceRecovery(String nerveTxHash, String[] seedManagers, String[] allManagers) throws NulsException;

    /**
     * 重新解析充值交易（当前节点遗漏了异构链交易解析）
     */
    Boolean reAnalysisDepositTx(String ethTxHash) throws Exception;

    /**
     * 异构链网络内部chainId
     */
    long getHeterogeneousNetworkChainId();

    /**
     * 创建或签名提现交易
     *
     * @return 异构链交易hash
     */
    default String createOrSignWithdrawTxII(String txHash, String toAddress, BigInteger value, Integer assetId, String signatureData) throws NulsException {
        return EMPTY_STRING;
    }

    /**
     * 验证管理员变更交易
     */
    default boolean validateManagerChangesTxII(String txHash, String[] addAddresses,
                                          String[] removeAddresses, int orginTxCount, String signatureData) throws NulsException {
        return false;
    }

    /**
     * 创建或签名管理员变更交易
     *
     * @return 异构链交易hash
     */
    default String createOrSignManagerChangesTxII(String txHash, String[] addAddresses,
                                          String[] removeAddresses, int orginTxCount, String signatureData) throws NulsException {
        return EMPTY_STRING;
    }

    /**
     * 创建或签名合约升级授权交易
     *
     * @return 异构链交易hash
     */
    default String createOrSignUpgradeTxII(String txHash, String upgradeContract, String signatureData) throws NulsException {
        return EMPTY_STRING;
    }

    /**
     * 签名提现
     */
    default String signWithdrawII(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        return EMPTY_STRING;
    }

    /**
     * 签名管理员变更
     */
    default String signManagerChangesII(String txHash, String[] addAddresses,
                                          String[] removeAddresses, int orginTxCount) throws NulsException {
        return EMPTY_STRING;
    }

    /**
     * 签名合约升级授权
     */
    default String signUpgradeII(String txHash, String upgradeContract) throws NulsException {
        return EMPTY_STRING;
    }

    /**
     * 验证签名提现
     */
    default Boolean verifySignWithdrawII(String signAddress, String txHash, String toAddress, BigInteger value, Integer assetId, String signed) throws NulsException {
        return false;
    }

    /**
     * 验证签名管理员变更
     */
    default Boolean verifySignManagerChangesII(String signAddress, String txHash, String[] addAddresses,
                                          String[] removeAddresses, int orginTxCount, String signed) throws NulsException {
        return false;
    }

    /**
     * 验证签名合约升级授权
     */
    default Boolean verifySignUpgradeII(String signAddress, String txHash, String upgradeContract, String signed) throws NulsException {
        return false;
    }

    /**
     * 当前流程处理接口的版本
     */
    default int version() {
        return 0;
    }

    default boolean isEnoughFeeOfWithdraw(BigDecimal nvtAmount, int hAssetId) {
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
}
