package io.nuls.api.rpc.controller;

import io.nuls.api.ApiContext;
import io.nuls.api.analysis.AnalysisHandler;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.DepositFixedType;
import io.nuls.api.db.*;
import io.nuls.api.exception.JsonRpcException;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.dto.SymbolUsdPercentDTO;
import io.nuls.api.model.entity.CallContractData;
import io.nuls.api.model.entity.CancelDeposit;
import io.nuls.api.model.entity.CreateContractData;
import io.nuls.api.model.entity.DeleteContractData;
import io.nuls.api.model.po.*;
import io.nuls.api.model.po.mini.CancelDepositInfo;
import io.nuls.api.model.po.mini.MiniCoinBaseInfo;
import io.nuls.api.model.po.mini.MiniCrossChainTransactionInfo;
import io.nuls.api.model.po.mini.MiniTransactionInfo;
import io.nuls.api.model.rpc.RpcErrorCode;
import io.nuls.api.model.rpc.RpcResult;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.api.utils.LoggerUtil;
import io.nuls.api.utils.VerifyUtils;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.nuls.api.constant.DBTableConstant.TX_COUNT;
import static io.nuls.core.constant.TxType.*;

@Controller
public class TransactionController {
    @Autowired
    private TransactionService txService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private DepositService depositService;
    @Autowired
    private PunishService punishService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private StatisticalService statisticalService;

    @Autowired
    SymbolRegService symbolRegService;

    @Autowired
    ConverterTxService converterTxService;

    @Autowired
    SymbolUsdtPriceProviderService symbolUsdtPriceProviderService;


