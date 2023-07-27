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
 * 共识链相关接口
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
     * 查看本节点是否为共识节点
     * */
    @CmdAnnotation(cmd = CMD_IS_CONSENSUS_AGENT, version = 1.0, description = "查询指定账户是否为出块地址")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_BLOCK_HEADER, parameterType = "String", parameterDes = "区块头")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "地址")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "是否为共识节点")
    }))
    public Response isConsensusAgent(Map<String, Object> params) {
        Result result = service.isConsensusAgent(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 区块分叉记录
     */
    @CmdAnnotation(cmd = CMD_ADD_EVIDENCE_RECORD, version = 1.0, description = "链分叉证据记录/add evidence record")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_BLOCK_HEADER, parameterType = "String", parameterDes = "分叉区块头一")
    @Parameter(parameterName = PARAM_EVIDENCE_HEADER, parameterType = "String", parameterDes = "分叉区块头二")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "处理结果")
    }))
    public Response addEvidenceRecord(Map<String, Object> params) {
        Result result = service.addEvidenceRecord(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 双花交易记录
     */
    @CmdAnnotation(cmd = CMD_ADD_DOUBLE_SPEND_RECORD, version = 1.0, description = "双花交易记录/double spend transaction record ")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_BLOCK, parameterType = "String", parameterDes = "区块信息")
    @Parameter(parameterName = PARAM_TX, parameterType = "String",parameterDes = "分叉交易")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "处理结果")
    }))
    public Response doubleSpendRecord(Map<String, Object> params) {
        Result result = service.doubleSpendRecord(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询惩罚列表
     */
    @CmdAnnotation(cmd = CMD_GET_PUNISH_LIST, version = 1.0, description = "查询红黄牌记录/query punish list")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "地址")
    @Parameter(parameterName = PARAM_TYPE, requestType = @TypeDescriptor(value = int.class), parameterDes = "惩罚类型 0红黄牌记录 1红牌记录 2黄牌记录")
    @ResponseData(name = "返回值", description = "返回一个Map对象，包含两个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RED_PUNISH,valueType = List.class, valueElement = String.class,  description = "获得的红牌列表"),
            @Key(name = PARAM_YELLOW_PUNISH, valueType = List.class, valueElement = String.class, description = "获得的黄牌惩罚列表")
    }))
    public Response getPublishList(Map<String, Object> params) {
        Result result = service.getPublishList(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询全网共识信息
     */
    @CmdAnnotation(cmd = CMD_GET_WHOLE_INFO, version = 1.0, description = "查询全网共识数据/query the consensus information of the whole network")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @ResponseData(name = "返回值", responseType = @TypeDescriptor(value = WholeNetConsensusInfoDTO.class))
    public Response getWholeInfo(Map<String, Object> params) {
        Result result = service.getWholeInfo(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询指定账户的共识信息
     */
    @CmdAnnotation(cmd = CMD_GET_INFO, version = 1.0, description = "查询指定账户共识数据/query consensus information for specified accounts")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "账户地址")
    @ResponseData(name = "返回值", responseType = @TypeDescriptor(value = AccountConsensusInfoDTO.class))
    public Response getInfo(Map<String, Object> params) {
        Result result = service.getInfo(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 获取当前轮次信息
     */
    @CmdAnnotation(cmd = CMD_GET_ROUND_INFO, version = 1.0, description = "获取当前轮次信息/get current round information")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @ResponseData(name = "返回值", responseType = @TypeDescriptor(value = MeetingRound.class))
    public Response getCurrentRoundInfo(Map<String, Object> params) {
        Result result = service.getCurrentRoundInfo(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询指定区块所在轮次成员列表
     */
    @CmdAnnotation(cmd = CMD_GET_ROUND_MEMBER_INFO, version = 1.0, description = "查询指定区块所在轮次的成员列表/Query the membership list of the specified block's rounds")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_EXTEND, parameterType = "String", parameterDes = "区块头扩展信息")
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_PACKING_ADDRESS_LIST,valueType = List.class, valueElement = String.class,  description = "当前伦次出块地址列表")
    }))
    public Response getRoundMemberList(Map<String, Object> params) {
        Result result = service.getRoundMemberList(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 获取共模块识配置信息
     */
    @CmdAnnotation(cmd = CMD_GET_CONSENSUS_CONFIG, version = 1.0, description = "获取共识模块配置信息/get consensus config")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_SEED_NODES, description = "种子节点列表"),
            @Key(name = PARAM_INFLATION_AMOUNT,valueType = Integer.class, description = "委托金额最大值"),
            @Key(name = PARAM_AGENT_ASSET_ID,valueType = Integer.class, description = "共识资产ID"),
            @Key(name = PARAM_AGENT_CHAIN_ID,valueType = Integer.class, description = "共识资产链ID"),
            @Key(name = PARAM_AWARD_ASSERT_ID,valueType = Integer.class, description = "奖励资产ID（共识奖励为本链资产）"),
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
     * 获取当前节点的出块账户信息
     * */
    @CmdAnnotation(cmd = CMD_GET_SEED_NODE_INFO, version = 1.0, description = "获取种子节点信息/get seed node info")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_ADDRESS, description = "当前节点出块地址"),
            @Key(name = PARAM_PASSWORD, description = "当前节点密码"),
            @Key(name = PARAM_PACKING_ADDRESS_LIST, valueType = List.class, valueElement = String.class, description = "当前打包地址列表"),
    }))
    public Response getSeedNodeInfo(Map<String,Object> params){
        Result result = service.getSeedNodeInfo(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询Stacking利率加成信息
     * */
    @CmdAnnotation(cmd = CMD_GET_RATE_ADDITION, version = 1.0, description = "查询Stacking利率加成信息")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = Boolean.class, description = "是否为共识节点")
    }))
    public Response getRateAddition(Map<String, Object> params) {
        Result result = service.getRateAddition(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询指定高度区块共识奖励单位
     * */
    @CmdAnnotation(cmd = CMD_GET_REWARD_UNIT, version = 1.0, description = "查询指定高度区块共识奖励单位")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_HEIGHT, requestType = @TypeDescriptor(value = int.class),parameterDes = "区块高度")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_RESULT_VALUE,valueType = double.class, description = "指定高度区块共识奖励单位")
    }))
    public Response getRewardUnit(Map<String, Object> params) {
        Result result = service.getRewardUnit(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }
}
