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
package network.nerve.converter.heterogeneouschain.ethII.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.ethII.docking.EthIIDocking;
import network.nerve.converter.heterogeneouschain.ethII.model.EthWaitingTxPo;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.utils.ConverterUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.ethII.constant.EthIIConstant.RESEND_TIME;
import static network.nerve.converter.heterogeneouschain.eth.context.EthContext.logger;

/**
 * @author: Mimi
 * @date: 2020-03-26
 */
@Component
public class EthIIResendHelper {

    @Autowired
    private EthIIInvokeTxHelper ethIIInvokeTxHelper;
    private Map<String, Integer> resendMap = new HashMap<>();
    private Map<String, Object> lockedMap = new HashMap<>();

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

    public boolean canResend(String nerveTxHash) {
        return getResendTimes(nerveTxHash) < RESEND_TIME;
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
    public String reSend(EthWaitingTxPo po) throws Exception {
        return this.reSend(po, 0);
    }

    public String reSend(EthWaitingTxPo po, int times) throws Exception {
        boolean checkOrder = false;
        String nerveTxHash = po.getNerveTxHash();
        if (!this.canResend(nerveTxHash)) {
            logger().warn("Nerve交易[{}]重发超过{}次，丢弃交易", nerveTxHash, RESEND_TIME);
            return EMPTY_STRING;
        }
        this.increase(nerveTxHash);
        try {
            EthIIDocking docking = EthIIDocking.getInstance();
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
                logger().info("Nerve交易[{}]重发完成, ethTxHash: {}", nerveTxHash, po.getTxHash());
                return po.getTxHash();
            }
            logger().error("交易重发失败，等待再次重发交易", e);
            throw e;
        }
    }

    private boolean regainSignatures(EthWaitingTxPo po, int times) {
        try {
            String nerveTxHash = po.getNerveTxHash();
            logger().info("[{}] [{}]重新索要拜占庭签名", EthConstant.ETH_CHAIN_ID, nerveTxHash);
            // 只执行一次，等待下一轮任务再执行
            if (times > 1) {
                logger().warn("[{}] 只执行一次，等待下一轮任务再执行", nerveTxHash);
                return false;
            }
            // 索要拜占庭签名
            List<HeterogeneousSign> regainSignatures = EthContext.getConverterCoreApi().regainSignatures(EthContext.NERVE_CHAINID, nerveTxHash, EthConstant.ETH_CHAIN_ID);
            if (regainSignatures.size() > EthConstant.MAX_MANAGERS) {
                logger().warn("获取拜占庭签名超过了最大数值, 获取数值: {}, 最大数值: {}", regainSignatures.size(), EthConstant.MAX_MANAGERS);
                regainSignatures = regainSignatures.subList(0, EthConstant.MAX_MANAGERS);
            }
            StringBuilder builder = new StringBuilder(EthConstant.HEX_PREFIX);
            regainSignatures.stream().forEach(signature -> builder.append(HexUtil.encode(signature.getSignature())));
            // 重置当前waitingPo的校验时间和校验高度
            ethIIInvokeTxHelper.clearEthWaitingTxPo(po);
            po.setSignatures(builder.toString());
            String ethTxHash = this.reSend(po, times);
            po.setTxHash(ethTxHash);
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

}
