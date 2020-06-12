package io.nuls.api.db;

import io.nuls.api.model.po.AccountLedgerInfo;
import io.nuls.api.model.po.SymbolRegInfo;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface AccountLedgerService {

    void initCache();

    AccountLedgerInfo getAccountLedgerInfo(int chainId, String key);

    void saveLedgerList(int chainId, Map<String, AccountLedgerInfo> accountLedgerInfoMap);

    List<AccountLedgerInfo> getAccountLedgerInfoList(int chainId, String address);

    List<AccountLedgerInfo> getAccountCrossLedgerInfoList(int chainId, String address);

    /**
     * 汇总查询各个币种的持币地址数量
     * key : assetChainId-assetId
     * @param chainId
     * @return
     */
    Map<String, Long> aggAssetAddressCount(int chainId);

    /**
     * 汇总查询各个币种的资产总量
     * key : assetChainId-assetId
     * @param chainId
     * @return
     */
    Map<String, BigInteger> aggAssetBalanceTotal(int chainId);


    /**
     * 获取所有资产的余额数据
     * document :
     *    chainId
     *    assetId
     *    balance  (string)
     * @param chainId
     * @return
     */
    List<Document> getAllBalance(int chainId);



}
