package network.nerve.pocbft.service;

import io.nuls.core.basic.Result;

import java.util.Map;

/**
 * @author tag
 * 2019/04/01
 * */
public interface ChainService {
    /**
     * 查询黄牌列表
     * @param params
     * @return Result
     * */
    Result getPublishList(Map<String,Object> params);

    /**
     * 查询全网共识信息
     * @param params
     * @return Result
     * */
    Result getWholeInfo(Map<String,Object> params);

    /**
     * 查询指定账户的共识信息
     * @param params
     * @return Result
     * */
    Result getInfo(Map<String,Object> params);

    /**
     * 获取当前轮次信息
     * @param params
     * @return Result
     * */
    Result getCurrentRoundInfo(Map<String,Object> params);

    /**
     * 获取指定区块轮次
     * @param params
     * @return Result
     * */
    Result getRoundMemberList(Map<String,Object> params);


    /**
     * 记录分叉证据
     * @param params
     * @return Result
     * */
    Result addEvidenceRecord(Map<String,Object> params);

    /**
     * 是否为共识节点
     * @param params
     * @return Result
     * */
    Result isConsensusAgent(Map<String,Object> params);

    /**
     * 双花交易记录
     * @param params
     * @return Result
     * */
    Result doubleSpendRecord(Map<String,Object> params);

    /**
     * 获取共模块识配置信息
     * @param params
     * @return Result
     * */
    Result getConsensusConfig(Map<String,Object> params);


    /**
     * 获取种子节点信息
     * @param params
     * @return Result
     * */
    Result getSeedNodeInfo(Map<String,Object> params);

    /**
     * 查询Stacking利率加成信息
     * @param params
     * @return Result
     * */
    Result getRateAddition(Map<String,Object> params);

    /**
     * 查询指定高度区块共识奖励单位
     * @param params
     * @return Result
     * */
    Result getRewardUnit(Map<String,Object> params);
}
