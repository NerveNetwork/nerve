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
import network.nerve.swap.model.dto.FarmInfoVO;
import network.nerve.swap.model.dto.FarmUserInfoDTO;
import network.nerve.swap.model.dto.FarmUserInfoVO;
import network.nerve.swap.service.FarmService;

import java.util.List;
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

    @CmdAnnotation(cmd = CREATE_FARM, version = 1.0, description = "establishFarm")
    @Parameters(value = {
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "stakeTokenStr", parameterType = "String", parameterDes = "pledgetokenType of, example：1-1"),
            @Parameter(parameterName = "syrupTokenStr", parameterType = "String", parameterDes = "rewardtokenType of, example：1-1"),
            @Parameter(parameterName = "syrupPerBlock", parameterType = "Double", parameterDes = "The number of rewards per block"),
            @Parameter(parameterName = "startHeight", parameterType = "Long", parameterDes = "Effective height"),
            @Parameter(parameterName = "lockedHeight", parameterType = "Long", parameterDes = "Minimum allowable exit height"),
            @Parameter(parameterName = "modifiable", parameterType = "Boolean", parameterDes = "Is modification allowed"),
            @Parameter(parameterName = "withdrawLockTime", parameterType = "Long", parameterDes = "quitfarmPost lock time（second）"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "Account password"),
    })
    @ResponseData(description = "transactionHash")
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

            boolean modifiable = (boolean) params.get("modifiable");
            long withdrawLockTime = Long.parseLong("" + params.get("withdrawLockTime"));

            Result<String> result = farmService.createFarm(address, stakeTokenStr, syrupTokenStr, syrupPerBlock, startHeight, lockedHeight, totalSyrupAmount, modifiable, withdrawLockTime, password);

            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = FARM_STAKE, version = 1.0, description = "Pledged assets")
    @Parameters(value = {
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "farmHash", parameterType = "String", parameterDes = "farmUnique identifier"),
            @Parameter(parameterName = "amount", parameterType = "Double", parameterDes = "Quantity to be transferred in"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "Account password"),
    })
    @ResponseData(description = "transactionHash")
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

    @CmdAnnotation(cmd = FARM_WAITHDRAW, version = 1.0, description = "Retrieve pledged assets")
    @Parameters(value = {
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "farmHash", parameterType = "String", parameterDes = "farmUnique identifier"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "Account password"),
    })
    @ResponseData(description = "transactionHash")
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

    @CmdAnnotation(cmd = FARM_INFO, version = 1.0, description = "obtainfarmcurrent state")
    @Parameters(value = {
            @Parameter(parameterName = "farmHash", parameterType = "String", parameterDes = "farmUnique identifier")
    })
    @ResponseData(description = "farmdetails")
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

    @CmdAnnotation(cmd = FARM_LIST, version = 1.0, description = "obtainfarmlist")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
    })
    @ResponseData(description = "farmlist")
    public Response getfarmList(Map<String, Object> params) {
        try {
            int chainId = (int )params.get("chainId");
            Result<List<FarmInfoVO>> result = farmService.getFarmList(chainId);
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = FARM_USER_INFO, version = 1.0, description = "obtainfarmcurrent state")
    @Parameters(value = {
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "farmHash", parameterType = "String", parameterDes = "farmUnique identifier")
    })
    @ResponseData(description = "transactionHash")
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

    @CmdAnnotation(cmd = FARM_INFO_DETAIL, version = 1.0, description = "obtainfarmcurrent state")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "farmHash", parameterType = "String", parameterDes = "farmUnique identifier")
    })
    @ResponseData(description = "farmdetails")
    public Response farmInfo(Map<String, Object> params) {
        try {
            int chainId = (int )params.get("chainId");
            String farmHash = (String) params.get("farmHash");
            Result<FarmInfoVO> result = farmService.farmDetail(chainId,farmHash);

            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = FARM_USER_DETAIL, version = 1.0, description = "obtainfarmcurrent state")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "farmHash", parameterType = "String", parameterDes = "farmUnique identifier")
    })
    @ResponseData(description = "transactionHash")
    public Response getStakeDetail(Map<String, Object> params) {
        try {
            int chainId = (int )params.get("chainId");
            String address = (String) params.get("userAddress");
            String farmHash = (String) params.get("farmHash");

            Result<FarmUserInfoVO> result = farmService.farmUserDetail(chainId,farmHash, address);

            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }
}
