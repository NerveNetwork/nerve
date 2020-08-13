/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.api.rpc.controller;

import com.mongodb.client.model.Filters;
import io.nuls.api.ApiContext;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.db.*;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.*;
import io.nuls.api.model.po.mini.MiniAccountInfo;
import io.nuls.api.model.po.mini.MiniCrossChainTransactionInfo;
import io.nuls.api.model.rpc.*;
import io.nuls.api.utils.LoggerUtil;
import io.nuls.api.utils.PropertyUtils;
import io.nuls.api.utils.VerifyUtils;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.converter.ConverterService;
import io.nuls.base.api.provider.converter.facade.GetHeterogeneousAssetInfoReq;
import io.nuls.base.api.provider.converter.facade.HeterogeneousAssetInfo;
import io.nuls.base.api.provider.ledger.LedgerProvider;
import io.nuls.base.api.provider.ledger.facade.GetAssetListReq;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.MapUtils;
import org.bson.conversions.Bson;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Eva
 */
@Controller
public class AccountController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private BlockService blockHeaderService;
    @Autowired
    private ChainService chainService;
    @Autowired
    private AccountLedgerService accountLedgerService;
    @Autowired
    private AliasService aliasService;

    @Autowired
    SymbolRegService symbolRegService;

    @Autowired
    private StatisticalService statisticalService;
    @Autowired
    private ConverterTxService converterTxService;

    @Autowired
    DepositService depositService;

    LedgerProvider ledgerProvider = ServiceManager.get(LedgerProvider.class);

    ConverterService converterService = ServiceManager.get(ConverterService.class);

    @RpcMethod("getAccountList")
    public RpcResult getAccountList(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId, pageNumber, pageSize;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }

        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        RpcResult result = new RpcResult();
        PageInfo<AccountInfo> pageInfo;
        if (CacheManager.isChainExist(chainId)) {
            pageInfo = accountService.pageQuery(chainId, pageNumber, pageSize);
        } else {
            pageInfo = new PageInfo<>(pageNumber, pageSize);
        }
        result.setResult(pageInfo);
        return result;

    }


    @RpcMethod("getAccountInnetTxs")
    public RpcResult getAccountInnetTxs(List<Object> params) {
        Bson filter = Filters.and(
                Filters.ne("type", TxType.CROSS_CHAIN),
                Filters.ne("type", TxType.RECHARGE),
                Filters.ne("type", TxType.WITHDRAWAL));
        if (params.size() > 4) {
            try {
                int type = (int) params.get(4);
                if (type > 0) {
                    filter = Filters.and(Filters.eq("type", type), filter);
                }
            } catch (Exception e) {
                return RpcResult.paramError("[type] is inValid");
            }
        }
        return getAccountTxsForCrossChain(params, filter);
    }


    @RpcMethod("getAccountOutnetTxs")
    public RpcResult getAccountOutnetTxs(List<Object> params) {
        int type = 0;
        try {
            type = (int) params.get(4);
        } catch (Exception e) {
            return RpcResult.paramError("[type] is inValid");
        }
        Bson filter;
        if (type == 1) {
            filter = Filters.eq("type", TxType.CROSS_CHAIN);
        } else if (type == 2) {
            filter = Filters.or(
                    Filters.eq("type", TxType.RECHARGE),
                    Filters.eq("type", TxType.WITHDRAWAL));
        } else {
            filter = Filters.or(
                    Filters.eq("type", TxType.CROSS_CHAIN),
                    Filters.eq("type", TxType.RECHARGE),
                    Filters.eq("type", TxType.WITHDRAWAL));
        }
        RpcResult listRpc = getAccountTxsForCrossChain(params, filter);
        if (listRpc.getResult() != null) {
            PageInfo pageInfo = (PageInfo) listRpc.getResult();
            List<MiniCrossChainTransactionInfo> list = (List<MiniCrossChainTransactionInfo>) pageInfo.getList().stream().map(data -> {
                TxRelationInfo d = (TxRelationInfo) data;
                ConverterTxInfo converterTxInfo = converterTxService.getByTxHash(ApiContext.defaultChainId, d.getTxHash());
                MiniCrossChainTransactionInfo miniCrossChainTransactionInfo = new MiniCrossChainTransactionInfo();
                PropertyUtils.copyProperties(miniCrossChainTransactionInfo, d);
                miniCrossChainTransactionInfo.setValue(d.getValues());
                miniCrossChainTransactionInfo.setHash(d.getTxHash());
                if (converterTxInfo != null) {
                    miniCrossChainTransactionInfo.setCrossChainType(converterTxInfo.getCrossChainType());
                    miniCrossChainTransactionInfo.setConverterType(converterTxInfo.getConverterType());
                    miniCrossChainTransactionInfo.setOuterTxHash(converterTxInfo.getOuterTxHash());
                }
                SymbolRegInfo symbolRegInfo = symbolRegService.get(d.getChainId(), d.getAssetId());
                miniCrossChainTransactionInfo.setSymbol(symbolRegInfo.getSymbol());
                miniCrossChainTransactionInfo.setNetwork(symbolRegInfo.getFullName());
                miniCrossChainTransactionInfo.setDecimals(symbolRegInfo.getDecimals());
                miniCrossChainTransactionInfo.setIcon(symbolRegInfo.getIcon());
                return miniCrossChainTransactionInfo;
            }).collect(Collectors.toList());
            return RpcResult.success(new PageInfo<>(pageInfo.getPageNumber(), pageInfo.getPageSize(), pageInfo.getTotalCount(), list));
        }
        return listRpc;
    }

    public RpcResult getAccountTxsForCrossChain(List<Object> params, Bson crossExpand) {
        VerifyUtils.verifyParams(params, 5);
        int chainId, pageNumber, pageSize, type = 0;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }
        try {
            address = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        RpcResult result = new RpcResult();
        try {
            PageInfo<TxRelationInfo> pageInfo;
            if (CacheManager.isChainExist(chainId)) {
                pageInfo = accountService.pageAccountTxs(crossExpand, chainId, address, pageNumber, pageSize, type, -1, -1, null, null);
                result.setResult(pageInfo);
            } else {
                result.setResult(new PageInfo<>(pageNumber, pageSize));
            }
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
        }
        return result;
    }


    @RpcMethod("getAccountTxs")
    public RpcResult getAccountTxs(List<Object> params) {
        VerifyUtils.verifyParams(params, 7);
        int chainId, pageNumber, pageSize, type;
        Integer assetChainId = null, assetId = null;
        String address;
        Bson crossTxExpandFilter = null;
        long startHeight, endHeight;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }
        try {
            address = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            type = (int) params.get(4);
        } catch (Exception e) {
            return RpcResult.paramError("[type] is inValid");
        }
        try {
            startHeight = Long.parseLong("" + params.get(5));
        } catch (Exception e) {
            return RpcResult.paramError("[startHeight] is invalid");
        }
        try {
            endHeight = Long.parseLong("" + params.get(6));
        } catch (Exception e) {
            return RpcResult.paramError("[endHeight] is invalid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (params.size() > 7) {
            try {
                assetChainId = (int) params.get(7);
            } catch (Exception e) {
                return RpcResult.paramError("[assetChainId] is inValid");
            }
        }
        if (params.size() > 8) {
            try {
                assetId = (int) params.get(8);
            } catch (Exception e) {
                return RpcResult.paramError("[assetId] is inValid");
            }
        }

        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        RpcResult result = new RpcResult();
        try {
            PageInfo<TxRelationInfo> pageInfo;
            if (CacheManager.isChainExist(chainId)) {
                pageInfo = accountService.pageAccountTxs(null, chainId, address, pageNumber, pageSize, type, startHeight, endHeight, assetChainId, assetId);
            } else {
                pageInfo = new PageInfo<>(pageNumber, pageSize);
            }
            result.setResult(pageInfo);
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
        }
        return result;
    }

    @RpcMethod("getAcctTxs")
    public RpcResult getAcctTxs(List<Object> params) {
        VerifyUtils.verifyParams(params, 7);
        int chainId, pageNumber, pageSize, type;
        String address;
        long startHeight, endHeight;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }
        try {
            address = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            type = (int) params.get(4);
        } catch (Exception e) {
            return RpcResult.paramError("[type] is inValid");
        }
        try {
            startHeight = Long.parseLong("" + params.get(5));
        } catch (Exception e) {
            return RpcResult.paramError("[startHeight] is invalid");
        }
        try {
            endHeight = Long.parseLong("" + params.get(6));
        } catch (Exception e) {
            return RpcResult.paramError("[endHeight] is invalid");
        }


        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }

        RpcResult result = new RpcResult();
        PageInfo<TxRelationInfo> pageInfo;
        if (CacheManager.isChainExist(chainId)) {
            pageInfo = accountService.getAcctTxs(chainId, address, pageNumber, pageSize, type, startHeight, endHeight);
        } else {
            pageInfo = new PageInfo<>(pageNumber, pageSize);
        }
        result.setResult(pageInfo);
        return result;

    }

    @RpcMethod("getAccount")
    public RpcResult getAccount(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }

        RpcResult result = new RpcResult();
        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.dataNotFound();
        }
        AccountInfo accountInfo = accountService.getAccountInfo(chainId, address);
        if (accountInfo == null) {
            accountInfo = new AccountInfo(address);
            accountInfo.setSymbol(ApiContext.defaultSymbol);
            Map<String, Object> res = MapUtils.beanToLinkedMap(accountInfo);
            res.put("stackingTotal", 0);
            return result.setResult(res);
        } else {
            AssetInfo defaultAsset = apiCache.getChainInfo().getDefaultAsset();
            BalanceInfo balanceInfo = WalletRpcHandler.getAccountBalance(chainId, address, defaultAsset.getChainId(), defaultAsset.getAssetId());
            accountInfo.setBalance(balanceInfo.getBalance());
            BigInteger stackingTotal = depositService.getStackingTotalAndTransferNVT(chainId, address);
            accountInfo.setTimeLock(balanceInfo.getTimeLock());
            Map<String, Object> res = MapUtils.beanToLinkedMap(accountInfo);
            res.put("stackingTotal", stackingTotal);
            statisticalService
                    .getAssetSnapshotAggSum(chainId, 4)
                    .stream()
                    .filter(d -> d.getAssetId() == ApiContext.defaultAssetId && d.getAssetChainId() == ApiContext.defaultChainId)
                    .findFirst().ifPresent(assetSnapshotInfo -> {
                BigDecimal rate = new BigDecimal(balanceInfo.getTotalBalance()).divide(new BigDecimal(assetSnapshotInfo.getTotal()), MathContext.DECIMAL64).setScale(4, RoundingMode.HALF_DOWN);
                res.put("rate", rate);
            });
            return result.setResult(res);
        }

    }

    @RpcMethod("getAccountByAlias")
    public RpcResult getAccountByAlias(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String alias;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            alias = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[alias] is inValid");
        }
        RpcResult result = new RpcResult();
        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.dataNotFound();
        }
        AliasInfo aliasInfo = aliasService.getByAlias(chainId, alias);
        if (aliasInfo == null) {
            return RpcResult.dataNotFound();
        }
        AccountInfo accountInfo = accountService.getAccountInfo(chainId, aliasInfo.getAddress());
        if (accountInfo == null) {
            return RpcResult.dataNotFound();
        } else {
            AssetInfo defaultAsset = apiCache.getChainInfo().getDefaultAsset();
            BalanceInfo balanceInfo = WalletRpcHandler.getAccountBalance(chainId, aliasInfo.getAddress(), defaultAsset.getChainId(), defaultAsset.getAssetId());
            accountInfo.setBalance(balanceInfo.getBalance());
//            accountInfo.setConsensusLock(balanceInfo.getConsensusLock());
            accountInfo.setTimeLock(balanceInfo.getTimeLock());
        }
        accountInfo.setSymbol(ApiContext.defaultSymbol);
        return result.setResult(accountInfo);

    }

    @RpcMethod("getCoinRanking")
    public RpcResult getCoinRanking(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId, pageNumber, pageSize, assetChainId = ApiContext.defaultChainId, assetId = ApiContext.defaultAssetId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }

        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }

        if (params.size() > 3) {
            try {
                assetChainId = (int) params.get(3);
            } catch (Exception e) {
                return RpcResult.paramError("[assetChainId] is inValid");
            }
        }
        if (params.size() > 4) {
            try {
                assetId = (int) params.get(4);
            } catch (Exception e) {
                return RpcResult.paramError("[assetId] is inValid");
            }
        }

        PageInfo<MiniAccountInfo> pageInfo;
        if (CacheManager.isChainExist(chainId)) {
            pageInfo = accountService.getCoinRanking(pageNumber, pageSize, chainId, assetChainId, assetId);
            if (pageInfo.getList().isEmpty()) {
                return new RpcResult().setResult(pageInfo);
            }
            SymbolRegInfo symbolRegInfo = symbolRegService.get(assetChainId, assetId);
            final int filterAssetChainId = assetChainId, filterAsstId = assetId;
            Optional<AssetSnapshotInfo> assetSnapshotInfoList = statisticalService.getAssetSnapshotAggSum(chainId, 4)
                    .stream().filter(d -> d.getAssetId() == filterAsstId && d.getAssetChainId() == filterAssetChainId).findFirst();
            for (var info : pageInfo.getList()) {
                BalanceInfo balanceInfo = WalletRpcHandler.getAccountBalance(chainId, info.getAddress(), assetChainId, assetId);
                info.setLockBalance(balanceInfo.getConsensusLock());
                info.setDecimals(symbolRegInfo.getDecimals());
                AliasInfo aliasInfo = aliasService.getAliasByAddress(chainId, info.getAddress());
                if (aliasInfo != null) {
                    info.setAlias(aliasInfo.getAlias());
                }
                assetSnapshotInfoList.ifPresent(assetSnapshotInfo -> {
                    BigDecimal rate = new BigDecimal(balanceInfo.getTotalBalance()).divide(new BigDecimal(assetSnapshotInfo.getTotal()), MathContext.DECIMAL64).setScale(4, RoundingMode.HALF_DOWN);
                    info.setRate(rate);
                });
            }
        } else {
            pageInfo = new PageInfo<>(pageNumber, pageSize);
        }
        return new RpcResult().setResult(pageInfo);
    }

    @RpcMethod("getAccountFreezes")
    public RpcResult getAccountFreezes(List<Object> params) {
        VerifyUtils.verifyParams(params, 6);
        int chainId, assetChainId, assetId, pageNumber, pageSize;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            assetChainId = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[assetChainId] is inValid");
        }
        try {
            assetId = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[assetChainId] is inValid");
        }
        try {
            address = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            pageNumber = (int) params.get(4);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(5);
        } catch (Exception e) {
            return RpcResult.paramError("[sortType] is inValid");
        }

        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }

        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }

        PageInfo<FreezeInfo> pageInfo;
        if (CacheManager.isChainExist(chainId)) {
            Result<PageInfo<FreezeInfo>> result = WalletRpcHandler.getFreezeList(chainId, assetChainId, assetId, address, pageNumber, pageSize);
            if (result.isFailed()) {
                return RpcResult.failed(result);
            }
            pageInfo = result.getData();
            return RpcResult.success(pageInfo);
        } else {
            pageInfo = new PageInfo<>(pageNumber, pageSize);
            return RpcResult.success(pageInfo);
        }
    }

    @RpcMethod("getAccountBalance")
    public RpcResult getAccountBalance(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId, assetChainId, assetId;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            assetChainId = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[assetChainId] is inValid");
        }
        try {
            assetId = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[assetId] is inValid");
        }
        try {
            address = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }

        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.dataNotFound();
        }
        if (assetId <= 0) {
            AssetInfo defaultAsset = apiCache.getChainInfo().getDefaultAsset();
            assetId = defaultAsset.getAssetId();
        }
        BalanceInfo balanceInfo = WalletRpcHandler.getAccountBalance(chainId, address, assetChainId, assetId);
        AccountInfo accountInfo = accountService.getAccountInfo(chainId, address);
        if (accountInfo != null) {
            balanceInfo.setConsensusLock(accountInfo.getConsensusLock());
        }
        return RpcResult.success(balanceInfo);

    }

    @RpcMethod("isAliasUsable")
    public RpcResult isAliasUsable(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String alias;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            alias = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[alias] is inValid");
        }
        if (StringUtils.isBlank(alias)) {
            return RpcResult.paramError("[alias] is inValid");
        }

        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.dataNotFound();
        }

        Result result = WalletRpcHandler.isAliasUsable(chainId, alias);
        return RpcResult.success(result.getData());
    }

    @RpcMethod("getAccountLedgerList")
    public RpcResult getAccountLedgerList(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }

        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.dataNotFound();
        }
        List<AccountLedgerInfo> list = accountLedgerService.getAccountLedgerInfoList(chainId, address);
        return RpcResult.success(list.stream().map(ledgerInfo -> {
            BalanceInfo balanceInfo = WalletRpcHandler.getAccountBalance(chainId, address, ledgerInfo.getChainId(), ledgerInfo.getAssetId());
            ledgerInfo.setBalance(balanceInfo.getBalance());
            ledgerInfo.setTimeLock(balanceInfo.getTimeLock());
            ledgerInfo.setConsensusLock(balanceInfo.getConsensusLock());
            AssetInfo assetInfo = CacheManager.getAssetInfoMap().get(ledgerInfo.getAssetKey());
            if (assetInfo != null) {
                ledgerInfo.setSymbol(assetInfo.getSymbol());
                ledgerInfo.setDecimals(assetInfo.getDecimals());
            }
            Map<String, Object> map = MapUtils.beanToMap(ledgerInfo);
            return map;
        }).collect(Collectors.toList()));
    }


    @RpcMethod("getAccountCrossLedgerList")
    public RpcResult getAccountCrossLedgerList(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId, showZero = 1;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }

        if (params.size() > 2) {
            try {
                showZero = (int) params.get(2);
            } catch (Exception e) {
                return RpcResult.paramError("[showZero] is inValid");
            }
        }

        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.dataNotFound();
        }
        List<AccountLedgerInfo> list = accountLedgerService.getAccountCrossLedgerInfoList(chainId, address);
        for (AccountLedgerInfo ledgerInfo : list) {
            BalanceInfo balanceInfo = WalletRpcHandler.getAccountBalance(chainId, address, ledgerInfo.getChainId(), ledgerInfo.getAssetId());
            ledgerInfo.setBalance(balanceInfo.getBalance());
            ledgerInfo.setTimeLock(balanceInfo.getTimeLock());
            ledgerInfo.setConsensusLock(balanceInfo.getConsensusLock());
            AssetInfo assetInfo = CacheManager.getAssetInfoMap().get(ledgerInfo.getAssetKey());
            if (assetInfo != null) {
                ledgerInfo.setSymbol(assetInfo.getSymbol());
                ledgerInfo.setDecimals(assetInfo.getDecimals());
            }
        }
        if (showZero == 1) {
            List<SymbolRegInfo> converList = symbolRegService.getListBySource(ApiConstant.SYMBOL_REG_SOURCE_CONVERTER);
            Map<String, AccountLedgerInfo> ledgerInfoMap = new HashMap<>(list.size() + converList.size());
            list.forEach(d -> ledgerInfoMap.put(d.getChainId() + "-" + d.getAssetId(), d));
            converList.forEach(d -> {
                AccountLedgerInfo accountLedgerInfo = new AccountLedgerInfo();
                PropertyUtils.copyProperties(accountLedgerInfo, d);
                accountLedgerInfo.setTotalBalance(BigInteger.ZERO);
                accountLedgerInfo.setBalance(BigInteger.ZERO);
                accountLedgerInfo.setConsensusLock(BigInteger.ZERO);
                accountLedgerInfo.setTimeLock(BigInteger.ZERO);
                accountLedgerInfo.setAddress(address);
                ledgerInfoMap.putIfAbsent(d.getChainId() + "-" + d.getAssetId(), accountLedgerInfo);
            });
            list = new ArrayList<>(ledgerInfoMap.values());
        }
        return RpcResult.success(list.stream().map(ledgerInfo -> {
            Map<String, Object> map = MapUtils.beanToMap(ledgerInfo);
            SymbolRegInfo symbolRegInfo = symbolRegService.get(ledgerInfo.getChainId(), ledgerInfo.getAssetId());
            map.put("source", symbolRegInfo.getSource());
            io.nuls.base.api.provider.Result<HeterogeneousAssetInfo> heterogeneousAssetInfoResult = converterService.getHeterogeneousAssetInfo(new GetHeterogeneousAssetInfoReq(ledgerInfo.getAssetId()));
            if (heterogeneousAssetInfoResult.isSuccess()) {
                map.put("chainName", heterogeneousAssetInfoResult.getData().getHeterogeneousChainSymbol());
                map.put("isToken", heterogeneousAssetInfoResult.getData().isToken());
                map.put("contractAddress", heterogeneousAssetInfoResult.getData().getContractAddress());
                map.put("heterogeneousChainMultySignAddress", heterogeneousAssetInfoResult.getData().getHeterogeneousChainMultySignAddress());
            }
            return map;
        }).collect(Collectors.toList()));

    }

    @RpcMethod("getAllAddressPrefix")
    public RpcResult getAllAddressPrefix(List<Object> params) {
        Result<List> result = WalletRpcHandler.getAllAddressPrefix();
        return RpcResult.success(result.getData());
    }


    @RpcMethod("getAssetList")
    public RpcResult getAssetList(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId, assetType = 0;
        String address = null;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        if (params.size() > 1) {
            try {
                assetType = (int) params.get(1);
            } catch (Exception e) {
                return RpcResult.paramError("[showZero] is inValid");
            }
        }
        if (params.size() > 2) {
            try {
                address = (String) params.get(2);
            } catch (Exception e) {
                return RpcResult.paramError("[address] is inValid");
            }
            if (!AddressTool.validAddress(chainId, address)) {
                return RpcResult.paramError("[address] is inValid");
            }
        }
        io.nuls.base.api.provider.Result<io.nuls.base.api.provider.ledger.facade.AssetInfo> res = ledgerProvider.getAssetList(new GetAssetListReq(chainId, assetType));

        if (res.isSuccess()) {
            if (StringUtils.isNotBlank(address)) {
                return RpcResult.success(res.getList().stream().filter(d -> {
                    BalanceInfo balanceInfo = WalletRpcHandler.getAccountBalance(chainId, (String) params.get(2), d.getAssetChainId(), d.getAssetId());
                    return balanceInfo.getTotalBalance().compareTo(BigInteger.ZERO) > 0;
                }).collect(Collectors.toList()));
            } else {
                return RpcResult.success(res.getList());
            }
        } else {
            return RpcResult.failed(CommonCodeConstanst.FAILED, res.getMessage());
        }
    }

}
