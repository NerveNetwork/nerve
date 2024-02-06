package io.nuls.crosschain.rpc.cmd;

import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.crosschain.servive.BlockService;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.util.Map;

/**
 * Provide interfaces for block module calls
 * @author tag
 * @date 2019/4/25
 */
@Component
@NerveCoreCmd(module = ModuleE.CC)
public class CcmBlockCmd extends BaseCmd {
    @Autowired
    private BlockService service;
    /**
     * Block module height change notification cross chain module
     * */
    @CmdAnnotation(cmd = "newBlockHeight", version = 1.0, description = "Chain block height change/receive new block height")
    @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID")
    @Parameter(parameterName = "height", parameterType = "long", parameterDes = "chainID")
    @ResponseData(description = "No specific return value, successful without errors")
    public Response newBlockHeight(Map<String,Object> params){
        Result result = service.newBlockHeight(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Node synchronization status change
     * Node synchronization state change
     * */
    @CmdAnnotation(cmd = "syncStatusUpdate", version = 1.0, description = "Node synchronization state change")
    @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID")
    @Parameter(parameterName = "status", parameterType = "int", parameterDes = "state0：In synchronization,1：Sync completed")
    @ResponseData(description = "No specific return value, successful without errors")
    public Response syncStatusUpdate(Map<String,Object> params){
        Result result = service.newBlockHeight(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }
}
