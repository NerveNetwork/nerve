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

package network.nerve.converter.utils;

import io.nuls.base.data.Transaction;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.model.bo.Chain;

import java.util.Set;

/**
 * @author: Loki
 * @date: 2020-03-05
 */
public class ConverterSignValidUtil {

    /**
     * 验证由虚拟银行节点签名的交易签名
     * @param chain
     * @param tx
     * @throws NulsException
     */
    public static void validateSign(Chain chain, Transaction tx) throws NulsException {
        // 签名资格 从交易签名中获取签名地址
        Set<String> addressSet = SignatureUtil.getAddressFromTX(tx, chain.getChainId());
        if (addressSet == null) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_ERROR);
        }
        boolean hasVirtualBankSigner = false;
        for (String address : addressSet) {
            if (chain.isVirtualBankBySignAddr(address)) {
                //签名地址存属于虚拟银行节点
                hasVirtualBankSigner = true;
                break;
            }
        }
        if (!hasVirtualBankSigner) {
            throw new NulsException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
        }
        // 验证签名本身正确性
        boolean rs = SignatureUtil.validateTransactionSignture(chain.getChainId(), tx);
        if (!rs) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_ERROR);
        }
    }

    /**
     * 验证拜占庭签名
     *
     * @param chain
     * @param tx
     * @throws NulsException
     */
    public static void validateByzantineSign(Chain chain, Transaction tx) throws NulsException {
        // 从交易签名中获取签名地址(去重)
        Set<String> addressSet = SignatureUtil.getAddressFromTX(tx, chain.getChainId());
        if (addressSet == null) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_ERROR);
        }

        // 统计签名地址中, 是当前虚拟银行节点地址的个数(有资格的签名数)
        int signCount = 0;
        for (String address : addressSet) {
            if (chain.isVirtualBankBySignAddr(address)) {
                //签名地址存属于虚拟银行节点
                signCount++;
            }
        }

        //虚拟银行节点签名数需要达到的拜占庭数
        int byzantineCount = VirtualBankUtil.getByzantineCount(chain);
        // 判断签名数是否足够
        if (signCount < byzantineCount) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_BYZANTINE_ERROR);
        }
        // 验证交易签名列表中所有签名本身正确性
        boolean rs = SignatureUtil.validateTransactionSignture(chain.getChainId(), tx);
        if (!rs) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_ERROR);
        }
    }

}
