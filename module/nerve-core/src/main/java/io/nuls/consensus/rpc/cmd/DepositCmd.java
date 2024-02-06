package io.nuls.consensus.rpc.cmd;

import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.consensus.model.bo.StackingAsset;
import io.nuls.consensus.model.dto.output.DepositDTO;
import io.nuls.consensus.service.DepositService;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import static io.nuls.consensus.constant.CommandConstant.*;
import static io.nuls.consensus.constant.ParameterConstant.*;

import java.util.List;
import java.util.Map;

/**
 * Consensus delegation related interfaces
 * @author tag
 * 2018/11/7
 * */
@Component
@NerveCoreCmd(module = ModuleE.CS)
public class DepositCmd extends BaseCmd {
    @Autowired
    private DepositService service;

    /**
     * Commission consensus
     * */
    @CmdAnnotation(cmd = CMD_DEPOSIT_TO_STACKING, version = 1.0, description = "Create entrusted transactions/deposit to stacking transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Account address")
    @Parameter(parameterName = PARAM_DEPOSIT, parameterType = "String", parameterDes = "Entrusted amount")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "Account password")
    @Parameter(parameterName = PARAM_ASSET_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset ChainID")
    @Parameter(parameterName = PARAM_ASSET_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID")
    @Parameter(parameterName = PARAM_DEPOSIT_TYPE, requestType = @TypeDescriptor(value = byte.class), parameterDes = "Entrustment type")
    @Parameter(parameterName = PARAM_TIME_TYPE, requestType = @TypeDescriptor(value = byte.class), parameterDes = "Entrustment time type")
    @ResponseData(name = "Return value", description = "Join consensus tradingHash", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX_HASH, description = "Join consensus tradingHash")
    }))
    public Response depositToAgent(Map<String,Object> params){
        Result result = service.depositToAgent(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Exit consensus
     * */
    @CmdAnnotation(cmd = CMD_WITHDRAW, version = 1.0, description = "Exit entrusted transaction/withdraw deposit agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Account address")
    @Parameter(parameterName = PARAM_TX_HASH, parameterType = "String", parameterDes = "Join consensus tradingHASH")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "Account password")
    @ResponseData(name = "Return value", description = "Exit consensus tradingHash", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX_HASH, description = "Exit consensus tradingHash")
    }))
    public Response withdraw(Map<String,Object> params){
        Result result = service.withdraw(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Exit consensus
     * */
    @CmdAnnotation(cmd = CMD_BATCH_WITHDRAW, version = 1.0, description = "Exit entrusted transaction/withdraw deposit agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Account address")
    @Parameter(parameterName = PARAM_TX_HASH, parameterType = "String", parameterDes = "Join consensus tradingHASHcharacter string")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "Account password")
    @ResponseData(name = "Return value", description = "Batch exit consensus transactionsHash", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "txHash", description = "Batch exit consensus transactionsHash")
    }))
    public Response batchWithdraw(Map<String,Object> params){
        Result result = service.batchWithdraw(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }
    /**
     * Exit consensus
     * */
    @CmdAnnotation(cmd = CMD_BATCH_STAKING_MERGE, version = 1.0, description = "Exit entrusted transaction/withdraw deposit agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Account address")
    @Parameter(parameterName = PARAM_TX_HASH, parameterType = "String", parameterDes = "Join consensus tradingHASHcharacter string")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "Account password")
    @Parameter(parameterName = "timeType", parameterType = "int", parameterDes = "Pledge term type")
    @ResponseData(name = "Return value", description = "Batch merger of pledge transactionsHash", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "txHash", description = "Batch merger of pledge transactionsHash")
    }))
    public Response batchStakingMerge(Map<String,Object> params){
        Result result = service.batchStakingMerge(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Query delegation information list
     * */
    @CmdAnnotation(cmd = CMD_GET_DEPOSIT_LIST, version = 1.0, description = "Query delegation information for a specified account or node/Query delegation information for a specified account or node")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_PAGE_NUMBER, requestType = @TypeDescriptor(value = int.class), parameterDes = "Page number")
    @Parameter(parameterName = PARAM_PAGE_SIZE, requestType = @TypeDescriptor(value = int.class), parameterDes = "Quantity per page")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Account address")
    @ResponseData(name = "Return value", description = "Return aPageObject, only described herePageCollection in objects",
            responseType = @TypeDescriptor(value = List.class, collectionElement = DepositDTO.class)
    )
    public Response getDepositList(Map<String,Object> params){
        Result result = service.getDepositList(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * adoptSimpleQuery the assets of mortgaged assetsidAnd asset chainid
     * */
    @CmdAnnotation(cmd = CMD_GET_ASSET_BY_SYMBOL, version = 1.0, description = "adoptSimpleQuery the assets of mortgaged assetsidAnd asset chainid")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_SYMBOL, parameterType = "String", parameterDes = "assetSIMPLE")
    @ResponseData(name = "Return value", description = "simpleCorresponding asset information", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_ASSET_CHAIN_ID, description = "simpleCorresponding asset chainID"),
            @Key(name = PARAM_ASSET_ID, description = "simpleCorresponding assetsID")
    }))
    public Response getAssetBySymbol(Map<String,Object> params){
        Result result = service.getAssetBySymbol(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Query participationstackingAsset List for
     * */
    @CmdAnnotation(cmd = CMD_GET_CAN_STACKING_ASSET_LIST, version = 1.0, description = "Query participationstackingAsset List for")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @ResponseData(name = "Return value", description = "Return aPageObject, only described herePageCollection in objects",
            responseType = @TypeDescriptor(value = List.class, collectionElement = StackingAsset.class)
    )
    public Response getCanStackingAssetList(Map<String,Object> params){
        Result result = service.getCanStackingAssetList(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }
}
