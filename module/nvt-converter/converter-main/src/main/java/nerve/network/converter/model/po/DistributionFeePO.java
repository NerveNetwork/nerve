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

package nerve.network.converter.model.po;

import io.nuls.base.data.NulsHash;

import java.io.Serializable;

/**
 * 手续费补贴交易确认业务数据 关系
 * @author: Chino
 * @date: 2020/3/20
 */
public class DistributionFeePO implements Serializable {

    /**
     * 原始依据jiaoyihash
     */
    private NulsHash basisTxHash;
    /**
     * 当前手续费不补贴交易hash
     */
    private NulsHash DistributionFeeTxHash;

    public DistributionFeePO() {
    }

    public DistributionFeePO(NulsHash basisTxHash, NulsHash distributionFeeTxHash) {
        this.basisTxHash = basisTxHash;
        DistributionFeeTxHash = distributionFeeTxHash;
    }

    public NulsHash getBasisTxHash() {
        return basisTxHash;
    }

    public void setBasisTxHash(NulsHash basisTxHash) {
        this.basisTxHash = basisTxHash;
    }

    public NulsHash getDistributionFeeTxHash() {
        return DistributionFeeTxHash;
    }

    public void setDistributionFeeTxHash(NulsHash distributionFeeTxHash) {
        DistributionFeeTxHash = distributionFeeTxHash;
    }
}
