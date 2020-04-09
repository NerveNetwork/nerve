/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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
package nerve.network.converter.core.heterogeneous.docking.interfaces;

import nerve.network.converter.model.bo.HeterogeneousAccount;
import nerve.network.converter.model.bo.HeterogeneousAssetInfo;
import nerve.network.converter.model.bo.HeterogeneousConfirmedInfo;
import nerve.network.converter.model.bo.HeterogeneousTransactionInfo;
import io.nuls.core.exception.NulsException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * @author: Chino
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
     * 确认异构链的交易状态
     */
    void txConfirmedCompleted(String txHash, Long blockHeight) throws Exception;

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
     * 获取当前异构链下所有的初始资产
     */
    List<HeterogeneousAssetInfo> getAllInitializedAssets();

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
     * 获取变更管理员确认交易的信息
     */
    HeterogeneousConfirmedInfo getChangeVirtualBankConfirmedTxInfo(String txHash) throws Exception;
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
                                        String[] removeAddresses, String[] currentAddresses) throws NulsException;
}
