package io.nuls.consensus.service;

import io.nuls.core.basic.Result;

import java.util.Map;

/**
 * @author tag
 * 2019/04/01
 * */
public interface ChainService {
    /**
     * Query Yellow Card List
     * @param params
     * @return Result
     * */
    Result getPublishList(Map<String,Object> params);

    /**
     * Query consensus information across the entire network
     * @param params
     * @return Result
     * */
    Result getWholeInfo(Map<String,Object> params);

    /**
     * Query consensus information for specified accounts
     * @param params
     * @return Result
     * */
    Result getInfo(Map<String,Object> params);

    /**
     * Obtain current round information
     * @param params
     * @return Result
     * */
    Result getCurrentRoundInfo(Map<String,Object> params);

    /**
     * Obtain specified block rounds
     * @param params
     * @return Result
     * */
    Result getRoundMemberList(Map<String,Object> params);


    /**
     * Record evidence of bifurcation
     * @param params
     * @return Result
     * */
    Result addEvidenceRecord(Map<String,Object> params);

    /**
     * Is it a consensus node
     * @param params
     * @return Result
     * */
    Result isConsensusAgent(Map<String,Object> params);

    /**
     * Shuanghua transaction records
     * @param params
     * @return Result
     * */
    Result doubleSpendRecord(Map<String,Object> params);

    /**
     * Obtain common module recognition configuration information
     * @param params
     * @return Result
     * */
    Result getConsensusConfig(Map<String,Object> params);


    /**
     * Obtain seed node information
     * @param params
     * @return Result
     * */
    Result getSeedNodeInfo(Map<String,Object> params);

    /**
     * queryStackingInterest rate markup information
     * @param params
     * @return Result
     * */
    Result getRateAddition(Map<String,Object> params);

    /**
     * Query specified height block consensus reward units
     * @param params
     * @return Result
     * */
    Result getRewardUnit(Map<String,Object> params);
}
