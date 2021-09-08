package network.nerve.converter.heterogeneouschain.trx.model;

import org.tron.trident.proto.Chain;

import java.math.BigInteger;

public class TrxTransaction {

    private Chain.Transaction tx;
    private String hash;
    private String from;
    private String to;
    private BigInteger value;
    private String input;
    private Chain.Transaction.Contract.ContractType type;

    private String gas;
    private String blockHash;
    private Long blockNumber;
    private String r;
    private String s;
    private long v;

    public TrxTransaction() {
    }

    public TrxTransaction(Chain.Transaction tx, String hash, String from, String to, BigInteger value, String input,
        Chain.Transaction.Contract.ContractType type) {
        this.tx = tx;
        this.hash = hash;
        this.from = from;
        this.to = to;
        this.value = value;
        this.input = input;
        this.type = type;
    }

    public Chain.Transaction getTx() {
        return tx;
    }

    public void setTx(Chain.Transaction tx) {
        this.tx = tx;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public Chain.Transaction.Contract.ContractType getType() {
        return type;
    }

    public void setType(Chain.Transaction.Contract.ContractType type) {
        this.type = type;
    }

    public String getGas() {
        return gas;
    }

    public void setGas(String gas) {
        this.gas = gas;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getR() {
        return r;
    }

    public void setR(String r) {
        this.r = r;
    }

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

    public long getV() {
        return v;
    }

    public void setV(long v) {
        this.v = v;
    }
}
