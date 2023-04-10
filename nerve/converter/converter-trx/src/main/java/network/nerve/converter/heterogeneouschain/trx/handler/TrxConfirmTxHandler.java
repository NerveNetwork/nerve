/**
 * MIT License
 * <p>
 * Copyrightg (c) 2019-2020 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rightgs
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyrightg notice and this permission notice shall be included in all
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
package network.nerve.converter.heterogeneouschain.trx.handler;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.enums.BroadcastTxValidateStatus;
import network.nerve.converter.heterogeneouschain.lib.helper.*;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxRelationStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant;
import network.nerve.converter.heterogeneouschain.trx.context.TrxContext;
import network.nerve.converter.heterogeneouschain.trx.core.TrxWalletApi;
import network.nerve.converter.heterogeneouschain.trx.helper.TrxParseTxHelper;
import network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil;
import network.nerve.converter.model.bo.HeterogeneousAddFeeCrossChainData;
import network.nerve.converter.model.bo.HeterogeneousOneClickCrossChainData;
import network.nerve.converter.utils.LoggerUtil;
import org.tron.trident.proto.Response;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.*;
import static network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant.SUN_PER_ENERGY_BASE;


/**
 * @author: Mimi
 * @date: 2020-03-02
 */
public class TrxConfirmTxHandler implements Runnable, BeanInitial {

