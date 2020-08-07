package io.nuls.api.db;

import io.nuls.api.model.po.AccountInfo;
import io.nuls.api.model.po.PageInfo;
import io.nuls.api.model.po.TxRelationInfo;
import io.nuls.api.model.po.mini.MiniAccountInfo;
import org.bson.conversions.Bson;

import java.math.BigInteger;
import java.util.Map;

public interface AccountService {

    void initCache();

    AccountInfo getAccountInfo(int chainId, String address);

    void saveAccounts(int chainId, Map<String, AccountInfo> accountInfoMap);

    PageInfo<AccountInfo> pageQuery(int chainId, int pageNumber, int pageSize);

    PageInfo<TxRelationInfo> pageAccountTxs(Bson filter,int chainId, String address, int pageIndex, int pageSize, int type, long startHeight, long endHeight, Integer assetChainId, Integer assetId);

    PageInfo<TxRelationInfo> getAcctTxs(int chainId, String address, int pageIndex, int pageSize, int type, long startHeight, long endHeight);

    PageInfo<MiniAccountInfo> getCoinRanking(int pageIndex, int pageSize, int chainId,int assetChainId,int assetId);

    BigInteger getAllAccountBalance(int chainId);

    BigInteger getAccountTotalBalance(int chainId, String address);

    void testBalance(int chainId);
}
