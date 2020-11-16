package network.nerve.pocbft.model.bo.config;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * 共识模块配置类
 * Consensus Module Configuration Class
 *
 * @author tag
 * 2018/11/7
 */
public class ChainConfig extends BaseNulsData {
    /**
     * 本链资产ID
     * assets id
     */
    private int assetId;

    /**
     * 本链链ID
     * chain id
     */
    private int chainId;

    /**
     * 打包间隔时间
     * Packing interval time
     */
    private long packingInterval;

    /**
     * 获得红牌保证金锁定时间
     * Lock-in time to get a red card margin
     */
    private long redPublishLockTime;

    /**
     * 注销节点保证金锁定时间
     * Log-off node margin locking time
     */
    private long stopAgentLockTime;

    /**
     * 减少保证金锁定时间
     * Reduce margin lock-in time
     */
    private long reducedDepositLockTime;

    /**
     * 创建节点的保证金最小值
     * Minimum margin for creating nodes
     */
    private BigInteger depositMin;

    /**
     * 节点的保证金最大值
     * Maximum margin for creating nodes
     */
    private BigInteger depositMax;

    /**
     * 节点参与共识节点竞选最小委托金
     * Minimum Trust Fund for node participating in consensus node campaign
     */
    private BigInteger packDepositMin;

    /**
     * 委托最小金额
     * Minimum amount entrusted
     */
    private BigInteger entrustMin;

    /**
     * 追加保证金最小金额
     * Minimum amount of additional margin
     */
    private BigInteger appendAgentDepositMin;

    /**
     * 退出保证金最小金额
     * Minimum amount of withdrawal deposit
     */
    private BigInteger reduceAgentDepositMin;

    private int byzantineRate;

    /**
     * 种子节点
     * Seed node
     */
    private String seedNodes;

    /**
     * 种子节点对应公钥
     */
    private String pubKeyList;

    /**
     * 出块节点密码
     */
    private String password;

    /**
     * 打包区块最大值
     */
    private long blockMaxSize;

    /**
     * 创建节点资产ID
     * agent assets id
     */
    private int agentAssetId;

    /**
     * 创建节点资产链ID
     * Create node asset chain ID
     */
    private int agentChainId;

    /**
     * 共识奖励资产ID
     * Award asset chain ID
     */
    private int awardAssetId;

    /**
     * 交易手续费单价
     * Transaction fee unit price
     */
    private long feeUnit;

    /**
     * 总通缩量
     * Total inflation amount
     */
    private BigInteger totalInflationAmount;

    /**
     * 初始通胀金额
     * Initial Inflation Amount
     */
    private BigInteger inflationAmount;

    /**
     * 通胀开始时间
     */
    private long initHeight;

    /**
     * 通缩比例
     */
    private double deflationRatio;

    /**
     * 通缩间隔时间
     */
    private long deflationHeightInterval;

    /**
     * 共识节点最大数量
     */
    private int agentCountMax;

    /**
     * Nuls和Nerve的权重基数
     */
    private double mainAssertBase;

    /**
     * 本链主资产的权重基数
     */
    private double localAssertBase;

    /**
     * 节点保证金基数
     */
    private double agentDepositBase;
    /**
     * 虚拟银行保证金基数
     */
    private double superAgentDepositBase;
    /**
     * 后备节点保证金基数
     */
    private double reservegentDepositBase;


    private int maxCoinToOfCoinbase;
    private long minRewardHeight;
    private long depositAwardChangeHeight;
    private long depositVerifyHeight;
    private Long v1_3_0Height;
    private Long v1_6_0Height;
    private BigInteger minStakingAmount;
    private BigInteger minAppendAndExitAmount;
    private Integer exitStakingLockHours;

    private List<AssetsStakingLimitCfg> limitCfgList;

    public List<AssetsStakingLimitCfg> getLimitCfgList() {
        return limitCfgList;
    }

    public void setLimitCfgList(List<AssetsStakingLimitCfg> limitCfgList) {
        this.limitCfgList = limitCfgList;
    }

    public long getDepositAwardChangeHeight() {
        return depositAwardChangeHeight;
    }

