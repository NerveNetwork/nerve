package io.nuls.api.model.po.mini;

import java.util.List;

/**
 * @Author: zhoulijun
 * @Time: 2020/8/26 16:03
 * @Description: 批量退出
 */
public class BatchWithdrawStackingInfo extends CancelDepositInfo {

    List<String> joinHashList;

    public List<String> getJoinHashList() {
        return joinHashList;
    }

    public void setJoinHashList(List<String> joinHashList) {
        this.joinHashList = joinHashList;
    }
}
