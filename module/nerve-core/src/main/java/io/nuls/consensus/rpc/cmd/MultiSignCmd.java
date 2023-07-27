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
 * 多签账户相关接口
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
     * 多签账户创建节点
     * */
    @CmdAnnotation(cmd = CMD_CREATE_MULTI_AGENT, version = 1.0, description = "多签账户创建节点/Multi-Sign Account create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_AGENT_ADDRESS, parameterType = "String", parameterDes = "节点地址(多签地址)")
    @Parameter(parameterName = PARAM_PACKING_ADDRESS, parameterType = "String", parameterDes = "节点出块地址")
    @Parameter(parameterName = PARAM_REWARD_ADDRESS, parameterType = "String", parameterDes = "奖励地址,默认节点地址", canNull = true)
    @Parameter(parameterName = PARAM_DEPOSIT, parameterType = "String", parameterDes = "抵押金额")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "签名账户密码")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "签名账户地址")
    @ResponseData(name = "返回值", description = "返回一个Map,包含三个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "完整交易序列化字符串,如果交易没达到最小签名数可继续签名"),
            @Key(name = PARAM_TX_HASH,  description = "交易hash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:交易已完成(已广播),false:交易没完成,没有达到最小签名数")
    }))
    public Response createMultiAgent(Map<String,Object> params){
        Result result = service.createMultiAgent(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 多签账户注销节点
     * */
    @CmdAnnotation(cmd = CMD_STOP_MULTI_AGENT, version = 1.0, description = "多签账户注销节点/Multi-Sign Account stop agent")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "节点地址(多签地址)")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "签名账户密码")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "签名账户地址")
    @ResponseData(name = "返回值", description = "返回一个Map,包含三个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "完整交易序列化字符串,如果交易没达到最小签名数可继续签名"),
            @Key(name = PARAM_TX_HASH,  description = "交易hash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:交易已完成(已广播),false:交易没完成,没有达到最小签名数")
    }))
    public Response stopMultiAgent(Map<String,Object> params){
        Result result = service.stopMultiAgent(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 追加节点委托
     * */
    @CmdAnnotation(cmd = CMD_APPEND_MULTI_AGENT_DEPOSIT, version = 1.0, description = "创建节点交易/create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "节点地址")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "密码")
    @Parameter(parameterName = PARAM_AMOUNT, parameterType = "String", parameterDes = "追加金额")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "签名账户地址")
    @ResponseData(name = "返回值", description = "返回一个Map,包含三个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "完整交易序列化字符串,如果交易没达到最小签名数可继续签名"),
            @Key(name = PARAM_TX_HASH,  description = "交易hash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:交易已完成(已广播),false:交易没完成,没有达到最小签名数")
    }))
    public Response appendMultiAgentDeposit(Map<String,Object> params){
        Result result = service.appendMultiAgentDeposit(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 减少节点委托
     * */
    @CmdAnnotation(cmd = CMD_REDUCE_MULTI_AGENT_DEPOSIT, version = 1.0, description = "创建节点交易/create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "节点地址")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "密码")
    @Parameter(parameterName = PARAM_AMOUNT, parameterType = "String", parameterDes = "追加金额")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "签名账户地址")
    @ResponseData(name = "返回值", description = "返回一个Map,包含三个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "完整交易序列化字符串,如果交易没达到最小签名数可继续签名"),
            @Key(name = PARAM_TX_HASH,  description = "交易hash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:交易已完成(已广播),false:交易没完成,没有达到最小签名数")
    }))
    public Response reduceMultiAgentDeposit(Map<String,Object> params){
        Result result = service.reduceMultiAgentDeposit(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 多签账户委托共识
     * */
    @CmdAnnotation(cmd = CMD_MULTI_DEPOSIT, version = 1.0, description = "多签账户委托共识/Multi-Sign Account deposit agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "账户地址")
    @Parameter(parameterName = PARAM_DEPOSIT, parameterType = "String", parameterDes = "委托金额")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "账户密码")
    @Parameter(parameterName = PARAM_ASSET_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "资产链ID")
    @Parameter(parameterName = PARAM_ASSET_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "资产ID")
    @Parameter(parameterName = PARAM_DEPOSIT_TYPE, requestType = @TypeDescriptor(value = byte.class), parameterDes = "委托类型")
    @Parameter(parameterName = PARAM_TIME_TYPE, requestType = @TypeDescriptor(value = byte.class), parameterDes = "委托时间类型")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "签名账户地址")
    @ResponseData(name = "返回值", description = "返回一个Map,包含三个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "完整交易序列化字符串,如果交易没达到最小签名数可继续签名"),
            @Key(name = PARAM_TX_HASH,  description = "交易hash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:交易已完成(已广播),false:交易没完成,没有达到最小签名数")
    }))
    public Response multiDeposit(Map<String,Object> params){
        Result result = service.multiDeposit(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 多签账户退出共识
     * */
    @CmdAnnotation(cmd = CMD_MULTI_WITHDRAW, version = 1.0, description = "多签账户退出共识/Multi-Sign Account withdraw deposit agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "多签账户地址")
    @Parameter(parameterName = PARAM_TX_HASH, parameterType = "String", parameterDes = "加入共识交易HASH")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "签名账户密码")
    @Parameter(parameterName = PARAM_SIGN_ADDRESS, parameterType = "String", parameterDes = "签名账户地址")
    @ResponseData(name = "返回值", description = "返回一个Map,包含三个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX,  description = "完整交易序列化字符串,如果交易没达到最小签名数可继续签名"),
            @Key(name = PARAM_TX_HASH,  description = "交易hash"),
            @Key(name = PARAM_COMPLETED, valueType = boolean.class, description = "true:交易已完成(已广播),false:交易没完成,没有达到最小签名数")
    }))
    public Response multiWithdraw(Map<String,Object> params){
        Result result = service.multiWithdraw(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }
}
