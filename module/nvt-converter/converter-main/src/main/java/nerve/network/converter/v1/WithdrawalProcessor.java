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

package nerve.network.converter.v1;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.base.protocol.TransactionProcessor;
import nerve.network.converter.config.ConverterContext;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.business.HeterogeneousService;
import nerve.network.converter.core.business.VirtualBankService;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.dto.SignAccountDTO;
import nerve.network.converter.model.po.TxSubsequentProcessPO;
import nerve.network.converter.model.txdata.WithdrawalTxData;
import nerve.network.converter.storage.TxSubsequentProcessStorageService;
import nerve.network.converter.utils.ConverterUtil;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.StringUtils;

import java.math.BigInteger;
import java.util.*;

/**
 * @author: Chino
 * @date: 2020-02-28
 */
@Component("WithdrawalV1")
public class WithdrawalProcessor implements TransactionProcessor {

    private final int COIN_SIZE_1 = 1;
    private final int COIN_SIZE_2 = 2;

    @Override
    public int getType() {
        return TxType.WITHDRAWAL;
    }

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private HeterogeneousService heterogeneousService;
    @Autowired
    private VirtualBankService virtualBankService;

    /**
     * 提现异构链与资产是否有效
     * 账户是否有足够提现金额（账本验证）
     * 验证签名
     *
     * @param chainId     链Id
     * @param txs         类型为{@link #getType()}的所有交易集合
     * @param txMap       不同交易类型与其对应交易列表键值对
     * @param blockHeader 区块头
     * @return
     */
    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = null;
        Map<String, Object> result = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger log = chain.getLogger();
            String errorCode = null;
            result = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();
            outer:
            for (Transaction tx : txs) {
                WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
                if (StringUtils.isBlank(txData.getHeterogeneousAddress())) {
                    failsList.add(tx);
                    // 异构链地址不能为空
                    errorCode = ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL.getMsg());
                    continue;
                }
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);

                //from中非本链资产主资产coin（提现资产）只能有一个
                if (coinData.getFrom().size() != COIN_SIZE_2) {
                    failsList.add(tx);
                    // 提现coin from or to size error组装错误
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error(ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }
                boolean hasCurrentAsset = false;
                String withdrawalFromInfo = null;

//                BigInteger feeFrom = BigInteger.ZERO;
                BigInteger feeTo = BigInteger.ZERO;

                // 是否组装补贴手续费
                boolean assembleCurrentAssetFee = false;
                for (CoinFrom coinFrom : coinData.getFrom()) {
                    // 判断是手续费
                    if (coinFrom.getAssetsChainId() == chain.getConfig().getChainId()
                            && coinFrom.getAssetsId() == chain.getConfig().getAssetId()) {
                        hasCurrentAsset = true;
//                        feeFrom = coinFrom.getAmount();
                    }
                    if (coinFrom.getAssetsChainId() != chainId) {
                        // 临时记录提现资产信息,稍后比对
                        assembleCurrentAssetFee = heterogeneousService.isAssembleCurrentAssetFee(coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
                        withdrawalFromInfo = coinFrom.getAssetsChainId() + "-" + coinFrom.getAssetsId() + "-" + coinFrom.getAmount().toString();
                    }
                }
                if (!hasCurrentAsset) {
                    failsList.add(tx);
                    // 手续费不存在
                    errorCode = ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getCode();
                    log.error(ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getMsg());
                    continue;
                }
                // 应该组装而没有组装补贴手续费, 不通过
                boolean noPassedAssemble = assembleCurrentAssetFee && coinData.getTo().size() != COIN_SIZE_2;
                // 不应该组装 但to个数不为1, 不通过
                boolean noPassedNotAssemble = !assembleCurrentAssetFee && coinData.getTo().size() != COIN_SIZE_1;
                if (noPassedAssemble || noPassedNotAssemble) {
                    failsList.add(tx);
                    // 提现coin from or to size error组装错误
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error(ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                String withdrawalToInfo = null;
                for (CoinTo coinTo : coinData.getTo()) {
                    // 手续费
                    if (coinTo.getAssetsChainId() == chain.getConfig().getChainId()
                            && coinTo.getAssetsId() == chain.getConfig().getAssetId()) {
                        feeTo = coinTo.getAmount();
                        // 验证to补贴手续费地址是补贴手续费地址公钥生成的地址
                        byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                        if (!Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                            failsList.add(tx);
                            // 提现cointo补贴手续费地址错误
                            errorCode = ConverterErrorCode.WITHDRAWAL_SUBSIDY_FEE_ADDRESS_ERROR.getCode();
                            log.error(ConverterErrorCode.WITHDRAWAL_SUBSIDY_FEE_ADDRESS_ERROR.getMsg());
                            continue outer;
                        }
                    }
                    //提现资产
                    if (coinTo.getAssetsChainId() != chainId) {
                        withdrawalToInfo = coinTo.getAssetsChainId() + "-" + coinTo.getAssetsId() + "-" + coinTo.getAmount().toString();
                        // 验证to地址是提现黑洞公钥生成的地址
                        byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
                        if (!Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                            failsList.add(tx);
                            // 提现cointo地址错误
                            errorCode = ConverterErrorCode.WITHDRAWAL_ARRIVE_ADDRESS_ERROR.getCode();
                            log.error(ConverterErrorCode.WITHDRAWAL_ARRIVE_ADDRESS_ERROR.getMsg());
                            continue;
                        }
                    }
                }
                if (assembleCurrentAssetFee && BigIntegerUtils.isLessThan(feeTo, ConverterContext.WITHDRAWAL_DISTRIBUTION_FEE)) {
                    failsList.add(tx);
                    // 提现补贴手续费不足
                    errorCode = ConverterErrorCode.WITHDRAWAL_INSUFFICIENT_SUBSIDY_FEE.getCode();
                    log.error(ConverterErrorCode.WITHDRAWAL_INSUFFICIENT_SUBSIDY_FEE.getMsg());
                    continue;
                }

                // todo 是否账本验证 验证提现from和to中资产金额一致
                if (StringUtils.isBlank(withdrawalFromInfo) || !withdrawalFromInfo.equals(withdrawalToInfo)) {
                    failsList.add(tx);
                    // 提现资产金额错误
                    errorCode = ConverterErrorCode.WITHDRAWAL_FROM_TO_ASSET_AMOUNT_ERROR.getCode();
                    log.error(ConverterErrorCode.WITHDRAWAL_FROM_TO_ASSET_AMOUNT_ERROR.getMsg());
                    continue;
                }
            }
            result.put("txList", failsList);
            result.put("errorCode", errorCode);
        } catch (Exception e) {
            chain.getLogger().error(e);
            result.put("txList", txs);
            result.put("errorCode", ConverterErrorCode.SYS_UNKOWN_EXCEPTION.getCode());
        }
        return result;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        if (txs.isEmpty()) {
            return true;
        }
        // 当区块出块正常运行状态时（非区块同步模式），才执行
        if (syncStatus == SyncStatusEnum.RUNNING.value()) {
            Chain chain = chainManager.getChain(chainId);
            SignAccountDTO signAccountDTO = null;
            try {
                signAccountDTO = virtualBankService.isCurrentDirector(chain);
                if (null != signAccountDTO) {
                    for (Transaction tx : txs) {
                        //放入类似队列处理机制 准备通知异构链组件执行提现
                        TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                        pendingPO.setTx(tx);
                        pendingPO.setBlockHeader(blockHeader);
                        pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                        txSubsequentProcessStorageService.save(chain, pendingPO);
                        chain.getPendingTxQueue().offer(pendingPO);
                        chain.getLogger().debug("[commit] 提现交易 hash:{}", tx.getHash().toHex());
                    }
                }
            } catch (NulsException e) {
                chain.getLogger().error(e);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        //提现无业务回滚
        return true;
    }
}
