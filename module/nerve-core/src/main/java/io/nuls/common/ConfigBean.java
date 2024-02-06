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
package io.nuls.common;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.consensus.model.bo.config.AssetsStakingLimitCfg;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * Transaction module chain setting
 * @author: Charlie
 * @date: 2019/03/14
 */
public class ConfigBean extends BaseNulsData {

    /** chain id*/
    private int chainId;
    /** assets id*/
    private int assetId;
    /*-------------------------[Block]-----------------------------*/
    /**
     * Block size threshold
     */
    private long blockMaxSize;
    /**
     * Network reset threshold
     */
    private long resetTime;
    /**
     * Chain switching occurs when the fork chain is several blocks higher than the main chain
     */
    private byte chainSwtichThreshold;
    /**
     * Forked chain、The maximum cache size of orphan chain blocks
     */
    private int cacheSize;
    /**
     * Scope of receiving new blocks
     */
    private int heightRange;
    /**
     * Maximum block size for each rollback
     */
    private int maxRollback;
    /**
     * Consistent node ratio
     */
    private byte consistencyNodePercent;
    /**
     * Minimum number of nodes for system operation
     */
    private byte minNodeAmount;
    /**
     * How many blocks are downloaded from a node each time
     */
    private byte downloadNumber;
    /**
     * The maximum length of the extended field in the block header
     */
    private int extendMaxSize;
    /**
     * To prevent malicious nodes from leaving the block prematurely,Set this parameter
     * Discard the block if its timestamp is greater than the current time
     */
    private int validBlockInterval;
    /**
     * How many cell blocks can be cached at most when the system is running normally and received from other nodes
     */
    private byte smallBlockCache;
    /**
     * Orphan Chain Maximum Age
     */
    private byte orphanChainMaxAge;
    /**
     * log level
     */
    private String logLevel;
    /**
     * The timeout for downloading a single block
     */
    private int singleDownloadTimeout;

    /**
     * Waiting for the time interval for network stability
     */
    private int waitNetworkInterval;

    /**
     * Genesis block configuration file path
     */
    private String genesisBlockPath;

    /**
     * Maximum number of cached block bytes during block synchronization process
     */
    private long cachedBlockSizeLimit;
    /*-------------------------[Protocol]-----------------------------*/
    /**
     * Statistical interval
     */
    private short interval;
    /**
     * The minimum effective ratio within each statistical interval
     */
    private byte effectiveRatioMinimum;
    /**
     * The minimum number of consecutive intervals that a protocol must meet in order to take effect
     */
    private short continuousIntervalCountMinimum;
    /*-------------------------[CrossChain]-----------------------------*/
    /**
     * Minimum number of links
     * Minimum number of links
     * */
    private int minNodes;

    /**
     * Maximum number of links
     * */
    private int maxOutAmount;

    /**
     * Maximum number of links
     * */
    private int maxInAmount;

    /**
     * How many blocks are packaged for cross chain transactions and broadcast to other chains
     * */
    private int sendHeight;

    /**
     * Byzantine proportion
     * */
    private int byzantineRatio;

    /**
     * Minimum number of signatures
     * */
    private int minSignature;

    /**
     * Main network verifier information
     * */
    private String verifiers;

    /**
     * Main network Byzantine proportion
     * */
    private int mainByzantineRatio;

    /**
     * Maximum number of signature verifications on the main network
     * */
    private int maxSignatureCount;

    /**
     * List of main network validators
     * */
    private Set<String> verifierSet = new HashSet<>();

    /*-------------------------[Consensus]-----------------------------*/
    /**
     * Packaging interval time
     * Packing interval time
     */
    private long packingInterval;

    /**
     * Obtaining red card deposit lock up time
     * Lock-in time to get a red card margin
     */
    private long redPublishLockTime;

    /**
     * Cancellation of node margin locking time
     * Log-off node margin locking time
     */
    private long stopAgentLockTime;

    /**
     * Reduce margin lock up time
     * Reduce margin lock-in time
     */
    private long reducedDepositLockTime;

