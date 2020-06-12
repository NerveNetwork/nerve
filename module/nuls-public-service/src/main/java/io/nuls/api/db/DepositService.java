package io.nuls.api.db;

import io.nuls.api.constant.DepositInfoType;
import io.nuls.api.model.po.DepositInfo;
import io.nuls.api.model.po.PageInfo;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface DepositService {

    DepositInfo getDepositInfoByKey(int chainId, String key);

    DepositInfo getDepositInfoByHash(int chainId, String hash);

    List<DepositInfo> getDepositListByAgentHash(int chainId, String hash);

    PageInfo<DepositInfo> pageDepositListByAgentHash(int chainID, String hash, int pageIndex, int pageSize);

    List<DepositInfo> getDepositListByHash(int chainID, String hash);

    void rollbackDeposit(int chainId, List<DepositInfo> depositInfoList);

    void saveDepositList(int chainId, List<DepositInfo> depositInfoList);

    List<DepositInfo> getDepositList(int chainId, long startHeight);

    List<DepositInfo> getDepositList(int chainId, long startHeight, int... types);

    BigInteger getDepositAmount(int chainId, String address, String agentHash);

    PageInfo<DepositInfo> pageDepositListByAgentHash(int chainId, String hash, int pageIndex, int pageSize, Integer... type);

    List<String> getAgentHashList(int chainId, String address);

    PageInfo<DepositInfo> getDepositListByAddress(int chainId,String agentHashm, int pageIndex, int pageSize);

    BigInteger getAgentDepositTotal(int chainId,long height);

    BigInteger getStackingTotalByNVT(int chainId,long height);

    Map<String,BigInteger> getStackingTotalByNVTGroupSymbol(int chainId, long height);

    /**
     * 查询各种币种的合计
     * @param chainId
     * @return
     */
    List<DepositInfo> getDepositSumList(int chainId,int... depositInfoTypes);

    /**
     * 获取指定地址参与stacking的列表
     * @param chainId
     * @param pageIndex
     * @param pageSize
     * @param address
     * @param isActive
     * @return
     */
    PageInfo<DepositInfo> getStackingListByAddress(int chainId, int pageIndex, int pageSize, String address, boolean isActive);

    PageInfo<DepositInfo> getStackRecordByAddress(int chainId, int pageIndex, int pageSize, String address);

}
