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
package network.nerve.converter.heterogeneouschain.eth.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.heterogeneouschain.eth.model.EthSendTransactionPo;
import network.nerve.converter.heterogeneouschain.eth.storage.EthTxRelationStorageService;

import java.util.HashMap;
import java.util.Map;

import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.RESEND_TIME;

/**
 * @author: Mimi
 * @date: 2020-03-26
 */
@Component
public class EthResendHelper {

    @Autowired
    private EthTxRelationStorageService ethTxRelationStorageService;
    private Map<String, Integer> resendMap = new HashMap<>();
    private Map<String, Object> lockedMap = new HashMap<>();

    public void increase(String nerveTxHash) {
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
     * Is the transaction sent by the current node
     */
    public boolean currentNodeSent(String ethTxHash) {
        if (StringUtils.isBlank(ethTxHash)) {
            return false;
        }
        return ethTxRelationStorageService.findNerveTxHash(ethTxHash) != null;
    }

    /**
     * Obtain transaction information that has been sent out
     */
    public EthSendTransactionPo getSentTransactionInfo(String ethTxHash) {
        return ethTxRelationStorageService.findEthSendTxPo(ethTxHash);
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
