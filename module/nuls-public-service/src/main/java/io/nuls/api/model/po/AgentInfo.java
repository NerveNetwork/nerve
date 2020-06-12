package io.nuls.api.model.po;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigInteger;

public class AgentInfo extends TxDataInfo {

    /**
     * agent HASH
     */
    private String txHash;

    /**
     * txhash后8位
     */
    private String agentId;

    /**
     * 创建节点的地址
     */
    private String agentAddress;

    /**
     * 出块地址
     */
    private String packingAddress;

    /**
     * 奖励接收地址
     */
    private String rewardAddress;

    /**
     * 节点别名
     */
    private String agentAlias;

    /**
     * 节点创建时间
     */
    private long createTime;

    // 0:待共识，1:共识中，2:退出共识
    private int status;

    /**
     * 抵押金额
     */
    private BigInteger deposit;

    /**
     * 信用值
     */
    private double creditValue;

    /**
     * 总出块数
     */
    private long totalPackingCount;

    private double lostRate;


    private long lastRewardHeight;

    private String deleteHash;

    private long blockHeight;

    private long deleteHeight;

    /**
     * 一共获取了多少共识奖励
     */
    private BigInteger reward;

    private int yellowCardCount;

    private int version;

    private int type;

    @JsonIgnore
    private boolean isNew;

    /**
     * 是否是虚拟银行节点
     */
    private boolean isBankNode;

    /**
     * 排名
     * 此字段不持久化，动态计算后返回
     */
    private Integer ranking;

    public AgentInfo() {

    }

    public void init() {
        reward = BigInteger.ZERO;
        deposit = BigInteger.ZERO;
    }

    public boolean isBankNode() {
        return isBankNode;
    }

    public void setBankNode(boolean bankNode) {
        isBankNode = bankNode;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

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

    public String getRewardAddress() {
        return rewardAddress;
    }

    public void setRewardAddress(String rewardAddress) {
        this.rewardAddress = rewardAddress;
    }

    public String getAgentAlias() {
        return agentAlias;
    }

    public void setAgentAlias(String agentAlias) {
        this.agentAlias = agentAlias;
    }


    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }



    public double getCreditValue() {
        return creditValue;
    }

    public void setCreditValue(double creditValue) {
        this.creditValue = creditValue;
    }

    public long getTotalPackingCount() {
        return totalPackingCount;
    }

    public void setTotalPackingCount(long totalPackingCount) {
        this.totalPackingCount = totalPackingCount;
    }

    public double getLostRate() {
        return lostRate;
    }

    public void setLostRate(double lostRate) {
        this.lostRate = lostRate;
    }

    public long getLastRewardHeight() {
        return lastRewardHeight;
    }

    public void setLastRewardHeight(long lastRewardHeight) {
        this.lastRewardHeight = lastRewardHeight;
    }

    public String getDeleteHash() {
        return deleteHash;
    }

    public void setDeleteHash(String deleteHash) {
        this.deleteHash = deleteHash;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public long getDeleteHeight() {
        return deleteHeight;
    }

    public void setDeleteHeight(long deleteHeight) {
        this.deleteHeight = deleteHeight;
    }



    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getYellowCardCount() {
        return yellowCardCount;
    }

    public void setYellowCardCount(int yellowCardCount) {
        this.yellowCardCount = yellowCardCount;
    }

    public BigInteger getDeposit() {
        return deposit;
    }

    public void setDeposit(BigInteger deposit) {
        this.deposit = deposit;
    }

    public BigInteger getReward() {
        return reward;
    }

    public void setReward(BigInteger reward) {
        this.reward = reward;
    }

    public AgentInfo copy() {
        AgentInfo agentInfo = new AgentInfo();
        agentInfo.txHash = this.txHash;
        agentInfo.agentId = this.agentId;
        agentInfo.agentAddress = this.agentAddress;
        agentInfo.packingAddress = this.packingAddress;
        agentInfo.rewardAddress = this.rewardAddress;
        agentInfo.agentAlias = this.agentAlias;
        agentInfo.createTime = this.createTime;
        agentInfo.status = this.status;
        agentInfo.deposit = new BigInteger(this.deposit.toString());
        agentInfo.creditValue = this.creditValue;
        agentInfo.totalPackingCount = this.totalPackingCount;
        agentInfo.lostRate = this.lostRate;
        agentInfo.lastRewardHeight = this.lastRewardHeight;
        agentInfo.deleteHash = this.deleteHash;
        agentInfo.blockHeight = this.blockHeight;
        agentInfo.deleteHeight = this.deleteHeight;
        agentInfo.reward = new BigInteger(this.reward.toString());
        agentInfo.yellowCardCount = this.yellowCardCount;
        agentInfo.version = this.version;
        agentInfo.type = this.type;
        agentInfo.isBankNode = this.isBankNode;
        return agentInfo;
    }

    public Integer getRanking() {
        return ranking;
    }

    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }
}
