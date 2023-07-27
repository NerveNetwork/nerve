package io.nuls.consensus.rpc.cmd;

import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.consensus.service.BlockService;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import static io.nuls.consensus.constant.CommandConstant.*;
import static io.nuls.consensus.constant.ParameterConstant.*;

import java.util.Map;

/**
 * 共识区块相关接口
 * @author tag
 * 2018/11/7
 * */
@Component
@NerveCoreCmd(module = ModuleE.CS)
public class BlockCmd extends BaseCmd {
    @Autowired
    private BlockService service;

    /**
     * 缓存新区块头
     * */
    @CmdAnnotation(cmd = CMD_ADD_BLOCK, priority = CmdPriority.HIGH, version = 1.0, description = "接收并缓存新区块/Receiving and caching new blocks")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_BLOCK_HEADER, parameterType = "String", parameterDes = "区块头")
    @Parameter(parameterName = PARAM_DOWN_LOAD, requestType = @TypeDescriptor(value = int.class), parameterDes = "区块状态")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "接口执行成功与否")
    }))
    public Response addBlock(Map<String,Object> params){
        Result result = service.addBlock(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 验证区块正确性
     * */
    @CmdAnnotation(cmd = CMD_VALID_BLOCK, priority = CmdPriority.HIGH, version = 1.0, description = "验证区块/verify block correctness")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_DOWN_LOAD, requestType = @TypeDescriptor(value = int.class), parameterDes = "区块状态")
    @Parameter(parameterName = PARAM_BLOCK, parameterType = "String", parameterDes = "区块信息")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "验证结果")
    }))
    public Response validBlock(Map<String,Object> params){
        Result result = service.validBlock(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 获取区块签名结果
     * */
    @CmdAnnotation(cmd = CMD_GET_VOTE_RESULT, priority = CmdPriority.HIGH, version = 1.0, description = "获取区块签名结果/get block voted result")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_BLOCK_HASH, requestType = @TypeDescriptor(value = String.class), parameterDes = "区块hash")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "voteResult",valueType = String.class, description = "区块投票结果")
    }))
    public Response getVoteResult(Map<String,Object> params){
        Result result = service.getVoteResult(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 推送区块签名结果
     * */
    @CmdAnnotation(cmd = CMD_NOTICE_VOTE_RESULT, priority = CmdPriority.HIGH, version = 1.0, description = "推送区块签名结果/push block voted result")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = "voteResult", requestType = @TypeDescriptor(value = String.class), parameterDes = "区块签名结果")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "result",valueType = Boolean.class, description = "操作结果")
    }))
    public Response noticeVoteResult(Map<String,Object> params){
        Result result = service.noticeVoteResult(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 接收需缓存的区块
     * */
    @CmdAnnotation(cmd = CMD_RECEIVE_HEADER_LIST, version = 1.0, description = "接收并缓存区块列表/Receive and cache block lists")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class),parameterDes = "链id")
    @Parameter(parameterName = PARAM_HEADER_LIST, parameterType = "List<String>",parameterDes = "区块头列表")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "是否成功接收处理")
    }))
    public Response receiveHeaderList(Map<String,Object> params){
        Result result = service.receiveHeaderList(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 区块回滚
     * */
    @CmdAnnotation(cmd = CMD_CHAIN_ROLLBACK, priority = CmdPriority.HIGH, version = 1.0, description = "区块回滚/chain rollback")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class),parameterDes = "链id")
    @Parameter(parameterName = PARAM_HEIGHT, requestType = @TypeDescriptor(value = int.class),parameterDes = "区块回滚到的高度")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "区块回滚结果")
    }))
    public Response chainRollBack(Map<String,Object> params){
        Result result = service.chainRollBack(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }
}
