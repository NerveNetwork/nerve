package io.nuls.base.api.provider.converter.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-17 17:53
 * @Description: 功能描述
 */
public class GetVirtualBankInfoReq extends BaseReq {

    boolean balance;

    public GetVirtualBankInfoReq() {
        this.balance = true;
    }

    public GetVirtualBankInfoReq(boolean balance) {
        this.balance = balance;
    }

    public boolean isBalance() {
        return balance;
    }

    public void setBalance(boolean balance) {
        this.balance = balance;
    }
}
