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
package network.nerve.converter.model.bo;

import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;

import java.io.Serializable;

/**
 * @author: Mimi
 * @date: 2020-03-23
 */
@ApiModel
public class HeterogeneousAssetInfo implements Serializable {

    @ApiModelProperty(description = "Heterogeneous chainchainId")
    private int chainId;
    @ApiModelProperty(description = "Asset symbols")
    private String symbol;
    @ApiModelProperty(description = "Decimal places of assets")
    private int decimals;
    @ApiModelProperty(description = "assetID")
    private int assetId;
    @ApiModelProperty(description = "Asset corresponding contract address(If there is any)")
    private String contractAddress;
    @ApiModelProperty(description = "HtgAssets tonerveDecimal difference of assets")
    private String decimalsSubtractedToNerve;

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getDecimalsSubtractedToNerve() {
        return decimalsSubtractedToNerve;
    }

    public void setDecimalsSubtractedToNerve(String decimalsSubtractedToNerve) {
        this.decimalsSubtractedToNerve = decimalsSubtractedToNerve;
    }
}
