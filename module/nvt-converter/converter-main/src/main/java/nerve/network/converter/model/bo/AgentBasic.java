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

package nerve.network.converter.model.bo;

import io.nuls.core.model.StringUtils;

import java.math.BigInteger;

/**
 * @author: Chino
 * @date: 2020-03-04
 */
public class AgentBasic {
    private String agentAddress;
    private String packingAddress;
    private String rewardAddress;
    private BigInteger deposit;
    private boolean seedNode;
    private String pubKey;

    public String getAgentAddress() {
        return agentAddress;
    }

    public void setAgentAddress(String agentAddress) {
        this.agentAddress = agentAddress;
    }

    public String getPackingAddress() {
        return packingAddress;
    }

    public void setPackingAddress(String packingAddress) {
        this.packingAddress = packingAddress;
    }

    public BigInteger getDeposit() {
        return deposit;
    }

    public void setDeposit(BigInteger deposit) {
        this.deposit = deposit;
    }

    public void setDeposit(String deposit) {
        if(StringUtils.isBlank(deposit)){
            deposit = "0";
        }
        this.deposit = new BigInteger(deposit);
    }

    public boolean getSeedNode() {
        return seedNode;
    }

    public void setSeedNode(boolean seedNode) {
        this.seedNode = seedNode;
    }

    public String getPubKey() {
        return pubKey;
    }

    public void setPubKey(String pubKey) {
        this.pubKey = pubKey;
    }

    public String getRewardAddress() {
        return rewardAddress;
    }

    public void setRewardAddress(String rewardAddress) {
        this.rewardAddress = rewardAddress;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) { return false;}

        AgentBasic that = (AgentBasic) o;

        if (agentAddress != null ? !agentAddress.equals(that.agentAddress) : that.agentAddress != null) {
            return false;
        }
        return packingAddress != null ? !packingAddress.equals(that.packingAddress) : that.packingAddress != null;
    }


    @Override
    public int hashCode() {
        int result = agentAddress.hashCode();
        result = 31 * result + packingAddress.hashCode();
        result = 31 * result + (rewardAddress != null ? rewardAddress.hashCode() : 0);
        result = 31 * result + deposit.hashCode();
        result = 31 * result + (seedNode ? 1 : 0);
        result = 31 * result + (pubKey != null ? pubKey.hashCode() : 0);
        return result;
    }
}
