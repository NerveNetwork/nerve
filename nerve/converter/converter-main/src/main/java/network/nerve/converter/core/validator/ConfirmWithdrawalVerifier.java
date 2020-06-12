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

package network.nerve.converter.core.validator;

import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.txdata.ConfirmWithdrawalTxData;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.HeterogeneousAssetConverterStorageService;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;

import java.math.BigInteger;
import java.util.List;

/**
 * 确认提现交易业务验证器
 * (创建交易后)
 *
 * @author: Loki
 * @date: 2020/4/15
 */
@Component
public class ConfirmWithdrawalVerifier {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private VirtualBankService virtualBankService;
    @Autowired
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService;

    public void validate(Chain chain, Transaction tx) throws NulsException {
        byte[] coinData = tx.getCoinData();
        if (coinData != null && coinData.length > 0) {
            // coindata存在数据(coinData应该没有数据)
            throw new NulsException(ConverterErrorCode.COINDATA_CANNOT_EXIST);
        }
        //区块内业务重复交易检查
        ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmWithdrawalTxData.class);
        // 判断该提现交易一否已经有对应的确认提现交易
        ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, txData.getWithdrawalTxHash());
        if (null != po) {
            // 说明该提现交易 已经发出过确认提现交易,本次交易为重复的确认提现交易
            // Nerve提现交易不存在
            throw new NulsException(ConverterErrorCode.CFM_IS_DUPLICATION);
        }
        //获取提现交易
        Transaction withdrawalTx = TransactionCall.getConfirmedTx(chain, txData.getWithdrawalTxHash());
        if (null == withdrawalTx) {
            // Nerve提现交易不存在
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        CoinData withdrawalCoinData = ConverterUtil.getInstance(withdrawalTx.getCoinData(), CoinData.class);
        CoinTo withdrawalTo = null;
        for (CoinTo coinTo : withdrawalCoinData.getTo()) {
            // 链内手续费 提现资产
            if (coinTo.getAssetsId()!= chain.getConfig().getAssetId()) {
                withdrawalTo = coinTo;
            }
        }
        // 根据提现交易 资产信息获取异构链交易信息(转换获取) 再验证
        HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(withdrawalTo.getAssetsId());
        if(null == heterogeneousAssetInfo){
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
        }
        // 获取提案中的异构链充值交易
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                heterogeneousAssetInfo.getChainId(),
                txData.getHeterogeneousTxHash(),
                HeterogeneousTxTypeEnum.WITHDRAWAL,
                this.heterogeneousDockingManager);
        if (null == info) {
            // 异构链提现交易不存在
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }

        // 验证 异构链签名列表
        List<HeterogeneousAddress> listDistributionFee = txData.getListDistributionFee();
        if (null == listDistributionFee || listDistributionFee.isEmpty()) {
            // 异构链签名列表是空的
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_SIGN_ADDRESS_LIST_EMPTY);
        }
        List<HeterogeneousAddress> listSigners = info.getSigners();
        if (!HeterogeneousUtil.listHeterogeneousAddressEquals(listDistributionFee, listSigners)) {
            // 异构链签名列表不匹配
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_SIGNER_LIST_MISMATCH);
        }

        WithdrawalTxData withdrawalTxData = ConverterUtil.getInstance(withdrawalTx.getTxData(), WithdrawalTxData.class);
        if (!withdrawalTxData.getHeterogeneousAddress().toLowerCase().equals(info.getTo().toLowerCase())) {
            // 提现交易确认交易中到账地址与异构确认交易到账地址数据不匹配
            throw new NulsException(ConverterErrorCode.CFM_WITHDRAWAL_ARRIVE_ADDRESS_MISMATCH);
        }

        if (txData.getHeterogeneousHeight() != info.getBlockHeight()) {
            // 提现交易确认交易中height与异构确认交易height数据不匹配
            throw new NulsException(ConverterErrorCode.CFM_WITHDRAWAL_HEIGHT_MISMATCH);
        }
        /**
         * 提现金额匹配涉及手续费问题
         * 目前异构链获取到信息中交易金额是包含手续费的, 所以是对比包含手续费的金额
         */
        BigInteger fee = new BigInteger("0");
        BigInteger arrivedAmount = withdrawalTo.getAmount().subtract(fee);
        if (arrivedAmount.compareTo(info.getValue()) != 0) {
            // 提现交易确认交易中金额与异构确认交易金额数据不匹配
            throw new NulsException(ConverterErrorCode.CFM_WITHDRAWAL_AMOUNT_MISMATCH);
        }
    }

}