    public void setDepositAwardChangeHeight(long depositAwardChangeHeight) {
        this.depositAwardChangeHeight = depositAwardChangeHeight;
    }

    public long getDepositVerifyHeight() {
        return depositVerifyHeight;
    }

    public void setDepositVerifyHeight(long depositVerifyHeight) {
        this.depositVerifyHeight = depositVerifyHeight;
    }

    public int getMaxCoinToOfCoinbase() {
        return maxCoinToOfCoinbase;
    }

    public void setMaxCoinToOfCoinbase(int maxCoinToOfCoinbase) {
        this.maxCoinToOfCoinbase = maxCoinToOfCoinbase;
    }

    public long getMinRewardHeight() {
        return minRewardHeight;
    }

    public void setMinRewardHeight(long minRewardHeight) {
        this.minRewardHeight = minRewardHeight;
    }

    public long getPackingInterval() {
        return packingInterval;
    }

    public void setPackingInterval(long packingInterval) {
        this.packingInterval = packingInterval;
    }


    public long getRedPublishLockTime() {
        return redPublishLockTime;
    }

    public void setRedPublishLockTime(long redPublishLockTime) {
        this.redPublishLockTime = redPublishLockTime;
    }

    public long getStopAgentLockTime() {
        return stopAgentLockTime;
    }

    public void setStopAgentLockTime(long stopAgentLockTime) {
        this.stopAgentLockTime = stopAgentLockTime;
    }

    public BigInteger getDepositMin() {
        return depositMin;
    }

    public void setDepositMin(BigInteger depositMin) {
        this.depositMin = depositMin;
    }

    public BigInteger getDepositMax() {
        return depositMax;
    }

    public void setDepositMax(BigInteger depositMax) {
        this.depositMax = depositMax;
    }

    public String getSeedNodes() {
        //不再配置种子节点地址，而是从公钥计算得到
        if (StringUtils.isBlank(seedNodes)) {
            String[] pubkeys = this.pubKeyList.split(",");
            StringBuilder ss = new StringBuilder("");
            for (String pub : pubkeys) {
                ss.append(",").append(AddressTool.getAddressString(HexUtil.decode(pub), this.chainId));
            }
            this.seedNodes = ss.toString().substring(1);
        }
        return seedNodes;
    }

    public String getPubKeyList() {
        return pubKeyList;
    }

