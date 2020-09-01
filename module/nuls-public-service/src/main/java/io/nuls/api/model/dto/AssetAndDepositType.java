package io.nuls.api.model.dto;

/**
 * @Author: zhoulijun
 * @Time: 2020/8/11 17:54
 * @Description: 功能描述
 */
public class AssetAndDepositType {

    private String symbol;

    private String type;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetAndDepositType)) return false;

        AssetAndDepositType that = (AssetAndDepositType) o;

        if (symbol != null ? !symbol.equals(that.symbol) : that.symbol != null) return false;
        return type != null ? type.equals(that.type) : that.type == null;
    }

    @Override
    public int hashCode() {
        int result = symbol != null ? symbol.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return symbol + "-" + type;
    }
}