    /**
     * Minimum margin value for creating nodes
     * Minimum margin for creating nodes
     */
    private BigInteger depositMin;

    /**
     * Maximum margin value for nodes
     * Maximum margin for creating nodes
     */
    private BigInteger depositMax;

    /**
     * Node participation consensus node election minimum commission
     * Minimum Trust Fund for node participating in consensus node campaign
     */
    private BigInteger packDepositMin;

    /**
     * Minimum amount entrusted
     * Minimum amount entrusted
     */
    private BigInteger entrustMin;

    /**
     * Seed node
     * Seed node
     */
    private String seedNodes;

    /**
     * The corresponding public key of the seed node
     */
    private String pubKeyList;

    /**
     * Block node password
     */
    private String password;

    /**
     * Maximum value of packaged blocks
     */
    private long blockConsensusMaxSize;

    /**
     * Create node assetsID
     * agent assets id
     */
    private int agentAssetId;

    /**
     * Create a node asset chainID
     * Create node asset chain ID
     */
    private int agentChainId;

    /**
     * Consensus reward assetsID
     * Award asset chain ID
     */
    private int awardAssetId;

    /**
     * Transaction fee unit price
     * Transaction fee unit price
     */
    private long feeUnit;

    /**
     * Total shrinkage
     * Total inflation amount
     */
    private BigInteger totalInflationAmount;

    /**
     * Initial inflation amount
     * Initial Inflation Amount
     */
    private BigInteger inflationAmount;

    /**
     * Inflation start time
     */
    private long initHeight;

    /**
     * Deflationary ratio
     */
    private double deflationRatio;

    /**
     * Deflation interval time
     */
    private long deflationHeightInterval;
    /**
     * Minimum amount of additional margin
     * Minimum amount of additional margin
     */
    private BigInteger appendAgentDepositMin;

    /**
     * Minimum amount of withdrawal margin
     * Minimum amount of withdrawal deposit
     */
    private BigInteger reduceAgentDepositMin;

    private int byzantineRate;
    /**
     * Maximum number of consensus nodes
     */
    private int agentCountMax;

    /**
     * The weight base of the main asset in this chain
     */
    private double localAssertBase;

    /**
     * Node margin base
     */
    private double agentDepositBase;
    /**
     * Virtual Bank Margin Base
     */
    private double superAgentDepositBase;
    /**
     * Reserve node margin base
     */
    private double reservegentDepositBase;


    private int maxCoinToOfCoinbase;
    private long minRewardHeight;
    private long depositAwardChangeHeight;
    private long depositVerifyHeight;
    private Long v1_3_0Height;
    private Long v1_6_0Height;
    private Long v1_7_0Height;
    private BigInteger minStakingAmount;
    private BigInteger minAppendAndExitAmount;
    private Integer exitStakingLockHours;

    private List<AssetsStakingLimitCfg> limitCfgList;

    public List<AssetsStakingLimitCfg> getLimitCfgList() {
        return limitCfgList;
    }

    public Map<String, Integer> weightMap = new HashMap<>();

    public void putWeight(int chainId, int assetId, int weight) {
        weightMap.put(chainId + "-" + assetId, weight);
    }

