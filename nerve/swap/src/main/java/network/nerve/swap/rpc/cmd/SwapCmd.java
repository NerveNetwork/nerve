package network.nerve.swap.rpc.cmd;

import fchClass.Cash;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import keyTools.KeyTools;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.config.SwapConfig;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.enums.BlockType;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.StableSwapHelper;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.TokenAmount;
import network.nerve.swap.model.bo.StableCoin;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.RemoveLiquidityBus;
import network.nerve.swap.model.business.stable.StableAddLiquidityBus;
import network.nerve.swap.model.business.stable.StableRemoveLiquidityBus;
import network.nerve.swap.model.dto.RealAddLiquidityOrderDTO;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.vo.RouteVO;
import network.nerve.swap.model.vo.StableCoinVo;
import network.nerve.swap.model.vo.SwapPairVO;
import network.nerve.swap.model.vo.TokenAmountVo;
import network.nerve.swap.rpc.call.AccountCall;
import network.nerve.swap.service.SwapService;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.utils.SwapUtils;
import network.nerve.swap.utils.fch.BtcSignData;
import network.nerve.swap.utils.fch.FchUtil;
import network.nerve.swap.utils.fch.WithdrawalUTXOTxData;
import org.bitcoinj.core.ECKey;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static network.nerve.swap.constant.SwapCmdConstant.*;
import static network.nerve.swap.utils.SwapUtils.wrapperFailed;

/**
 * Heterogeneous chain information provision command
 *
 * @author: Mimi
 * @date: 2020-02-28
 */
@Component
public class SwapCmd extends BaseCmd {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private SwapConfig swapConfig;
    @Autowired
    private SwapService swapService;
    @Autowired
    private StableSwapHelper stableSwapHelper;
    @Autowired
    private SwapHelper swapHelper;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;

    private NulsLogger logger() {
        return SwapContext.logger;
    }