    @RpcMethod("getTx")
    public RpcResult getTx(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String hash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            hash = "" + params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[hash] is inValid");
        }
        if (StringUtils.isBlank(hash)) {
            return RpcResult.paramError("[hash] is required");
        }
        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }

        Result<TransactionInfo> result = WalletRpcHandler.getTx(chainId, hash);
        if (result == null) {
            return RpcResult.dataNotFound();
        }
        if (result.isFailed()) {
            throw new JsonRpcException(result.getErrorCode());
        }
        TransactionInfo tx = result.getData();
        if (tx == null) {
            return RpcResult.dataNotFound();
        }
        try {
            RpcResult rpcResult = new RpcResult();
            if (tx.getType() == TxType.COIN_BASE) {
                BlockHeaderInfo headerInfo = blockService.getBlockHeader(chainId, tx.getHeight());
                MiniCoinBaseInfo coinBaseInfo = new MiniCoinBaseInfo(headerInfo.getRoundIndex(), headerInfo.getPackingIndexOfRound(), tx.getHash());
                tx.setTxData(coinBaseInfo);
            } else if (tx.getType() == TxType.DEPOSIT || tx.getType() == TxType.CONTRACT_DEPOSIT) {
                DepositInfo depositInfo = (DepositInfo) tx.getTxData();
                tx.setTxData(depositInfo);
            } else if (tx.getType() == TxType.CANCEL_DEPOSIT || tx.getType() == TxType.CONTRACT_CANCEL_DEPOSIT) {
                CancelDepositInfo depositInfo = (CancelDepositInfo) tx.getTxData();
                tx.setTxData(depositInfo);
            } else if (tx.getType() == TxType.STOP_AGENT || tx.getType() == TxType.CONTRACT_STOP_AGENT) {
                AgentInfo agentInfo = (AgentInfo) tx.getTxData();
                agentInfo = agentService.getAgentByHash(chainId, agentInfo.getTxHash());
                tx.setTxData(agentInfo);
            } else if (tx.getType() == TxType.YELLOW_PUNISH) {
                List<TxDataInfo> punishLogs = punishService.getYellowPunishLog(chainId, tx.getHash());
                tx.setTxDataList(punishLogs);
            } else if (tx.getType() == TxType.RED_PUNISH) {
                PunishLogInfo punishLog = punishService.getRedPunishLog(chainId, tx.getHash());
                tx.setTxData(punishLog);
            } else if (tx.getType() == CREATE_CONTRACT) {
//                try {
//                    ContractResultInfo resultInfo = contractService.getContractResultInfo(tx.getHash());
//                    ContractInfo contractInfo = (ContractInfo) tx.getTxData();
//                    contractInfo.setResultInfo(resultInfo);
//                } catch (Exception e) {
//                    Log.error(e);
//                }
            } else if (tx.getType() == CALL_CONTRACT) {
//                try {
//                    ContractResultInfo resultInfo = contractService.getContractResultInfo(tx.getHash());
//                    ContractCallInfo contractCallInfo = (ContractCallInfo) tx.getTxData();
//                    contractCallInfo.setResultInfo(resultInfo);
//                } catch (Exception e) {
//                    Log.error(e);
//                }
            }
            rpcResult.setResult(tx);
            return rpcResult;
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
            return RpcResult.failed(RpcErrorCode.TX_PARSE_ERROR);
        }
    }

    @RpcMethod("getTxList")
    public RpcResult getTxList(List<Object> params) {
        VerifyUtils.verifyParams(params, 5);
        int chainId, pageNumber, pageSize, type;
        boolean isHidden;
        String address = null;
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
            type = (int) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[type] is inValid");
        }
        try {
            isHidden = (boolean) params.get(4);
        } catch (Exception e) {
            return RpcResult.paramError("[isHidden] is inValid");
        }
        if(params.size() > 5){
            try {
                address = (String) params.get(5);
            } catch (Exception e) {
                return RpcResult.paramError("[address] is inValid");
            }
            if (!AddressTool.validAddress(chainId, address)) {
                return RpcResult.paramError("[address] is inValid");
            }
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        PageInfo<MiniTransactionInfo> pageInfo;
        if (!CacheManager.isChainExist(chainId)) {
            pageInfo = new PageInfo<>(pageNumber, pageSize);
        } else {
            if (type > 0) {
                pageInfo = txService.getInnerChainTxList(chainId, address, pageNumber, pageSize, isHidden, type);
            } else {
                pageInfo = txService.getInnerChainTxList(chainId, address, pageNumber, pageSize, isHidden);
            }
        }
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(pageInfo);
        return rpcResult;
    }

    @RpcMethod("getBlockTxList")
    public RpcResult getBlockTxList(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int chainId, pageNumber, pageSize, type;
        long height;
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
            height = Long.valueOf(params.get(3).toString());
        } catch (Exception e) {
            return RpcResult.paramError("[height] is inValid");
        }
        try {
            type = Integer.parseInt("" + params.get(4));
        } catch (Exception e) {
            return RpcResult.paramError("[type] is inValid");
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }

        PageInfo<MiniTransactionInfo> pageInfo;
        if (!CacheManager.isChainExist(chainId)) {
            pageInfo = new PageInfo<>(pageNumber, pageSize);
        } else {
            pageInfo = txService.getBlockTxList(chainId, pageNumber, pageSize, height, type);
        }
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(pageInfo);
        return rpcResult;
    }

    @RpcMethod("getTxStatistical")
    public RpcResult getTxStatistical(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId, type,timezoneOffset = 0;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            type = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[type] is inValid");
        }
        if(params.size() > 2){
            try {
                timezoneOffset = (int) params.get(2);
            } catch (Exception e) {
                return RpcResult.paramError("[type] is inValid");
            }
        }
        //取反
        timezoneOffset = -timezoneOffset;
        List list = this.statisticalService.getStatisticalList(chainId, type, TX_COUNT, timezoneOffset);
        return new RpcResult().setResult(list);
    }

    @RpcMethod("validateTx")
    public RpcResult validateTx(List<Object> params) {
        if (!ApiContext.isReady) {
            return RpcResult.chainNotReady();
        }
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String txHex;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            txHex = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[txHex] is inValid");
        }
        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(txHex)) {
            return RpcResult.paramError("[txHex] is inValid");
        }
        Result result = WalletRpcHandler.validateTx(chainId, txHex);
        if (result.isSuccess()) {
            return RpcResult.success(result.getData());
        } else {
            return RpcResult.failed(result);
        }
    }

    @RpcMethod("broadcastTx")
    public RpcResult broadcastTx(List<Object> params) {
        if (!ApiContext.isReady) {
            return RpcResult.chainNotReady();
        }
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String txHex;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            txHex = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[txHex] is inValid");
        }

        try {
            if (!CacheManager.isChainExist(chainId)) {
                return RpcResult.dataNotFound();
            }
            int type = this.extractTxTypeFromTx(txHex);
            Result result = Result.getSuccess(null);
            switch (type) {
                case CREATE_CONTRACT:
                    Transaction tx = new Transaction();
                    tx.parse(new NulsByteBuffer(RPCUtil.decode(txHex)));
                    CreateContractData create = new CreateContractData();
                    create.parse(new NulsByteBuffer(tx.getTxData()));
                    result = WalletRpcHandler.validateContractCreate(chainId,
                            AddressTool.getStringAddressByBytes(create.getSender()),
                            create.getGasLimit(),
                            create.getPrice(),
                            RPCUtil.encode(create.getCode()),
                            create.getArgs());
                    break;
                case CALL_CONTRACT:
                    Transaction callTx = new Transaction();
                    callTx.parse(new NulsByteBuffer(RPCUtil.decode(txHex)));
                    CallContractData call = new CallContractData();
                    call.parse(new NulsByteBuffer(callTx.getTxData()));
                    result = WalletRpcHandler.validateContractCall(chainId,
                            AddressTool.getStringAddressByBytes(call.getSender()),
                            call.getValue(),
                            call.getGasLimit(),
                            call.getPrice(),
                            AddressTool.getStringAddressByBytes(call.getContractAddress()),
                            call.getMethodName(),
                            call.getMethodDesc(),
                            call.getArgs());
                    break;
                case DELETE_CONTRACT:
                    Transaction deleteTx = new Transaction();
                    deleteTx.parse(new NulsByteBuffer(RPCUtil.decode(txHex)));
                    DeleteContractData delete = new DeleteContractData();
                    delete.parse(new NulsByteBuffer(deleteTx.getTxData()));
                    result = WalletRpcHandler.validateContractDelete(chainId,
                            AddressTool.getStringAddressByBytes(delete.getSender()),
                            AddressTool.getStringAddressByBytes(delete.getContractAddress()));
                    break;
                default:
                    break;
            }
            Map contractMap = (Map) result.getData();
            if (contractMap != null && Boolean.FALSE.equals(contractMap.get("success"))) {
                result.setErrorCode(CommonCodeConstanst.DATA_ERROR);
                result.setMsg((String) contractMap.get("msg"));
                return RpcResult.failed(result);
            }

            result = WalletRpcHandler.broadcastTx(chainId, txHex);

            if (result.isSuccess()) {
                Transaction tx = new Transaction();
                tx.parse(new NulsByteBuffer(RPCUtil.decode(txHex)));
                TransactionInfo txInfo = AnalysisHandler.toTransaction(chainId, tx);
                txService.saveUnConfirmTx(chainId, txInfo, txHex);
                return RpcResult.success(result.getData());
            } else {
                return RpcResult.failed(result);
            }
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
            return RpcResult.failed(RpcErrorCode.TX_PARSE_ERROR);
        }
    }

    private int extractTxTypeFromTx(String txString) throws NulsException {
        String txTypeHexString = txString.substring(0, 4);
        NulsByteBuffer byteBuffer = new NulsByteBuffer(RPCUtil.decode(txTypeHexString));
        return byteBuffer.readUint16();
    }

