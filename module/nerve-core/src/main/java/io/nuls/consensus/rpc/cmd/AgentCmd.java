package io.nuls.consensus.rpc.cmd;

import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.consensus.model.dto.output.AgentBasicDTO;
import io.nuls.consensus.model.dto.output.AgentDTO;
import io.nuls.consensus.model.dto.output.ReduceNonceDTO;
import io.nuls.consensus.service.AgentService;
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
 * Consensus node related interfaces
 * @author tag
 * 2018/11/7
 * */
@Component
@NerveCoreCmd(module = ModuleE.CS)
public class AgentCmd extends BaseCmd {
    @Autowired
    private AgentService service;

    /**
     * Create nodes
     * */
    @CmdAnnotation(cmd = CMD_CREATE_AGENT, version = 1.0, description = "Create node transactions/create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_AGENT_ADDRESS, parameterType = "String", parameterDes = "Node address")
    @Parameter(parameterName = PARAM_PACKING_ADDRESS, parameterType = "String", parameterDes = "Node block address")
    @Parameter(parameterName = PARAM_REWARD_ADDRESS, parameterType = "String", parameterDes = "Reward Address,Default node address", canNull = true)
    @Parameter(parameterName = PARAM_DEPOSIT, parameterType = "String", parameterDes = "Mortgage amount")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "password")
    @ResponseData(name = "Return value", description = "Create node transactionsHASH", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX_HASH, description = "Create node transactionsHASH")
    }))
    public Response createAgent(Map<String,Object> params){
        Result result = service.createAgent(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Add node delegation
     * */
    @CmdAnnotation(cmd = CMD_APPEND_AGENT_DEPOSIT, version = 1.0, description = "Create node transactions/create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Node address")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "password")
    @Parameter(parameterName = PARAM_AMOUNT, parameterType = "String", parameterDes = "Additional amount")
    @ResponseData(name = "Return value", description = "Create node transactionsHASH", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX_HASH, description = "Create node transactionsHASH")
    }))
    public Response appendAgentDeposit(Map<String,Object> params){
        Result result = service.appendAgentDeposit(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Reduce node delegation
     * */
    @CmdAnnotation(cmd = CMD_REDUCE_AGENT_DEPOSIT, version = 1.0, description = "Create node transactions/create agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Node address")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "password")
    @Parameter(parameterName = PARAM_AMOUNT, parameterType = "String", parameterDes = "Additional amount")
    @ResponseData(name = "Return value", description = "Create node transactionsHASH", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX_HASH, description = "Create node transactionsHASH")
    }))
    public Response reduceAgentDeposit(Map<String,Object> params){
        Result result = service.reduceAgentDeposit(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Unregister node
     * */
    @CmdAnnotation(cmd = CMD_STOP_AGENT, version = 1.0, description = "Unregister node/stop agent")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Node address")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "password")
    @ResponseData(name = "Return value", description = "Stop node transactionsHASH", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "txHash", description = "Stop node transactionsHASH")
    }))
    public Response stopAgent(Map<String,Object> params){
        Result result = service.stopAgent(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Query consensus node list
     * */
    @CmdAnnotation(cmd = CMD_GET_AGENT_LIST, version = 1.0, description = "Query the list of consensus nodes in the current network/Query the list of consensus nodes in the current network")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_PAGE_NUMBER, requestType = @TypeDescriptor(value = int.class), parameterDes = "Page number", canNull = true)
    @Parameter(parameterName = PARAM_PAGE_SIZE, requestType = @TypeDescriptor(value = int.class), parameterDes = "Page size", canNull = true)
    @Parameter(parameterName = PARAM_KEY_WORD, parameterType = "String", parameterDes = "keyword", canNull = true)
    @ResponseData(name = "Return value", description = "Return aPageObject, only described herePageCollection in objects",
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
     * Heterogeneous cross chain query consensus node list
     * */
    @CmdAnnotation(cmd = CMD_GET_BASIC_AGENT_LIST, version = 1.0, description = "Heterogeneous cross chain query consensus node list/Heterogeneous cross chain query consensus node list")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_HEIGHT, requestType = @TypeDescriptor(value = long.class), parameterDes = "block height", canNull = true)
    @ResponseData(name = "Return value", description = "Return aPageObject, only described herePageCollection in objects",
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
     * Query specified node information
     * */
    @CmdAnnotation(cmd = CMD_GET_AGENT_INFO, version = 1.0, description = "Query detailed information of pointing nodes/Query pointer node details")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_AGENT_HASH, parameterType = "String", parameterDes = "nodeHASH")
    @ResponseData(name = "Return value", responseType = @TypeDescriptor(value = AgentDTO.class))
    public Response getAgentInfo(Map<String,Object> params){
        Result result = service.getAgentInfo(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Get the current node's outbound address
     * */
    @CmdAnnotation(cmd = CMD_GET_PACK_ADDRESS_LIST, version = 1.0, description = "Get the current node's outbound address/Get the current node's out-of-block address")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @ResponseData(name = "Return value", description = "Current node block address", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_PACK_ADDRESS, description = "Current node block address")
    }))
    public Response getNodePackingAddress(Map<String,Object> params){
        Result result = service.getNodePackingAddress(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Get all node block addresses/specifyNBlock output address
     * */
    @CmdAnnotation(cmd = CMD_GET_AGENT_ADDRESS_LIST, version = 1.0, description = "Obtain the current consensus node block address list or query the most recentNOutbound address of blocks/Get all node out-of-block addresses or specify N block out-of-block designations")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @ResponseData(name = "Return value", description = "Consensus node list", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_PACK_ADDRESS, description = "Consensus node list")
    }))
    public Response getAgentAddressList(Map<String,Object> params){
        Result result = service.getAgentAddressList(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Query the status of specified consensus nodes
     * */
    @CmdAnnotation(cmd = CMD_GET_AGENT_STATUS, version = 1.0, description = "Query the status of specified consensus nodes/query the specified consensus node status 1.0")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_AGENT_HASH, parameterType = "String", parameterDes = "nodeHASH")
    @ResponseData(name = "Return value", description = "Node status", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_STATUS,valueType = Byte.class, description = "Node status")
    }))
    public Response getAgentStatus(Map<String,Object> params){
        Result result = service.getAgentStatus(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Modify node consensus status
     * */
    @CmdAnnotation(cmd = CMD_UPDATE_AGENT_CONSENSUS_STATUS, version = 1.0, description = "Modify node consensus status/modifying the Node Consensus State")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @ResponseData(description = "No specific return value, no error indicates successful modification of node consensus state")
    public Response updateAgentConsensusStatus(Map<String,Object> params){
        Result result = service.updateAgentConsensusStatus(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Modify node packaging status
     * */
    @CmdAnnotation(cmd = CMD_UPDATE_AGENT_SATATUS, version = 1.0, description = "Modify node packaging status/modifying the Packing State of Nodes")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_STATUS, requestType = @TypeDescriptor(value = int.class), parameterDes = "Node status")
    @ResponseData(description = "No specific return value, no error indicates successful modification of node packaging status")
    public Response updateAgentStatus(Map<String,Object> params){
        Result result = service.updateAgentStatus(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Obtain the outbound account information of the current node
     * */
    @CmdAnnotation(cmd = CMD_GET_PACKER_INFO, version = 1.0, description = "Obtain the outbound account information of the current node/modifying the Packing State of Nodes")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_ADDRESS, description = "Current node block address"),
            @Key(name = PARAM_PASSWORD, description = "Current node password"),
            @Key(name = PARAM_PACKING_ADDRESS_LIST, valueType = List.class, valueElement = String.class, description = "Current packaging address list"),
    }))
    public Response getPackerInfo(Map<String,Object> params){
        Result result = service.getPackerInfo(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Obtain node change information between two rounds of consensus
     * */
    @CmdAnnotation(cmd = CMD_GET_AGENT_CHANGE_INFO, version = 1.0, description = "Obtain node change information between two rounds of consensus")
    @Parameter(parameterName = PARAM_CHAIN_ID, parameterType = "int")
    public Response getAgentChangeInfo(Map<String,Object> params){
        Result result = service.getAgentChangeInfo(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Obtained when exiting node marginNONCEValue List
     * */
    @CmdAnnotation(cmd = CMD_GET_REDUCE_DEPOSIT_LIST, version = 1.0, description = "Query the status of specified consensus nodes/query the specified consensus node status 1.0")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_AGENT_HASH, parameterType = "String", parameterDes = "nodeHASH")
    @Parameter(parameterName = PARAM_QUIT_ALL, requestType = @TypeDescriptor(value = int.class), parameterDes = "Do you want to exit all")
    @Parameter(parameterName = PARAM_REDUCE_AMOUNT, parameterType = "String", parameterDes = "Exit amount")
    @ResponseData(name = "Return value", description = "returnNONCEData List",
            responseType = @TypeDescriptor(value = List.class, collectionElement = ReduceNonceDTO.class)
    )
    public Response getReduceDepositNonceList(Map<String,Object> params){
        Result result = service.getReduceDepositNonceList(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Virtual Bank Node Change
     * Virtual bank node change
     * */
    @CmdAnnotation(cmd = CMD_GET_VIRTUAL_AGENT_CHANGE, version = 1.0, description = "Virtual Bank Node Change/Virtual bank node change")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_HEIGHT, requestType = @TypeDescriptor(value = long.class), parameterDes = "height")
    @Parameter(parameterName = PARAM_VIRTUAL_AGENT_LIST, parameterDes = "Virtual Bank Node List", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class))
    @ResponseData(description = "No specific return value, no error indicates successful modification of node packaging status")
    public Response virtualAgentChange(Map<String,Object> params){
        Result result = service.virtualAgentChange(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }
}
