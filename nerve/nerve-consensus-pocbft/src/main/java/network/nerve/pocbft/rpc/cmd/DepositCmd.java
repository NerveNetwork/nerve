package network.nerve.pocbft.rpc.cmd;

import io.nuls.core.rpc.model.*;
import network.nerve.pocbft.model.bo.StackingAsset;
import network.nerve.pocbft.model.dto.output.DepositDTO;
import network.nerve.pocbft.service.DepositService;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import static network.nerve.pocbft.constant.CommandConstant.*;
import static network.nerve.pocbft.constant.ParameterConstant.*;

import java.util.List;
import java.util.Map;

/**
 * 共识委托相关接口
 * @author tag
 * 2018/11/7
 * */
@Component
public class DepositCmd extends BaseCmd {
    @Autowired
    private DepositService service;

    /**
     * 委托共识
     * */
    @CmdAnnotation(cmd = CMD_DEPOSIT_TO_STACKING, version = 1.0, description = "创建委托交易/deposit to stacking transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "账户地址")
    @Parameter(parameterName = PARAM_DEPOSIT, parameterType = "String", parameterDes = "委托金额")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "账户密码")
    @Parameter(parameterName = PARAM_ASSET_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "资产链ID")
    @Parameter(parameterName = PARAM_ASSET_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "资产ID")
    @Parameter(parameterName = PARAM_DEPOSIT_TYPE, requestType = @TypeDescriptor(value = byte.class), parameterDes = "委托类型")
    @Parameter(parameterName = PARAM_TIME_TYPE, requestType = @TypeDescriptor(value = byte.class), parameterDes = "委托时间类型")
    @ResponseData(name = "返回值", description = "加入共识交易Hash", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX_HASH, description = "加入共识交易Hash")
    }))
    public Response depositToAgent(Map<String,Object> params){
        Result result = service.depositToAgent(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 退出共识
     * */
    @CmdAnnotation(cmd = CMD_WITHDRAW, version = 1.0, description = "退出委托交易/withdraw deposit agent transaction")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "账户地址")
    @Parameter(parameterName = PARAM_TX_HASH, parameterType = "String", parameterDes = "加入共识交易HASH")
    @Parameter(parameterName = PARAM_PASSWORD, parameterType = "String", parameterDes = "账户密码")
    @ResponseData(name = "返回值", description = "退出共识交易Hash", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_TX_HASH, description = "退出共识交易Hash")
    }))
    public Response withdraw(Map<String,Object> params){
        Result result = service.withdraw(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询委托信息列表
     * */
    @CmdAnnotation(cmd = CMD_GET_DEPOSIT_LIST, version = 1.0, description = "查询指定账户或指定节点的委托信息/Query delegation information for a specified account or node")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_PAGE_NUMBER, requestType = @TypeDescriptor(value = int.class), parameterDes = "页码")
    @Parameter(parameterName = PARAM_PAGE_SIZE, requestType = @TypeDescriptor(value = int.class), parameterDes = "每页数量")
    @Parameter(parameterName = PARAM_ADDRESS, parameterType = "String", parameterDes = "账户地址")
    @ResponseData(name = "返回值", description = "返回一个Page对象，这里只描述Page对象中的集合",
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
     * 通过Simple查询抵押资产的资产id和资产链id
     * */
    @CmdAnnotation(cmd = CMD_GET_ASSET_BY_SYMBOL, version = 1.0, description = "通过Simple查询抵押资产的资产id和资产链id")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @Parameter(parameterName = PARAM_SYMBOL, parameterType = "String", parameterDes = "资产SIMPLE")
    @ResponseData(name = "返回值", description = "simple对应的资产信息", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = PARAM_ASSET_CHAIN_ID, description = "simple对应的资产链ID"),
            @Key(name = PARAM_ASSET_ID, description = "simple对应的资产ID")
    }))
    public Response getAssetBySymbol(Map<String,Object> params){
        Result result = service.getAssetBySymbol(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询可参与stacking的资产列表
     * */
    @CmdAnnotation(cmd = CMD_GET_CAN_STACKING_ASSET_LIST, version = 1.0, description = "查询可参与stacking的资产列表")
    @Parameter(parameterName = PARAM_CHAIN_ID, requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    @ResponseData(name = "返回值", description = "返回一个Page对象，这里只描述Page对象中的集合",
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
