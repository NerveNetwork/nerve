package io.nuls.api.constant;

/**
 * @Author: zhoulijun
 * @Time: 2020-02-27 15:56
 * @Description: 功能描述
 */
public final class DepositInfoType {

    /**
     * 创建共识节点
     */
    public static final int CREATE_AGENT = 1;

    /**
     * 注销节点
     */
    public static final int STOP_AGENT = -1;

    /**
     * 追加节点抵押
     */
    public static final int APPEND_AGENT_DEPOSIT = 2;

    /**
     * 减少节点抵押
     */
    public static final int REDUCE_AGENT_DEPOSIT = -2;

    /**
     * stacking
     */
    public static final int STACKING = 3;

    /**
     * 减少stacking
     */
    public static final int CANCEL_STACKING = -3;


}
