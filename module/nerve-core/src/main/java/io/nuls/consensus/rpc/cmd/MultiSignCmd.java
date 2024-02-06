package io.nuls.consensus.rpc.cmd;

import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.consensus.service.MultiSignService;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;

import static io.nuls.consensus.constant.CommandConstant.*;
import static io.nuls.consensus.constant.ParameterConstant.*;

import java.util.Map;

/**
 * Multiple account related interfaces
 * Multi-Sign Account Related Interface
 *
 * @author tag
 * 2019/7/25
 * */
@Component
@NerveCoreCmd(module = ModuleE.CS)
public class MultiSignCmd extends BaseCmd {
    @Autowired
    private MultiSignService service;

    /**
     * Multiple account creation nodes
     * */
    @CmdAnnotation(cmd = CMD_CREATE_MULTI_AGENT, version = 1.0, description = "Multiple account creation nodes/Multi-Sign Account create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_AGENT_ADDRESS, parameterType = "String", parameterDes = "Node address(Multiple signed addresses)")
    @Parameter(parameterName = PARAM_PACKING_ADDRESS, parameterType = "String", parameterDes = "Node block address")
    @Parameter(parameterName = PARAM_REWARD_ADDRESS, parameterType = "String", parameterDes = "Reward Address,Default node address", canNull = true)
    @Parameter(parameterName = PARAM_DEPOSIT, parameterType = "String", parameterDes = "Mortgage amount")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "Signature account password")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "Signature account address")
    @ResponseData(name = "Return value", description = "Return aMap,Including threekey", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "Complete transaction serialization string,If the transaction does not reach the minimum number of signatures, you can continue to sign"),
            @Key(name = PARAM_TX_HASH,  description = "transactionhash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:Transaction completed(Broadcasted),false:Transaction not completed,Not reaching the minimum number of signatures")
    }))
    public Response createMultiAgent(Map<String,Object> params){
        Result result = service.createMultiAgent(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Multiple account cancellation nodes
     * */
    @CmdAnnotation(cmd = CMD_STOP_MULTI_AGENT, version = 1.0, description = "Multiple account cancellation nodes/Multi-Sign Account stop agent")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Node address(Multiple signed addresses)")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "Signature account password")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "Signature account address")
    @ResponseData(name = "Return value", description = "Return aMap,Including threekey", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "Complete transaction serialization string,If the transaction does not reach the minimum number of signatures, you can continue to sign"),
            @Key(name = PARAM_TX_HASH,  description = "transactionhash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:Transaction completed(Broadcasted),false:Transaction not completed,Not reaching the minimum number of signatures")
    }))
    public Response stopMultiAgent(Map<String,Object> params){
        Result result = service.stopMultiAgent(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Add node delegation
     * */
    @CmdAnnotation(cmd = CMD_APPEND_MULTI_AGENT_DEPOSIT, version = 1.0, description = "Create node transactions/create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Node address")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "password")
    @Parameter(parameterName = PARAM_AMOUNT, parameterType = "String", parameterDes = "Additional amount")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "Signature account address")
    @ResponseData(name = "Return value", description = "Return aMap,Including threekey", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "Complete transaction serialization string,If the transaction does not reach the minimum number of signatures, you can continue to sign"),
            @Key(name = PARAM_TX_HASH,  description = "transactionhash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:Transaction completed(Broadcasted),false:Transaction not completed,Not reaching the minimum number of signatures")
    }))
    public Response appendMultiAgentDeposit(Map<String,Object> params){
        Result result = service.appendMultiAgentDeposit(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Reduce node delegation
     * */
    @CmdAnnotation(cmd = CMD_REDUCE_MULTI_AGENT_DEPOSIT, version = 1.0, description = "Create node transactions/create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Node address")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "password")
    @Parameter(parameterName = PARAM_AMOUNT, parameterType = "String", parameterDes = "Additional amount")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "Signature account address")
    @ResponseData(name = "Return value", description = "Return aMap,Including threekey", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "Complete transaction serialization string,If the transaction does not reach the minimum number of signatures, you can continue to sign"),
            @Key(name = PARAM_TX_HASH,  description = "transactionhash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:Transaction completed(Broadcasted),false:Transaction not completed,Not reaching the minimum number of signatures")
    }))
    public Response reduceMultiAgentDeposit(Map<String,Object> params){
        Result result = service.reduceMultiAgentDeposit(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Multiple account delegation consensus
     * */
    @CmdAnnotation(cmd = CMD_MULTI_DEPOSIT, version = 1.0, description = "Multiple account delegation consensus/Multi-Sign Account deposit agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Account address")
    @Parameter(parameterName = PARAM_DEPOSIT, parameterType = "String", parameterDes = "Entrusted amount")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "Account password")
    @Parameter(parameterName = PARAM_ASSET_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset ChainID")
    @Parameter(parameterName = PARAM_ASSET_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID")
    @Parameter(parameterName = PARAM_DEPOSIT_TYPE, requestType = @TypeDescriptor(value = byte.class), parameterDes = "Entrustment type")
    @Parameter(parameterName = PARAM_TIME_TYPE, requestType = @TypeDescriptor(value = byte.class), parameterDes = "Entrustment time type")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "Signature account address")
    @ResponseData(name = "Return value", description = "Return aMap,Including threekey", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "Complete transaction serialization string,If the transaction does not reach the minimum number of signatures, you can continue to sign"),
            @Key(name = PARAM_TX_HASH,  description = "transactionhash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:Transaction completed(Broadcasted),false:Transaction not completed,Not reaching the minimum number of signatures")
    }))
    public Response multiDeposit(Map<String,Object> params){
        Result result = service.multiDeposit(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Consensus on account exit with multiple signatures
     * */
    @CmdAnnotation(cmd = CMD_MULTI_WITHDRAW, version = 1.0, description = "Consensus on account exit with multiple signatures/Multi-Sign Account withdraw deposit agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Multiple account addresses signed")
    @Parameter(parameterName = PARAM_TX_HASH, parameterType = "String", parameterDes = "Join consensus tradingHASH")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "Signature account password")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "Signature account address")
    @ResponseData(name = "Return value", description = "Return aMap,Including threekey", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "Complete transaction serialization string,If the transaction does not reach the minimum number of signatures, you can continue to sign"),
            @Key(name = PARAM_TX_HASH,  description = "transactionhash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:Transaction completed(Broadcasted),false:Transaction not completed,Not reaching the minimum number of signatures")
    }))
    public Response multiWithdraw(Map<String,Object> params){
        Result result = service.multiWithdraw(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }
}
