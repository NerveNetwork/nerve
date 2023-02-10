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
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousWithdrawTxInfo;
import network.nerve.converter.model.bo.WithdrawalTotalFeeInfo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * @author: Mimi
 * @date: 2020-05-08
 */
public interface IConverterCoreApi {
    /**
     * 获取Nerve网络当前区块高度
     */
    long getCurrentBlockHeightOnNerve();

    /**
     * 当前节点是否为虚拟银行
     */
    boolean isVirtualBankByCurrentNode();

    /**
     * 当前节点是否为种子虚拟银行
     */
    boolean isSeedVirtualBankByCurrentNode();

    /**
     * 获取当前节点加入虚拟银行时的顺序
     */
    int getVirtualBankOrder();

    /**
     * 获取当前虚拟银行成员的数量
     */
    int getVirtualBankSize();

    /**
     * 获取Nerve交易
     */
    Transaction getNerveTx(String hash);

    /**
     * 当前节点是否在运行状态
     */
    boolean isRunning();

    /**
     * 获取异构链提现数据
     */
    HeterogeneousWithdrawTxInfo getWithdrawTxInfo(String nerveTxHash) throws NulsException;

    /**
     * 获取指定异构链的虚拟银行列表
     */
    Map<String, Integer>  currentVirtualBanks(int hChainId);

    /**
     * 重新获取拜占庭签名
     */
    List<HeterogeneousSign> regainSignatures(int nerveChainId, String nerveTxHash, int hChainId);

    /**
     * 异构链资产是否为绑定已存在的NERVE资产
     */
    boolean isBoundHeterogeneousAsset(int hChainId, int hAssetId);

    /**
     * 提现交易异构链是否确认(通过获取提现确认交易业务数据来判断)
     * @param nerveChainId
     * @param hash
     * @return
     */
    boolean isWithdrawalComfired(String nerveTxHash);

    /**
     * 获取提现交易提供的NVT手续费，包含用户追加的手续费
     */
    WithdrawalTotalFeeInfo getFeeOfWithdrawTransaction(String nerveTxHash) throws NulsException;

    /**
     * 获取指定资产的USDT价格
     */
    BigDecimal getUsdtPriceByAsset(AssetName assetName);

    /**
     * 是否支持新的提现手续费机制
     */
    boolean isSupportNewMechanismOfWithdrawalFee();
    /**
     * 是否支持转账即销毁部分的ERC20
     */
    boolean isSupportProtocol12ERC20OfTransferBurn();

    /**
     * 验证是否为nerve地址
     */
    boolean validNerveAddress(String address);

    /**
     * 是否支持充值ERC20的新验证方式I
     */
    boolean isSupportProtocol13NewValidationOfERC20();

    /**
     * 是否支持协议14 v1.14.0
     */
    boolean isProtocol14();

    /**
     * 是否支持波场跨链 v1.15.0
     */
    boolean isSupportProtocol15TrxCrossChain();

    /**
     * 是否支持协议16 v1.16.0
     */
    boolean isProtocol16();

    /**
     * 是否支持协议21 v1.21.0
     */
    boolean isProtocol21();

    /**
     * 是否支持协议22 v1.22.0
     */
    boolean isProtocol22();

    /**
     * 是否支持协议23 v1.23.0
     */
    boolean isProtocol23();


    /**
     * 添加任务
     */
    void addHtgConfirmTxHandler(Runnable runnable);
    void addHtgRpcAvailableHandler(Runnable runnable);
    void addHtgWaitingTxInvokeDataHandler(Runnable runnable);

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
}
