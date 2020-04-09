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

package nerve.network.converter.utils;

import io.nuls.base.data.Transaction;
import io.nuls.base.signture.SignatureUtil;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.model.bo.Chain;
import io.nuls.core.exception.NulsException;

import java.util.Set;

/**
 * @author: Chino
 * @date: 2020-03-05
 */
public class ConverterSignValidUtil {

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
}