    private TrxWalletApi htgWalletApi;
    private TrxParseTxHelper htgParseTxHelper;
    private HtgCallBackManager htgCallBackManager;
    private ConverterConfig converterConfig;
    private HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    private HtgTxRelationStorageService htgTxRelationStorageService;
    private HtgTxStorageService htgTxStorageService;
    private HtgStorageHelper htgStorageHelper;
    private HtgLocalBlockHelper htgLocalBlockHelper;
    private HtgListener htgListener;
    private HtgInvokeTxHelper htgInvokeTxHelper;
    private HtgResendHelper htgResendHelper;
    private HtgPendingTxHelper htgPendingTxHelper;
    private HtgContext htgContext;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    public void run() {
        HtgUnconfirmedTxPo po = null;
        LinkedBlockingDeque<HtgUnconfirmedTxPo> queue = htgContext.UNCONFIRMED_TX_QUEUE();
        try {
            if (!htgContext.getConverterCoreApi().isSupportProtocol15TrxCrossChain()) return;
            if (!htgContext.getConverterCoreApi().isRunning()) {
                LoggerUtil.LOG.debug("[{}]忽略同步区块模式", htgContext.getConfig().getSymbol());
                return;
            }
            try {
                BigInteger energyFee = htgWalletApi.getCurrentGasPrice();
                htgContext.logger().debug("当前{}网络的EnergyFee: {}.", htgContext.getConfig().getSymbol(), energyFee);
                TrxContext trxContext = (TrxContext) htgContext;
                if (energyFee != null && trxContext.SUN_PER_ENERGY.intValue() != energyFee.intValue()) {
                    trxContext.FEE_LIMIT_OF_WITHDRAW = TrxConstant.FEE_LIMIT_OF_WITHDRAW_BASE.multiply(energyFee).divide(SUN_PER_ENERGY_BASE);
                    trxContext.FEE_LIMIT_OF_CHANGE = TrxConstant.FEE_LIMIT_OF_CHANGE_BASE.multiply(energyFee).divide(SUN_PER_ENERGY_BASE);
                    trxContext.SUN_PER_ENERGY = energyFee;
                    trxContext.gasInfo = null;
                    htgContext.logger().info("更新当前{}网络的EnergyFee: {}, FEE_LIMIT_OF_WITHDRAW: {}, FEE_LIMIT_OF_CHANGE: {}.", htgContext.getConfig().getSymbol(), energyFee, trxContext.FEE_LIMIT_OF_WITHDRAW, trxContext.FEE_LIMIT_OF_CHANGE);
                }
            } catch (Exception e) {
                htgContext.logger().error(String.format("同步%s当前EnergyFee失败", htgContext.getConfig().getSymbol()), e);
            }
            if (!htgContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
                LoggerUtil.LOG.debug("[{}]非虚拟银行成员，跳过此任务", htgContext.getConfig().getSymbol());
                return;
            }
            if (!htgContext.isAvailableRPC()) {
                htgContext.logger().error("[{}]网络RPC不可用，暂停此任务", htgContext.getConfig().getSymbol());
                return;
            }
            htgWalletApi.checkApi(htgContext.getConverterCoreApi().getVirtualBankOrder());
            LoggerUtil.LOG.debug("[{}交易确认任务] - 每隔{}秒执行一次。", htgContext.getConfig().getSymbol(), htgContext.getConfig().getConfirmTxQueuePeriod());
            // 等待重启应用时，加载的持久化未确认交易
            htgContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH().await();
            long ethNewestHeight = htgWalletApi.getBlockHeight();
            int size = htgContext.UNCONFIRMED_TX_QUEUE().size();
            for (int i = 0; i < size; i++) {
                po = htgContext.UNCONFIRMED_TX_QUEUE().poll();
                if (po == null) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("移除空值PO");
                    }
                    continue;
                }
                // 清理无用的变更任务
                if (po.getTxType() == HeterogeneousChainTxType.RECOVERY) {
                    clearUnusedChange();
                    break;
                }
                // 当充值确认任务异常超过重试次数后，丢弃这个任务
                if (po.isDepositExceedErrorTime(RESEND_TIME)) {
                    logger().error("充值确认任务异常超过重试次数，移除此交易，详情: {}", po.toString());
                    this.clearDB(po.getTxHash());
                    continue;
                }
                HtgUnconfirmedTxPo poFromDB = null;
                if (po.getBlockHeight() == null) {
                    poFromDB = htgUnconfirmedTxStorageService.findByTxHash(po.getTxHash());
                    if (poFromDB != null) {
                        po.setBlockHeight(poFromDB.getBlockHeight());
                    }
                }
                if (po.getBlockHeight() == null) {
                    // 区块高度为空，检查10次后，查询eth交易所在高度，若节点同步的eth高度大于了交易所在高度，则说明此交易已经被其他节点处理过，应移除此交易
                    boolean needRemovePo = checkBlockHeightTimes(po);
                    if(needRemovePo) {
                        logger().info("区块高度为空，此交易已处理过，移除此交易，详情: {}", po.toString());
                        this.clearDB(po.getTxHash());
                        continue;
                    }
                    if(logger().isDebugEnabled()) {
                        logger().debug("区块高度为空，放回队列等待下次处理，详情: {}", po.toString());
                    }
                    queue.offer(po);
                    continue;
                }

                // 交易触发重新验证后，等待`skipTimes`的轮次，再做验证
                if (po.getSkipTimes() > 0) {
                    po.setSkipTimes(po.getSkipTimes() - 1);
                    queue.offer(po);
                    if(logger().isDebugEnabled()) {
                        logger().debug("交易触发重新验证，剩余等待再次验证的轮次数量: {}", po.getSkipTimes());
                    }
                    continue;
                }
                // 未达到确认高度，放回队列中，下次继续检查
                int confirmation = htgContext.getConfig().getTxBlockConfirmations();
                if (po.getTxType() == HeterogeneousChainTxType.WITHDRAW) {
                    confirmation = htgContext.getConfig().getTxBlockConfirmationsOfWithdraw();
                }
                if (ethNewestHeight - po.getBlockHeight() < confirmation) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("交易[{}]确认高度等待: {}", po.getTxHash(), confirmation - (ethNewestHeight - po.getBlockHeight()));
                    }
                    queue.offer(po);
                    continue;
                }
                switch (po.getTxType()) {
                    case DEPOSIT:
                        if (dealDeposit(po, poFromDB)) {
                            if(logger().isDebugEnabled()) {
                                logger().debug("充值交易重新放回队列, 详情: {}", poFromDB != null ? poFromDB.toString() : po.toString());
                            }
                            queue.offer(po);
                        }
                        break;
                    case WITHDRAW:
                    case CHANGE:
                    case UPGRADE:
                        if (dealBroadcastTx(po, poFromDB)) {
                            if(logger().isDebugEnabled()) {
                                logger().debug("广播交易重新放回队列, 详情: {}", poFromDB != null ? poFromDB.toString() : po.toString());
                            }
                            queue.offer(po);
                        }
                        break;
                    default:
                        logger().error("unkown tx: {}", po.toString());
                        this.clearDB(po.getTxHash());
                        break;
                }
            }
        } catch (Exception e) {
            logger().error("confirming error", e);
            if (po != null) {
                queue.offer(po);
            }
        }
    }

    private void clearUnusedChange() throws Exception {
        Iterator<HtgUnconfirmedTxPo> iterator = htgContext.UNCONFIRMED_TX_QUEUE().iterator();
        while(iterator.hasNext()) {
            HtgUnconfirmedTxPo po = iterator.next();
            if (po.getTxType() == HeterogeneousChainTxType.CHANGE) {
                iterator.remove();
                this.clearDB(po.getTxHash());
            }
        }
    }

    private boolean checkBlockHeightTimes(HtgUnconfirmedTxPo po) throws Exception {
        po.increaseBlockHeightTimes();
        if(po.getBlockHeightTimes() < 10) {
            return false;
        }
        po.setBlockHeightTimes(0);
        String txHash = po.getTxHash();
        if(StringUtils.isBlank(txHash)) {
            return false;
        }
        Response.TransactionInfo txReceipt = htgWalletApi.getTransactionReceipt(txHash);
        if(txReceipt == null) {
            return false;
        }
        long blockNumber = txReceipt.getBlockNumber();
        // 本地最新的区块
        HtgSimpleBlockHeader localMax = htgLocalBlockHelper.getLatestLocalBlockHeader();
        if(localMax == null) {
            return false;
        }
        if(localMax.getHeight().longValue() > blockNumber) {
            return true;
        }
        return false;
    }

    private boolean dealDeposit(HtgUnconfirmedTxPo po, HtgUnconfirmedTxPo poFromDB) throws Exception {
        boolean isReOfferQueue = true;
        String htgTxHash = po.getTxHash();
        if (!htgContext.getConverterCoreApi().validNerveAddress(po.getNerveAddress())) {
            logger().warn("[充值地址异常] 交易[{}], 移除队列, [1]充值地址: {}", htgTxHash, po.getNerveAddress());
            this.clearDB(htgTxHash);
            return !isReOfferQueue;
        }
        HtgUnconfirmedTxPo txPo = poFromDB;
        if(txPo == null) {
            txPo = htgUnconfirmedTxStorageService.findByTxHash(htgTxHash);
        }
        if (txPo == null) {
            logger().warn("[充值任务异常] DB中未获取到PO，队列中PO: {}", po.toString());
            return !isReOfferQueue;
        }
        // 当状态为移除，不再回调Nerve核心，放回队列中，等待达到移除高度后，从DB中删除，从队列中移除
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(htgTxHash);
                isReOfferQueue = false;
                logger().info("[{}]交易[{}]已确认超过{}个高度, 移除队列, nerve高度: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), HtgConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            // 补充po内存数据，po打印日志，方便查看数据
            po.setDelete(txPo.isDelete());
            po.setDeletedHeight(txPo.getDeletedHeight());
            return isReOfferQueue;
        }
        if (!po.isValidateTx()) {
            //再次验证交易
            boolean validateTx = validateDepositTxConfirmedInHtgNet(htgTxHash, po.isIfContractAsset());
            if (!validateTx) {
                // 验证失败，从DB和队列中移除交易
                this.clearDB(htgTxHash);
                return !isReOfferQueue;
            }
            po.setValidateTx(validateTx);
        }
        // 回调充值交易
        String nerveTxHash;
        Long height = po.getBlockHeight();
        Long txTime = po.getTxTime();
        try {
            do {
                ConverterConfig converterConfig = htgContext.getConverterCoreApi().getConverterConfig();
                byte[] withdrawalBlackhole = AddressTool.getAddressByPubKeyStr(converterConfig.getBlackHolePublicKey(), converterConfig.getChainId());
                byte[] feeAddress = AddressTool.getAddressByPubKeyStr(converterConfig.getFeePubkey(), converterConfig.getChainId());
                nerveTxHash = this.submitNerveAddFeeCrossChainTx(po, height, txTime, feeAddress);
                if (StringUtils.isNotBlank(nerveTxHash)) {
                    // 此交易是跨链追加手续费交易
                    break;
                }
                nerveTxHash = this.submitNerveOneClickCrossChainTx(po, height, txTime, withdrawalBlackhole);
                if (StringUtils.isNotBlank(nerveTxHash)) {
                    // 此交易是一键跨链交易
                    break;
                }
                // 充值的nerve接收地址不能是黑洞或者手续费补贴地址
                if (Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), withdrawalBlackhole) || Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), feeAddress)) {
                    logger().error("[{}][充值地址异常][黑洞或者手续费补贴地址]Deposit Nerve address error:{}, heterogeneousHash:{}", htgContext.HTG_CHAIN_ID(), po.getNerveAddress(), po.getTxHash());
                    // 验证失败，从DB和队列中移除交易
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                if (po.isDepositIIMainAndToken()) {
                    // 同时充值两种资产的充值交易
                    nerveTxHash = htgCallBackManager.getDepositTxSubmitter().depositIITxSubmit(
                            htgTxHash,
                            height,
                            po.getFrom(),
                            po.getTo(),
                            po.getValue(),
                            txTime,
                            po.getDecimals(),
                            po.getContractAddress(),
                            po.getAssetId(),
                            po.getNerveAddress(),
                            po.getDepositIIMainAssetValue(), po.getDepositIIExtend());
                } else {
                    nerveTxHash = htgCallBackManager.getDepositTxSubmitter().txSubmit(
                            htgTxHash,
                            height,
                            po.getFrom(),
                            po.getTo(),
                            po.getValue(),
                            txTime,
                            po.getDecimals(),
                            po.isIfContractAsset(),
                            po.getContractAddress(),
                            po.getAssetId(),
                            po.getNerveAddress(), po.getDepositIIExtend());
                }
            } while (false);

            po.setNerveTxHash(nerveTxHash);
            txPo.setNerveTxHash(nerveTxHash);
            // 当未确认交易数据产生变化时，更新DB数据
            boolean nerveTxHashNotBlank = StringUtils.isNotBlank(nerveTxHash);
            if (nerveTxHashNotBlank) {
                String updateHash = nerveTxHash;
                htgUnconfirmedTxStorageService.update(txPo, update -> update.setNerveTxHash(updateHash));
                if (nerveTxHashNotBlank) {
                    htgStorageHelper.saveTxInfo(txPo);
                }
            }
        } catch (Exception e) {
            // 交易已存在，移除队列
            if (e instanceof NulsException &&
                    (TX_ALREADY_EXISTS_0.equals(((NulsException) e).getErrorCode())
                            || TX_ALREADY_EXISTS_2.equals(((NulsException) e).getErrorCode()))) {
                logger().info("Nerve交易已存在，从队列中移除待确认的{}交易[{}]", htgContext.getConfig().getSymbol(), htgTxHash);
                return !isReOfferQueue;
            }
            po.increaseDepositErrorTime();
            throw e;
        }
        return isReOfferQueue;
    }

    private String submitNerveAddFeeCrossChainTx(HtgUnconfirmedTxPo po, Long height, Long txTime, byte[] feeAddress) throws Exception {
        if (!htgContext.getConverterCoreApi().isProtocol21()) {
            return null;
        }
        HeterogeneousAddFeeCrossChainData data = htgParseTxHelper.parseAddFeeCrossChainData(po.getDepositIIExtend(), logger());
        if (data == null) {
            return null;
        }
        // 跨链追加手续费的nerve接收地址只能是手续费补贴地址
        if (!Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), feeAddress)) {
            logger().error("[{}]AddFeeCrossChain Nerve address error:{}, heterogeneousHash:{}", htgContext.HTG_CHAIN_ID(), po.getNerveAddress(), po.getTxHash());
            return null;
        }
        // 不能有token资产
        if (po.isIfContractAsset()) {
            logger().error("[{}]AddFeeCrossChain Asset error, token address: {}, heterogeneousHash:{}", htgContext.HTG_CHAIN_ID(), po.getContractAddress(), po.getTxHash());
            return null;
        }
        String nerveTxHash = htgCallBackManager.getDepositTxSubmitter().addFeeCrossChainTxSubmit(
                po.getTxHash(),
                height,
                po.getFrom(),
                po.getTo(),
                txTime,
                po.getNerveAddress(),
                po.getValue(),
                data.getNerveTxHash(),
                data.getSubExtend());
        return nerveTxHash;
    }

    // P21生效, 一键跨链交易
    private String submitNerveOneClickCrossChainTx(HtgUnconfirmedTxPo po, Long height, Long txTime, byte[] withdrawalBlackhole) throws Exception {
        if (!htgContext.getConverterCoreApi().isProtocol21()) {
            return null;
        }
        HeterogeneousOneClickCrossChainData data = htgParseTxHelper.parseOneClickCrossChainData(po.getDepositIIExtend(), logger());
        if (data == null) {
            return null;
        }
        // 一键跨链的nerve接收地址只能是黑洞地址
        if (!Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), withdrawalBlackhole)) {
            logger().error("[{}]OneClickCrossChain Nerve address error:{}, heterogeneousHash:{}", htgContext.HTG_CHAIN_ID(), po.getNerveAddress(), po.getTxHash());
            return null;
        }
        BigInteger erc20Value, mainAssetValue;
        Integer erc20Decimals, erc20AssetId;
        if (po.isDepositIIMainAndToken()) {
            erc20Value = po.getValue();
            erc20Decimals = po.getDecimals();
            erc20AssetId = po.getAssetId();
            mainAssetValue = po.getDepositIIMainAssetValue();
        } else if (po.isIfContractAsset()) {
            erc20Value = po.getValue();
            erc20Decimals = po.getDecimals();
            erc20AssetId = po.getAssetId();
            mainAssetValue = BigInteger.ZERO;
        } else {
            erc20Value = BigInteger.ZERO;
            erc20Decimals = 0;
            erc20AssetId = 0;
            mainAssetValue = po.getValue();
        }
        String nerveTxHash = htgCallBackManager.getDepositTxSubmitter().oneClickCrossChainTxSubmit(
                po.getTxHash(),
                height,
                po.getFrom(),
                po.getTo(),
                erc20Value,
                txTime,
                erc20Decimals,
                po.getContractAddress(),
                erc20AssetId,
                po.getNerveAddress(),
                mainAssetValue,
                data.getFeeAmount(),
                data.getDesChainId(),
                data.getDesToAddress(),
                data.getTipping(),
                data.getTippingAddress(),
                data.getDesExtend());
        return nerveTxHash;
    }

    private boolean dealBroadcastTx(HtgUnconfirmedTxPo po, HtgUnconfirmedTxPo poFromDB) throws Exception {
        boolean isReOfferQueue = true;
        String htgTxHash = po.getTxHash();
        HtgUnconfirmedTxPo txPo = poFromDB;
        if (txPo == null) {
            txPo = htgUnconfirmedTxStorageService.findByTxHash(htgTxHash);
        }
        if (txPo == null) {
            logger().warn("[{}任务异常] DB中未获取到PO，队列中PO: {}", po.getTxType(), po.toString());
            return !isReOfferQueue;
        }
        String nerveTxHash = po.getNerveTxHash();
        // 当状态为移除，不再回调Nerve核心，放回队列中，等待达到移除高度后，从DB中删除，不放回队列
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(htgTxHash);
                isReOfferQueue = false;
                htgResendHelper.clear(nerveTxHash);
                logger().info("[{}]交易[{}]已确认超过{}个高度, 移除队列, nerve高度: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), HtgConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            // 补充po内存数据，po打印日志，方便查看数据
            po.setDelete(txPo.isDelete());
            po.setDeletedHeight(txPo.getDeletedHeight());
            return isReOfferQueue;
        }

        switch (txPo.getStatus()) {
            case INITIAL:
                break;
            case FAILED:
                if (!htgResendHelper.canResend(nerveTxHash)) {
                    logger().warn("Nerve交易[{}]重发超过{}次，丢弃交易", nerveTxHash, RESEND_TIME);
                    htgResendHelper.clear(htgTxHash);
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                logger().info("失败的{}交易[{}]，检查当前节点是否可发交易", htgContext.getConfig().getSymbol(), htgTxHash);
                // 检查自己的顺序是否可发交易
                HtgWaitingTxPo waitingTxPo = htgInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                // 检查三个轮次是否有waitingTxPo，否则移除此FAILED任务
                if (waitingTxPo == null && !po.checkFailedTimeOut()) {
                    return isReOfferQueue;
                }
                // 查询nerve交易对应的eth交易是否成功
                if (htgInvokeTxHelper.isSuccessfulNerve(nerveTxHash)) {
                    logger().info("Nerve tx 在NERVE网络已确认, 成功移除队列, nerveHash: {}", nerveTxHash);
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                if (waitingTxPo == null) {
                    logger().info("检查三个轮次没有waitingTxPo，移除此FAILED任务, htgTxHash: {}", htgTxHash);
                    return !isReOfferQueue;
                }
                if (this.checkIfSendByOwn(waitingTxPo, txPo.getFrom())) {
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                // 失败的交易，不由当前节点处理，从队列和DB中移除
                logger().info("失败的{}交易[{}]，当前节点不是下一顺位，不由当前节点处理，移除队列", htgContext.getConfig().getSymbol(), htgTxHash);
                this.clearDB(htgTxHash);
                return !isReOfferQueue;
            case COMPLETED:
                if (!po.isValidateTx()) {
                    //再次验证交易
                    BroadcastTxValidateStatus validate = validateBroadcastTxConfirmedInHtgNet(po);
                    switch (validate) {
                        case RE_VALIDATE:
                            // 放回队列，再次验证
                            return isReOfferQueue;
                        case RE_SEND:
                            // 若交易状态已完成，再次验证失败，则重发交易
                            HtgWaitingTxPo _waitingTxPo = htgInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                            // 检查三个轮次是否有waitingTxPo，否则移除此FAILED任务
                            if (_waitingTxPo == null && po.checkFailedTimeOut()) {
                                return isReOfferQueue;
                            }
                            htgResendHelper.reSend(_waitingTxPo);
                            this.clearDB(htgTxHash);
                            return !isReOfferQueue;
                        case SUCCESS:
                        default:
                            break;
                    }
                    po.setValidateTx(validate == BroadcastTxValidateStatus.SUCCESS);
                }
                try {
                    String realNerveTxHash = nerveTxHash;
                    logger().info("[{}]签名完成的{}交易[{}]调用Nerve确认[{}]", po.getTxType(), htgContext.getConfig().getSymbol(), htgTxHash, realNerveTxHash);
                    // 签名完成的交易将触发回调Nerve Core
                    htgCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            po.getTxType(),
                            realNerveTxHash,
                            htgTxHash,
                            txPo.getBlockHeight(),
                            txPo.getTxTime(),
                            htgContext.MULTY_SIGN_ADDRESS(),
                            txPo.getSigners());
                } catch (NulsException e) {
                    // 交易已存在，等待确认移除
                    if (TX_ALREADY_EXISTS_0.equals(e.getErrorCode()) || TX_ALREADY_EXISTS_1.equals(e.getErrorCode())) {
                        logger().info("Nerve交易[{}]已存在，从队列中移除待确认的{}交易[{}]", txPo.getNerveTxHash(), htgContext.getConfig().getSymbol(), htgTxHash);
                        return !isReOfferQueue;
                    }
                    throw e;
                }
                break;
        }
        return isReOfferQueue;
    }

    /**
     * 若eth交易失败，则检查当前节点是否为下一顺位发交易，是则重发交易
     */
    private boolean checkIfSendByOwn(HtgWaitingTxPo waitingTxPo, String txFrom) throws Exception {
        String nerveTxHash = waitingTxPo.getNerveTxHash();
        Map<String, Integer> virtualBanks = waitingTxPo.getCurrentVirtualBanks();
        Integer totalBank = virtualBanks.size();
        Integer sendOrderCurrentNode = waitingTxPo.getCurrentNodeSendOrder();//
        int sendOrderFailure = virtualBanks.get(txFrom);
        // 检查失败的order是否为管理员顺位中最后一位，是则重置当前节点已发HT交易的记录，重置节点等待时间，从第一顺位开始重发交易
        if (sendOrderFailure == totalBank) {
            htgInvokeTxHelper.clearRecordOfCurrentNodeSentEthTx(nerveTxHash, waitingTxPo);
            if (sendOrderCurrentNode == 1) {
                // 发起交易
                htgResendHelper.reSend(waitingTxPo);
                return true;
            }
        }
        if (sendOrderFailure + 1 == sendOrderCurrentNode) {
            // 当前节点是下一顺位发交易，检查是否已发出交易，否则发出交易
            if (!htgInvokeTxHelper.currentNodeSentEthTx(nerveTxHash)) {
                // 发起交易
                htgResendHelper.reSend(waitingTxPo);
                return true;
            }
        }
        return false;
    }

    /**
     * 验证充值交易
     */
    private boolean validateDepositTxConfirmedInHtgNet(String htgTxHash, boolean ifContractAsset) throws Exception {
        boolean validateTx = false;
        do {
            Response.TransactionInfo receipt = htgWalletApi.getTransactionReceipt(htgTxHash);
            if (receipt == null) {
                logger().error("再次验证交易[{}]失败，获取不到receipt", htgTxHash);
                break;
            }
            if (!TrxUtil.checkTransactionSuccess(receipt)) {
                logger().error("再次验证交易[{}]失败，receipt状态不正确", htgTxHash);
                break;
            } else if (ifContractAsset && (receipt.getLogCount() == 0)) {
                logger().error("再次验证交易[{}]失败，receipt.Log状态不正确", htgTxHash);
                break;
            }
            validateTx = true;
        } while (false);
        return validateTx;
    }

    /**
     * 验证发到TRX网络的交易是否确认，若有异常情况，则根据条件重发交易
     */
    private BroadcastTxValidateStatus validateBroadcastTxConfirmedInHtgNet(HtgUnconfirmedTxPo po) throws Exception {

        BroadcastTxValidateStatus status;
        String htgTxHash = po.getTxHash();
        do {
            Response.TransactionInfo receipt = htgWalletApi.getTransactionReceipt(htgTxHash);
            if (receipt == null) {
                boolean timeOut = System.currentTimeMillis() - po.getCreateDate() > HtgConstant.MINUTES_20;
                logger().error("再次验证交易[{}]失败，获取不到receipt", htgTxHash);
                if (timeOut) {
                    // 交易二十分钟未确认，则重发交易
                    status = BroadcastTxValidateStatus.RE_SEND;
                } else {
                    // 未获取到交易收据，20分钟内，每3轮验证一次
                    po.setSkipTimes(3);
                    status = BroadcastTxValidateStatus.RE_VALIDATE;
                }
                break;
            }
            if (!TrxUtil.checkTransactionSuccess(receipt)) {
                status = BroadcastTxValidateStatus.RE_SEND;
                logger().error("再次验证交易[{}]失败，receipt状态不正确", htgTxHash);
                break;
            } else if (receipt.getLogCount() == 0) {
                status = BroadcastTxValidateStatus.RE_SEND;
                logger().error("再次验证交易[{}]失败，receipt.Log状态不正确", htgTxHash);
                break;
            }
            status = BroadcastTxValidateStatus.SUCCESS;
        } while (false);
        return status;
    }

    private void clearDB(String htgTxHash) throws Exception {
        if(StringUtils.isBlank(htgTxHash)) {
            return;
        }
        htgUnconfirmedTxStorageService.deleteByTxHash(htgTxHash);
        htgTxRelationStorageService.deleteByTxHash(htgTxHash);
    }

    private long getCurrentBlockHeightOnNerve() {
        return htgContext.getConverterCoreApi().getCurrentBlockHeightOnNerve();
    }
}
