package io.nuls.api.model.po;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.nuls.api.ApiContext;
import io.nuls.api.constant.DepositFixedType;
import io.nuls.api.constant.DepositInfoType;
import io.nuls.api.utils.DBUtil;

import java.math.BigInteger;
import java.util.Objects;

public class DepositInfo extends TxDataInfo {

    private String key;

    private String txHash;

    private int assetChainId;

    private int assetId;

    private String symbol;

    private int decimal;

    private Long amount;

    private String agentHash;

    private String address;

    private long createTime;

    private String deleteKey;

    private long blockHeight;

    private long deleteHeight = 0;

    private BigInteger fee;

    @JsonIgnore
    private boolean isNew;

    /**
     * @see DepositInfoType
     */
    private int type;

    /**
     * @see io.nuls.api.constant.DepositFixedType#name()
     */
    private String fixedType = DepositFixedType.NONE.name();

    public void copyInfoWithDeposit(DepositInfo depositInfo) {
        this.amount = depositInfo.amount;
        this.address = depositInfo.address;
        this.agentHash = depositInfo.getAgentHash();
    }


    public static DepositInfo buildByAgent(BigInteger amount,AgentInfo agentInfo,TransactionInfo tx, int depositType){
        if(!(depositType == DepositInfoType.CREATE_AGENT || depositType == DepositInfoType.STOP_AGENT)){
            throw new IllegalArgumentException("参数错误，通过agentinfo生成抵押流水时只能传入DepositType.CREATE_AGENT或DepositType.STOP_AGENT" );
        }
        //将创建节点的初始押金写入抵押流水中
        DepositInfo depositInfo = new DepositInfo();
        depositInfo.setAssetChainId(ApiContext.defaultChainId);
        depositInfo.setAssetId(ApiContext.defaultAssetId);
        depositInfo.setSymbol(ApiContext.defaultSymbol);
        depositInfo.setAddress(agentInfo.getAgentAddress());
        depositInfo.setAgentHash(agentInfo.getTxHash());
        depositInfo.setBlockHeight(tx.getHeight());
        depositInfo.setCreateTime(tx.getCreateTime());
        depositInfo.setFee(tx.getFee().getValue());
        depositInfo.setNew(true);
        depositInfo.setAmount(amount.longValue());
        depositInfo.setTxHash(tx.getHash());
        depositInfo.setType(depositType);
        depositInfo.setDeleteHeight(BigInteger.ONE.negate().longValue());
        depositInfo.setKey(DBUtil.getDepositKey(tx.getHash(), depositInfo.getAddress()));
        depositInfo.setFixedType(DepositFixedType.NONE.name());
        return depositInfo;
    }

    @Override
    public String toString() {
        return new StringBuilder("{")
                .append("\"key\":\"")
                .append(key).append('\"')
                .append(",\"txHash\":\"")
                .append(txHash).append('\"')
                .append(",\"assetChainId\":")
                .append(assetChainId)
                .append(",\"assetId\":")
                .append(assetId)
                .append(",\"amount\":")
                .append(amount)
                .append(",\"agentHash\":\"")
                .append(agentHash).append('\"')
                .append(",\"address\":\"")
                .append(address).append('\"')
                .append(",\"createTime\":")
                .append(createTime)
                .append(",\"deleteKey\":\"")
                .append(deleteKey).append('\"')
                .append(",\"blockHeight\":")
                .append(blockHeight)
                .append(",\"deleteHeight\":")
                .append(deleteHeight)
                .append(",\"fee\":")
                .append(fee)
                .append(",\"isNew\":")
                .append(isNew)
                .append(",\"type\":")
                .append(type)
                .append('}').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DepositInfo)) return false;

        DepositInfo that = (DepositInfo) o;

        if (assetChainId != that.assetChainId) return false;
        if (assetId != that.assetId) return false;
        if (createTime != that.createTime) return false;
        if (blockHeight != that.blockHeight) return false;
        if (deleteHeight != that.deleteHeight) return false;
        if (isNew != that.isNew) return false;
        if (type != that.type) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (txHash != null ? !txHash.equals(that.txHash) : that.txHash != null) return false;
        if (amount != null ? !amount.equals(that.amount) : that.amount != null) return false;
        if (agentHash != null ? !agentHash.equals(that.agentHash) : that.agentHash != null) return false;
        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        if (deleteKey != null ? !deleteKey.equals(that.deleteKey) : that.deleteKey != null) return false;
        return fee != null ? fee.equals(that.fee) : that.fee == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (txHash != null ? txHash.hashCode() : 0);
        result = 31 * result + assetChainId;
        result = 31 * result + assetId;
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (agentHash != null ? agentHash.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (int) (createTime ^ (createTime >>> 32));
        result = 31 * result + (deleteKey != null ? deleteKey.hashCode() : 0);
        result = 31 * result + (int) (blockHeight ^ (blockHeight >>> 32));
        result = 31 * result + (int) (deleteHeight ^ (deleteHeight >>> 32));
        result = 31 * result + (fee != null ? fee.hashCode() : 0);
        result = 31 * result + (isNew ? 1 : 0);
        result = 31 * result + type;
        return result;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
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
//
//    public Long getAmount() {
//        return amount;
//    }

    public BigInteger getAmount() {
        return BigInteger.valueOf(amount);
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public void setAmount(BigInteger amount) {
        Objects.requireNonNull(amount);
        this.amount = amount.longValue();
    }

    public String getAgentHash() {
        return agentHash;
    }

    public void setAgentHash(String agentHash) {
        this.agentHash = agentHash;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getDeleteKey() {
        return deleteKey;
    }

    public void setDeleteKey(String deleteKey) {
        this.deleteKey = deleteKey;
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

    public BigInteger getFee() {
        return fee;
    }

    public void setFee(BigInteger fee) {
        this.fee = fee;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getFixedType() {
        return fixedType;
    }

    public void setFixedType(String fixedType) {
        this.fixedType = fixedType;
    }

    public int getDecimal() {
        return decimal;
    }

    public void setDecimal(int decimal) {
        this.decimal = decimal;
    }
}
