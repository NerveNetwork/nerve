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

package network.nerve.converter.model.dto;

import network.nerve.converter.enums.ProposalVoteRangeTypeEnum;

/**
 * @author: Loki
 * @date: 2020-03-03
 */
public class ProposalTxDTO {

    /**
     * 提案类型
     */
    byte type;

    /**
     * 投票范围
     */
    byte voteRangeType = ProposalVoteRangeTypeEnum.BANK.value();
    /**
     * 提案类容
     */
    String content;

    /**
     * 异构链ChainId
     */
    int heterogeneousChainId;
    /**
     * 异构链交易hash
     */
    String heterogeneousTxHash;
    /**
     * 业务地址
     */
    String businessAddress;
    /**
     * 链内交易hash
     */
    String hash;
    /**
     * 备注
     */
    String remark;

    /**
     * 签名信息
     */
    private SignAccountDTO signAccountDTO;

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getVoteRangeType() {
        return voteRangeType;
    }

    public void setVoteRangeType(byte voteRangeType) {
        this.voteRangeType = voteRangeType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public String getHeterogeneousTxHash() {
        return heterogeneousTxHash;
    }

    public void setHeterogeneousTxHash(String heterogeneousTxHash) {
        this.heterogeneousTxHash = heterogeneousTxHash;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public SignAccountDTO getSignAccountDTO() {
        return signAccountDTO;
    }

    public void setSignAccountDTO(SignAccountDTO signAccountDTO) {
        this.signAccountDTO = signAccountDTO;
    }
}
