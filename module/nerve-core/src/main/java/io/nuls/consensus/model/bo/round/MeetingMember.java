/*
 * *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2019 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */
package io.nuls.consensus.model.bo.round;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.parse.SerializeUtils;

/**
 * Rotation member information class
 * Round Membership Information Class
 *
 * @author tag
 * 2018/11/12
 */
@ApiModel(name = "Rotation member information")
public class MeetingMember implements Comparable<MeetingMember> {
    /**
    * Round index
    * Subscript in order
    * */
    @ApiModelProperty(description = "Round index")
    private long roundIndex;
    /**
    * Starting packaging time of the round
    * Round start packing time
    * */
    @ApiModelProperty(description = "Start time of round")
    private long roundStartTime;
    /**
    * The subscript of a node in a round（Which block is produced）
    * Subscription of Nodes in Rounds (Number of Blocks)
    * */
    @ApiModelProperty(description = "Which block did this node exit in this round")
    private int packingIndexOfRound;
    /**
    * Consensus Node Object
    * Consensus node object
    * */
    @ApiModelProperty(description = "Consensus node information")
    private Agent agent;
    /**
    * Sorting values
    * Ranking value
    * */
    @ApiModelProperty(description = "Sorting values")
    private String sortValue;

    /**
     * Calculate node packing and sorting values
     * Computing Packing Sort Value of Nodes
     * */
    public String getSortValue() {
        if (this.sortValue == null) {
            byte[] hash;
            if(roundIndex == ConsensusConstant.INIT_ROUND_INDEX){
                hash = agent.getPackingAddress();
            }else{
                hash = ByteUtils.concatenate(agent.getPackingAddress(), SerializeUtils.uint64ToByteArray(roundStartTime));
            }
            sortValue = Sha256Hash.twiceOf(hash).toString();
        }
        return sortValue;
    }

    public int getPackingIndexOfRound() {
        return packingIndexOfRound;
    }

    public void setPackingIndexOfRound(int packingIndexOfRound) {
        this.packingIndexOfRound = packingIndexOfRound;
    }

    @Override
    public int compareTo(MeetingMember o2) {
        return this.getSortValue().compareTo(o2.getSortValue());
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }


    public void setSortValue(String sortValue) {
        this.sortValue = sortValue;
    }

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(long roundStartTime) {
        this.roundStartTime = roundStartTime;
    }

    public long getRoundIndex() {
        return roundIndex;
    }

    public void setRoundIndex(long roundIndex) {
        this.roundIndex = roundIndex;
    }
}
