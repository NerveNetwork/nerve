/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.consensus.model.dto.output;


import io.nuls.consensus.model.bo.tx.txdata.Deposit;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;
import io.nuls.core.model.BigIntegerUtils;

/**
 * Consensus Information
 * Consensus information class
 *
 * @author tag
 * 2018/11/20
 */
@ApiModel(name = "Entrustment information")
public class DepositDTO {
    @ApiModelProperty(description = "Entrusted amount")
    private String deposit;
    @ApiModelProperty(description = "nodeHASH")
    private String address;
    @ApiModelProperty(description = "Entrustment time")
    private Long time;
    @ApiModelProperty(description = "Entrusted transactionHASH")
    private String txHash;
    @ApiModelProperty(description = "The packaging height of entrusted transactions")
    private Long blockHeight;
    @ApiModelProperty(description = "Exit commission height")
    private Long delHeight;
    @ApiModelProperty(description = "Asset ChainID")
    private int assetChainId;
    @ApiModelProperty(description = "assetID")
    private int assetId;
    @ApiModelProperty(description = "Entrustment type")
    private byte depositType;
    @ApiModelProperty(description = "Entrustment duration")
    private byte timeType;

    public DepositDTO(Deposit deposit) {
        this.deposit = BigIntegerUtils.bigIntegerToString(deposit.getDeposit());
        this.address = AddressTool.getStringAddressByBytes(deposit.getAddress());
        this.time = deposit.getTime();
        this.txHash = deposit.getTxHash().toHex();
        this.blockHeight = deposit.getBlockHeight();
        this.delHeight = deposit.getDelHeight();
        this.assetChainId = deposit.getAssetChainId();
        this.assetId = deposit.getAssetId();
        this.depositType = deposit.getDepositType();
        this.timeType = deposit.getTimeType();
    }

    public String getDeposit() {
        return deposit;
    }

    public void setDeposit(String deposit) {
        this.deposit = deposit;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public Long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public Long getDelHeight() {
        return delHeight;
    }

    public void setDelHeight(Long delHeight) {
        this.delHeight = delHeight;
    }

    public int getAssetChainId() {
        return assetChainId;
    }

    public void setAssetChainId(int assetChainId) {
        this.assetChainId = assetChainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public byte getDepositType() {
        return depositType;
    }

    public void setDepositType(byte depositType) {
        this.depositType = depositType;
    }

    public byte getTimeType() {
        return timeType;
    }

    public void setTimeType(byte timeType) {
        this.timeType = timeType;
    }
}