    public void setPubKeyList(String pubKeyList) {
        this.pubKeyList = pubKeyList;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public BigInteger getInflationAmount() {
        return inflationAmount;
    }

    public void setInflationAmount(BigInteger inflationAmount) {
        this.inflationAmount = inflationAmount;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getBlockMaxSize() {
        return blockMaxSize;
    }

    public void setBlockMaxSize(long blockMaxSize) {
        this.blockMaxSize = blockMaxSize;
    }

    public int getAgentAssetId() {
        return agentAssetId;
    }

    public void setAgentAssetId(int agentAssetId) {
        this.agentAssetId = agentAssetId;
    }

    public int getAgentChainId() {
        return agentChainId;
    }

    public void setAgentChainId(int agentChainId) {
        this.agentChainId = agentChainId;
    }

    public int getAwardAssetId() {
        return awardAssetId;
    }

    public void setAwardAssetId(int awardAssetId) {
        this.awardAssetId = awardAssetId;
    }

    public long getFeeUnit() {
        return feeUnit;
    }

    public void setFeeUnit(long feeUnit) {
        this.feeUnit = feeUnit;
    }

    public long getInitHeight() {
        return initHeight;
    }

    public void setInitHeight(long initHeight) {
        this.initHeight = initHeight;
    }

    public long getDeflationHeightInterval() {
        return deflationHeightInterval;
    }

    public void setDeflationHeightInterval(long deflationHeightInterval) {
        this.deflationHeightInterval = deflationHeightInterval;
    }

    public double getMainAssertBase() {
        return mainAssertBase;
    }

    public void setMainAssertBase(double mainAssertBase) {
        this.mainAssertBase = mainAssertBase;
    }

    public double getAgentDepositBase() {
        return agentDepositBase;
    }

    public void setAgentDepositBase(double agentDepositBase) {
        this.agentDepositBase = agentDepositBase;
    }

    public double getSuperAgentDepositBase() {
        return superAgentDepositBase;
    }

    public void setSuperAgentDepositBase(double superAgentDepositBase) {
        this.superAgentDepositBase = superAgentDepositBase;
    }

    public double getDeflationRatio() {
        return deflationRatio;
    }

    public void setDeflationRatio(double deflationRatio) {
        this.deflationRatio = deflationRatio;
    }

    public BigInteger getTotalInflationAmount() {
        return totalInflationAmount;
    }

    public void setTotalInflationAmount(BigInteger totalInflationAmount) {
        this.totalInflationAmount = totalInflationAmount;
    }

    public BigInteger getPackDepositMin() {
        return packDepositMin;
    }

    public void setPackDepositMin(BigInteger packDepositMin) {
        this.packDepositMin = packDepositMin;
    }


    public long getReducedDepositLockTime() {
        return reducedDepositLockTime;
    }

    public void setReducedDepositLockTime(long reducedDepositLockTime) {
        this.reducedDepositLockTime = reducedDepositLockTime;
    }

    public BigInteger getEntrustMin() {
        return entrustMin;
    }

    public void setEntrustMin(BigInteger entrustMin) {
        this.entrustMin = entrustMin;
    }

    public BigInteger getAppendAgentDepositMin() {
        return appendAgentDepositMin;
    }

    public void setAppendAgentDepositMin(BigInteger appendAgentDepositMin) {
        this.appendAgentDepositMin = appendAgentDepositMin;
    }

    public BigInteger getReduceAgentDepositMin() {
        return reduceAgentDepositMin;
    }

    public void setReduceAgentDepositMin(BigInteger reduceAgentDepositMin) {
        this.reduceAgentDepositMin = reduceAgentDepositMin;
    }

    public int getByzantineRate() {
        return byzantineRate;
    }

    public void setByzantineRate(int byzantineRate) {
        this.byzantineRate = byzantineRate;
    }

    public int getAgentCountMax() {
        return agentCountMax;
    }

    public void setAgentCountMax(int agentCountMax) {
        this.agentCountMax = agentCountMax;
    }

    public double getLocalAssertBase() {
        return localAssertBase;
    }

    public void setLocalAssertBase(double localAssertBase) {
        this.localAssertBase = localAssertBase;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint32(packingInterval);
        stream.writeUint32(redPublishLockTime);
        stream.writeUint32(stopAgentLockTime);
        stream.writeUint32(reducedDepositLockTime);
        stream.writeBigInteger(depositMin);
        stream.writeBigInteger(depositMax);
        stream.writeBigInteger(packDepositMin);
        stream.writeBigInteger(entrustMin);
        stream.writeString(seedNodes);
        stream.writeString(pubKeyList);
        stream.writeUint16(assetId);
        stream.writeUint16(chainId);
        stream.writeString(password);
        stream.writeUint48(blockMaxSize);
        stream.writeUint16(agentAssetId);
        stream.writeUint16(agentChainId);
        stream.writeUint16(awardAssetId);
        stream.writeUint32(feeUnit);
        stream.writeBigInteger(totalInflationAmount);
        stream.writeBigInteger(inflationAmount);
        stream.writeUint32(initHeight);
        stream.writeDouble(deflationRatio);
        stream.writeUint32(deflationHeightInterval);
        stream.writeBigInteger(appendAgentDepositMin);
        stream.writeBigInteger(reduceAgentDepositMin);
        stream.writeUint16(byzantineRate);
        stream.writeUint16(agentCountMax);
        stream.writeDouble(mainAssertBase);
        stream.writeDouble(localAssertBase);
        stream.writeDouble(agentDepositBase);
        stream.writeDouble(superAgentDepositBase);
        stream.writeDouble(reservegentDepositBase);
        stream.writeUint32(this.maxCoinToOfCoinbase);
        stream.writeUint32(this.minRewardHeight);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.packingInterval = byteBuffer.readUint32();
        this.redPublishLockTime = byteBuffer.readUint32();
        this.stopAgentLockTime = byteBuffer.readUint32();
        this.reducedDepositLockTime = byteBuffer.readUint32();
        this.depositMin = byteBuffer.readBigInteger();
        this.depositMax = byteBuffer.readBigInteger();
        this.packDepositMin = byteBuffer.readBigInteger();
        this.entrustMin = byteBuffer.readBigInteger();
        this.seedNodes = byteBuffer.readString();
        this.pubKeyList = byteBuffer.readString();
        this.assetId = byteBuffer.readUint16();
        this.chainId = byteBuffer.readUint16();
        this.password = byteBuffer.readString();
        this.blockMaxSize = byteBuffer.readUint48();
        this.agentAssetId = byteBuffer.readUint16();
        this.agentChainId = byteBuffer.readUint16();
        this.awardAssetId = byteBuffer.readUint16();
        this.feeUnit = byteBuffer.readUint32();
        this.totalInflationAmount = byteBuffer.readBigInteger();
        this.inflationAmount = byteBuffer.readBigInteger();
        this.initHeight = byteBuffer.readUint32();
        this.deflationRatio = byteBuffer.readDouble();
        this.deflationHeightInterval = byteBuffer.readUint32();
        this.appendAgentDepositMin = byteBuffer.readBigInteger();
        this.reduceAgentDepositMin = byteBuffer.readBigInteger();
        this.byzantineRate = byteBuffer.readUint16();
        this.agentCountMax = byteBuffer.readUint16();
        this.mainAssertBase = byteBuffer.readDouble();
        this.localAssertBase = byteBuffer.readDouble();
        this.agentDepositBase = byteBuffer.readDouble();
        this.superAgentDepositBase = byteBuffer.readDouble();
        this.reservegentDepositBase = byteBuffer.readDouble();
        this.maxCoinToOfCoinbase = (int) byteBuffer.readUint32();
        this.minRewardHeight = byteBuffer.readUint32();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfUint48();
        size += SerializeUtils.sizeOfUint32() * 6;
        size += SerializeUtils.sizeOfDouble(deflationRatio);
        size += SerializeUtils.sizeOfBigInteger() * 8;
        size += SerializeUtils.sizeOfString(seedNodes);
        size += SerializeUtils.sizeOfString(pubKeyList);
        size += SerializeUtils.sizeOfUint16() * 5;
        size += SerializeUtils.sizeOfString(password);
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfDouble(localAssertBase);
        size += SerializeUtils.sizeOfDouble(mainAssertBase);
        size += SerializeUtils.sizeOfDouble(agentDepositBase);
        size += SerializeUtils.sizeOfDouble(superAgentDepositBase);
        size += SerializeUtils.sizeOfDouble(reservegentDepositBase);
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint32();
        return size;
    }


    public long getPackingIntervalMills() {
        return 1000 * this.getPackingInterval();
    }

    public double getReservegentDepositBase() {
        return reservegentDepositBase;
    }

    public void setReservegentDepositBase(double reservegentDepositBase) {
        this.reservegentDepositBase = reservegentDepositBase;
    }

    public void setV130Height(Long v130Height) {
        this.v1_3_0Height = v130Height;
    }

    public Long getV130Height() {
        return v1_3_0Height;
    }


    public Long getV1_6_0Height() {
        return v1_6_0Height;
    }

    public void setV1_6_0Height(Long v1_6_0Height) {
        this.v1_6_0Height = v1_6_0Height;
    }

    public void setMinStakingAmount(BigInteger minStakingAmount) {
        this.minStakingAmount = minStakingAmount;
    }

    public BigInteger getMinStakingAmount() {
        return minStakingAmount;
    }

    public void setMinAppendAndExitAmount(BigInteger minAppendAndExitAmount) {
        this.minAppendAndExitAmount = minAppendAndExitAmount;
    }

    public BigInteger getMinAppendAndExitAmount() {
        return minAppendAndExitAmount;
    }

    public void setExitStakingLockHours(Integer exitStakingLockHours) {
        this.exitStakingLockHours = exitStakingLockHours;
    }

    public Integer getExitStakingLockHours() {
        return exitStakingLockHours;
    }
}
