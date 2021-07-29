package network.nerve.swap.rpc.cmd;

import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.Parameter;
import io.nuls.core.rpc.model.Parameters;
import io.nuls.core.rpc.model.ResponseData;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.swap.config.SwapConfig;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.dto.FarmInfoDTO;
import network.nerve.swap.model.dto.FarmUserInfoDTO;
import network.nerve.swap.service.FarmService;

import java.util.Map;

import static network.nerve.swap.constant.SwapCmdConstant.*;

@Component
public class FarmCmd extends BaseCmd {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private SwapConfig swapConfig;

    @Autowired
    private FarmService farmService;

    private NulsLogger logger() {
        return SwapContext.logger;
    }

    @CmdAnnotation(cmd = CREATE_FARM, version = 1.0, description = "创建Farm")
    @Parameters(value = {
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "stakeTokenStr", parameterType = "String", parameterDes = "质押token的类型，示例：1-1"),
            @Parameter(parameterName = "syrupTokenStr", parameterType = "String", parameterDes = "奖励token的类型，示例：1-1"),
            @Parameter(parameterName = "syrupPerBlock", parameterType = "Double", parameterDes = "每个区块的奖励数量"),
            @Parameter(parameterName = "startHeight", parameterType = "Long", parameterDes = "生效高度"),
            @Parameter(parameterName = "lockedHeight", parameterType = "Long", parameterDes = "最小允许退出高度"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
    })
    @ResponseData(description = "交易Hash")
    public Response createFarm(Map<String, Object> params) {
        try {
            String address = (String) params.get("address");
            String stakeTokenStr = (String) params.get("stakeTokenStr");
            String syrupTokenStr = (String) params.get("syrupTokenStr");
            Double syrupPerBlock = (Double) params.get("syrupPerBlock");
            Long startHeight = Long.parseLong("" + params.get("startHeight"));
            Long lockedHeight = Long.parseLong("" + params.get("lockedHeight"));
            String password = (String) params.get("password");
            Double totalSyrupAmount = (Double) params.get("totalSyrupAmount");

            Result<String> result = farmService.createFarm(address, stakeTokenStr, syrupTokenStr, syrupPerBlock, startHeight, lockedHeight, totalSyrupAmount, password);

            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = FARM_STAKE, version = 1.0, description = "质押资产")
    @Parameters(value = {
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "farmHash", parameterType = "String", parameterDes = "farm唯一标识"),
            @Parameter(parameterName = "amount", parameterType = "Double", parameterDes = "要转入的数量"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
    })
    @ResponseData(description = "交易Hash")
    public Response stake(Map<String, Object> params) {
        try {
            String address = (String) params.get("address");
            String farmHash = (String) params.get("farmHash");
            Double amount = (Double) params.get("amount");
            String password = (String) params.get("password");

            Result<String> result = farmService.stake(address, farmHash, amount, password);

            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = FARM_WAITHDRAW, version = 1.0, description = "取回质押资产")
    @Parameters(value = {
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "farmHash", parameterType = "String", parameterDes = "farm唯一标识"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
    })
    @ResponseData(description = "交易Hash")
    public Response withdraw(Map<String, Object> params) {
        try {
            String address = (String) params.get("address");
            String farmHash = (String) params.get("farmHash");
            Double amount = (Double) params.get("amount");
            String password = (String) params.get("password");

            Result<String> result = farmService.withdraw(address, farmHash, amount, password);

            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = FARM_INFO, version = 1.0, description = "获取farm当前状态")
    @Parameters(value = {
            @Parameter(parameterName = "farmHash", parameterType = "String", parameterDes = "farm唯一标识")
    })
    @ResponseData(description = "交易Hash")
    public Response getfarm(Map<String, Object> params) {
        try {
            String farmHash = (String) params.get("farmHash");

            Result<FarmInfoDTO> result = farmService.farmInfo(farmHash);

            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = FARM_USER_INFO, version = 1.0, description = "获取farm当前状态")
    @Parameters(value = {
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "farmHash", parameterType = "String", parameterDes = "farm唯一标识")
    })
    @ResponseData(description = "交易Hash")
    public Response getStakeInfo(Map<String, Object> params) {
        try {
            String address = (String) params.get("userAddress");
            String farmHash = (String) params.get("farmHash");

            Result<FarmUserInfoDTO> result = farmService.farmUserInfo(farmHash, address);

            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }
}
