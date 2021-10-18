/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
package network.nerve.converter.model.bo;

import io.nuls.base.data.Coin;
import network.nerve.converter.enums.AssetName;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/10/9
 */
public class WithdrawalTotalFeeInfo {
    private BigInteger fee;
    private boolean nvtAsset;
    private Coin feeCoin;
    private AssetName htgMainAssetName;

    public WithdrawalTotalFeeInfo(BigInteger fee) {
        this.fee = fee;
        this.nvtAsset = true;
        this.htgMainAssetName = AssetName.NVT;
    }

    public WithdrawalTotalFeeInfo(BigInteger fee, boolean nvtAsset) {
        this.fee = fee;
        this.nvtAsset = nvtAsset;
    }

    public WithdrawalTotalFeeInfo() {
    }

    public WithdrawalTotalFeeInfo(BigInteger fee, boolean nvtAsset, Coin feeCoin) {
        this.fee = fee;
        this.nvtAsset = nvtAsset;
        this.feeCoin = feeCoin;
    }

    public BigInteger getFee() {
        return fee;
    }

    public void setFee(BigInteger fee) {
        this.fee = fee;
    }

    public boolean isNvtAsset() {
        return nvtAsset;
    }

    public void setNvtAsset(boolean nvtAsset) {
        this.nvtAsset = nvtAsset;
    }

    public Coin getFeeCoin() {
        return feeCoin;
    }

    public void setFeeCoin(Coin feeCoin) {
        this.feeCoin = feeCoin;
    }

    public AssetName getHtgMainAssetName() {
        return htgMainAssetName;
    }

    public void setHtgMainAssetName(AssetName htgMainAssetName) {
        this.htgMainAssetName = htgMainAssetName;
    }
}
