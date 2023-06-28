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
package network.nerve.pocbft.model.bo.round;

import io.nuls.core.model.DateUtils;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Address;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;
import io.nuls.core.rpc.model.TypeDescriptor;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import network.nerve.pocbft.utils.ConsensusNetUtil;

import java.util.*;

/**
 * 轮次信息类
 * Information about rotation
 *
 * @author tag
 * 2018/11/12
 */
@ApiModel(name = "轮次信息")
public class MeetingRound {
    /**
     * 本地打包节点在当前轮次的下标
     * Subscription of Local Packing Node in Current Round
     */
    @ApiModelProperty(description = "轮次下标")
    private long index;
    /**
     * 当前轮次开始打包时间
     * Current Round Start Packing Time
     */
    @ApiModelProperty(description = "轮次开始时间")
    private long startTime;
    /**
     * 当前轮次打包节点数量
     * Number of Packing Nodes in Current Round
     */
    @ApiModelProperty(description = "本轮次出块节点数")
    private int memberCount;
    /**
     * 当前轮次打包成员列表
     * Current rounds packaged membership list
     */
    @ApiModelProperty(description = "本轮次出块成员信息", type = @TypeDescriptor(value = List.class, collectionElement = MeetingMember.class))
    private List<MeetingMember> memberList;
    /**
     * 上一轮轮次信息
     * Last round of information
     */
    @ApiModelProperty(description = "上一轮信息")
    private MeetingRound preRound;
    /**
     * 本地打包成员信息
     * Locally packaged member information
     */
    @ApiModelProperty(description = "当前节点出块信息")
    private MeetingMember localMember;

    /**
     * 本轮次出块节点地址列表
     * Node address list of block out this round
     */
    private Set<String> memberAddressSet;
    private int packingIndexOfRound = 1;
    private boolean confirmed;
    /**
     * 当前轮次的延迟时间
     */
    private long delayedSeconds;

    public MeetingRound getPreRound() {
        return preRound;
    }

    public void setPreRound(MeetingRound preRound) {
        this.preRound = preRound;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getStartTimeMills() {
        return startTime * 1000;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public Set<String> getMemberAddressSet() {
        return memberAddressSet;
    }

    public void setMemberAddressSet(Set<String> memberAddressSet) {
        this.memberAddressSet = memberAddressSet;
    }

    /**
     * 初始化轮次信息
     * Initialization Round Information
     *
     * @param memberList 打包成员信息列表/Packaged Member Information List
     */
    public void init(List<MeetingMember> memberList) {
        assert (startTime > 0L);
        this.memberList = memberList;
        if (null == memberList || memberList.isEmpty()) {
            throw new NulsRuntimeException(ConsensusErrorCode.DATA_ERROR);
        }
        Collections.sort(memberList);
        this.memberCount = memberList.size();
        Set<String> memberAddressList = new HashSet<>();
        MeetingMember member;
        for (int i = 0; i < memberCount; i++) {
            member = memberList.get(i);
            member.setRoundStartTime(this.getStartTime());
            member.setPackingIndexOfRound(i + 1);
            memberAddressList.add(AddressTool.getStringAddressByBytes(memberList.get(i).getAgent().getPackingAddress()));
        }
        this.memberAddressSet = memberAddressList;
    }

    public MeetingMember getMemberByOrder(int order) {
        if (order == 0) {
            throw new NulsRuntimeException(ConsensusErrorCode.DATA_ERROR);
        }
        if (null == memberList || memberList.isEmpty()) {
            throw new NulsRuntimeException(ConsensusErrorCode.DATA_ERROR);
        }
        return this.memberList.get(order - 1);
    }

    public MeetingMember getMemberByPackingAddress(byte[] address) {
        for (MeetingMember member : memberList) {
            if (Arrays.equals(address, member.getAgent().getPackingAddress())) {
                return member;
            }
        }
        return null;
    }

    public MeetingMember getMemberByOrder(byte[] address, Chain chain) {
        for (MeetingMember member : memberList) {
            if (Arrays.equals(address, member.getAgent().getPackingAddress()) && validAccount(chain, AddressTool.getStringAddressByBytes(member.getAgent().getPackingAddress()))) {
                return member;
            }
        }
        return null;
    }

    private boolean validAccount(Chain chain, String address) {
        try {
            return CallMethodUtils.accountValid(chain.getConfig().getChainId(), address, chain.getConfig().getPassword());
        } catch (Exception e) {
            Log.error(e);
        }
        return false;
    }

    /**
     * 根据节点地址获取节点对应的打包信息
     * Get the packing information corresponding to the node according to the address of the node
     */
    public MeetingMember getMemberByAgentAddress(byte[] address) {
        for (MeetingMember member : memberList) {
            if (Arrays.equals(address, member.getAgent().getAgentAddress())) {
                return member;
            }
        }
        return null;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }


    public List<MeetingMember> getMemberList() {
        return memberList;
    }

    public MeetingMember getLocalMember() {
        return localMember;
    }

    public void calcLocalPacker(List<byte[]> localAddressList, Chain chain) {
        for (byte[] address : localAddressList) {
            MeetingMember member = getMemberByOrder(address, chain);
            if (null != member) {
                localMember = member;
                break;
            }
        }

    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (MeetingMember member : this.getMemberList()) {
            str.append(Address.fromHashs(member.getAgent().getPackingAddress()).getBase58());
            str.append(" ,order:" + member.getPackingIndexOfRound());
            str.append(",creditVal:" + member.getAgent().getRealCreditVal());
            str.append("\n");
        }
        if (null == this.getPreRound()) {
            return ("\nround:index:" + this.getIndex() + " ,\n     start:" + DateUtils.timeStamp2Str(this.getStartTime() * 1000)
                    + ", \nnowNetTime:" + DateUtils.timeStamp2Str(NulsDateUtils.getCurrentTimeMillis()) + "\ndelayedTime:" + this.delayedSeconds + "\npackingIndex:" + this.packingIndexOfRound +
                    "\nisConfirmed:" + this.confirmed + " ,\nmembers:\n" + str);
        } else {
            return ("\nround:index:" + this.getIndex() + " ,\npreIndex:" + this.getPreRound().getIndex() + " , \n     start:" + DateUtils.timeStamp2Str(this.getStartTime() * 1000)
                    + ", \nnowNetTime:" + DateUtils.timeStamp2Str(NulsDateUtils.getCurrentTimeMillis()) + "\ndelayedTime:" + this.delayedSeconds + "\npackingIndex:" + this.packingIndexOfRound +
                    "\nisConfirmed:" + this.confirmed + " , \nmembers:\n" + str);
        }
    }

    public void setPackingIndexOfRound(int packingIndexOfRound) {
        this.packingIndexOfRound = packingIndexOfRound;
    }

    public int getPackingIndexOfRound() {
        return packingIndexOfRound;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public long getDelayedSeconds() {
        return delayedSeconds;
    }

    public void setDelayedSeconds(long delayedSeconds) {
        if (delayedSeconds < 0) {
            delayedSeconds = 0;
        }
        this.delayedSeconds = delayedSeconds;
    }

    public void resetMemberOrder() {
        for (MeetingMember meetingMember : this.memberList) {
            meetingMember.setRoundStartTime(this.getStartTime());
            meetingMember.setSortValue(null);
        }
        this.init(this.memberList);
    }
}
