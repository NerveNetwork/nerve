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
package network.nerve.converter.heterogeneouschain.lib.helper;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.docking.HtgDocking;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxRelationStorageService;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.utils.ConverterUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.constant.ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW;

/**
 * @author: Mimi
 * @date: 2020-03-26
 */
public class HtgResendHelper implements BeanInitial {

    private Map<String, Integer> resendMap = new HashMap<>();
    private Map<String, Object> lockedMap = new HashMap<>();

    private HtgInvokeTxHelper htgInvokeTxHelper;
    private HtgTxRelationStorageService htTxRelationStorageService;
    private HtgContext htgContext;

    //public HtgResendHelper(BeanMap beanMap) {
    //    this.htgInvokeTxHelper = (HtgInvokeTxHelper) beanMap.get("htgInvokeTxHelper");
    //    this.htTxRelationStorageService = (HtgTxRelationStorageService) beanMap.get("htTxRelationStorageService");
    //    this.htgContext = (HtgContext) beanMap.get("htgContext");
    //}

    private NulsLogger logger() {
        return this.htgContext.logger();
    }

    private void increase(String nerveTxHash) {
        synchronized (getLock(nerveTxHash)) {
            Integer time = resendMap.get(nerveTxHash);
            if(time == null) {
                time = 1;
            } else {
                time++;
            }
            resendMap.put(nerveTxHash, time);
        }
    }

    private void decrease(String nerveTxHash) {
        synchronized (getLock(nerveTxHash)) {
            Integer time = resendMap.get(nerveTxHash);
            if(time == null) {
                time = 0;
            }
            if (time != 0) {
                time--;
            }
            resendMap.put(nerveTxHash, time);
        }
    }

    public boolean canResend(String nerveTxHash) {
        return getResendTimes(nerveTxHash) < HtgConstant.RESEND_TIME;
    }

    public void clear(String nerveTxHash) {
        if(StringUtils.isNotBlank(nerveTxHash)) {
            resendMap.remove(nerveTxHash);
            lockedMap.remove(nerveTxHash);
        }
    }

    /**
     * 交易重发
     */
    public String reSend(HtgWaitingTxPo po) throws Exception {
        return this.reSend(po, 0);
    }

    public String reSend(HtgWaitingTxPo po, int times) throws Exception {
        boolean checkOrder = false;
        String nerveTxHash = po.getNerveTxHash();
        if (!this.canResend(nerveTxHash)) {
            logger().warn("Nerve交易[{}]重发超过{}次，丢弃交易", nerveTxHash, HtgConstant.RESEND_TIME);
            return EMPTY_STRING;
        }
        this.increase(nerveTxHash);
        try {
            HtgDocking docking = htgContext.DOCKING();
            logger().info("[{}]交易[{}]准备重发, 详情: {}", po.getTxType(), nerveTxHash, po.toString());
            switch (po.getTxType()) {
                case WITHDRAW:
                    String ethWithdrawHash = docking.createOrSignWithdrawTxII(nerveTxHash, po.getTo(), po.getValue(), po.getAssetId(), po.getSignatures(), checkOrder);
                    if(StringUtils.isBlank(ethWithdrawHash)) {
                        logger().info("Nerve交易[{}]已完成，无需重发", nerveTxHash);
                    }
                    return ethWithdrawHash;
                case CHANGE:
                    String ethChangesHash = docking.createOrSignManagerChangesTxII(nerveTxHash, po.getAddAddresses(), po.getRemoveAddresses(), po.getOrginTxCount(), po.getSignatures(), checkOrder);
                    if(StringUtils.isBlank(ethChangesHash)) {
                        logger().info("Nerve交易[{}]已完成，无需重发", nerveTxHash);
                    }
                    return ethChangesHash;
                case UPGRADE:
                    String ethUpgradeHash = docking.createOrSignUpgradeTxII(nerveTxHash, po.getUpgradeContract(), po.getSignatures(), checkOrder);
                    if(StringUtils.isBlank(ethUpgradeHash)) {
                        logger().info("Nerve交易[{}]已完成，无需重发", nerveTxHash);
                    }
                    return ethUpgradeHash;
                default:
                    break;
            }
            return EMPTY_STRING;
        } catch (Exception e) {
            // 当出现交易签名不足导致的执行失败时，向CORE重新索要交易的拜占庭签名
            if (e instanceof NulsException && ConverterUtil.isInsufficientSignature((NulsException) e) && this.regainSignatures(po, times + 1)) {
                logger().info("Nerve交易[{}]重发完成, htgTxHash: {}", nerveTxHash, po.getTxHash());
                return po.getTxHash();
            }
            if (e instanceof NulsException && INSUFFICIENT_FEE_OF_WITHDRAW.equals(((NulsException) e).getErrorCode())) {
                // 当提现手续费不足时，不计入重发次数
                this.decrease(nerveTxHash);
            }
            logger().error("交易重发失败，等待再次重发交易", e);
            throw e;
        }
    }

    private boolean regainSignatures(HtgWaitingTxPo po, int times) {
        try {
            String nerveTxHash = po.getNerveTxHash();
            logger().info("[{}] [{}]重新索要拜占庭签名", htgContext.getConfig().getChainId(), nerveTxHash);
            // 只执行一次，等待下一轮任务再执行
            if (times > 1) {
                logger().warn("[{}] 只执行一次，等待下一轮任务再执行", nerveTxHash);
                return false;
            }
            // 索要拜占庭签名
            List<HeterogeneousSign> regainSignatures = htgContext.getConverterCoreApi().regainSignatures(htgContext.NERVE_CHAINID(), nerveTxHash, htgContext.getConfig().getChainId());
            if (regainSignatures.size() > HtgConstant.MAX_MANAGERS) {
                logger().warn("获取拜占庭签名超过了最大数值, 获取数值: {}, 最大数值: {}", regainSignatures.size(), HtgConstant.MAX_MANAGERS);
                regainSignatures = regainSignatures.subList(0, HtgConstant.MAX_MANAGERS);
            }
            StringBuilder builder = new StringBuilder(HtgConstant.HEX_PREFIX);
            regainSignatures.stream().forEach(signature -> builder.append(HexUtil.encode(signature.getSignature())));
            // 重置当前waitingPo的校验时间和校验高度
            htgInvokeTxHelper.clearEthWaitingTxPo(po);
            po.setSignatures(builder.toString());
            String htTxHash = this.reSend(po, times);
            po.setTxHash(htTxHash);
            return true;
        } catch (Exception e) {
            logger().error("重新获取拜占庭签名失败", e);
            return false;
        }
    }

    private int getResendTimes(String nerveTxHash) {
        Integer time = resendMap.get(nerveTxHash);
        if(time == null) {
            time = 1;
        }
        return time.intValue();
    }

    private Object getLock(String nerveTxHash) {
        return lockedMap.computeIfAbsent(nerveTxHash, key -> new Object());
    }

    /**
     * 是否当前节点发出的交易
     */
    public boolean currentNodeSent(String htTxHash) {
        if (StringUtils.isBlank(htTxHash)) {
            return false;
        }
        return htTxRelationStorageService.findNerveTxHash(htTxHash) != null;
    }

    /**
     * 获取已发出的交易信息
     */
    public HtgSendTransactionPo getSentTransactionInfo(String htTxHash) {
        return htTxRelationStorageService.findEthSendTxPo(htTxHash);
    }

}