    @CmdAnnotation(cmd = BATCH_BEGIN, version = 1.0, description = "Start notification for a batch, generating information for the current batch/batch begin")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "blockType", parameterType = "int", parameterDes = "Block processing mode, Packaging blocks - 0, Verify Block - 1"),
            @Parameter(parameterName = "blockHeight", parameterType = "long", parameterDes = "The height of the currently packaged blocks"),
            @Parameter(parameterName = "preStateRoot", parameterType = "String", parameterDes = "The previous block'sstateRoot"),
            @Parameter(parameterName = "blockTime", parameterType = "long", parameterDes = "The current packaged block time")
    })
    @ResponseData(description = "No specific return value, successful without errors")
    public Response batchBegin(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            Integer blockType = (Integer) params.get("blockType");
            ChainManager.chainHandle(chainId, blockType);
            Long blockHeight = Long.parseLong(params.get("blockHeight").toString());
            Long blockTime = Long.parseLong(params.get("blockTime").toString());
            String preStateRoot = (String) params.get("preStateRoot");
            logger().info("[{}]Swapmodule[{}]start, chainId: {}, blockTime: {}, preStateRoot: {}",
                    blockHeight, BlockType.getType(blockType).name(), chainId, blockTime, preStateRoot);
            swapService.begin(chainId, blockHeight, blockTime, preStateRoot);
            return success();
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = INVOKE, version = 1.0, description = "After the batch notification starts, execute it one by one/invoke one by one")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "blockType", parameterType = "int", parameterDes = "Block processing mode, Packaging blocks - 0, Verify Block - 1"),
            @Parameter(parameterName = "blockHeight", parameterType = "long", parameterDes = "The height of the currently packaged blocks"),
            @Parameter(parameterName = "blockTime", parameterType = "long", parameterDes = "The current packaged block time"),
            @Parameter(parameterName = "tx", parameterType = "String", parameterDes = "Serialized transactionHEXEncoding string")
    })
    @ResponseData(name = "Return value", description = "Return aMapObject, containing twokey", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "success", valueType = Boolean.class, description = "Whether the execution was successful"),
            @Key(name = "txList", valueType = List.class, valueElement = String.class, description = "Newly generated system transaction serialization string list(Currently, only one transaction has been returned, resulting in a successful transaction perhaps Failed return transaction)")
    }))
    public Response invokeOneByOne(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            Integer blockType = (Integer) params.get("blockType");
            ChainManager.chainHandle(chainId, blockType);
            Long blockHeight = Long.parseLong(params.get("blockHeight").toString());
            Long blockTime = Long.parseLong(params.get("blockTime").toString());
            String txData = (String) params.get("tx");
            Transaction tx = new Transaction();
            tx.parse(RPCUtil.decode(txData), 0);
            logger().info("[{}]Swapmodule[{}]handle, chainId: {}, blockTime: {}, txStr: {}",
                    blockHeight, BlockType.getType(blockType).name(), chainId, blockTime, txData);
            Result result = swapService.invokeOneByOne(chainId, blockHeight, blockTime, tx);
            if (result.isFailed()) {
                logger().error("Processing failed: {}", result.toString());
                return wrapperFailed(result);
            }
            if (result.getData() == null) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("txList", List.of());
                return success(resultData);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = BATCH_END, version = 1.0, description = "Notify the end of the current batch and return the result/batch end")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "blockType", parameterType = "int", parameterDes = "Block processing mode, Packaging blocks - 0, Verify Block - 1"),
            @Parameter(parameterName = "blockHeight", parameterType = "long", parameterDes = "The height of the currently packaged blocks")
    })
    @ResponseData(name = "Return value", description = "Return aMapObject, containing twokey", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "stateRoot", description = "currentstateRoot")
    }))
    public Response batchEnd(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            Integer blockType = (Integer) params.get("blockType");
            ChainManager.chainHandle(chainId, blockType);
            Long blockHeight = Long.parseLong(params.get("blockHeight").toString());

            Result result = swapService.end(chainId, blockHeight);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            Map<String, Object> dataMap = (Map<String, Object>) result.getData();
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("stateRoot", dataMap.get("stateRoot"));

            logger().info("[{}]Swapmodule[{}]finish, chainId: {}, stateRoot: {}",
                    blockHeight, BlockType.getType(blockType).name(), chainId, dataMap.get("stateRoot"));
            return success(resultMap);
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = IS_LEGAL_COIN_FOR_ADD_STABLE, version = 1.0, description = "Check if the currency added to the stablecoin exchange pool is legal")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "Transaction to address"),
            @Parameter(parameterName = "assetChainId", parameterType = "int", parameterDes = "Currency ChainID"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterDes = "Currency assetsID"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Whether the execution was successful")
    }))
    public Response checkLegalCoinForAddStable(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            Integer assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            logger().debug("chainId: {}, stablePairAddress: {}, assetChainId: {}, assetId: {},", chainId, stablePairAddress, assetChainId, assetId);
            boolean legalCoin = stableSwapHelper.isLegalCoinForAddStable(chainId, stablePairAddress, assetChainId, assetId);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", legalCoin);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = IS_LEGAL_COIN_FOR_REMOVE_STABLE, version = 1.0, description = "Check if the currency removed from the stablecoin exchange pool is legal")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "Transaction to address"),
            @Parameter(parameterName = "assetChainId", parameterType = "int", parameterDes = "Currency ChainID"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterDes = "Currency assetsID"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Whether the execution was successful")
    }))
    public Response checkLegalCoinForRemoveStable(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            Integer assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            logger().debug("chainId: {}, stablePairAddress: {}, assetChainId: {}, assetId: {},", chainId, stablePairAddress, assetChainId, assetId);
            boolean legalCoin = stableSwapHelper.isLegalCoinForRemoveStable(chainId, stablePairAddress, assetChainId, assetId);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", legalCoin);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = IS_LEGAL_COIN_FOR_STABLE, version = 1.0, description = "Check if the currency from the stablecoin exchange pool is legal")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "Transaction to address"),
            @Parameter(parameterName = "assetChainId", parameterType = "int", parameterDes = "Currency ChainID"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterDes = "Currency assetsID"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Whether the execution was successful")
    }))
    public Response checkLegalCoinForStable(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            Integer assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            logger().debug("chainId: {}, stablePairAddress: {}, assetChainId: {}, assetId: {},", chainId, stablePairAddress, assetChainId, assetId);
            boolean legalCoin = stableSwapHelper.isLegalCoinForStable(chainId, stablePairAddress, assetChainId, assetId);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", legalCoin);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = IS_LEGAL_STABLE, version = 1.0, description = "Check if the stablecoin exchange pool is legal")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "Transaction to address")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Whether the execution was successful")
    }))
    public Response checkLegalStable(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            logger().debug("chainId: {}, stablePairAddress: {}", chainId, stablePairAddress);
            boolean legalCoin = stableSwapHelper.isLegalStable(chainId, stablePairAddress);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", legalCoin);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = IS_LEGAL_SWAP_FEE_RATE, version = 1.0, description = "Check if the transaction is legal for fee customization")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "swapPairAddress", parameterType = "String", parameterDes = "Transaction to address"),
            @Parameter(parameterName = "feeRate", parameterType = "int", parameterDes = "Handling fee rate")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Whether the execution was successful")
    }))
    public Response checkLegalSwapFeeRate(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String swapPairAddress = (String) params.get("swapPairAddress");
            Integer feeRate = Integer.parseInt(params.get("feeRate").toString());
            logger().debug("chainId: {}, swapPairAddress: {}, feeRate: {}", chainId, swapPairAddress, feeRate);
            boolean legal = swapHelper.isLegalSwapFeeRate(chainId, swapPairAddress, feeRate);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", legal);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = ADD_COIN_FOR_STABLE, version = 1.0, description = "Add currency to the stablecoin exchange pool")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "Transaction to address"),
            @Parameter(parameterName = "assetChainId", parameterType = "int", parameterDes = "Currency ChainID"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterDes = "Currency assetsID"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Whether the execution was successful")
    }))
    public Response addCoinForStable(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            Integer assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            boolean legalCoin = stableSwapHelper.addCoinForStable(chainId, stablePairAddress, assetChainId, assetId);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", legalCoin);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = REMOVE_COIN_FOR_STABLE, version = 1.0, description = "Remove currency from the stablecoin exchange pool")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "Transaction to address"),
            @Parameter(parameterName = "assetChainId", parameterType = "int", parameterDes = "Currency ChainID"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterDes = "Currency assetsID"),
            @Parameter(parameterName = "status", parameterType = "String", parameterDes = "remove/recovery"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Whether the execution was successful")
    }))
    public Response removeCoinForStable(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            Integer assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            String status = (String) params.get("status");
            boolean legalCoin = stableSwapHelper.removeCoinForStableV2(chainId, stablePairAddress, assetChainId, assetId, status);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", legalCoin);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = PAUSE_COIN_FOR_STABLE, version = 1.0, description = "Suspend currency trading in the Multi-Routing pool")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "Transaction to address"),
            @Parameter(parameterName = "assetChainId", parameterType = "int", parameterDes = "Currency ChainID"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterDes = "Currency assetsID"),
            @Parameter(parameterName = "status", parameterType = "String", parameterDes = "remove/recovery"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Whether the execution was successful")
    }))
    public Response pauseCoinForStable(Map<String, Object> params) {
        try {
            if (!swapHelper.isSupportProtocol31()) {
                throw new RuntimeException("error protocol");
            }
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            Integer assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            String status = (String) params.get("status");
            boolean legalCoin = stableSwapHelper.pauseCoinForStable(chainId, stablePairAddress, assetChainId, assetId, status);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", legalCoin);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = UPDATE_SWAP_PAIR_FEE_RATE, version = 1.0, description = "SWAP Customized handling fee")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "swapPairAddress", parameterType = "String", parameterDes = "Transaction to address"),
            @Parameter(parameterName = "feeRate", parameterType = "int", parameterDes = "Handling fee rate"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Whether the execution was successful")
    }))
    public Response updateSwapPairFeeRate(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String swapPairAddress = (String) params.get("swapPairAddress");
            Integer feeRate = Integer.parseInt(params.get("feeRate").toString());
            boolean success = swapHelper.updateSwapPairFeeRate(chainId, swapPairAddress, feeRate);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", success);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = ADD_STABLE_FOR_SWAP_TRADE, version = 1.0, description = "Add stablecoin trading pairs-Used forSwaptransaction")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "Transaction to address")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Whether the execution was successful")
    }))
    public Response addStableForSwapTrade(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            boolean success = stableSwapHelper.addStableForSwapTrade(chainId, stablePairAddress);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", success);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = REMOVE_STABLE_FOR_SWAP_TRADE, version = 1.0, description = "Remove stablecoin trading pairs-Used forSwaptransaction")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "Transaction to address")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Whether the execution was successful")
    }))
    public Response removeStableForSwapTrade(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            boolean success = stableSwapHelper.removeStableForSwapTrade(chainId, stablePairAddress);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", success);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = BEST_TRADE_EXACT_IN, version = 1.0, description = "Finding the best trading path")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "tokenInStr", parameterType = "String", parameterDes = "Types of assets sold, examples：1-1"),
            @Parameter(parameterName = "tokenInAmount", requestType = @TypeDescriptor(value = String.class), parameterDes = "Number of assets sold"),
            @Parameter(parameterName = "tokenOutStr", parameterType = "String", parameterDes = "Types of purchased assets, examples：1-1"),
            @Parameter(parameterName = "maxPairSize", requestType = @TypeDescriptor(value = int.class), parameterDes = "Deepest trading path"),
            @Parameter(parameterName = "pairs", requestType = @TypeDescriptor(value = String[].class), parameterDes = "List of all transaction pairs in the current network"),
            @Parameter(parameterName = "resultRule", parameterType = "String", parameterDes = "`bestPrice`, `impactPrice`. according to[Optimal price]and[Price impact]Using factors to obtain results, defaults to using[Price impact]Using factors to obtain results")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "tokenPath", valueType = List.class, description = "Best trading path"),
            @Key(name = "tokenAmountIn", valueType = TokenAmountVo.class, description = "Selling assets"),
            @Key(name = "tokenAmountOut", valueType = TokenAmountVo.class, description = "Buying assets"),
    }))
    public Response bestTradeExactIn(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String tokenInStr = params.get("tokenInStr").toString();
            String tokenOutStr = params.get("tokenOutStr").toString();
            String resultRule = (String) params.get("resultRule");
            BigInteger tokenInAmount = new BigInteger(params.get("tokenInAmount").toString());
            Integer maxPairSize = Integer.parseInt(params.get("maxPairSize").toString());
            List<String> pairsList = (List<String>) params.get("pairs");
            TokenAmount tokenAmountIn = new TokenAmount(SwapUtils.parseTokenStr(tokenInStr), tokenInAmount);
            NerveToken tokenOut = SwapUtils.parseTokenStr(tokenOutStr);
            List<RouteVO> bestTrades = swapHelper.bestTradeExactIn(chainId, pairsList, tokenAmountIn, tokenOut, maxPairSize, resultRule);
            if (bestTrades == null || bestTrades.isEmpty()) {
                return failed(SwapErrorCode.DATA_NOT_FOUND);
            }
            Map<String, Object> resultData = this.makeBestTradeExactIn(bestTrades.get(0));
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    private Map makeBestTradeExactIn(RouteVO routeVO) {
        Map<String, Object> resultData = new HashMap<>();
        List<SwapPairVO> path = routeVO.getPath();
        TokenAmount tokenAmountIn = routeVO.getTokenAmountIn();
        List<String> tokenPath = new ArrayList<>();
        NerveToken in = tokenAmountIn.getToken();
        tokenPath.add(in.str());
        for (SwapPairVO vo : path) {
            NerveToken token0 = vo.getToken0();
            NerveToken token1 = vo.getToken1();
            if (in.equals(token0)) {
                tokenPath.add(token1.str());
                in = token1;
            } else {
                tokenPath.add(token0.str());
                in = token0;
            }
        }
        resultData.put("tokenPath", tokenPath);
        resultData.put("tokenAmountIn", tokenAmountIn.toVo());
        resultData.put("tokenAmountOut", routeVO.getTokenAmountOut().toVo());
        return resultData;
    }

    @CmdAnnotation(cmd = SWAP_CREATE_PAIR, version = 1.0, description = "establishSwapTransaction pairs")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "Account password"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "assetBType of, example：1-1")
    })
    @ResponseData(description = "transactionhash")
    public Response swapCreatePair(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String address = (String) params.get("address");
            String tokenAStr = (String) params.get("tokenAStr");
            String tokenBStr = (String) params.get("tokenBStr");
            String password = (String) params.get("password");
            Result<String> result = swapService.swapCreatePair(chainId, address, password, tokenAStr, tokenBStr);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_ADD_LIQUIDITY, version = 1.0, description = "AddSwapLiquidity")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "Account password"),
            @Parameter(parameterName = "amountA", parameterType = "String", parameterDes = "Added assetsAQuantity of"),
            @Parameter(parameterName = "amountB", parameterType = "String", parameterDes = "Added assetsBQuantity of"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "assetBType of, example：1-1"),
            @Parameter(parameterName = "amountAMin", parameterType = "String", parameterDes = "assetAMinimum added value"),
            @Parameter(parameterName = "amountBMin", parameterType = "String", parameterDes = "assetBMinimum added value"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "Expiration time"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "Liquidity share receiving address")
    })
    @ResponseData(description = "transactionhash")
    public Response swapAddLiquidity(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            BigInteger amountA = new BigInteger(params.get("amountA").toString());
            BigInteger amountB = new BigInteger(params.get("amountB").toString());
            String tokenAStr = (String) params.get("tokenAStr");
            String tokenBStr = (String) params.get("tokenBStr");
            BigInteger amountAMin = new BigInteger(params.get("amountAMin").toString());
            BigInteger amountBMin = new BigInteger(params.get("amountBMin").toString());
            Long deadline = Long.parseLong(params.get("deadline").toString());
            String to = (String) params.get("to");
            Result<String> result = swapService.swapAddLiquidity(chainId, address, password, amountA, amountB,
                    tokenAStr, tokenBStr, amountAMin, amountBMin, deadline, to);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_REMOVE_LIQUIDITY, version = 1.0, description = "removeSwapLiquidity")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "Account password"),
            @Parameter(parameterName = "amountLP", parameterType = "String", parameterDes = "Removed assetsLPQuantity of"),
            @Parameter(parameterName = "tokenLPStr", parameterType = "String", parameterDes = "assetLPType of, example：1-1"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "assetBType of, example：1-1"),
            @Parameter(parameterName = "amountAMin", parameterType = "String", parameterDes = "assetAMinimum removal value"),
            @Parameter(parameterName = "amountBMin", parameterType = "String", parameterDes = "assetBMinimum removal value"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "Expiration time"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "Remove liquidity share receiving address")
    })
    @ResponseData(description = "transactionhash")
    public Response swapRemoveLiquidity(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            BigInteger amountLP = new BigInteger(params.get("amountLP").toString());
            String tokenLPStr = (String) params.get("tokenLPStr");
            String tokenAStr = (String) params.get("tokenAStr");
            String tokenBStr = (String) params.get("tokenBStr");
            BigInteger amountAMin = new BigInteger(params.get("amountAMin").toString());
            BigInteger amountBMin = new BigInteger(params.get("amountBMin").toString());
            Long deadline = Long.parseLong(params.get("deadline").toString());
            String to = (String) params.get("to");
            Result<String> result = swapService.swapRemoveLiquidity(chainId, address, password, amountLP, tokenLPStr,
                    tokenAStr, tokenBStr, amountAMin, amountBMin, deadline, to);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_TOKEN_TRADE, version = 1.0, description = "SwapCurrency exchange")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "Account password"),
            @Parameter(parameterName = "amountIn", parameterType = "String", parameterDes = "Number of assets sold"),
            @Parameter(parameterName = "tokenPath", parameterType = "String[]", parameterDes = "Currency exchange asset path, the last asset in the path is the asset that the user wants to buy, such as sellingAbuyB: [A, B] or [A, C, B]"),
            @Parameter(parameterName = "amountOutMin", parameterType = "String", parameterDes = "Minimum number of assets to be purchased"),
            @Parameter(parameterName = "feeTo", parameterType = "String", parameterDes = "Withdraw a portion of the transaction fee to the designated receiving address"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "Expiration time"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "Asset receiving address")
    })
    @ResponseData(description = "transactionhash")
    public Response swapTokenTrade(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            BigInteger amountIn = new BigInteger(params.get("amountIn").toString());
            BigInteger amountOutMin = new BigInteger(params.get("amountOutMin").toString());
            List<String> tokenPathList = (List<String>) params.get("tokenPath");
            String[] tokenPath = tokenPathList.toArray(new String[tokenPathList.size()]);
            Long deadline = Long.parseLong(params.get("deadline").toString());
            String feeTo = (String) params.get("feeTo");
            String to = (String) params.get("to");
            Result<String> result = swapService.swapTokenTrade(chainId, address, password, amountIn,
                    amountOutMin, tokenPath, feeTo, deadline, to);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = STABLE_SWAP_CREATE_PAIR, version = 1.0, description = "establishStableSwapTransaction pairs")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "Account password"),
            @Parameter(parameterName = "coins", parameterType = "String[]", parameterDes = "List of asset types, example：[1-1, 1-2]"),
            @Parameter(parameterName = "symbol", parameterType = "String", parameterDes = "LPname(Optional filling)")
    })
    @ResponseData(description = "transactionhash")
    public Response stableSwapCreatePair(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            List<String> coinList = (List<String>) params.get("coins");
            String symbol = (String) params.get("symbol");
            String[] coins = coinList.toArray(new String[coinList.size()]);
            Result<String> result = swapService.stableSwapCreatePair(chainId, address, password, coins, symbol);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = STABLE_SWAP_ADD_LIQUIDITY, version = 1.0, description = "AddStableSwapLiquidity")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "Account password"),
            @Parameter(parameterName = "amounts", parameterType = "String[]", parameterDes = "List of added asset quantities"),
            @Parameter(parameterName = "tokens", parameterType = "String[]", parameterDes = "List of added asset types, example：[1-1, 1-2]"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "Transaction to address"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "Expiration time"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "Liquidity share receiving address")
    })
    @ResponseData(description = "transactionhash")
    public Response stableSwapAddLiquidity(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            List<String> amountList = (List<String>) params.get("amounts");
            BigInteger[] amounts = new BigInteger[amountList.size()];
            int i = 0;
            for (String amountStr : amountList) {
                amounts[i++] = new BigInteger(amountStr);
            }
            List<String> tokenList = (List<String>) params.get("tokens");
            String[] tokens = tokenList.toArray(new String[tokenList.size()]);
            String pairAddress = (String) params.get("pairAddress");
            Long deadline = Long.parseLong(params.get("deadline").toString());
            String to = (String) params.get("to");
            Result<String> result = swapService.stableSwapAddLiquidity(chainId, address, password, amounts, tokens,
                    pairAddress, deadline, to);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = STABLE_SWAP_REMOVE_LIQUIDITY, version = 1.0, description = "removeStableSwapLiquidity")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "Account password"),
            @Parameter(parameterName = "amountLP", parameterType = "String", parameterDes = "Removed assetsLPQuantity of"),
            @Parameter(parameterName = "tokenLPStr", parameterType = "String", parameterDes = "assetLPType of, example：1-1"),
            @Parameter(parameterName = "receiveOrderIndexs", parameterType = "int[]", parameterDes = "Receive assets in currency index order"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "Transaction to address"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "Expiration time"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "Remove liquidity share receiving address")
    })
    @ResponseData(description = "transactionhash")
    public Response stableSwapRemoveLiquidity(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            BigInteger amountLP = new BigInteger(params.get("amountLP").toString());
            String tokenLPStr = (String) params.get("tokenLPStr");
            List<Integer> receiveOrderIndexList = (List<Integer>) params.get("receiveOrderIndexs");
            Integer[] receiveOrderIndexs = receiveOrderIndexList.toArray(new Integer[receiveOrderIndexList.size()]);
            String pairAddress = (String) params.get("pairAddress");
            Long deadline = Long.parseLong(params.get("deadline").toString());
            String to = (String) params.get("to");
            Result<String> result = swapService.stableSwapRemoveLiquidity(chainId, address, password, amountLP, tokenLPStr,
                    receiveOrderIndexs, pairAddress, deadline, to);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = STABLE_SWAP_TOKEN_TRADE, version = 1.0, description = "StableSwapCoin trading")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "Account password"),
            @Parameter(parameterName = "amountsIn", parameterType = "String[]", parameterDes = "List of sold assets"),
            @Parameter(parameterName = "tokensIn", parameterType = "String[]", parameterDes = "List of asset types sold"),
            @Parameter(parameterName = "tokenOutIndex", parameterType = "int", parameterDes = "Index of purchased assets"),
            @Parameter(parameterName = "feeTo", parameterType = "String", parameterDes = "Transaction fee receiving address"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "Transaction to address"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "Expiration time"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "Asset receiving address"),
            @Parameter(parameterName = "feeToken", parameterType = "String", parameterDes = "Handling fee asset type, example：1-1"),
            @Parameter(parameterName = "feeAmount", parameterType = "String", parameterDes = "Transaction fees")
    })
    @ResponseData(description = "transactionhash")
    public Response stableSwapTokenTrade(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            List<String> amountInList = (List<String>) params.get("amountsIn");
            BigInteger[] amountsIn = new BigInteger[amountInList.size()];
            int i = 0;
            for (String amountStr : amountInList) {
                amountsIn[i++] = new BigInteger(amountStr);
            }
            List<String> tokenInList = (List<String>) params.get("tokensIn");
            String[] tokensIn = tokenInList.toArray(new String[tokenInList.size()]);
            Integer tokenOutIndex = (Integer) params.get("tokenOutIndex");
            String feeTo = (String) params.get("feeTo");
            String pairAddress = (String) params.get("pairAddress");
            Long deadline = Long.parseLong(params.get("deadline").toString());
            String to = (String) params.get("to");
            String feeTokenStr = (String) params.get("feeToken");
            String feeAmountStr = (String) params.get("feeAmount");
            Result<String> result = swapService.stableSwapTokenTrade(chainId, address, password, amountsIn, tokensIn,
                    tokenOutIndex, feeTo, pairAddress, deadline, to, feeTokenStr, feeAmountStr);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_MIN_AMOUNT_ADD_LIQUIDITY, version = 1.0, description = "Query AddSwapThe minimum number of assets with liquidity")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "amountA", parameterType = "String", parameterDes = "Added assetsAQuantity of"),
            @Parameter(parameterName = "amountB", parameterType = "String", parameterDes = "Added assetsBQuantity of"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "assetBType of, example：1-1")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "amountAMin", valueType = String.class, description = "assetAMinimum added value"),
            @Key(name = "amountBMin", valueType = String.class, description = "assetBMinimum added value")
    }))
    public Response calMinAmountOnSwapAddLiquidity(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            BigInteger amountA = new BigInteger(params.get("amountA").toString());
            BigInteger amountB = new BigInteger(params.get("amountB").toString());
            String tokenAStr = (String) params.get("tokenAStr");
            String tokenBStr = (String) params.get("tokenBStr");
            RealAddLiquidityOrderDTO dto = SwapUtils.calcAddLiquidity(chainId, iPairFactory,
                    SwapUtils.parseTokenStr(tokenAStr), SwapUtils.parseTokenStr(tokenBStr), amountA, amountB, BigInteger.ZERO, BigInteger.ZERO);
            if (dto == null) {
                return failed(SwapErrorCode.DATA_ERROR);
            }
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("amountAMin", dto.getRealAmountA());
            resultData.put("amountBMin", dto.getRealAmountB());
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_MIN_AMOUNT_REMOVE_LIQUIDITY, version = 1.0, description = "Query removalSwapThe minimum number of assets with liquidity")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "amountLP", parameterType = "String", parameterDes = "Removed assetsLPQuantity of"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "assetBType of, example：1-1")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "amountAMin", valueType = String.class, description = "assetAMinimum removal value"),
            @Key(name = "amountBMin", valueType = String.class, description = "assetBMinimum removal value")
    }))
    public Response calMinAmountOnSwapRemoveLiquidity(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            BigInteger amountLP = new BigInteger(params.get("amountLP").toString());
            String tokenAStr = (String) params.get("tokenAStr");
            String tokenBStr = (String) params.get("tokenBStr");
            NerveToken tokenA = SwapUtils.parseTokenStr(tokenAStr);
            NerveToken tokenB = SwapUtils.parseTokenStr(tokenBStr);
            byte[] pairAddress = SwapUtils.getPairAddress(chainId, tokenA, tokenB);

            RemoveLiquidityBus bus = SwapUtils.calRemoveLiquidityBusiness(chainId, iPairFactory, pairAddress, amountLP,
                    tokenA, tokenB, BigInteger.ZERO, BigInteger.ZERO, swapHelper.isSupportProtocol24());
            if (bus == null) {
                return failed(SwapErrorCode.DATA_ERROR);
            }
            BigInteger amountAMin, amountBMin;
            if (tokenA.equals(bus.getToken0())) {
                amountAMin = bus.getAmount0();
                amountBMin = bus.getAmount1();
            } else {
                amountAMin = bus.getAmount1();
                amountBMin = bus.getAmount0();
            }
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("amountAMin", amountAMin);
            resultData.put("amountBMin", amountBMin);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_MIN_AMOUNT_TOKEN_TRADE, version = 1.0, description = "querySwapMinimum buy in for coin exchangetoken")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "amountIn", parameterType = "String", parameterDes = "Number of assets sold"),
            @Parameter(parameterName = "tokenPath", parameterType = "String[]", parameterDes = "Currency exchange asset path, the last asset in the path is the asset that the user wants to buy, such as sellingAbuyB: [A, B] or [A, C, B]")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "amountOutMin", valueType = String.class, description = "Minimum number of assets to be purchased"),
    }))
    public Response calMinAmountOnSwapTokenTrade(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            BigInteger amountIn = new BigInteger(params.get("amountIn").toString());
            List<String> tokenPathList = (List<String>) params.get("tokenPath");
            String[] tokenPath = tokenPathList.toArray(new String[tokenPathList.size()]);
            NerveToken[] path = new NerveToken[tokenPath.length];
            int i = 0;
            for (String tokenStr : tokenPathList) {
                path[i++] = SwapUtils.parseTokenStr(tokenStr);
            }
            BigInteger[] amountOutMin = SwapUtils.getAmountsOut(chainId, iPairFactory, amountIn, path);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("amountOutMin", amountOutMin[amountOutMin.length - 1]);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }


    @CmdAnnotation(cmd = STABLE_SWAP_MIN_AMOUNT_ADD_LIQUIDITY, version = 1.0, description = "STABLE_SWAP_MIN_AMOUNT_ADD_LIQUIDITY")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "pairAddress"),
            @Parameter(parameterName = "tokenStr", parameterType = "String", parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "tokenAmount", parameterType = "String", parameterDes = "tokenAmount")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value")
    }))
    public Response calMinAmountOnStableSwapAddLiquidity(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String pairAddress = (String) params.get("pairAddress");
            String tokenStr = (String) params.get("tokenStr");
            NerveToken token = SwapUtils.parseTokenStr(tokenStr);
            BigInteger tokenAmount = new BigInteger(params.get("tokenAmount").toString());
            StableSwapPairDTO pairDTO = stableSwapPairCache.get(pairAddress);
            StableSwapPairPo po = pairDTO.getPo();
            NerveToken[] coins = po.getCoins();
            int index = -1;
            for (int i = 0; i < coins.length; i++) {
                if (coins[i].equals(token)) {
                    index = i;
                    break;
                }
            }
            BigInteger[] amounts = SwapUtils.emptyFillZero(new BigInteger[coins.length]);
            amounts[index] = tokenAmount;
            StableAddLiquidityBus dto = SwapUtils.calStableAddLiquididy(swapHelper, chainId, iPairFactory, pairAddress, SwapContext.AWARD_FEE_DESTRUCTION_ADDRESS, amounts, SwapContext.AWARD_FEE_DESTRUCTION_ADDRESS);
            if (dto == null) {
                return failed(SwapErrorCode.DATA_ERROR);
            }
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", dto.getLiquidity().toString());
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = STABLE_SWAP_MIN_AMOUNT_REMOVE_LIQUIDITY, version = 1.0, description = "STABLE_SWAP_MIN_AMOUNT_REMOVE_LIQUIDITY")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "pairAddress"),
            @Parameter(parameterName = "tokenStr", parameterType = "String", parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "liquidity", parameterType = "String", parameterDes = "Removed assetsLPQuantity of")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value")
    }))
    public Response calMinAmountOnStableSwapRemoveLiquidity(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String pairAddress = (String) params.get("pairAddress");
            String tokenStr = (String) params.get("tokenStr");
            NerveToken token = SwapUtils.parseTokenStr(tokenStr);
            BigInteger liquidity = new BigInteger(params.get("liquidity").toString());
            StableSwapPairDTO pairDTO = stableSwapPairCache.get(pairAddress);
            StableSwapPairPo po = pairDTO.getPo();
            NerveToken[] coins = po.getCoins();
            int index = -1;
            for (int i = 0; i < coins.length; i++) {
                if (coins[i].equals(token)) {
                    index = i;
                    break;
                }
            }
            byte[] indexes = new byte[]{(byte) index};
            StableRemoveLiquidityBus dto = SwapUtils.calStableRemoveLiquidityBusiness(swapHelper, chainId, iPairFactory, liquidity, indexes, AddressTool.getAddress(pairAddress), SwapContext.AWARD_FEE_DESTRUCTION_ADDRESS);
            if (dto == null) {
                return failed(SwapErrorCode.DATA_ERROR);
            }
            BigInteger[] amounts = dto.getAmounts();
            int count = 0;
            for (BigInteger amount : amounts) {
                if (amount.compareTo(BigInteger.ZERO) > 0) {
                    count++;
                }
                if (count > 1) {
                    return failed(SwapErrorCode.INSUFFICIENT_LIQUIDITY);
                }
            }
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", (dto.getAmounts()[index]).toString());
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_PAIR_INFO, version = 1.0, description = "querySwapTransaction pair information")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "assetBType of, example：1-1")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = SwapPairDTO.class))
    public Response getSwapPairInfo(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String tokenAStr = (String) params.get("tokenAStr");
            String tokenBStr = (String) params.get("tokenBStr");
            NerveToken tokenA = SwapUtils.parseTokenStr(tokenAStr);
            NerveToken tokenB = SwapUtils.parseTokenStr(tokenBStr);
            String pairAddress = SwapUtils.getStringPairAddress(chainId, tokenA, tokenB);
            SwapPairDTO pairDTO = swapPairCache.get(pairAddress);
            if (pairDTO == null) {
                return failed(SwapErrorCode.DATA_NOT_FOUND);
            }

            Map<String, Object> resultData = JSONUtils.jsonToMap(pairDTO.toString());
            return success(resultData);
        } catch (Exception e) {
            //logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_PAIR_INFO_BY_ADDRESS, version = 1.0, description = "Address based on transaction pairs querySwapTransaction pair information")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "Transaction to address")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = SwapPairDTO.class, description = "Transaction pair information")
    }))
    public Response getSwapPairInfoByPairAddress(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String pairAddress = (String) params.get("pairAddress");
            SwapPairDTO pairDTO = swapPairCache.get(pairAddress);
            if (pairDTO == null) {
                return failed(SwapErrorCode.DATA_NOT_FOUND);
            }

            Map<String, Object> resultData = JSONUtils.jsonToMap(pairDTO.toString());
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = STABLE_SWAP_PAIR_INFO, version = 1.0, description = "queryStable-SwapTransaction pair information")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "Transaction to address")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = StableSwapPairDTO.class, description = "Transaction pair information")
    }))
    public Response getStableSwapPairInfo(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String pairAddress = (String) params.get("pairAddress");
            StableSwapPairDTO pairDTO = stableSwapPairCache.get(pairAddress);
            if (pairDTO == null) {
                return failed(SwapErrorCode.DATA_NOT_FOUND);
            }
            Map<String, Object> resultData = JSONUtils.jsonToMap(pairDTO.toString());
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_RESULT_INFO, version = 1.0, description = "Query transaction execution results")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "txHash", parameterType = "String", parameterDes = "transactionhash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = SwapResult.class, description = "Transaction execution results")
    }))
    public Response getSwapResultInfo(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String txHash = (String) params.get("txHash");
            SwapResult result = swapExecuteResultStorageService.getResult(chainId, NulsHash.fromHex(txHash));
            if (result == null) {
                return failed(SwapErrorCode.DATA_NOT_FOUND);
            }
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", result);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_PAIR_BY_LP, version = 1.0, description = "according toLPAsset inquiry transaction address")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "tokenLPStr", parameterType = "String", parameterDes = "assetLPType of, example：1-1"),
    })
    @ResponseData(description = "Transaction to address")
    public Response getPairAddressByTokenLP(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String tokenLPStr = (String) params.get("tokenLPStr");
            Result<String> result = swapService.getPairAddressByTokenLP(chainId, tokenLPStr);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_PAIR_INFO_BY_LP, version = 1.0, description = "according toLPAsset inquiry transaction information")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "tokenLPStr", parameterType = "String", parameterDes = "assetLPType of, example：1-1"),
    })
    @ResponseData(description = "Transaction to address")
    public Response getPairInfoByTokenLP(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String tokenLPStr = (String) params.get("tokenLPStr");
            Result<String> result = swapService.getPairAddressByTokenLP(chainId, tokenLPStr);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            String pairAddress = result.getData();
            SwapPairDTO pairDTO = swapPairCache.get(pairAddress);
            if (pairDTO == null) {
                return failed(SwapErrorCode.DATA_NOT_FOUND);
            }

            Map<String, Object> resultData = JSONUtils.jsonToMap(pairDTO.toString());
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_PAIRS_ALL, version = 1.0, description = "Query all transaction pairs addresses")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
    })
    @ResponseData(description = "All transactions against addresses", responseType = @TypeDescriptor(value = List.class, collectionElement = String.class))
    public Response getAllSwapPairs(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            return success(swapPairCache.getList().stream().map(s -> AddressTool.getStringAddressByBytes(s.getPo().getAddress())).collect(Collectors.toList()));
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = STABLE_SWAP_PAIRS_ALL, version = 1.0, description = "Query all stablecoin transaction pairs addresses")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
    })
    @ResponseData(description = "All stablecoin transactions against addresses", responseType = @TypeDescriptor(value = List.class, collectionElement = String.class))
    public Response getAllStableSwapPairs(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            return success(stableSwapPairCache.getList().stream().map(s -> AddressTool.getStringAddressByBytes(s.getPo().getAddress())).collect(Collectors.toList()));
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_GET_STABLE_PAIR_LIST_FOR_SWAP_TRADE, version = 1.0, description = "Queries available forSwapStable currency trading for transactions")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
    })
    @ResponseData(name = "Return value", description = "Return a collection", responseType = @TypeDescriptor(value = List.class, collectionElement = StableCoinVo.class))
    public Response getStablePairListForSwapTrade(Map params) {
        try {
            Integer chainId = Integer.parseInt(params.get("chainId").toString());
            List<StableCoin> groupList = SwapContext.stableCoinGroup.getGroupList();
            List<StableCoinVo> collect = groupList.stream().filter(s -> StringUtils.isNotBlank(s.getAddress())).map(s -> new StableCoinVo(s.getAddress(), stableSwapPairCache.get(s.getAddress()).getPo().getTokenLP(), s.getGroupCoin())).collect(Collectors.toList());
            return success(collect);
        } catch (Exception e) {
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_GET_AVAILABLE_STABLE_PAIR_LIST, version = 1.0, description = "Query all valid stablecoin transaction pairs")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
    })
    @ResponseData(name = "Return value", description = "Return a collection", responseType = @TypeDescriptor(value = List.class, collectionElement = StableCoinVo.class))
    public Response getAvailableStablePairList(Map params) {
        try {
            Integer chainId = Integer.parseInt(params.get("chainId").toString());
            Collection<StableSwapPairDTO> list = stableSwapPairCache.getList();
            List<StableCoinVo> resultList = new ArrayList<>();
            for (StableSwapPairDTO dto : list) {
                String stableAddress = AddressTool.getStringAddressByBytes(dto.getPo().getAddress());
                if (chainId == 5 && "TNVTdTSQkncm2UqXw1gLzmtnjTRN5YqB8Tg1n".equalsIgnoreCase(stableAddress)) {
                    continue;
                } else if (chainId == 9 && SwapConstant.UNAVAILABLE_STABLE_PAIR.equalsIgnoreCase(stableAddress)) {
                    continue;
                }
                resultList.add(new StableCoinVo(stableAddress, stableSwapPairCache.get(stableAddress).getPo().getTokenLP(), dto.getPo().getCoins(), dto.getPo().getRemoves()));
            }
            return success(resultList);
        } catch (Exception e) {
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SIGN_FCH_WITHDRAW, version = 1.0, description = "SIGN_FCH_WITHDRAW")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "withdrawalUTXO", parameterType = "String", parameterDes = "utxo info"),
            @Parameter(parameterName = "signer", parameterType = "String", parameterDes = "signer"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "to"),
            @Parameter(parameterName = "amount", parameterType = "long", parameterDes = "amount"),
            @Parameter(parameterName = "feeRate", parameterType = "long", parameterDes = "feeRate"),
            @Parameter(parameterName = "opReturn", parameterType = "String", parameterDes = "opReturn"),
            @Parameter(parameterName = "m", parameterType = "int", parameterDes = "m"),
            @Parameter(parameterName = "n", parameterType = "int", parameterDes = "n"),
            @Parameter(parameterName = "useAllUTXO", parameterType = "boolean", parameterDes = "useAllUTXO"),
            @Parameter(parameterName = "splitGranularity", parameterType = "long", parameterDes = "splitGranularity"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value")
    }))
    public Response signFchWithdraw(Map<String, Object> params) {
        try {

            Integer chainId = (Integer) params.get("chainId");
            String withdrawalUTXO = (String) params.get("withdrawalUTXO");
            String signer = (String) params.get("signer");
            String to = (String) params.get("to");
            Long amount = Long.parseLong(params.get("amount").toString());
            Long feeRate = Long.parseLong(params.get("feeRate").toString());
            String opReturn = (String) params.get("opReturn");
            Integer m = (Integer) params.get("m");
            Integer n = (Integer) params.get("n");
            Boolean useAllUTXO = (Boolean) params.get("useAllUTXO");
            Long splitGranularity = null;
            Object obj = params.get("splitGranularity");
            if (obj != null) {
                splitGranularity = Long.parseLong(obj.toString());
            }

            WithdrawalUTXOTxData utxoTxData = new WithdrawalUTXOTxData();
            utxoTxData.parse(HexUtil.decode(withdrawalUTXO), 0);
            List<ECKey> pubEcKeys = utxoTxData.getPubs().stream().map(b -> ECKey.fromPublicOnly(b)).collect(Collectors.toList());
            List<Cash> inputs = utxoTxData.getUtxoDataList().stream().map(utxo -> FchUtil.converterUTXOToCash(utxo.getTxid(), utxo.getVout(), utxo.getAmount().longValue())).collect(Collectors.toList());


            String prikey = AccountCall.getAccountPrikey(chainId, signer, SwapContext.PASSWORD);
            ECKey privKey = ECKey.fromPrivate(HexUtil.decode(prikey));
            byte[] signData = FchUtil.createMultiSignByOne(
                    privKey,
                    pubEcKeys,
                    inputs,
                    to,
                    amount,
                    opReturn,
                    m, n,
                    feeRate,
                    useAllUTXO,
                    splitGranularity);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", HexUtil.encode(signData));
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = VERIFY_FCH_WITHDRAW, version = 1.0, description = "VERIFY_FCH_WITHDRAW")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "withdrawalUTXO", parameterType = "String", parameterDes = "utxo info"),
            @Parameter(parameterName = "signData", parameterType = "String", parameterDes = "signData"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "to"),
            @Parameter(parameterName = "amount", parameterType = "long", parameterDes = "amount"),
            @Parameter(parameterName = "feeRate", parameterType = "long", parameterDes = "feeRate"),
            @Parameter(parameterName = "opReturn", parameterType = "String", parameterDes = "opReturn"),
            @Parameter(parameterName = "m", parameterType = "int", parameterDes = "m"),
            @Parameter(parameterName = "n", parameterType = "int", parameterDes = "n"),
            @Parameter(parameterName = "useAllUTXO", parameterType = "boolean", parameterDes = "useAllUTXO"),
            @Parameter(parameterName = "splitGranularity", parameterType = "long", parameterDes = "splitGranularity"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value")
    }))
    public Response verifyFchWithdraw(Map<String, Object> params) {
        try {

            Integer chainId = (Integer) params.get("chainId");
            String withdrawalUTXO = (String) params.get("withdrawalUTXO");
            String signData = (String) params.get("signData");
            String to = (String) params.get("to");
            Long amount = Long.parseLong(params.get("amount").toString());
            Long feeRate = Long.parseLong(params.get("feeRate").toString());
            String opReturn = (String) params.get("opReturn");
            Integer m = (Integer) params.get("m");
            Integer n = (Integer) params.get("n");
            Boolean useAllUTXO = (Boolean) params.get("useAllUTXO");
            Long splitGranularity = null;
            Object obj = params.get("splitGranularity");
            if (obj != null) {
                splitGranularity = Long.parseLong(obj.toString());
            }

            WithdrawalUTXOTxData utxoTxData = new WithdrawalUTXOTxData();
            utxoTxData.parse(HexUtil.decode(withdrawalUTXO), 0);
            List<ECKey> pubEcKeys = utxoTxData.getPubs().stream().map(b -> ECKey.fromPublicOnly(b)).collect(Collectors.toList());
            List<Cash> inputs = utxoTxData.getUtxoDataList().stream().map(utxo -> FchUtil.converterUTXOToCash(utxo.getTxid(), utxo.getVout(), utxo.getAmount().longValue())).collect(Collectors.toList());

            BtcSignData fchSignData = new BtcSignData();
            fchSignData.parse(HexUtil.decode(signData), 0);

            boolean verified = FchUtil.verifyMultiSignByOne(
                    fchSignData.getPubkey(),
                    fchSignData.getSignatures(),
                    pubEcKeys,
                    inputs,
                    to,
                    amount,
                    opReturn,
                    m, n,
                    feeRate,
                    useAllUTXO,
                    splitGranularity);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", verified);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = VERIFY_FCH_WITHDRAW_COUNT, version = 1.0, description = "VERIFY_FCH_WITHDRAW_COUNT")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "withdrawalUTXO", parameterType = "String", parameterDes = "utxo info"),
            @Parameter(parameterName = "signData", parameterType = "String", parameterDes = "signData"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "to"),
            @Parameter(parameterName = "amount", parameterType = "long", parameterDes = "amount"),
            @Parameter(parameterName = "feeRate", parameterType = "long", parameterDes = "feeRate"),
            @Parameter(parameterName = "opReturn", parameterType = "String", parameterDes = "opReturn"),
            @Parameter(parameterName = "m", parameterType = "int", parameterDes = "m"),
            @Parameter(parameterName = "n", parameterType = "int", parameterDes = "n"),
            @Parameter(parameterName = "useAllUTXO", parameterType = "boolean", parameterDes = "useAllUTXO"),
            @Parameter(parameterName = "splitGranularity", parameterType = "long", parameterDes = "splitGranularity"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value")
    }))
    public Response verifyFchWithdrawCount(Map<String, Object> params) {
        try {

            Integer chainId = (Integer) params.get("chainId");
            String withdrawalUTXO = (String) params.get("withdrawalUTXO");
            String signatureData = (String) params.get("signData");
            String to = (String) params.get("to");
            Long amount = Long.parseLong(params.get("amount").toString());
            Long feeRate = Long.parseLong(params.get("feeRate").toString());
            String opReturn = (String) params.get("opReturn");
            Integer m = (Integer) params.get("m");
            Integer n = (Integer) params.get("n");
            Boolean useAllUTXO = (Boolean) params.get("useAllUTXO");
            Long splitGranularity = null;
            Object obj = params.get("splitGranularity");
            if (obj != null) {
                splitGranularity = Long.parseLong(obj.toString());
            }

            WithdrawalUTXOTxData utxoTxData = new WithdrawalUTXOTxData();
            utxoTxData.parse(HexUtil.decode(withdrawalUTXO), 0);
            List<ECKey> pubEcKeys = utxoTxData.getPubs().stream().map(b -> ECKey.fromPublicOnly(b)).collect(Collectors.toList());
            List<Cash> inputs = utxoTxData.getUtxoDataList().stream().map(utxo -> FchUtil.converterUTXOToCash(utxo.getTxid(), utxo.getVout(), utxo.getAmount().longValue())).collect(Collectors.toList());

            Map<String, List<byte[]>> signatures = new HashMap<>();
            String[] signDatas = signatureData.split(",");
            for (String signData : signDatas) {
                BtcSignData signDataObj = new BtcSignData();
                signDataObj.parse(HexUtil.decode(signData.trim()), 0);
                signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures());
            }

            int verifiedCount = FchUtil.verifyMultiSignCount(
                    signatures,
                    pubEcKeys,
                    inputs,
                    to,
                    amount,
                    opReturn,
                    m, n,
                    feeRate,
                    useAllUTXO,
                    splitGranularity);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", verifiedCount);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = CREATE_FCH_MULTISIGN_WITHDRAW_TX, version = 1.0, description = "CREATE_FCH_MULTISIGN_WITHDRAW_TX")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "withdrawalUTXO", parameterType = "String", parameterDes = "utxo info"),
            @Parameter(parameterName = "signData", parameterType = "String", parameterDes = "signData"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "to"),
            @Parameter(parameterName = "amount", parameterType = "long", parameterDes = "amount"),
            @Parameter(parameterName = "feeRate", parameterType = "long", parameterDes = "feeRate"),
            @Parameter(parameterName = "opReturn", parameterType = "String", parameterDes = "opReturn"),
            @Parameter(parameterName = "m", parameterType = "int", parameterDes = "m"),
            @Parameter(parameterName = "n", parameterType = "int", parameterDes = "n"),
            @Parameter(parameterName = "useAllUTXO", parameterType = "boolean", parameterDes = "useAllUTXO"),
            @Parameter(parameterName = "splitGranularity", parameterType = "long", parameterDes = "splitGranularity"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value")
    }))
    public Response createFchMultiSignWithdrawTx(Map<String, Object> params) {
        try {

            Integer chainId = (Integer) params.get("chainId");
            String withdrawalUTXO = (String) params.get("withdrawalUTXO");
            String signatureData = (String) params.get("signData");
            String to = (String) params.get("to");
            Long amount = Long.parseLong(params.get("amount").toString());
            Long feeRate = Long.parseLong(params.get("feeRate").toString());
            String opReturn = (String) params.get("opReturn");
            Integer m = (Integer) params.get("m");
            Integer n = (Integer) params.get("n");
            Boolean useAllUTXO = (Boolean) params.get("useAllUTXO");
            Long splitGranularity = null;
            Object obj = params.get("splitGranularity");
            if (obj != null) {
                splitGranularity = Long.parseLong(obj.toString());
            }

            WithdrawalUTXOTxData utxoTxData = new WithdrawalUTXOTxData();
            utxoTxData.parse(HexUtil.decode(withdrawalUTXO), 0);
            List<ECKey> pubEcKeys = utxoTxData.getPubs().stream().map(b -> ECKey.fromPublicOnly(b)).collect(Collectors.toList());
            List<Cash> inputs = utxoTxData.getUtxoDataList().stream().map(utxo -> FchUtil.converterUTXOToCash(utxo.getTxid(), utxo.getVout(), utxo.getAmount().longValue())).collect(Collectors.toList());

            Map<String, List<byte[]>> signatures = new HashMap<>();
            String[] signDatas = signatureData.split(",");
            for (String signData : signDatas) {
                BtcSignData signDataObj = new BtcSignData();
                signDataObj.parse(HexUtil.decode(signData.trim()), 0);
                signatures.put(KeyTools.pubKeyToFchAddr(signDataObj.getPubkey()), signDataObj.getSignatures());
            }

            String signedTx = FchUtil.createMultiSignTx(
                    signatures,
                    pubEcKeys,
                    inputs,
                    to,
                    amount,
                    opReturn,
                    m, n,
                    feeRate,
                    useAllUTXO,
                    splitGranularity);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", signedTx);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

}
