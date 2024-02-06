package io.nuls.consensus.rpc.cmd;

import io.nuls.common.ConfigBean;
import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.model.dto.output.AccountConsensusInfoDTO;
import io.nuls.consensus.model.dto.output.WholeNetConsensusInfoDTO;
import io.nuls.consensus.service.ChainService;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;

import static io.nuls.consensus.constant.CommandConstant.*;
import static io.nuls.consensus.constant.ParameterConstant.*;

import java.util.List;
import java.util.Map;

/**
 * Consensus Chain Related Interface
 *
 * @author tag
 * 2018/11/7
 */
@Component
@NerveCoreCmd(module = ModuleE.CS)
public class ChainCmd extends BaseCmd {
    @Autowired
    private ChainService service;

    /**
     * Check if this node is a consensus node
     * */
    @CmdAnnotation(cmd = CMD_IS_CONSENSUS_AGENT, version = 1.0, description = "Query whether the specified account is an outgoing address")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_BLOCK_HEADER, parameterType = "String", parameterDes = "Block head")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "address")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "Is it a consensus node")
    }))
    public Response isConsensusAgent(Map<String, Object> params) {
        Result result = service.isConsensusAgent(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Block fork record
     */
    @CmdAnnotation(cmd = CMD_ADD_EVIDENCE_RECORD, version = 1.0, description = "Chain fork evidence record/add evidence record")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_BLOCK_HEADER, parameterType = "String", parameterDes = "Fork block head one")
    @Parameter(parameterName = PARAM_EVIDENCE_HEADER, parameterType = "String", parameterDes = "Fork block head two")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "Processing results")
    }))
    public Response addEvidenceRecord(Map<String, Object> params) {
        Result result = service.addEvidenceRecord(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Shuanghua transaction records
     */
    @CmdAnnotation(cmd = CMD_ADD_DOUBLE_SPEND_RECORD, version = 1.0, description = "Shuanghua transaction records/double spend transaction record ")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_BLOCK, parameterType = "String", parameterDes = "Block information")
    @Parameter(parameterName = PARAM_TX, parameterType = "String",parameterDes = "Forked transaction")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "Processing results")
    }))
    public Response doubleSpendRecord(Map<String, Object> params) {
        Result result = service.doubleSpendRecord(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Query penalty list
     */
    @CmdAnnotation(cmd = CMD_GET_PUNISH_LIST, version = 1.0, description = "Query red and yellow card records/query punish list")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "address")
    @Parameter(parameterName = PARAM_TYPE, requestType = @TypeDescriptor(value = int.class), parameterDes = "Punishment type 0Red and yellow card records 1Red Card Record 2Yellow card record")
    @ResponseData(name = "Return value", description = "Return aMapObject, containing twokey", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RED_PUNISH,valueType = List.class, valueElement = String.class,  description = "List of red cards obtained"),
            @Key(name = PARAM_YELLOW_PUNISH, valueType = List.class, valueElement = String.class, description = "List of yellow card penalties obtained")
    }))
    public Response getPublishList(Map<String, Object> params) {
        Result result = service.getPublishList(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Query consensus information across the entire network
     */
    @CmdAnnotation(cmd = CMD_GET_WHOLE_INFO, version = 1.0, description = "Query consensus data across the entire network/query the consensus information of the whole network")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @ResponseData(name = "Return value", responseType = @TypeDescriptor(value = WholeNetConsensusInfoDTO.class))
    public Response getWholeInfo(Map<String, Object> params) {
        Result result = service.getWholeInfo(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Query consensus information for specified accounts
     */
    @CmdAnnotation(cmd = CMD_GET_INFO, version = 1.0, description = "Query consensus data for specified accounts/query consensus information for specified accounts")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "Account address")
    @ResponseData(name = "Return value", responseType = @TypeDescriptor(value = AccountConsensusInfoDTO.class))
    public Response getInfo(Map<String, Object> params) {
        Result result = service.getInfo(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Obtain current round information
     */
    @CmdAnnotation(cmd = CMD_GET_ROUND_INFO, version = 1.0, description = "Obtain current round information/get current round information")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @ResponseData(name = "Return value", responseType = @TypeDescriptor(value = MeetingRound.class))
    public Response getCurrentRoundInfo(Map<String, Object> params) {
        Result result = service.getCurrentRoundInfo(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Query the member list of the specified block in the round
     */
    @CmdAnnotation(cmd = CMD_GET_ROUND_MEMBER_INFO, version = 1.0, description = "Query the member list of the specified block in the round/Query the membership list of the specified block's rounds")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_EXTEND, parameterType = "String", parameterDes = "Block header extension information")
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_PACKING_ADDRESS_LIST,valueType = List.class, valueElement = String.class,  description = "Current block address list")
    }))
    public Response getRoundMemberList(Map<String, Object> params) {
        Result result = service.getRoundMemberList(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Obtain common module recognition configuration information
     */
    @CmdAnnotation(cmd = CMD_GET_CONSENSUS_CONFIG, version = 1.0, description = "Obtain consensus module configuration information/get consensus config")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_SEED_NODES, description = "Seed node list"),
            @Key(name = PARAM_INFLATION_AMOUNT,valueType = Integer.class, description = "Maximum entrusted amount"),
            @Key(name = PARAM_AGENT_ASSET_ID,valueType = Integer.class, description = "Consensus assetsID"),
            @Key(name = PARAM_AGENT_CHAIN_ID,valueType = Integer.class, description = "Consensus Asset ChainID"),
            @Key(name = PARAM_AWARD_ASSERT_ID,valueType = Integer.class, description = "Reward assetsID（Consensus rewards are assets of this chain）"),
    }))
    @SuppressWarnings("unchecked")
    public Response getConsensusConfig(Map<String, Object> params) {
        Result<ConfigBean> result = service.getConsensusConfig(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Obtain the outbound account information of the current node
     * */
    @CmdAnnotation(cmd = CMD_GET_SEED_NODE_INFO, version = 1.0, description = "Obtain seed node information/get seed node info")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_ADDRESS, description = "Current node block address"),
            @Key(name = PARAM_PASSWORD, description = "Current node password"),
            @Key(name = PARAM_PACKING_ADDRESS_LIST, valueType = List.class, valueElement = String.class, description = "Current packaging address list"),
    }))
    public Response getSeedNodeInfo(Map<String,Object> params){
        Result result = service.getSeedNodeInfo(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * queryStackingInterest rate markup information
     * */
    @CmdAnnotation(cmd = CMD_GET_RATE_ADDITION, version = 1.0, description = "queryStackingInterest rate markup information")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "Is it a consensus node")
    }))
    public Response getRateAddition(Map<String, Object> params) {
        Result result = service.getRateAddition(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * Query specified height block consensus reward units
     * */
    @CmdAnnotation(cmd = CMD_GET_REWARD_UNIT, version = 1.0, description = "Query specified height block consensus reward units")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    @Parameter(parameterName = PARAM_HEIGHT, requestType = @TypeDescriptor(value = int.class),parameterDes = "block height")
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = double.class, description = "Specify high block consensus reward units")
    }))
    public Response getRewardUnit(Map<String, Object> params) {
        Result result = service.getRewardUnit(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }
}
