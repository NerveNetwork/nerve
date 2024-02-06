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
 * Consensus Block Related Interface
 * @author tag
 * 2018/11/7
 * */
@Component
@NerveCoreCmd(module = ModuleE.CS)
public class BlockCmd extends BaseCmd {
    @Autowired
    private BlockService service;

    /**
     * Cache new block header
     * */
    @CmdAnnotation(cmd = CMD_ADD_BLOCK, priority = CmdPriority.HIGH, version = 1.0, description = "Receive and cache new blocks/Receiving and caching new blocks")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_BLOCK_HEADER, parameterType = "String", parameterDes = "Block head")
    @Parameter(parameterName = PARAM_DOWN_LOAD, requestType = @TypeDescriptor(value = int.class), parameterDes = "Block status")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "Whether the interface execution is successful or not")
    }))
    public Response addBlock(Map<String,Object> params){
        Result result = service.addBlock(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Verify block correctness
     * */
    @CmdAnnotation(cmd = CMD_VALID_BLOCK, priority = CmdPriority.HIGH, version = 1.0, description = "Verify Block/verify block correctness")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_DOWN_LOAD, requestType = @TypeDescriptor(value = int.class), parameterDes = "Block status")
    @Parameter(parameterName = PARAM_BLOCK, parameterType = "String", parameterDes = "Block information")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "Verification results")
    }))
    public Response validBlock(Map<String,Object> params){
        Result result = service.validBlock(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Obtain block signature results
     * */
    @CmdAnnotation(cmd = CMD_GET_VOTE_RESULT, priority = CmdPriority.HIGH, version = 1.0, description = "Obtain block signature results/get block voted result")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_BLOCK_HASH, requestType = @TypeDescriptor(value = String.class), parameterDes = "blockhash")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "voteResult",valueType = String.class, description = "Block voting results")
    }))
    public Response getVoteResult(Map<String,Object> params){
        Result result = service.getVoteResult(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Push block signature results
     * */
    @CmdAnnotation(cmd = CMD_NOTICE_VOTE_RESULT, priority = CmdPriority.HIGH, version = 1.0, description = "Push block signature results/push block voted result")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = "voteResult", requestType = @TypeDescriptor(value = String.class), parameterDes = "Block signature result")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "result",valueType = Boolean.class, description = "Operation results")
    }))
    public Response noticeVoteResult(Map<String,Object> params){
        Result result = service.noticeVoteResult(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Receive blocks that require caching
     * */
    @CmdAnnotation(cmd = CMD_RECEIVE_HEADER_LIST, version = 1.0, description = "Receive and cache block list/Receive and cache block lists")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class),parameterDes = "chainid")
    @Parameter(parameterName = PARAM_HEADER_LIST, parameterType = "List<String>",parameterDes = "Block header list")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "Successfully received and processed")
    }))
    public Response receiveHeaderList(Map<String,Object> params){
        Result result = service.receiveHeaderList(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Block rollback
     * */
    @CmdAnnotation(cmd = CMD_CHAIN_ROLLBACK, priority = CmdPriority.HIGH, version = 1.0, description = "Block rollback/chain rollback")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class),parameterDes = "chainid")
    @Parameter(parameterName = PARAM_HEIGHT, requestType = @TypeDescriptor(value = int.class),parameterDes = "The height to which the block is rolled back")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "Block rollback result")
    }))
    public Response chainRollBack(Map<String,Object> params){
        Result result = service.chainRollBack(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }
}
