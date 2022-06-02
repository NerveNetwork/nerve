package io.nuls.provider.model.dto;

import io.nuls.base.api.provider.consensus.facade.AgentInfo;
import io.nuls.base.api.provider.consensus.facade.ReduceNonceInfo;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;
import org.checkerframework.checker.units.qual.A;


@ApiModel(name = "节点详情数据")
public class AgentInfoDTO {
    private String agentHash;

    private String agentAddress;

    private String packingAddress;

    private String rewardAddress;

    private String deposit;

    private String agentName;

    private String agentId;

    private long time;

    private long blockHeight = -1L;

    private long delHeight = -1L;

    private int status;

    private double creditVal;

    private String txHash;

    private int memberCount;

    private String version;

    public AgentInfoDTO(AgentInfo info) {
        this.agentHash = info.getAgentHash();
        this.agentAddress = info.getAgentAddress();
        this.packingAddress = info.getPackingAddress();
        this.rewardAddress = info.getRewardAddress();
        this.deposit = info.getDeposit();
        this.agentName = info.getAgentName();
        this.agentId = info.getAgentId();
        this.time = info.getTime();
        this.blockHeight = info.getBlockHeight();
        this.delHeight = info.getDelHeight();
        this.status = info.getStatus();
        this.creditVal = info.getCreditVal();
        this.txHash = info.getTxHash();
        this.memberCount = info.getMemberCount();
        this.version = info.getVersion();
    }

    public String getAgentHash() {
        return agentHash;
    }

    public String getAgentAddress() {
        return agentAddress;
    }

    public String getPackingAddress() {
        return packingAddress;
    }

    public String getRewardAddress() {
        return rewardAddress;
    }

    public String getDeposit() {
        return deposit;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getAgentId() {
        return agentId;
    }

    public long getTime() {
        return time;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public long getDelHeight() {
        return delHeight;
    }

    public int getStatus() {
        return status;
    }

    public double getCreditVal() {
        return creditVal;
    }

    public String getTxHash() {
        return txHash;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public String getVersion() {
        return version;
    }
}