    public int getWeight(int chainId, int assetId) {
        Integer weight = weightMap.get(chainId + "-" + assetId);
        if (null == weight) {
            return 1;
        }
        return weight.intValue();
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
        //No longer configuring the seed node address, but calculating it from the public key
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

    public long getBlockConsensusMaxSize() {
        return blockConsensusMaxSize;
    }

    public void setBlockConsensusMaxSize(long blockConsensusMaxSize) {
        this.blockConsensusMaxSize = blockConsensusMaxSize;
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

    public Long getV1_7_0Height() {
        return v1_7_0Height;
    }

    public void setV1_7_0Height(Long v1_7_0Height) {
        this.v1_7_0Height = v1_7_0Height;
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


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        // block
        stream.writeUint16(chainId);
        stream.writeUint16(assetId);
        stream.writeUint32(blockMaxSize);
        stream.writeUint32(resetTime);
        stream.writeByte(chainSwtichThreshold);
        stream.writeUint16(cacheSize);
        stream.writeUint16(heightRange);
        stream.writeUint16(maxRollback);
        stream.writeByte(consistencyNodePercent);
        stream.writeByte(minNodeAmount);
        stream.writeByte(downloadNumber);
        stream.writeUint16(extendMaxSize);
        stream.writeUint16(validBlockInterval);
        stream.writeByte(smallBlockCache);
        stream.writeByte(orphanChainMaxAge);
        stream.writeString(logLevel);
        stream.writeUint16(singleDownloadTimeout);
        stream.writeUint16(waitNetworkInterval);
        stream.writeString(genesisBlockPath);
        stream.writeUint32(cachedBlockSizeLimit);
        // protocol
        stream.writeShort(interval);
        stream.writeByte(effectiveRatioMinimum);
        stream.writeShort(continuousIntervalCountMinimum);
        // cross
        stream.writeUint16(minNodes);
        stream.writeUint16(maxOutAmount);
        stream.writeUint16(maxInAmount);
        stream.writeUint16(sendHeight);
        stream.writeUint16(byzantineRatio);
        stream.writeUint16(minSignature);
        stream.writeString(verifiers);
        stream.writeUint16(mainByzantineRatio);
        stream.writeUint16(maxSignatureCount);
        int registerCount = verifierSet == null ? 0 : verifierSet.size();
        stream.writeVarInt(registerCount);
        if(verifierSet != null){
            for (String registerAgent:verifierSet) {
                stream.writeString(registerAgent);
            }
        }
        // consensus
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
        stream.writeString(password);
        stream.writeUint48(blockConsensusMaxSize);
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
        stream.writeDouble(0);
        stream.writeDouble(localAssertBase);
        stream.writeDouble(agentDepositBase);
        stream.writeDouble(superAgentDepositBase);
        stream.writeDouble(reservegentDepositBase);
        stream.writeUint32(this.maxCoinToOfCoinbase);
        stream.writeUint32(this.minRewardHeight);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        //block
        this.chainId = byteBuffer.readUint16();
        this.assetId = byteBuffer.readUint16();
        this.blockMaxSize = byteBuffer.readUint32();
        this.resetTime = byteBuffer.readUint32();
        this.chainSwtichThreshold = byteBuffer.readByte();
        this.cacheSize = byteBuffer.readUint16();
        this.heightRange = byteBuffer.readUint16();
        this.maxRollback = byteBuffer.readUint16();
        this.consistencyNodePercent = byteBuffer.readByte();
        this.minNodeAmount = byteBuffer.readByte();
        this.downloadNumber = byteBuffer.readByte();
        this.extendMaxSize = byteBuffer.readUint16();
        this.validBlockInterval = byteBuffer.readUint16();
        this.smallBlockCache = byteBuffer.readByte();
        this.orphanChainMaxAge = byteBuffer.readByte();
        this.logLevel = byteBuffer.readString();
        this.singleDownloadTimeout = byteBuffer.readUint16();
        this.waitNetworkInterval = byteBuffer.readUint16();
        this.genesisBlockPath = byteBuffer.readString();
        this.cachedBlockSizeLimit = byteBuffer.readUint32();
        //protocol
        this.interval = byteBuffer.readShort();
        this.effectiveRatioMinimum = byteBuffer.readByte();
        this.continuousIntervalCountMinimum = byteBuffer.readShort();
        //cross
        this.minNodes = byteBuffer.readUint16();
        this.maxOutAmount = byteBuffer.readUint16();
        this.maxInAmount = byteBuffer.readUint16();
        this.sendHeight = byteBuffer.readUint16();
        this.byzantineRatio = byteBuffer.readUint16();
        this.minNodes = byteBuffer.readUint16();
        this.verifiers = byteBuffer.readString();
        this.mainByzantineRatio = byteBuffer.readUint16();
        this.maxSignatureCount = byteBuffer.readUint16();
        int registerCount = (int) byteBuffer.readVarInt();
        if(registerCount > 0){
            Set<String> verifierSet = new HashSet<>();
            for (int i = 0; i < registerCount; i++) {
                verifierSet.add(byteBuffer.readString());
            }
            this.verifierSet = verifierSet;
        }
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
        this.password = byteBuffer.readString();
        this.blockConsensusMaxSize = byteBuffer.readUint48();
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
        byteBuffer.readDouble();
        this.localAssertBase = byteBuffer.readDouble();
        this.agentDepositBase = byteBuffer.readDouble();
        this.superAgentDepositBase = byteBuffer.readDouble();
        this.reservegentDepositBase = byteBuffer.readDouble();
        this.maxCoinToOfCoinbase = (int) byteBuffer.readUint32();
        this.minRewardHeight = byteBuffer.readUint32();
    }

    @Override
    public int size() {
        // block
        int size = 36;
        size += SerializeUtils.sizeOfString(logLevel);
        size += SerializeUtils.sizeOfString(genesisBlockPath);
        // protocol
        size += 5;
        // cross
        size += SerializeUtils.sizeOfUint16() * 8;
        size += SerializeUtils.sizeOfString(verifiers);
        size += SerializeUtils.sizeOfVarInt(verifierSet == null ? 0 : verifierSet.size());
        if(verifierSet != null){
            for (String verifier:verifierSet) {
                size += SerializeUtils.sizeOfString(verifier);
            }
        }
        // consensus
        size += SerializeUtils.sizeOfUint48();
        size += SerializeUtils.sizeOfUint32() * 6;
        size += SerializeUtils.sizeOfDouble(deflationRatio);
        size += SerializeUtils.sizeOfBigInteger() * 8;
        size += SerializeUtils.sizeOfString(seedNodes);
        size += SerializeUtils.sizeOfString(pubKeyList);
        size += SerializeUtils.sizeOfUint16() * 5;
        size += SerializeUtils.sizeOfString(password);
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfDouble(localAssertBase);
        size += SerializeUtils.sizeOfDouble(0.0);
        size += SerializeUtils.sizeOfDouble(agentDepositBase);
        size += SerializeUtils.sizeOfDouble(superAgentDepositBase);
        size += SerializeUtils.sizeOfDouble(reservegentDepositBase);
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint32();
        return  size;
    }

    public ConfigBean() {
    }

    public ConfigBean(int chainId, int assetId) {
        this.chainId = chainId;
        this.assetId = assetId;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public long getBlockMaxSize() {
        return blockMaxSize;
    }

    public void setBlockMaxSize(long blockMaxSize) {
        this.blockMaxSize = blockMaxSize;
    }

    public long getResetTime() {
        return resetTime;
    }

    public void setResetTime(long resetTime) {
        this.resetTime = resetTime;
    }

    public byte getChainSwtichThreshold() {
        return chainSwtichThreshold;
    }

    public void setChainSwtichThreshold(byte chainSwtichThreshold) {
        this.chainSwtichThreshold = chainSwtichThreshold;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getHeightRange() {
        return heightRange;
    }

    public void setHeightRange(int heightRange) {
        this.heightRange = heightRange;
    }

    public int getMaxRollback() {
        return maxRollback;
    }

    public void setMaxRollback(int maxRollback) {
        this.maxRollback = maxRollback;
    }

    public byte getConsistencyNodePercent() {
        return consistencyNodePercent;
    }

    public void setConsistencyNodePercent(byte consistencyNodePercent) {
        this.consistencyNodePercent = consistencyNodePercent;
    }

    public byte getMinNodeAmount() {
        return minNodeAmount;
    }

    public void setMinNodeAmount(byte minNodeAmount) {
        this.minNodeAmount = minNodeAmount;
    }

    public byte getDownloadNumber() {
        return downloadNumber;
    }

    public void setDownloadNumber(byte downloadNumber) {
        this.downloadNumber = downloadNumber;
    }

    public int getExtendMaxSize() {
        return extendMaxSize;
    }

    public void setExtendMaxSize(int extendMaxSize) {
        this.extendMaxSize = extendMaxSize;
    }

    public int getValidBlockInterval() {
        return validBlockInterval;
    }

    public void setValidBlockInterval(int validBlockInterval) {
        this.validBlockInterval = validBlockInterval;
    }

    public byte getSmallBlockCache() {
        return smallBlockCache;
    }

    public void setSmallBlockCache(byte smallBlockCache) {
        this.smallBlockCache = smallBlockCache;
    }

    public byte getOrphanChainMaxAge() {
        return orphanChainMaxAge;
    }

    public void setOrphanChainMaxAge(byte orphanChainMaxAge) {
        this.orphanChainMaxAge = orphanChainMaxAge;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public int getSingleDownloadTimeout() {
        return singleDownloadTimeout;
    }

    public void setSingleDownloadTimeout(int singleDownloadTimeout) {
        this.singleDownloadTimeout = singleDownloadTimeout;
    }

    public int getWaitNetworkInterval() {
        return waitNetworkInterval;
    }

    public void setWaitNetworkInterval(int waitNetworkInterval) {
        this.waitNetworkInterval = waitNetworkInterval;
    }

    public String getGenesisBlockPath() {
        return genesisBlockPath;
    }

    public void setGenesisBlockPath(String genesisBlockPath) {
        this.genesisBlockPath = genesisBlockPath;
    }

    public long getCachedBlockSizeLimit() {
        return cachedBlockSizeLimit;
    }

    public void setCachedBlockSizeLimit(long cachedBlockSizeLimit) {
        this.cachedBlockSizeLimit = cachedBlockSizeLimit;
    }

    public short getInterval() {
        return interval;
    }

    public void setInterval(short interval) {
        this.interval = interval;
    }

    public byte getEffectiveRatioMinimum() {
        return effectiveRatioMinimum;
    }

    public void setEffectiveRatioMinimum(byte effectiveRatioMinimum) {
        this.effectiveRatioMinimum = effectiveRatioMinimum;
    }

    public short getContinuousIntervalCountMinimum() {
        return continuousIntervalCountMinimum;
    }

    public void setContinuousIntervalCountMinimum(short continuousIntervalCountMinimum) {
        this.continuousIntervalCountMinimum = continuousIntervalCountMinimum;
    }

    public int getMinNodes() {
        return minNodes;
    }

    public void setMinNodes(int minNodes) {
        this.minNodes = minNodes;
    }

    public int getMaxOutAmount() {
        return maxOutAmount;
    }

    public void setMaxOutAmount(int maxOutAmount) {
        this.maxOutAmount = maxOutAmount;
    }

    public int getMaxInAmount() {
        return maxInAmount;
    }

    public void setMaxInAmount(int maxInAmount) {
        this.maxInAmount = maxInAmount;
    }

    public int getSendHeight() {
        return sendHeight;
    }

    public void setSendHeight(int sendHeight) {
        this.sendHeight = sendHeight;
    }

    public int getByzantineRatio() {
        return byzantineRatio;
    }

    public void setByzantineRatio(int byzantineRatio) {
        this.byzantineRatio = byzantineRatio;
    }

    public int getMinSignature() {
        return minSignature;
    }

    public void setMinSignature(int minSignature) {
        this.minSignature = minSignature;
    }

    public String getVerifiers() {
        return verifiers;
    }

    public void setVerifiers(String verifiers) {
        this.verifiers = verifiers;
    }

    public int getMainByzantineRatio() {
        return mainByzantineRatio;
    }

    public void setMainByzantineRatio(int mainByzantineRatio) {
        this.mainByzantineRatio = mainByzantineRatio;
    }

    public int getMaxSignatureCount() {
        return maxSignatureCount;
    }

    public void setMaxSignatureCount(int maxSignatureCount) {
        this.maxSignatureCount = maxSignatureCount;
    }

    public Set<String> getVerifierSet() {
        return verifierSet;
    }

    public void setVerifierSet(Set<String> verifierSet) {
        this.verifierSet = verifierSet;
    }
}