//
//    @RpcMethod("sendCrossTx")
//    public RpcResult sendCrossTx(List<Object> params) {
//        if (!ApiContext.isReady) {
//            return RpcResult.chainNotReady();
//        }
//        VerifyUtils.verifyParams(params, 2);
//        int chainId;
//        String txHex;
//        try {
//            chainId = (int) params.get(0);
//        } catch (Exception e) {
//            return RpcResult.paramError("[chainId] is inValid");
//        }
//        try {
//            txHex = (String) params.get(1);
//        } catch (Exception e) {
//            return RpcResult.paramError("[txHex] is inValid");
//        }
//        if (!CacheManager.isChainExist(chainId)) {
//            return RpcResult.dataNotFound();
//        }
//        try {
//            Result result = WalletRpcHandler.sendCrossTx(chainId, txHex);
//
//            if (result.isSuccess()) {
//                Transaction tx = new Transaction();
//                tx.parse(new NulsByteBuffer(RPCUtil.decode(txHex)));
//                TransactionInfo txInfo = AnalysisHandler.toTransaction(chainId, tx);
//                txService.saveUnConfirmTx(chainId, txInfo, txHex);
//                return RpcResult.success(result.getData());
//            } else {
//                return RpcResult.failed(result);
//            }
//        } catch (Exception e) {
//            LoggerUtil.commonLog.error(e);
//            return RpcResult.failed(RpcErrorCode.TX_PARSE_ERROR);
//        }
//    }

    @RpcMethod("broadcastTxWithNoContractValidation")
    public RpcResult broadcastTxWithNoContractValidation(List<Object> params) {
        if (!ApiContext.isReady) {
            return RpcResult.chainNotReady();
        }
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String txHex;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            txHex = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[txHex] is inValid");
        }

        try {
            if (!CacheManager.isChainExist(chainId)) {
                return RpcResult.dataNotFound();
            }
            Result result = WalletRpcHandler.broadcastTx(chainId, txHex);
            if (result.isSuccess()) {
                Transaction tx = new Transaction();
                tx.parse(new NulsByteBuffer(RPCUtil.decode(txHex)));
                TransactionInfo txInfo = AnalysisHandler.toTransaction(chainId, tx);
                txService.saveUnConfirmTx(chainId, txInfo, txHex);
                return RpcResult.success(result.getData());
            } else {
                return RpcResult.failed(result);
            }
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
            return RpcResult.failed(RpcErrorCode.TX_PARSE_ERROR);
        }
    }

    /**
     * 获取最近24小时各个币种交易统计
     *
     * @return
     */
    @RpcMethod("getTxCountFor24Hour")
    public RpcResult getTxCountFor24Hour(List<Object> params) {
        //交易快照表查询 ，数据还未统计StatisticalInfo
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        List<AssetSnapshotInfo> list = statisticalService.getAssetSnapshotAggSum(chainId, 4);
//        SymbolPrice usdPrice = symbolUsdtPriceProviderService.getSymbolPriceForUsdt(ApiConstant.USD);
//        Map<String,BigDecimal> symbolUsdTxTotalMap = new HashMap<>(list.size());
//        BigDecimal allSymbolTxTotalUsdValue = list.stream().map(d->{
//            SymbolPrice symbolPrice = symbolUsdtPriceProviderService.getSymbolPriceForUsdt(d.getSymbol());
//            if(symbolPrice.getPrice().equals(BigDecimal.ZERO))return BigDecimal.ZERO;
//            SymbolRegInfo symbolRegInfo = symbolRegService.get(d.getAssetChainId(),d.getAssetId());
//            //计算当前币种交易对应的的USD总量
//            BigDecimal total = new BigDecimal(d.getTxTotal()).movePointLeft(symbolRegInfo.getDecimals());
//            BigDecimal usdTotal = usdPrice.transfer(symbolPrice,total);
//            symbolUsdTxTotalMap.put(d.getSymbol(),usdTotal);
//            return usdTotal;
//        }).reduce(BigDecimal.ZERO,(v1,v2)->v1.add(v2));
        Map<String,BigInteger> symbolList = list.stream().map(d -> Map.of(d.getSymbol(), d.getTxTotal())).reduce(new HashMap<>(list.size()),
                (d1, d2) -> {
                    d1.putAll(d2);
                    return d1;
                });
        return RpcResult.success(list.stream().map(d -> {
//            BigDecimal rate;
//            BigDecimal usdValue = symbolUsdTxTotalMap.getOrDefault(d.getSymbol(),BigDecimal.ZERO);
//            if(allSymbolTxTotalUsdValue.equals(BigDecimal.ZERO)){
//                rate = new BigDecimal(100);
//            }else if (usdValue.equals(BigDecimal.ZERO)){
//                rate = BigDecimal.ZERO;
//            }else {
//                rate = symbolUsdTxTotalMap.get(d.getSymbol()).divide(allSymbolTxTotalUsdValue,2, RoundingMode.HALF_UP);
//            }
            SymbolUsdPercentDTO symbolUsdPercentDTO = symbolUsdtPriceProviderService.calcRate(d.getSymbol(), symbolList);
            return Map.of("symbol", d.getSymbol(), "total", d.getTotal(), "txTotal", d.getTxTotal(), "txTotalRate", symbolUsdPercentDTO.getPer(), "usdValue", symbolUsdPercentDTO.getUsdVal());
        }).collect(Collectors.toList()));
    }

    /**
     * 获取跨链交易列表
     * type
     *   1:生态内
     *   2:异构跨链
     * @return
     */
    @RpcMethod("getCrossChainTxList")
    public RpcResult getCrossChainTxList(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId, pageNumber, pageSize, type = 0;
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

        if(params.size() > 3){
            try {
                type = (int) params.get(3);
            } catch (Exception e) {
                return RpcResult.paramError("[type] is inValid");
            }
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        int[] types;
        if(type == 1){
            types = new int[]{CROSS_CHAIN};
        }else if(type == 2){
            types = new int[]{RECHARGE,WITHDRAWAL};
        } else {
            types = new int[]{CROSS_CHAIN,RECHARGE,WITHDRAWAL};
        }
        PageInfo<MiniCrossChainTransactionInfo> pageInfo;
        if (!CacheManager.isChainExist(chainId)) {
            pageInfo = new PageInfo<>(pageNumber, pageSize);
            return RpcResult.success(pageInfo);
        } else {
            return RpcResult.success(txService.getCrossChainTxList(chainId, null,pageNumber, pageSize,types));
        }

    }

}
