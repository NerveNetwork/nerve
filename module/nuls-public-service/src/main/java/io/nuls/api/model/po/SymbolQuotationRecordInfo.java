package io.nuls.api.model.po;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-19 16:19
 * @Description: 功能描述
 */
public class SymbolQuotationRecordInfo extends StackSymbolPriceInfo{

    private String address;

    private String alias;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
