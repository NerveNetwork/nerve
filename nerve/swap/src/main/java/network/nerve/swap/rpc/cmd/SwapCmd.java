package network.nerve.swap.rpc.cmd;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.config.SwapConfig;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.StableSwapHelper;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.TokenAmount;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.RemoveLiquidityBus;
import network.nerve.swap.model.dto.RealAddLiquidityOrderDTO;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.model.vo.RouteVO;
import network.nerve.swap.model.vo.SwapPairVO;
import network.nerve.swap.model.vo.TokenAmountVo;
import network.nerve.swap.service.SwapService;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.swap.constant.SwapCmdConstant.*;
import static network.nerve.swap.utils.SwapUtils.wrapperFailed;

/**
 * 异构链信息提供命令
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

    @CmdAnnotation(cmd = BATCH_BEGIN, version = 1.0, description = "一个批次的开始通知，生成当前批次的信息/batch begin")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "blockType", parameterType = "int", parameterDes = "区块处理模式, 打包区块 - 0, 验证区块 - 1"),
            @Parameter(parameterName = "blockHeight", parameterType = "long", parameterDes = "当前打包的区块高度"),
            @Parameter(parameterName = "preStateRoot", parameterType = "String", parameterDes = "前一个区块的stateRoot"),
            @Parameter(parameterName = "blockTime", parameterType = "long", parameterDes = "当前打包的区块时间")
    })
    @ResponseData(description = "无特定返回值，没有错误即成功")
    public Response batchBegin(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            Integer blockType = (Integer) params.get("blockType");
            ChainManager.chainHandle(chainId, blockType);
            Long blockHeight = Long.parseLong(params.get("blockHeight").toString());
            Long blockTime = Long.parseLong(params.get("blockTime").toString());
            String preStateRoot = (String) params.get("preStateRoot");
            swapService.begin(chainId, blockHeight, blockTime, preStateRoot);
            return success();
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = INVOKE, version = 1.0, description = "批次通知开始后，一笔一笔执行/invoke one by one")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "blockType", parameterType = "int", parameterDes = "区块处理模式, 打包区块 - 0, 验证区块 - 1"),
            @Parameter(parameterName = "blockHeight", parameterType = "long", parameterDes = "当前打包的区块高度"),
            @Parameter(parameterName = "blockTime", parameterType = "long", parameterDes = "当前打包的区块时间"),
            @Parameter(parameterName = "tx", parameterType = "String", parameterDes = "交易序列化的HEX编码字符串")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象，包含两个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "success", valueType = Boolean.class, description = "执行是否成功"),
            @Key(name = "txList", valueType = List.class, valueElement = String.class, description = "新生成的系统交易序列化字符串列表(目前只返回一个交易，成交交易 或者 失败返还交易)")
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
            Result result = swapService.invokeOneByOne(chainId, blockHeight, blockTime, tx);
            if (result.isFailed()) {
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

    @CmdAnnotation(cmd = BATCH_END, version = 1.0, description = "通知当前批次结束并返回结果/batch end")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "blockType", parameterType = "int", parameterDes = "区块处理模式, 打包区块 - 0, 验证区块 - 1"),
            @Parameter(parameterName = "blockHeight", parameterType = "long", parameterDes = "当前打包的区块高度")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象，包含两个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "stateRoot", description = "当前stateRoot")
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
            return success(resultMap);
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = IS_LEGAL_COIN_FOR_ADD_STABLE, version = 1.0, description = "检查在稳定币兑换池中添加的币种是否合法")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "交易对地址"),
            @Parameter(parameterName = "assetChainId", parameterType = "int", parameterDes = "币种链ID"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterDes = "币种资产ID"),
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "执行是否成功")
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

    @CmdAnnotation(cmd = ADD_COIN_FOR_STABLE, version = 1.0, description = "在稳定币兑换池中添加币种")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "交易对地址"),
            @Parameter(parameterName = "assetChainId", parameterType = "int", parameterDes = "币种链ID"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterDes = "币种资产ID"),
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "执行是否成功")
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

    @CmdAnnotation(cmd = BEST_TRADE_EXACT_IN, version = 1.0, description = "寻找最佳交易路径")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "tokenInStr", parameterType = "String", parameterDes = "卖出资产的类型，示例：1-1"),
            @Parameter(parameterName = "tokenInAmount", requestType = @TypeDescriptor(value = String.class), parameterDes = "卖出资产数量"),
            @Parameter(parameterName = "tokenOutStr", parameterType = "String", parameterDes = "买进资产的类型，示例：1-1"),
            @Parameter(parameterName = "maxPairSize", requestType = @TypeDescriptor(value = int.class), parameterDes = "交易最深路径"),
            @Parameter(parameterName = "pairs", requestType = @TypeDescriptor(value = String[].class), parameterDes = "当前网络所有交易对列表")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "tokenPath", valueType = List.class, description = "最佳交易路径"),
            @Key(name = "tokenAmountIn", valueType = TokenAmountVo.class, description = "卖出资产"),
            @Key(name = "tokenAmountOut", valueType = TokenAmountVo.class, description = "买进资产"),
    }))
    public Response bestTradeExactIn(Map<String, Object> params) {
        try {
            Integer chainId = (Integer) params.get("chainId");
            String tokenInStr = params.get("tokenInStr").toString();
            String tokenOutStr = params.get("tokenOutStr").toString();
            BigInteger tokenInAmount = new BigInteger(params.get("tokenInAmount").toString());
            Integer maxPairSize = Integer.parseInt(params.get("maxPairSize").toString());
            List<String> pairsList = (List<String>) params.get("pairs");
            TokenAmount tokenAmountIn = new TokenAmount(SwapUtils.parseTokenStr(tokenInStr), tokenInAmount);
            NerveToken tokenOut = SwapUtils.parseTokenStr(tokenOutStr);
            List<RouteVO> bestTrades = swapHelper.bestTradeExactIn(chainId, pairsList, tokenAmountIn, tokenOut, maxPairSize);
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

    @CmdAnnotation(cmd = SWAP_CREATE_PAIR, version = 1.0, description = "创建Swap交易对")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1")
    })
    @ResponseData(description = "交易hash")
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

    @CmdAnnotation(cmd = SWAP_ADD_LIQUIDITY, version = 1.0, description = "添加Swap流动性")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amountA", parameterType = "String", parameterDes = "添加的资产A的数量"),
            @Parameter(parameterName = "amountB", parameterType = "String", parameterDes = "添加的资产B的数量"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1"),
            @Parameter(parameterName = "amountAMin", parameterType = "String", parameterDes = "资产A最小添加值"),
            @Parameter(parameterName = "amountBMin", parameterType = "String", parameterDes = "资产B最小添加值"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "流动性份额接收地址")
    })
    @ResponseData(description = "交易hash")
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

    @CmdAnnotation(cmd = SWAP_REMOVE_LIQUIDITY, version = 1.0, description = "移除Swap流动性")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amountLP", parameterType = "String", parameterDes = "移除的资产LP的数量"),
            @Parameter(parameterName = "tokenLPStr", parameterType = "String", parameterDes = "资产LP的类型，示例：1-1"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1"),
            @Parameter(parameterName = "amountAMin", parameterType = "String", parameterDes = "资产A最小移除值"),
            @Parameter(parameterName = "amountBMin", parameterType = "String", parameterDes = "资产B最小移除值"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "移除流动性份额接收地址")
    })
    @ResponseData(description = "交易hash")
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

    @CmdAnnotation(cmd = SWAP_TOKEN_TRADE, version = 1.0, description = "Swap币币交换")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amountIn", parameterType = "String", parameterDes = "卖出的资产数量"),
            @Parameter(parameterName = "tokenPath", parameterType = "String[]", parameterDes = "币币交换资产路径，路径中最后一个资产，是用户要买进的资产，如卖A买B: [A, B] or [A, C, B]"),
            @Parameter(parameterName = "amountOutMin", parameterType = "String", parameterDes = "最小买进的资产数量"),
            @Parameter(parameterName = "feeTo", parameterType = "String", parameterDes = "交易手续费取出一部分给指定的接收地址"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "资产接收地址")
    })
    @ResponseData(description = "交易hash")
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

    @CmdAnnotation(cmd = STABLE_SWAP_CREATE_PAIR, version = 1.0, description = "创建StableSwap交易对")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "coins", parameterType = "String[]", parameterDes = "资产类型列表，示例：[1-1, 1-2]"),
            @Parameter(parameterName = "symbol", parameterType = "String", parameterDes = "LP名称(选填)")
    })
    @ResponseData(description = "交易hash")
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

    @CmdAnnotation(cmd = STABLE_SWAP_ADD_LIQUIDITY, version = 1.0, description = "添加StableSwap流动性")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amounts", parameterType = "String[]", parameterDes = "添加的资产数量列表"),
            @Parameter(parameterName = "tokens", parameterType = "String[]", parameterDes = "添加的资产类型列表，示例：[1-1, 1-2]"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "交易对地址"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "流动性份额接收地址")
    })
    @ResponseData(description = "交易hash")
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

    @CmdAnnotation(cmd = STABLE_SWAP_REMOVE_LIQUIDITY, version = 1.0, description = "移除StableSwap流动性")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amountLP", parameterType = "String", parameterDes = "移除的资产LP的数量"),
            @Parameter(parameterName = "tokenLPStr", parameterType = "String", parameterDes = "资产LP的类型，示例：1-1"),
            @Parameter(parameterName = "receiveOrderIndexs", parameterType = "int[]", parameterDes = "按币种索引顺序接收资产"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "交易对地址"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "移除流动性份额接收地址")
    })
    @ResponseData(description = "交易hash")
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

    @CmdAnnotation(cmd = STABLE_SWAP_TOKEN_TRADE, version = 1.0, description = "StableSwap币币交易")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码"),
            @Parameter(parameterName = "amountsIn", parameterType = "String[]", parameterDes = "卖出的资产数量列表"),
            @Parameter(parameterName = "tokensIn", parameterType = "String[]", parameterDes = "卖出的资产类型列表"),
            @Parameter(parameterName = "tokenOutIndex", parameterType = "int", parameterDes = "买进的资产索引"),
            @Parameter(parameterName = "feeTo", parameterType = "String", parameterDes = "交易手续费取出一部分给指定的接收地址"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "交易对地址"),
            @Parameter(parameterName = "deadline", parameterType = "long", parameterDes = "过期时间"),
            @Parameter(parameterName = "to", parameterType = "String", parameterDes = "资产接收地址")
    })
    @ResponseData(description = "交易hash")
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
            Result<String> result = swapService.stableSwapTokenTrade(chainId, address, password, amountsIn, tokensIn,
                    tokenOutIndex, feeTo, pairAddress, deadline, to);
            if (result.isFailed()) {
                return wrapperFailed(result);
            }
            return success(result.getData());
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_MIN_AMOUNT_ADD_LIQUIDITY, version = 1.0, description = "查询添加Swap流动性的最小资产数量")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "amountA", parameterType = "String", parameterDes = "添加的资产A的数量"),
            @Parameter(parameterName = "amountB", parameterType = "String", parameterDes = "添加的资产B的数量"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "amountAMin", valueType = String.class, description = "资产A最小添加值"),
            @Key(name = "amountBMin", valueType = String.class, description = "资产B最小添加值")
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

    @CmdAnnotation(cmd = SWAP_MIN_AMOUNT_REMOVE_LIQUIDITY, version = 1.0, description = "查询移除Swap流动性的最小资产数量")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "amountLP", parameterType = "String", parameterDes = "移除的资产LP的数量"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "amountAMin", valueType = String.class, description = "资产A最小移除值"),
            @Key(name = "amountBMin", valueType = String.class, description = "资产B最小移除值")
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
                    tokenA, tokenB, BigInteger.ZERO, BigInteger.ZERO);
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

    @CmdAnnotation(cmd = SWAP_MIN_AMOUNT_TOKEN_TRADE, version = 1.0, description = "查询Swap币币交换最小买进token")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "amountIn", parameterType = "String", parameterDes = "卖出的资产数量"),
            @Parameter(parameterName = "tokenPath", parameterType = "String[]", parameterDes = "币币交换资产路径，路径中最后一个资产，是用户要买进的资产，如卖A买B: [A, B] or [A, C, B]")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "amountOutMin", valueType = String.class, description = "最小买进的资产数量"),
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

    @CmdAnnotation(cmd = SWAP_PAIR_INFO, version = 1.0, description = "查询Swap交易对信息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "tokenAStr", parameterType = "String", parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", parameterType = "String", parameterDes = "资产B的类型，示例：1-1")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = SwapPairDTO.class))
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
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = SWAP_PAIR_INFO_BY_ADDRESS, version = 1.0, description = "根据交易对地址 查询Swap交易对信息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "交易对地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = SwapPairDTO.class, description = "交易对信息")
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

    @CmdAnnotation(cmd = STABLE_SWAP_PAIR_INFO, version = 1.0, description = "查询Stable-Swap交易对信息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "交易对地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = StableSwapPairDTO.class, description = "交易对信息")
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

    @CmdAnnotation(cmd = SWAP_RESULT_INFO, version = 1.0, description = "查询交易执行结果")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "txHash", parameterType = "String", parameterDes = "交易hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = SwapResult.class, description = "交易执行结果")
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

    @CmdAnnotation(cmd = SWAP_PAIR_BY_LP, version = 1.0, description = "根据LP资产查询交易对地址")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "tokenLPStr", parameterType = "String", parameterDes = "资产LP的类型，示例：1-1"),
    })
    @ResponseData(description = "交易对地址")
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

    @CmdAnnotation(cmd = SWAP_PAIR_INFO_BY_LP, version = 1.0, description = "根据LP资产查询交易信息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id"),
            @Parameter(parameterName = "tokenLPStr", parameterType = "String", parameterDes = "资产LP的类型，示例：1-1"),
    })
    @ResponseData(description = "交易对地址")
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

}
