package io.nuls.base.api.provider;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-07 15:11
 * @Description: 功能描述
 */
public class BaseReq {

    private Long timeOut;
    private Integer chainId;

    public Long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(Long timeOut) {
        this.timeOut = timeOut;
    }

    public Integer getChainId() {
        return chainId;
    }

    public void setChainId(Integer chainId) {
        this.chainId = chainId;
    }


}
