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
package network.nerve.converter.heterogeneouschain.bitcoinlib.helper;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.docking.HtgDocking;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgInvokeTxHelper;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxRelationStorageService;
import network.nerve.converter.message.ComponentSignMessage;
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
public class BitCoinLibResendHelper implements BeanInitial {

    private Map<String, Integer> resendMap = new HashMap<>();
    private Map<String, Object> lockedMap = new HashMap<>();

    private HtgInvokeTxHelper htgInvokeTxHelper;
    private HtgTxRelationStorageService htTxRelationStorageService;
    private HtgContext htgContext;

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
     * Transaction resend
     */
    public String reSend(HtgWaitingTxPo po) throws Exception {
        return this.reSend(po, 0);
    }

    public String reSend(HtgWaitingTxPo po, int times) throws Exception {
        boolean checkOrder = false;
        String nerveTxHash = po.getNerveTxHash();
        if (!this.canResend(nerveTxHash)) {
            logger().warn("Nervetransaction[{}]Resend over{}Second, discard transaction", nerveTxHash, HtgConstant.RESEND_TIME);
            return EMPTY_STRING;
        }
        this.increase(nerveTxHash);
        try {
            HtgDocking docking = (HtgDocking) htgContext.DOCKING();
            logger().info("[{}]transaction[{}]Prepare to resend, details: {}", po.getTxType(), nerveTxHash, po.toString());
            switch (po.getTxType()) {
                case WITHDRAW:
                    String ethWithdrawHash = docking.getBitCoinApi().createOrSignWithdrawTx(nerveTxHash, po.getTo(), po.getValue(), po.getAssetId(), po.getSignatures(), checkOrder);
                    if(StringUtils.isBlank(ethWithdrawHash)) {
                        logger().info("Nervetransaction[{}]Completed, no need to resend", nerveTxHash);
                    }
                    return ethWithdrawHash;
                case CHANGE:
                    String ethChangesHash = docking.getBitCoinApi().createOrSignManagerChangesTx(nerveTxHash, po.getAddAddresses(), po.getRemoveAddresses(), po.getOrginTxCount(), po.getSignatures(), checkOrder);
                    if(StringUtils.isBlank(ethChangesHash)) {
                        logger().info("Nervetransaction[{}]Completed, no need to resend", nerveTxHash);
                    }
                    return ethChangesHash;
                case UPGRADE:
                    String ethUpgradeHash = docking.createOrSignUpgradeTxII(nerveTxHash, po.getUpgradeContract(), po.getSignatures(), checkOrder);
                    if(StringUtils.isBlank(ethUpgradeHash)) {
                        logger().info("Nervetransaction[{}]Completed, no need to resend", nerveTxHash);
                    }
                    return ethUpgradeHash;
                default:
                    break;
            }
            return EMPTY_STRING;
        } catch (Exception e) {
            // When execution fails due to insufficient transaction signatures, report toCORERequesting Byzantine signatures for the transaction again
            if (e instanceof NulsException && ConverterUtil.isInsufficientSignature((NulsException) e) && this.regainSignatures(po, times + 1)) {
                logger().info("Nervetransaction[{}]Resend completed, htgTxHash: {}", nerveTxHash, po.getTxHash());
                return po.getTxHash();
            }
            if (e instanceof NulsException && INSUFFICIENT_FEE_OF_WITHDRAW.equals(((NulsException) e).getErrorCode())) {
                // When the withdrawal fee is insufficient, it will not be counted as the number of resends
                this.decrease(nerveTxHash);
            }
            logger().error("Transaction resend failed, waiting for resend transaction", e);
            throw e;
        }
    }

    private boolean regainSignatures(HtgWaitingTxPo po, int times) {
        try {
            String nerveTxHash = po.getNerveTxHash();
            logger().info("[{}] [{}]Requesting a new Byzantine signature", htgContext.getConfig().getChainId(), nerveTxHash);
            // Execute only once, wait for the next round of tasks before executing
            if (times > 1) {
                logger().warn("[{}] Execute only once, wait for the next round of tasks before executing", nerveTxHash);
                return false;
            }
            // Requesting Byzantine signatures
            List<HeterogeneousSign> regainSignatures = htgContext.getConverterCoreApi().regainSignatures(htgContext.NERVE_CHAINID(), nerveTxHash, htgContext.getConfig().getChainId());
            if (regainSignatures.size() > HtgConstant.MAX_MANAGERS) {
                logger().warn("Obtaining Byzantine signature exceeds the maximum value, Obtain numerical values: {}, Maximum value: {}", regainSignatures.size(), HtgConstant.MAX_MANAGERS);
                regainSignatures = regainSignatures.subList(0, HtgConstant.MAX_MANAGERS);
            }
            StringBuilder builder = new StringBuilder();
            for (HeterogeneousSign signature : regainSignatures) {
                builder.append(HexUtil.encode(signature.getSignature())).append(",");
            }
            builder.deleteCharAt(builder.length() - 1);

            // Reset CurrentwaitingPoVerification time and verification height
            htgInvokeTxHelper.clearEthWaitingTxPo(po);
            po.setSignatures(builder.toString());
            String htTxHash = this.reSend(po, times);
            po.setTxHash(htTxHash);
            return true;
        } catch (Exception e) {
            logger().error("Failed to retrieve Byzantine signature again", e);
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
     * Is the transaction sent by the current node
     */
    public boolean currentNodeSent(String htTxHash) {
        if (StringUtils.isBlank(htTxHash)) {
            return false;
        }
        return htTxRelationStorageService.findNerveTxHash(htTxHash) != null;
    }

    /**
     * Obtain transaction information that has been sent out
     */
    public HtgSendTransactionPo getSentTransactionInfo(String htTxHash) {
        return htTxRelationStorageService.findEthSendTxPo(htTxHash);
    }

}
