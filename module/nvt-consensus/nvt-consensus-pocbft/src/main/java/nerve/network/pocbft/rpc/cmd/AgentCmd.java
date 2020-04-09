package nerve.network.pocbft.rpc.cmd;

import io.nuls.core.rpc.model.*;
import nerve.network.pocbft.model.dto.output.AgentBasicDTO;
import nerve.network.pocbft.model.dto.output.AgentDTO;
import nerve.network.pocbft.service.AgentService;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import static nerve.network.pocbft.constant.CommandConstant.*;
import static nerve.network.pocbft.constant.ParameterConstant.*;

import java.util.List;
import java.util.Map;

/**
 * 共识节点相关接口
 * @author: Jason
 * 2018/11/7
 * */
@Component
public class AgentCmd extends BaseCmd {
    @Autowired
    private AgentService service;

    /**
     * 创建节点
     * */
    @CmdAnnotation(cmd = CMD_CREATE_AGENT, version = 1.0, description = "创建节点交易/create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_AGENT_ADDRESS, parameterType = "String", parameterDes = "节点地址")
    @Parameter(parameterName = PARAM_PACKING_ADDRESS, parameterType = "String", parameterDes = "节点出块地址")
    @Parameter(parameterName = PARAM_REWARD_ADDRESS, parameterType = "String", parameterDes = "奖励地址,默认节点地址", canNull = true)
    @Parameter(parameterName = PARAM_DEPOSIT, parameterType = "String", parameterDes = "抵押金额")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "密码")
    @ResponseData(name = "返回值", description = "创建节点交易HASH", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX_HASH, description = "创建节点交易HASH")
    }))
    public Response createAgent(Map<String,Object> params){
        Result result = service.createAgent(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 追加节点委托
     * */
    @CmdAnnotation(cmd = CMD_APPEND_AGENT_DEPOSIT, version = 1.0, description = "创建节点交易/create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "节点地址")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "密码")
    @Parameter(parameterName = PARAM_AMOUNT, parameterType = "String", parameterDes = "追加金额")
    @Parameter(parameterName = PARAM_AGENT_HASH, parameterType = "String", parameterDes = "节点HASH")
    @ResponseData(name = "返回值", description = "创建节点交易HASH", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX_HASH, description = "创建节点交易HASH")
    }))
    public Response appendAgentDeposit(Map<String,Object> params){
        Result result = service.appendAgentDeposit(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 减少节点委托
     * */
    @CmdAnnotation(cmd = CMD_REDUCE_AGENT_DEPOSIT, version = 1.0, description = "创建节点交易/create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "节点地址")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "密码")
    @Parameter(parameterName = PARAM_AMOUNT, parameterType = "String", parameterDes = "追加金额")
    @Parameter(parameterName = PARAM_AGENT_HASH, parameterType = "String", parameterDes = "节点HASH")
    @ResponseData(name = "返回值", description = "创建节点交易HASH", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX_HASH, description = "创建节点交易HASH")
    }))
    public Response reduceAgentDeposit(Map<String,Object> params){
        Result result = service.reduceAgentDeposit(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 注销节点
     * */
    @CmdAnnotation(cmd = CMD_STOP_AGENT, version = 1.0, description = "注销节点/stop agent")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "节点地址")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "密码")
    @ResponseData(name = "返回值", description = "停止节点交易HASH", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "txHash", description = "停止节点交易HASH")
    }))
    public Response stopAgent(Map<String,Object> params){
        Result result = service.stopAgent(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询共识节点列表
     * */
    @CmdAnnotation(cmd = CMD_GET_AGENT_LIST, version = 1.0, description = "查询当前网络中的共识节点列表/Query the list of consensus nodes in the current network")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_PAGE_NUMBER, requestType = @TypeDescriptor(value = int.class), parameterDes = "页码", canNull = true)
    @Parameter(parameterName = PARAM_PAGE_SIZE, requestType = @TypeDescriptor(value = int.class), parameterDes = "每页大小", canNull = true)
    @Parameter(parameterName = PARAM_KEY_WORD, parameterType = "String", parameterDes = "关键字", canNull = true)
    @ResponseData(name = "返回值", description = "返回一个Page对象，这里只描述Page对象中的集合",
            responseType = @TypeDescriptor(value = List.class, collectionElement = AgentDTO.class)
    )
    public Response getAgentList(Map<String,Object> params){
        Result result = service.getAgentList(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 异构跨链查询共识节点列表
     * */
    @CmdAnnotation(cmd = CMD_GET_BASIC_AGENT_LIST, version = 1.0, description = "异构跨链查询共识节点列表/Heterogeneous cross chain query consensus node list")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_HEIGHT, requestType = @TypeDescriptor(value = long.class), parameterDes = "区块高度", canNull = true)
    @ResponseData(name = "返回值", description = "返回一个Page对象，这里只描述Page对象中的集合",
            responseType = @TypeDescriptor(value = List.class, collectionElement = AgentBasicDTO.class)
    )
    public Response getAgentBasicList(Map<String,Object> params){
        Result result = service.getAgentBasicList(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询指定节点信息
     * */
    @CmdAnnotation(cmd = CMD_GET_AGENT_INFO, version = 1.0, description = "查询指点节点节点详细信息/Query pointer node details")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_AGENT_HASH, parameterType = "String", parameterDes = "节点HASH")
    @ResponseData(name = "返回值", responseType = @TypeDescriptor(value = AgentDTO.class))
    public Response getAgentInfo(Map<String,Object> params){
        Result result = service.getAgentInfo(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 获取当前节点出块地址
     * */
    @CmdAnnotation(cmd = CMD_GET_PACK_ADDRESS_LIST, version = 1.0, description = "获取当前节点出块地址/Get the current node's out-of-block address")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @ResponseData(name = "返回值", description = "当前节点出块地址", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_PACK_ADDRESS, description = "当前节点出块地址")
    }))
    public Response getNodePackingAddress(Map<String,Object> params){
        Result result = service.getNodePackingAddress(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 获取所有节点出块地址/指定N个区块出块地址
     * */
    @CmdAnnotation(cmd = CMD_GET_AGENT_ADDRESS_LIST, version = 1.0, description = "获取当前网络共识节点出块地址列表或则查询最近N个区块的出块地址/Get all node out-of-block addresses or specify N block out-of-block designations")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @ResponseData(name = "返回值", description = "共识节点列表", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_PACK_ADDRESS, description = "共识节点列表")
    }))
    public Response getAgentAddressList(Map<String,Object> params){
        Result result = service.getAgentAddressList(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询指定共识节点状态
     * */
    @CmdAnnotation(cmd = CMD_GET_AGENT_STATUS, version = 1.0, description = "查询指定共识节点状态/query the specified consensus node status 1.0")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_AGENT_HASH, parameterType = "String", parameterDes = "节点HASH")
    @ResponseData(name = "返回值", description = "节点状态", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_STATUS,valueType = Byte.class, description = "节点状态")
    }))
    public Response getAgentStatus(Map<String,Object> params){
        Result result = service.getAgentStatus(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 修改节点共识状态
     * */
    @CmdAnnotation(cmd = CMD_UPDATE_AGENT_CONSENSUS_STATUS, version = 1.0, description = "修改节点共识状态/modifying the Node Consensus State")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @ResponseData(description = "无特定返回值，无错误则表示节点共识状态修改成功")
    public Response updateAgentConsensusStatus(Map<String,Object> params){
        Result result = service.updateAgentConsensusStatus(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 修改节点打包状态
     * */
    @CmdAnnotation(cmd = CMD_UPDATE_AGENT_SATATUS, version = 1.0, description = "修改节点打包状态/modifying the Packing State of Nodes")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_STATUS, requestType = @TypeDescriptor(value = int.class), parameterDes = "节点状态")
    @ResponseData(description = "无特定返回值，无错误则表示节点打包状态修改成功")
    public Response updateAgentStatus(Map<String,Object> params){
        Result result = service.updateAgentStatus(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 获取当前节点的出块账户信息
     * */
    @CmdAnnotation(cmd = CMD_GET_PACKER_INFO, version = 1.0, description = "获取当前节点的出块账户信息/modifying the Packing State of Nodes")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_ADDRESS, description = "当前节点出块地址"),
            @Key(name = PARAM_PASSWORD, description = "当前节点密码"),
            @Key(name = PARAM_PACKING_ADDRESS_LIST, valueType = List.class, valueElement = String.class, description = "当前打包地址列表"),
    }))
    public Response getPackerInfo(Map<String,Object> params){
        Result result = service.getPackerInfo(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 获取共识两轮次间节点变化信息
     * */
    @CmdAnnotation(cmd = CMD_GET_AGENT_CHANGE_INFO, version = 1.0, description = "获取共识两轮次间节点变化信息")
    @Parameter(parameterName = PARAM_CHAIN_ID, parameterType = "int")
    public Response getAgentChangeInfo(Map<String,Object> params){
        Result result = service.getAgentChangeInfo(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }
}
