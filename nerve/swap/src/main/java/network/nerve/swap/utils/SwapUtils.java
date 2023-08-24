package network.nerve.swap.utils;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.model.FormatValidUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.model.message.MessageUtil;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.TokenAmount;
import network.nerve.swap.model.bo.StableCoinGroup;
import network.nerve.swap.model.business.RemoveLiquidityBus;
import network.nerve.swap.model.business.stable.StableAddLiquidityBus;
import network.nerve.swap.model.business.stable.StableRemoveLiquidityBus;
import network.nerve.swap.model.business.stable.StableSwapTradeBus;
import network.nerve.swap.model.dto.RealAddLiquidityOrderDTO;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.vo.RouteOfPriceImpactVO;
import network.nerve.swap.model.vo.RouteVO;
import network.nerve.swap.model.vo.SwapPairVO;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static io.nuls.core.model.FormatValidUtils.NULS;
import static network.nerve.swap.constant.SwapConstant.*;
import static network.nerve.swap.constant.SwapErrorCode.*;
import static network.nerve.swap.context.SwapContext.stableCoinGroup;

/**
 * @author Niels
 */
public class SwapUtils {

    public static String getStringPairAddress(int chainId, NerveToken token0, NerveToken token1) {
        return AddressTool.getStringAddressByBytes(getPairAddress(chainId, token0, token1));
    }

    public static String getStringPairAddress(int chainId, NerveToken token0, NerveToken token1, String prefix) {
        return AddressTool.getStringAddressByBytes(getPairAddress(chainId, token0, token1), prefix);
    }

    public static byte[] getPairAddress(int chainId, NerveToken token0, NerveToken token1) {
        return getSwapAddress(chainId, token0, token1, SwapConstant.PAIR_ADDRESS_TYPE);
    }

    private static byte[] getSwapAddress(int chainId, NerveToken token0, NerveToken token1, byte addressType) {
        if (token0 == null || token1 == null) {
            throw new NulsRuntimeException(CommonCodeConstanst.NULL_PARAMETER);
        }
        NerveToken[] array = tokenSort(token0, token1);
        byte[] all = ArraysTool.concatenate(
                Sha256Hash.hash(SerializeUtils.int32ToBytes(array[0].getChainId())),
                Sha256Hash.hash(SerializeUtils.int32ToBytes(array[0].getAssetId())),
                Sha256Hash.hash(SerializeUtils.int32ToBytes(array[1].getChainId())),
                Sha256Hash.hash(SerializeUtils.int32ToBytes(array[1].getAssetId()))
        );
        return AddressTool.getAddress(Sha256Hash.hash(all), chainId, addressType);
    }

    public static String getStableSwapAddress(int chainId, String hash) {
        byte[] stablePairAddressBytes = AddressTool.getAddress(HexUtil.decode(hash), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
        return stablePairAddress;
    }

    public static byte[] getFarmAddress(int chainId) {
        return AddressTool.getAddress(NulsHash.EMPTY_NULS_HASH.getBytes(), chainId, SwapConstant.FARM_ADDRESS_TYPE);
    }

    public static String getStringFarmAddress(int chainId) {
        return AddressTool.getStringAddressByBytes(getFarmAddress(chainId));
    }

    public static NerveToken[] tokenSort(NerveToken token0, NerveToken token1) {
        if (token0 == null || token1 == null) {
            throw new NulsRuntimeException(CommonCodeConstanst.NULL_PARAMETER);
        }
        if (token0.getChainId() == token1.getChainId() && token0.getAssetId() == token1.getAssetId()) {
            throw new NulsRuntimeException(CommonCodeConstanst.PARAMETER_ERROR);
        }
        boolean positiveSequence = token0.getChainId() < token1.getChainId() || (token0.getChainId() == token1.getChainId() && token0.getAssetId() < token1.getAssetId());
        if (positiveSequence) {
            return new NerveToken[]{token0, token1};
        }
        return new NerveToken[]{token1, token0};
    }

    public static BigInteger quote(BigInteger amountA, BigInteger reserveA, BigInteger reserveB) {
        if (amountA.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_AMOUNT);
        }
        if (reserveA.compareTo(BigInteger.ZERO) <= 0 || reserveB.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_LIQUIDITY);
        }
        BigInteger amountB = amountA.multiply(reserveB).divide(reserveA);
        return amountB;
    }

    /*
     常量乘积公式:
     rI * rO = (rI + aI) * (rO - aO) = rI * rO - rI * aO + ai * rO - aI * aO
     rI * aO + aI * aO = aI * rO
     aO * (rI + aI) = aI * rO

     换算后得到amountOut:
     aO = aI * rO / (rI + aI)

     加入手续费率:
     aO = rO * aI * 997 / 1000 / (rI + aI *997 / 1000)
        = rO * aI * 997 / (rI * 1000 + aI *997 / 1000 * 1000)
        = rO * aI * 997 / (rI * 1000 + aI *997)
     */
    public static BigInteger getAmountOut(BigInteger amountIn, BigInteger reserveIn, BigInteger reserveOut, BigInteger feeRate) {
        if (amountIn.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_INPUT_AMOUNT);
        }
        if (reserveIn.compareTo(BigInteger.ZERO) <= 0 || reserveOut.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_LIQUIDITY);
        }
        // update by pierre at 2023/3/9 p24
        //BigInteger amountInWithFee = amountIn.multiply(BI_997);
        BigInteger amountInWithFee = amountIn.multiply(BI_1000.subtract(feeRate));
        BigInteger numerator = amountInWithFee.multiply(reserveOut);
        BigInteger denominator = reserveIn.multiply(BI_1000).add(amountInWithFee);
        BigInteger amountOut = numerator.divide(denominator);
        return amountOut;
    }

    /*
     常量乘积公式:
     rI * rO = (rI + aI) * (rO - aO) = rI * rO - rI * aO + aI * rO - aI * aO
     rI * aO = aI * rO - aI * aO
     aI * (rO - aO) = rI * aO

     换算后得到amountIn:
     aI = rI * aO / (rO - aO)

     加入手续费率:
     aI * 997 / 1000 = rI * aO / (rO - aO);
     aI = rI * aO * 1000 / (rO - aO) / 997
     aI = rI * aO * 1000 / ((rO - aO) * 997)
     */
    public static BigInteger getAmountIn(BigInteger amountOut, BigInteger reserveIn, BigInteger reserveOut, BigInteger feeRate) {
        if (amountOut.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_OUTPUT_AMOUNT);
        }
        if (reserveOut.compareTo(amountOut) <= 0 || reserveIn.compareTo(BigInteger.ZERO) <= 0 || reserveOut.compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsRuntimeException(INSUFFICIENT_LIQUIDITY);
        }
        BigInteger numerator = reserveIn.multiply(amountOut).multiply(BI_1000);
        //BigInteger denominator = reserveOut.subtract(amountOut).multiply(BI_997);
        BigInteger denominator = reserveOut.subtract(amountOut).multiply(BI_1000.subtract(feeRate));
        BigInteger amountIn = numerator.divide(denominator).add(BigInteger.ONE);
        return amountIn;
    }

    private static BigInteger[] getReservesAndFeeRate(int chainId, IPairFactory pairFactory, NerveToken tokenA, NerveToken tokenB) {
        NerveToken[] nerveTokens = tokenSort(tokenA, tokenB);
        NerveToken token0 = nerveTokens[0];
        IPair pair = pairFactory.getPair(getStringPairAddress(chainId, tokenA, tokenB));
        BigInteger[] reserves = pair.getReserves();
        // 返回feeRate
        BigInteger feeRate = BigInteger.valueOf(pair.getPair().getFeeRate().intValue());
        BigInteger[] result = tokenA.equals(token0) ? new BigInteger[]{reserves[0], reserves[1], feeRate} : new BigInteger[]{reserves[1], reserves[0], feeRate};
        return result;
    }

    public static BigInteger[] getAmountsOut(int chainId, IPairFactory pairFactory, BigInteger amountIn, NerveToken[] path) {
        int pathLength = path.length;
        if (pathLength < 2 || pathLength > 100) {
            throw new NulsRuntimeException(INVALID_PATH);
        }
        BigInteger[] amounts = new BigInteger[pathLength];
        amounts[0] = amountIn;
        BigInteger reserveIn, reserveOut, feeRate;
        for (int i = 0; i < pathLength - 1; i++) {
            BigInteger[] reserves = getReservesAndFeeRate(chainId, pairFactory, path[i], path[i + 1]);
            reserveIn = reserves[0];
            reserveOut = reserves[1];
            feeRate = reserves[2];
            amounts[i + 1] = getAmountOut(amounts[i], reserveIn, reserveOut, feeRate);
        }
        return amounts;
    }

    public static boolean groupCombining(NerveToken token1, NerveToken token2) {
        return stableCoinGroup.groupIndex(token1, token2) != -1;
    }

    // 根据 stablePairFactory 余额校验、精度转换
    public static BigInteger getStableOutAmountByGroupIndex(int groupIndex, NerveToken tokenIn, BigInteger amountIn, NerveToken tokenOut, IPairFactory stablePairFactory, StableCoinGroup stableCoinGroup) throws NulsException {
        String stableAddress = stableCoinGroup.getAddressByIndex(groupIndex);
        IStablePair stablePair = stablePairFactory.getStablePair(stableAddress);
        StableSwapPairPo po = stablePair.getPair();
        NerveToken[] coins = po.getCoins();
        int tokenInIndex = 0, tokenOutIndex = 0, length = coins.length;
        for (int k = 0; k < length; k++) {
            NerveToken token = coins[k];
            if (token.equals(tokenIn)) {
                tokenInIndex = k;
            } else if (token.equals(tokenOut)) {
                tokenOutIndex = k;
            }
        }
        // 精度转换
        int[] decimalsOfCoins = po.getDecimalsOfCoins();
        // 计算 tokenOutAmount
        BigInteger tokenOutAmount = new BigDecimal(amountIn).movePointLeft(decimalsOfCoins[tokenInIndex]).movePointRight(decimalsOfCoins[tokenOutIndex]).toBigInteger();
        // 余额校验
        BigInteger[] balances = stablePair.getBalances();
        if (balances[tokenOutIndex].compareTo(tokenOutAmount) < 0) {
            throw new NulsException(SwapErrorCode.INSUFFICIENT_OUTPUT_AMOUNT);
        }
        return tokenOutAmount;
    }

    public static BigInteger[] getAmountsOutByCombining(int chainId, IPairFactory pairFactory, BigInteger amountIn, NerveToken[] path, StableCoinGroup stableCoinGroup) throws NulsException {
        int pathLength = path.length;
        if (pathLength < 2 || pathLength > 100) {
            throw new NulsRuntimeException(INVALID_PATH);
        }
        BigInteger[] amounts = new BigInteger[pathLength];
        amounts[0] = amountIn;
        BigInteger reserveIn, reserveOut, feeRate;
        NerveToken tokenIn, tokenOut;
        for (int i = 0; i < pathLength - 1; i++) {
            tokenIn = path[i];
            tokenOut = path[i + 1];
            // 整合稳定币币池后，稳定币币种1:1兑换
            int groupIndex;
            if ((groupIndex = stableCoinGroup.groupIndex(tokenIn, tokenOut)) != -1) {
                amounts[i + 1] = getStableOutAmountByGroupIndex(groupIndex, tokenIn, amounts[i], tokenOut, pairFactory, stableCoinGroup);
                continue;
            }
            BigInteger[] reserves = getReservesAndFeeRate(chainId, pairFactory, tokenIn, tokenOut);
            reserveIn = reserves[0];
            reserveOut = reserves[1];
            feeRate = reserves[2];
            amounts[i + 1] = getAmountOut(amounts[i], reserveIn, reserveOut, feeRate);
        }
        return amounts;
    }

    public static BigInteger[] getAmountsIn(int chainId, IPairFactory pairFactory, BigInteger amountOut, NerveToken[] path) {
        int pathLength = path.length;
        if (pathLength < 2 || pathLength > 100) {
            throw new NulsRuntimeException(INVALID_PATH);
        }
        BigInteger[] amounts = new BigInteger[pathLength];
        amounts[pathLength - 1] = amountOut;
        BigInteger reserveIn, reserveOut, feeRate;
        for (int i = pathLength - 1; i > 0; i--) {
            BigInteger[] reserves = getReservesAndFeeRate(chainId, pairFactory, path[i - 1], path[i]);
            reserveIn = reserves[0];
            reserveOut = reserves[1];
            feeRate = reserves[2];
            amounts[i - 1] = getAmountIn(amounts[i], reserveIn, reserveOut, feeRate);
        }
        return amounts;
    }

    public static RealAddLiquidityOrderDTO calcAddLiquidity(
            int chainId, IPairFactory iPairFactory,
            NerveToken tokenA,
            NerveToken tokenB,
            BigInteger amountADesired,
            BigInteger amountBDesired,
            BigInteger amountAMin,
            BigInteger amountBMin
    ) throws NulsException {
        BigInteger[] _reserves = getReservesAndFeeRate(chainId, iPairFactory, tokenA, tokenB);
        BigInteger reserveA = _reserves[0];
        BigInteger reserveB = _reserves[1];
        BigInteger[] realAmount;
        BigInteger[] refund;
        if (reserveA.equals(BigInteger.ZERO) && reserveB.equals(BigInteger.ZERO)) {
            realAmount = new BigInteger[]{amountADesired, amountBDesired};
            refund = new BigInteger[]{BigInteger.ZERO, BigInteger.ZERO};
        } else {
            BigInteger amountBOptimal = SwapUtils.quote(amountADesired, reserveA, reserveB);
            if (amountBOptimal.compareTo(amountBDesired) <= 0) {
                if (amountBOptimal.compareTo(amountBMin) < 0) {
                    throw new NulsException(INSUFFICIENT_B_AMOUNT);
                }
                realAmount = new BigInteger[]{amountADesired, amountBOptimal};
                refund = new BigInteger[]{BigInteger.ZERO, amountBDesired.subtract(amountBOptimal)};
            } else {
                BigInteger amountAOptimal = SwapUtils.quote(amountBDesired, reserveB, reserveA);
                if (amountAOptimal.compareTo(amountADesired) > 0) {
                    throw new NulsException(INSUFFICIENT_A_AMOUNT);
                }
                if (amountAOptimal.compareTo(amountAMin) < 0) {
                    throw new NulsException(INSUFFICIENT_A_AMOUNT);
                }
                realAmount = new BigInteger[]{amountAOptimal, amountBDesired};
                refund = new BigInteger[]{amountADesired.subtract(amountAOptimal), BigInteger.ZERO};
            }
        }
        BigInteger[] reserves = new BigInteger[]{reserveA, reserveB};

        // 计算用户获取的LP资产
        IPair pair = iPairFactory.getPair(SwapUtils.getStringPairAddress(chainId, tokenA, tokenB));
        BigInteger totalSupply = pair.totalSupply();
        BigInteger liquidity;
        if (totalSupply.equals(BigInteger.ZERO)) {
            liquidity = realAmount[0].multiply(realAmount[1]).sqrt().subtract(SwapConstant.MINIMUM_LIQUIDITY);
        } else {
            BigInteger _liquidity0 = realAmount[0].multiply(totalSupply).divide(reserves[0]);
            BigInteger _liquidity1 = realAmount[1].multiply(totalSupply).divide(reserves[1]);
            liquidity = _liquidity0.compareTo(_liquidity1) < 0 ? _liquidity0 : _liquidity1;
        }
        if (liquidity.compareTo(BigInteger.ZERO) < 0) {
            throw new NulsException(SwapErrorCode.INSUFFICIENT_LIQUIDITY_MINTED);
        }

        return new RealAddLiquidityOrderDTO(realAmount, reserves, refund, liquidity);
    }

    public static RemoveLiquidityBus calRemoveLiquidityBusiness(
            int chainId, IPairFactory iPairFactory,
            byte[] pairAddress, BigInteger liquidity,
            NerveToken tokenA,
            NerveToken tokenB,
            BigInteger amountAMin,
            BigInteger amountBMin, boolean protocol24) throws NulsException {
        IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairAddress));
        BigInteger[] reserves = pair.getReserves();
        SwapPairPO pairPO = pair.getPair();
        NerveToken token0 = pairPO.getToken0();
        NerveToken token1 = pairPO.getToken1();
        BigInteger balance0 = reserves[0];
        BigInteger balance1 = reserves[1];
        BigInteger totalSupply = pair.totalSupply();
        // 可赎回的资产
        BigInteger amount0 = liquidity.multiply(balance0).divide(totalSupply);
        BigInteger amount1 = liquidity.multiply(balance1).divide(totalSupply);
        if (protocol24) {
            if (amount0.compareTo(BigInteger.ZERO) <= 0 && amount1.compareTo(BigInteger.ZERO) <= 0) {
                throw new NulsException(INSUFFICIENT_LIQUIDITY_BURNED);
            }
        } else {
            if (amount0.compareTo(BigInteger.ZERO) <= 0 || amount1.compareTo(BigInteger.ZERO) <= 0) {
                throw new NulsException(INSUFFICIENT_LIQUIDITY_BURNED);
            }
        }

        boolean firstTokenA = tokenA.equals(token0);
        BigInteger amountA, amountB;
        if (firstTokenA) {
            amountA = amount0;
            amountB = amount1;
        } else {
            amountA = amount1;
            amountB = amount0;
        }
        if (amountA.compareTo(amountAMin) < 0) {
            throw new NulsException(INSUFFICIENT_A_AMOUNT);
        }
        if (amountB.compareTo(amountBMin) < 0) {
            throw new NulsException(INSUFFICIENT_B_AMOUNT);
        }
        RemoveLiquidityBus bus = new RemoveLiquidityBus(amount0, amount1, balance0, balance1, liquidity, pair, token0, token1);
        bus.setPreBlockHeight(pair.getBlockHeightLast());
        bus.setPreBlockTime(pair.getBlockTimeLast());
        return bus;
    }

    public static StableAddLiquidityBus calStableAddLiquididy(int chainId, IPairFactory iPairFactory, String pairAddress, byte[] from, BigInteger[] amounts, byte[] to) throws NulsException {
        if (!AddressTool.validAddress(chainId, to)) {
            throw new NulsException(SwapErrorCode.RECEIVE_ADDRESS_ERROR);
        }
        // 用户添加多少资产，就往池子里填入多少资产
        BigInteger[] realAmounts = amounts;
        BigInteger[] refundAmounts = emptyFillZero(new BigInteger[amounts.length]);

        // 计算用户获取的LP资产
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        StableSwapPairPo pairPo = stablePair.getPair();
        // add by pierre at 2023/8/16，币种状态检查
        NerveToken[] coins = pairPo.getCoins();
        boolean[] removes = pairPo.getRemoves();
        if (removes != null) {
            for (int i = 0; i < amounts.length; i++) {
                if (amounts[i].compareTo(BigInteger.ZERO) > 0 && removes[i]) {
                    SwapContext.logger.info("[{}] error removed coin: {}", pairAddress, coins[i].str());
                    throw new NulsException(SwapErrorCode.INVALID_COINS);
                }
            }
        }
        // end code by pierre
        int[] decimalsOfCoins = pairPo.getDecimalsOfCoins();
        BigInteger[] balances = stablePair.getBalances();
        BigInteger totalSupply = stablePair.totalSupply();
        BigInteger liquidity;
        // 计算总量时，把精度都填充到18位
        if (totalSupply.equals(BigInteger.ZERO)) {
            liquidity = SwapUtils.getCumulativeAmountsOfStableSwap(realAmounts, decimalsOfCoins);
        } else {
            // 计算池子总量
            BigInteger poolTotal = SwapUtils.getCumulativeAmountsOfStableSwap(balances, decimalsOfCoins);
            // 计算用户本次添加总量
            BigInteger currentInTotal = SwapUtils.getCumulativeAmountsOfStableSwap(realAmounts, decimalsOfCoins);
            liquidity = currentInTotal.multiply(totalSupply).divide(poolTotal);
        }
        StableAddLiquidityBus bus = new StableAddLiquidityBus(from, realAmounts, liquidity, balances, refundAmounts, to);
        bus.setPreBlockHeight(stablePair.getBlockHeightLast());
        bus.setPreBlockTime(stablePair.getBlockTimeLast());
        return bus;
    }

    public static StableRemoveLiquidityBus calStableRemoveLiquidityBusiness(
            int chainId, IPairFactory iPairFactory,
            BigInteger liquidity, byte[] indexes, byte[] pairAddressBytes, byte[] to) throws NulsException {
        if (!AddressTool.validAddress(chainId, to)) {
            throw new NulsException(SwapErrorCode.RECEIVE_ADDRESS_ERROR);
        }
        // 计算用户赎回的资产
        String pairAddress = AddressTool.getStringAddressByBytes(pairAddressBytes);
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        StableSwapPairPo pairPo = stablePair.getPair();
        int[] decimalsOfCoins = pairPo.getDecimalsOfCoins();
        BigInteger[] balances = stablePair.getBalances();
        BigInteger totalSupply = stablePair.totalSupply();
        // 检查indexs合法与重复
        NerveToken[] coins = pairPo.getCoins();
        int length = coins.length;
        // 检查参数中`indexs`是否合法
        // indexs本身是等差数列，根据等差数列求和公式算出期望值
        int expect = (length - 1) * length / 2;
        // 累加indexs的实际值
        int fact = 0;
        for (byte index : indexes) {
            if (index > length - 1) {
                throw new NulsException(SwapErrorCode.INVALID_COINS);
            }
            fact = fact + index;
        }
        if (fact != expect) {
            throw new NulsException(SwapErrorCode.INVALID_COINS);
        }
        // 获取池子总量
        BigInteger poolTotal = SwapUtils.getCumulativeAmountsOfStableSwap(balances, decimalsOfCoins);
        // 用户可赎回资产数量(PRECISION_MUL)
        BigInteger totalReceives = poolTotal.multiply(liquidity).divide(totalSupply);
        final BigInteger finalTotalReceives = totalReceives;
        // 按照用户选择的提取顺序，扣减池子数量
        BigInteger[] receives = new BigInteger[length];
        for (byte index : indexes) {
            BigInteger balance = balances[index].multiply(BigInteger.TEN.pow(18 - decimalsOfCoins[index]));
            if (totalReceives.compareTo(balance) < 0) {
                receives[index] = totalReceives;
                break;
            } else {
                receives[index] = balance;
                totalReceives = totalReceives.subtract(balance);
                if (totalReceives.equals(BigInteger.ZERO)) {
                    break;
                }
            }
        }
        receives = SwapUtils.emptyFillZero(receives);
        BigInteger _totalReceives = SwapUtils.getCumulativeAmounts(receives);
        if (!_totalReceives.equals(finalTotalReceives)) {
            throw new NulsException(SwapErrorCode.INVALID_AMOUNTS);
        }
        // 还原receives <= 以上代码计算用户可赎回资产数量时，把精度都填充到了18位，最终结果按每个coin的实际精度，还原数值
        for (int i = 0; i < length; i++) {
            BigInteger receive = receives[i];
            if (receive.equals(BigInteger.ZERO)) {
                continue;
            }
            receives[i] = receive.divide(BigInteger.TEN.pow(18 - decimalsOfCoins[i]));
        }

        StableRemoveLiquidityBus bus = new StableRemoveLiquidityBus(receives, balances, liquidity, pairAddressBytes, to);
        bus.setPreBlockHeight(stablePair.getBlockHeightLast());
        bus.setPreBlockTime(stablePair.getBlockTimeLast());
        return bus;
    }

    public static StableRemoveLiquidityBus calStableRemoveLiquidityBusinessP21(
            int chainId, IPairFactory iPairFactory,
            BigInteger liquidity, byte[] indexes, byte[] pairAddressBytes, byte[] to) throws NulsException {
        if (!AddressTool.validAddress(chainId, to)) {
            throw new NulsException(SwapErrorCode.RECEIVE_ADDRESS_ERROR);
        }
        // 计算用户赎回的资产
        String pairAddress = AddressTool.getStringAddressByBytes(pairAddressBytes);
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        StableSwapPairPo pairPo = stablePair.getPair();
        int[] decimalsOfCoins = pairPo.getDecimalsOfCoins();
        BigInteger[] balances = stablePair.getBalances();
        BigInteger totalSupply = stablePair.totalSupply();
        // 检查indexs合法与重复
        NerveToken[] coins = pairPo.getCoins();
        // add by pierre at 2023/8/16，币种状态检查
        boolean[] removes = pairPo.getRemoves();
        if (removes != null) {
            for (int index : indexes) {
                if (removes[index]) {
                    SwapContext.logger.info("[{}] error removed coin: {}", pairAddress, coins[index].str());
                    throw new NulsException(SwapErrorCode.INVALID_COINS);
                }
            }
        }
        // end code by pierre
        int lengthOfCoins = coins.length;
        int lengthOfIndexes = indexes.length;
        // 检查参数中`indexs`是否合法
        if (lengthOfIndexes == lengthOfCoins) {
            // indexs本身是等差数列，根据等差数列求和公式算出期望值
            int expect = (lengthOfCoins - 1) * lengthOfCoins / 2;
            // 累加indexs的实际值
            int fact = 0;
            for (byte index : indexes) {
                if (index > lengthOfCoins - 1) {
                    throw new NulsException(SwapErrorCode.INVALID_COINS);
                }
                fact = fact + index;
            }
            if (fact != expect) {
                throw new NulsException(SwapErrorCode.INVALID_COINS);
            }
        } else if (lengthOfIndexes < lengthOfCoins) {
            byte[] _indexes = new byte[lengthOfCoins];
            byte[] temp = new byte[lengthOfCoins];
            for (int i = 0; i < lengthOfIndexes; i++) {
                byte index = indexes[i];
                if (index > lengthOfCoins - 1) {
                    throw new NulsException(SwapErrorCode.INVALID_COINS);
                }
                temp[index] = 1;
                _indexes[i] = index;
            }
            int k = 0;
            for (int i = 0; i < lengthOfCoins; i++) {
                if (temp[i] == 1) {
                    continue;
                }
                _indexes[k++ + lengthOfIndexes] = (byte) i;
            }
            indexes = _indexes;
        } else {
            throw new NulsException(SwapErrorCode.INVALID_COINS);
        }
        // 获取池子总量
        BigInteger poolTotal = SwapUtils.getCumulativeAmountsOfStableSwap(balances, decimalsOfCoins);
        // 用户可赎回资产数量(PRECISION_MUL)
        BigInteger totalReceives = poolTotal.multiply(liquidity).divide(totalSupply);
        final BigInteger finalTotalReceives = totalReceives;
        // 按照用户选择的提取顺序，扣减池子数量
        BigInteger[] receives = new BigInteger[lengthOfCoins];
        for (byte index : indexes) {
            BigInteger balance = balances[index].multiply(BigInteger.TEN.pow(18 - decimalsOfCoins[index]));
            if (totalReceives.compareTo(balance) < 0) {
                receives[index] = totalReceives;
                break;
            } else {
                receives[index] = balance;
                totalReceives = totalReceives.subtract(balance);
                if (totalReceives.equals(BigInteger.ZERO)) {
                    break;
                }
            }
        }
        receives = SwapUtils.emptyFillZero(receives);
        BigInteger _totalReceives = SwapUtils.getCumulativeAmounts(receives);
        if (!_totalReceives.equals(finalTotalReceives)) {
            throw new NulsException(SwapErrorCode.INVALID_AMOUNTS);
        }
        // 还原receives <= 以上代码计算用户可赎回资产数量时，把精度都填充到了18位，最终结果按每个coin的实际精度，还原数值
        for (int i = 0; i < lengthOfCoins; i++) {
            BigInteger receive = receives[i];
            if (receive.equals(BigInteger.ZERO)) {
                continue;
            }
            receives[i] = receive.divide(BigInteger.TEN.pow(18 - decimalsOfCoins[i]));
        }

        StableRemoveLiquidityBus bus = new StableRemoveLiquidityBus(receives, balances, liquidity, pairAddressBytes, to);
        bus.setPreBlockHeight(stablePair.getBlockHeightLast());
        bus.setPreBlockTime(stablePair.getBlockTimeLast());
        return bus;
    }

    public static StableSwapTradeBus calStableSwapTradeBusiness(
            int chainId, IPairFactory iPairFactory,
            BigInteger[] amountsIn, byte tokenOutIndex, byte[] pairAddressBytes, byte[] to, byte[] feeTo) throws NulsException {
        if (!AddressTool.validAddress(chainId, to)) {
            throw new NulsException(SwapErrorCode.RECEIVE_ADDRESS_ERROR);
        }
        if (feeTo != null && !AddressTool.validAddress(chainId, feeTo)) {
            throw new NulsException(SwapErrorCode.FEE_RECEIVE_ADDRESS_ERROR);
        }
        String pairAddress = AddressTool.getStringAddressByBytes(pairAddressBytes);
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        StableSwapPairPo pairPo = stablePair.getPair();
        int[] decimalsOfCoins = pairPo.getDecimalsOfCoins();
        NerveToken[] coins = pairPo.getCoins();
        int length = coins.length;
        BigInteger[] unLiquidityAwardFees = new BigInteger[length];
        BigInteger[] changeBalances = new BigInteger[length];
        BigInteger[] balances = stablePair.getBalances();
        // 把精度填充到18位
        BigInteger outBalance = balances[tokenOutIndex].multiply(BigInteger.TEN.pow(18 - decimalsOfCoins[tokenOutIndex]));
        BigInteger totalAmountOut = BigInteger.ZERO;
        for (int i = 0; i < length; i++) {
            BigInteger amountIn = amountsIn[i];
            if (amountIn.equals(BigInteger.ZERO)) {
                unLiquidityAwardFees[i] = BigInteger.ZERO;
                continue;
            }
            BigInteger coinFee = amountIn.multiply(SwapContext.FEE_PERMILLAGE_STABLE_SWAP).divide(BI_1000);
            amountIn = amountIn.subtract(coinFee);
            // 把精度填充到18位
            BigInteger _amountIn = amountIn.multiply(BigInteger.TEN.pow(18 - decimalsOfCoins[i]));
            BigInteger amountOut = _amountIn;
            if (outBalance.compareTo(amountOut) < 0) {
                throw new NulsException(SwapErrorCode.INSUFFICIENT_OUTPUT_AMOUNT);
            }
            unLiquidityAwardFees[i] = coinFee.multiply(SwapContext.FEE_PERCENT_ALLOCATION_UN_LIQUIDIDY_STABLE_SWAP).divide(BI_100);
            outBalance = outBalance.subtract(amountOut);
            totalAmountOut = totalAmountOut.add(amountOut);
        }
        // 还原`totalAmountOut` <= 以上代码计算用户买进资产数量时，把精度填充到了18位，最终结果按实际精度，还原数值
        totalAmountOut = totalAmountOut.divide(BigInteger.TEN.pow(18 - decimalsOfCoins[tokenOutIndex]));

        changeBalances = SwapUtils.emptyFillZero(changeBalances);
        for (int i = 0; i < length; i++) {
            changeBalances[i] = changeBalances[i].add(amountsIn[i]).subtract(unLiquidityAwardFees[i]);
        }
        changeBalances[tokenOutIndex] = changeBalances[tokenOutIndex].subtract(totalAmountOut);
        StableSwapTradeBus bus = new StableSwapTradeBus(pairAddressBytes, changeBalances, balances, amountsIn, unLiquidityAwardFees, tokenOutIndex, totalAmountOut, to);
        bus.setPreBlockHeight(stablePair.getBlockHeightLast());
        bus.setPreBlockTime(stablePair.getBlockTimeLast());
        return bus;
    }

    public static StableSwapTradeBus calStableSwapTradeBusinessP21(
            int chainId, IPairFactory iPairFactory,
            BigInteger[] amountsIn, byte tokenOutIndex, byte[] pairAddressBytes, byte[] to, CoinTo feeTo) throws NulsException {
        if (!AddressTool.validAddress(chainId, to)) {
            throw new NulsException(SwapErrorCode.RECEIVE_ADDRESS_ERROR);
        }
        boolean hasFeeTo = feeTo != null;
        String pairAddress = AddressTool.getStringAddressByBytes(pairAddressBytes);
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        StableSwapPairPo pairPo = stablePair.getPair();
        int[] decimalsOfCoins = pairPo.getDecimalsOfCoins();
        NerveToken[] coins = pairPo.getCoins();
        int length = coins.length;
        // add by pierre at 2023/8/16，币种状态检查
        boolean[] removes = pairPo.getRemoves();
        if (removes != null) {
            for (int i = 0; i < length; i++) {
                if (amountsIn[i].compareTo(BigInteger.ZERO) > 0 && removes[i]) {
                    SwapContext.logger.info("[{}] error removed coin: {}", pairAddress, coins[i].str());
                    throw new NulsException(INVALID_COINS);
                }
            }
            if (removes[tokenOutIndex]) {
                SwapContext.logger.info("[{}] error removed coin: {}", pairAddress, coins[tokenOutIndex].str());
                throw new NulsException(INVALID_COINS);
            }
        }
        // end code by pierre
        BigInteger[] changeBalances = new BigInteger[length];
        BigInteger[] balances = stablePair.getBalances();
        // 把精度填充到18位
        BigInteger outBalance = balances[tokenOutIndex].multiply(BigInteger.TEN.pow(18 - decimalsOfCoins[tokenOutIndex]));
        BigInteger totalAmountOut = BigInteger.ZERO;
        BigInteger feeAmountExtend = BigInteger.ZERO;
        for (int i = 0; i < length; i++) {
            BigInteger amountIn = amountsIn[i];
            if (amountIn.equals(BigInteger.ZERO)) {
                continue;
            }
            if (hasFeeTo) {
                NerveToken coin = coins[i];
                if (coin.getChainId() == feeTo.getAssetsChainId() && coin.getAssetId() == feeTo.getAssetsId()) {
                    feeAmountExtend = feeTo.getAmount().multiply(BigInteger.TEN.pow(18 - decimalsOfCoins[i]));
                }
            }
            // 把精度填充到18位
            BigInteger _amountIn = amountIn.multiply(BigInteger.TEN.pow(18 - decimalsOfCoins[i]));
            BigInteger amountOut = _amountIn;
            if (outBalance.compareTo(amountOut) < 0) {
                throw new NulsException(SwapErrorCode.INSUFFICIENT_OUTPUT_AMOUNT);
            }
            outBalance = outBalance.subtract(amountOut);
            totalAmountOut = totalAmountOut.add(amountOut);
        }
        // 检查手续费比例
        if (hasFeeTo) {
            BigDecimal percent = new BigDecimal(feeAmountExtend).divide(new BigDecimal(totalAmountOut.add(feeAmountExtend)), 2, RoundingMode.UP).movePointRight(2);
            if (percent.compareTo(SwapContext.FEE_MAX_PERCENT_STABLE_SWAP) > 0) {
                throw new NulsException(FEE_AMOUNT_ERROR);
            }
        }

        // 还原`totalAmountOut` <= 以上代码计算用户买进资产数量时，把精度填充到了18位，最终结果按实际精度，还原数值
        totalAmountOut = totalAmountOut.divide(BigInteger.TEN.pow(18 - decimalsOfCoins[tokenOutIndex]));

        changeBalances = SwapUtils.emptyFillZero(changeBalances);
        for (int i = 0; i < length; i++) {
            changeBalances[i] = changeBalances[i].add(amountsIn[i]);
        }
        changeBalances[tokenOutIndex] = changeBalances[tokenOutIndex].subtract(totalAmountOut);
        StableSwapTradeBus bus = new StableSwapTradeBus(pairAddressBytes, changeBalances, balances, amountsIn, SwapUtils.emptyFillZero(new BigInteger[length]), tokenOutIndex, totalAmountOut, to);
        bus.setPreBlockHeight(stablePair.getBlockHeightLast());
        bus.setPreBlockTime(stablePair.getBlockTimeLast());
        return bus;
    }

    private static BigInteger getAmountOutForBestTrade(BigInteger amountIn, BigInteger reserveIn, BigInteger reserveOut, BigInteger feeRate) {
        if (amountIn.compareTo(BigInteger.ZERO) <= 0) {
            return BigInteger.ZERO;
        }
        if (reserveIn.compareTo(BigInteger.ZERO) <= 0 || reserveOut.compareTo(BigInteger.ZERO) <= 0) {
            return BigInteger.ZERO;
        }
        // update by pierre at 2023/3/9 p24
        //BigInteger amountInWithFee = amountIn.multiply(BI_997);
        BigInteger amountInWithFee = amountIn.multiply(BI_1000.subtract(feeRate));
        BigInteger numerator = amountInWithFee.multiply(reserveOut);
        BigInteger denominator = reserveIn.multiply(BI_1000).add(amountInWithFee);
        BigInteger amountOut = numerator.divide(denominator);
        return amountOut;
    }

    public static List<RouteVO> bestTradeExactIn(int chainId, IPairFactory iPairFactory, List<SwapPairVO> pairs, TokenAmount tokenAmountIn,
                                                 NerveToken out, LinkedHashSet<SwapPairVO> currentPath,
                                                 List<RouteVO> bestTrade, TokenAmount orginTokenAmountIn, int maxPairSize, String resultRule) {
        // 筛选出直接可换的交易对
        NerveToken tokenIn = tokenAmountIn.getToken();
        NerveToken[] tokens = tokenSort(tokenIn, out);
        int length = pairs.size();
        int subIndex = -1;
        for (int i = 0; i < length; i++) {
            SwapPairVO pair = pairs.get(i);
            if (pair.getToken0().equals(tokens[0]) && pair.getToken1().equals(tokens[1])) {
                subIndex = i;
                break;
            }
        }
        if (subIndex != -1) {
            SwapPairVO removePair = pairs.remove(subIndex);
            BigInteger[] reserves = getReservesAndFeeRate(chainId, iPairFactory, tokenIn, out);
            BigInteger amountOut = getAmountOutForBestTrade(tokenAmountIn.getAmount(), reserves[0], reserves[1], reserves[2]);
            bestTrade.add(new RouteVO(List.of(removePair), orginTokenAmountIn, new TokenAmount(out, amountOut)));
        }
        // 查找所有匹配的交易路径
        List<RouteVO> routes = bestTradeExactIn(chainId, iPairFactory, pairs, tokenAmountIn, out, currentPath, bestTrade, orginTokenAmountIn, 0, maxPairSize);
        if (routes.size() == 1) {
            return routes;
        } else if (BEST_PRICE.equals(resultRule)) {
            // 按最优价格排序
            routes.sort(RouteVOSort.INSTANCE);
        } else {
            // 按价格影响排序
            List<RouteOfPriceImpactVO> impactTrades = calcImpactPrice(routes);
            impactTrades.sort(RouteImpactVOSort.INSTANCE);
        }
        return routes;
    }

    private static List<RouteOfPriceImpactVO> calcImpactPrice(List<RouteVO> list) {
        List<RouteOfPriceImpactVO> resultList = new ArrayList<>();
        for (RouteVO route : list) {
            TokenAmount tokenAmountIn = route.getTokenAmountIn();
            TokenAmount tokenAmountOut = route.getTokenAmountOut();
            List<SwapPairVO> path = route.getPath();
            NerveToken tokenIn = tokenAmountIn.getToken();
            BigInteger amountIn = tokenAmountIn.getAmount();
            NerveToken tokenOut = tokenAmountOut.getToken();
            BigInteger amountOut = tokenAmountOut.getAmount();
            SwapPairVO pairIn = path.get(0);
            BigInteger reserveIn, reserveOut, _reserveIn, _reserveOut;
            reserveIn = pairIn.getToken0().equals(tokenIn) ? pairIn.getReserve0() : pairIn.getReserve1();
            if (path.size() == 1) {
                reserveOut = pairIn.getToken0().equals(tokenIn) ? pairIn.getReserve1() : pairIn.getReserve0();
            } else {
                SwapPairVO pairOut = path.get(path.size() - 1);
                reserveOut = pairOut.getToken0().equals(tokenOut) ? pairOut.getReserve0() : pairOut.getReserve1();
            }
            _reserveIn = reserveIn.add(amountIn);
            _reserveOut = reserveOut.subtract(amountOut);
            if (_reserveIn.compareTo(BigInteger.ZERO) == 0 || reserveOut.compareTo(BigInteger.ZERO) == 0 ) {
                continue;
            }
            BigDecimal impact = BigDecimal.ONE.subtract(
                            new BigDecimal(_reserveOut.multiply(reserveIn)).divide(
                                    new BigDecimal(_reserveIn.multiply(reserveOut)), 78, RoundingMode.UP))
                    .abs();
            resultList.add(new RouteOfPriceImpactVO(route, impact));
        }
        return resultList;
    }

    private static List<RouteVO> bestTradeExactIn(int chainId, IPairFactory iPairFactory, List<SwapPairVO> pairs, TokenAmount tokenAmountIn,
                                                  NerveToken out, LinkedHashSet<SwapPairVO> currentPath,
                                                  List<RouteVO> bestTrade, TokenAmount orginTokenAmountIn, int depth, int maxPairSize) {
        int length = pairs.size();
        for (int i = 0; i < length; i++) {
            SwapPairVO pair = pairs.get(i);
            NerveToken tokenIn = tokenAmountIn.getToken();
            if (!pair.getToken0().equals(tokenIn) && !pair.getToken1().equals(tokenIn)) continue;
            NerveToken tokenOut = pair.getToken0().equals(tokenIn) ? pair.getToken1() : pair.getToken0();
            if (containsCurrency(currentPath, tokenOut)) continue;
            BigInteger[] reserves = getReservesAndFeeRate(chainId, iPairFactory, tokenIn, tokenOut);
            if (BigInteger.ZERO.equals(reserves[0]) || BigInteger.ZERO.equals(reserves[1])) continue;
            BigInteger amountOut = getAmountOutForBestTrade(tokenAmountIn.getAmount(), reserves[0], reserves[1], reserves[2]);

            if (tokenOut.equals(out)) {
                LinkedHashSet<SwapPairVO> cloneCurrentPath = cloneLinkedHashSet(currentPath);
                cloneCurrentPath.add(pair);
                //currentPath.add(pair);
                bestTrade.add(new RouteVO(cloneCurrentPath.stream().collect(Collectors.toList()), orginTokenAmountIn, new TokenAmount(tokenOut, amountOut)));
            } else if (depth < (maxPairSize - 1) && pairs.size() > 1) {
                LinkedHashSet cloneLinkedHashSet = cloneLinkedHashSet(currentPath);
                cloneLinkedHashSet.add(pair);
                List<SwapPairVO> subList = subList(pairs, 0, i);
                subList.addAll(subList(pairs, i + 1, length));
                bestTradeExactIn(chainId, iPairFactory, subList, new TokenAmount(tokenOut, amountOut), out, cloneLinkedHashSet, bestTrade, orginTokenAmountIn, depth + 1, maxPairSize);
            }
        }
        return bestTrade;
    }

    private static LinkedHashSet cloneLinkedHashSet(LinkedHashSet set) {
        LinkedHashSet<Object> objects = new LinkedHashSet<>();
        objects.addAll(set);
        return objects;
    }

    private static boolean containsCurrency(LinkedHashSet<SwapPairVO> currentPath, NerveToken tokenOut) {
        for (SwapPairVO pair : currentPath) {
            if (pair.hasToken(tokenOut)) {
                return true;
            }
        }
        return false;
    }

    public static List subList(List list, int fromIndex, int toIndex) {
        List objs = new ArrayList();
        for (int i = fromIndex, length = toIndex; i < length; i++) {
            objs.add(list.get(i));
        }
        return objs;
    }

    public static int extractTxTypeFromTx(String txString) throws NulsException {
        String txTypeHexString = txString.substring(0, 4);
        NulsByteBuffer byteBuffer = new NulsByteBuffer(RPCUtil.decode(txTypeHexString));
        return byteBuffer.readUint16();
    }

    public static BigInteger minus(BigInteger a, BigInteger b) {
        BigInteger result = a.subtract(b);
        if (result.compareTo(BigInteger.ZERO) < 0) {
            throw new RuntimeException("Negative number detected.");
        }
        return result;
    }

    public static String asStringByBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] asBytesByBase64(String string) {
        return Base64.getDecoder().decode(string);
    }

    public static String nulsData2Hex(BaseNulsData nulsData) {
        return HexUtil.encode(nulsData2HexBytes(nulsData));
    }

    public static byte[] nulsData2HexBytes(BaseNulsData nulsData) {
        try {
            return nulsData.serialize();
        } catch (IOException e) {
            throw new NulsRuntimeException(e);
        }
    }

    public static Response wrapperFailed(Result result) {
        String msg;
        ErrorCode errorCode;
        if (result != null) {
            errorCode = result.getErrorCode();
            msg = result.getMsg();
            if (StringUtils.isBlank(msg)) {
                msg = errorCode.getMsg();
            }
            Response res = MessageUtil.newFailResponse("", msg);
            res.setResponseErrorCode(errorCode.getCode());
            return res;
        } else {
            return MessageUtil.newFailResponse("", FAILED);
        }
    }

    /**
     * 获取交易签名地址
     *
     * @param tx 交易
     */
    public static byte[] getSingleAddressFromTX(Transaction tx, int chainId, boolean verifySign) throws NulsException {

        if (tx.getTransactionSignature() == null || tx.getTransactionSignature().length == 0) {
            return null;
        }
        try {
            if (tx.isMultiSignTx()) {
                MultiSignTxSignature txSignature = new MultiSignTxSignature();
                txSignature.parse(tx.getTransactionSignature(), 0);
                List<String> pubkeyList = new ArrayList<>();


                if (verifySign) {
                    if ((txSignature.getP2PHKSignatures() == null || txSignature.getP2PHKSignatures().size() == 0)) {
                        throw new NulsException(SwapErrorCode.SIGNATURE_ERROR);
                    }
                    List<P2PHKSignature> validSignatures = txSignature.getValidSignature();
                    int validCount = 0;
                    for (P2PHKSignature signature : validSignatures) {
                        if (ECKey.verify(tx.getHash().getBytes(), signature.getSignData().getSignBytes(), signature.getPublicKey())) {
                            validCount++;
                        } else {
                            throw new NulsException(SwapErrorCode.SIGNATURE_ERROR);
                        }
                    }
                    if (validCount < txSignature.getM()) {
                        throw new NulsException(SwapErrorCode.SIGNATURE_ERROR);
                    }
                }


                for (byte[] pub : txSignature.getPubKeyList()) {
                    pubkeyList.add(HexUtil.encode(pub));
                }
                Address address;
                try {
                    address = new Address(chainId, BaseConstant.P2SH_ADDRESS_TYPE, SerializeUtils.sha256hash160(AddressTool.createMultiSigAccountOriginBytes(chainId, txSignature.getM(), pubkeyList)));
                } catch (Exception e) {
                    Log.error(e);
                    throw new NulsException(SwapErrorCode.FARM_SYRUP_DEPOSIT_ERROR);
                }
                return address.getAddressBytes();
            } else {
                TransactionSignature transactionSignature = new TransactionSignature();
                transactionSignature.parse(tx.getTransactionSignature(), 0);
                List<P2PHKSignature> p2PHKSignatures = transactionSignature.getP2PHKSignatures();
                if (p2PHKSignatures.size() > 1) {
                    throw new NulsException(SwapErrorCode.SIGNATURE_ERROR);
                }
                if (verifySign) {
                    boolean r = ECKey.verify(tx.getHash().getBytes(), p2PHKSignatures.get(0).getSignData().getSignBytes(), p2PHKSignatures.get(0).getPublicKey());
                    if (!r) {
                        throw new NulsException(SwapErrorCode.SIGNATURE_ERROR);
                    }
                }
                return AddressTool.getAddress(p2PHKSignatures.get(0).getPublicKey(), chainId);
            }
        } catch (NulsException e) {
            Log.error("TransactionSignature parse error!");
            throw e;
        }
    }


    public static void updatePool(FarmPoolPO farm, long blockHeight) {
        if (blockHeight <= farm.getLastRewardBlock()) {
            return;
        }
        BigInteger lpSupply = farm.getStakeTokenBalance();
        if (lpSupply.compareTo(BigInteger.ZERO) <= 0) {
            farm.setLastRewardBlock(blockHeight);
            return;
        }
        long realEnd = blockHeight;
        if (blockHeight > SwapContext.PROTOCOL_1_16_0) {
            if (farm.getStopHeight() != null && farm.getStopHeight() > 0 && farm.getStopHeight() < blockHeight) {
                realEnd = farm.getStopHeight();
            }
        }
        long height = realEnd - farm.getLastRewardBlock();
        if (height < 0) {
            height = 0;
        }
        BigInteger syrupReward = BigInteger.valueOf(height).multiply(farm.getSyrupPerBlock());
        BigInteger accSyrupPerShare = farm.getAccSyrupPerShare();
//        pool.accSushiPerShare.add(sushiReward.mul(1e12).div(lpSupply));  // 计算每个lp可分到的sushi数量
        accSyrupPerShare = accSyrupPerShare.add(syrupReward.multiply(SwapConstant.BI_1E12).divide(lpSupply));

        farm.setAccSyrupPerShare(accSyrupPerShare);
        farm.setLastRewardBlock(realEnd);
    }

    public static NerveToken parseTokenStr(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        String s[] = str.split("-");
        if (s.length != 2) {
            return null;
        }
        return new NerveToken(Integer.parseInt(s[0].trim()), Integer.parseInt(s[1].trim()));
    }

    public static void signTx(Transaction tx, byte[] prikey) throws IOException {
        P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByEckey(tx.getHash(), ECKey.fromPrivate(prikey));
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> list = new ArrayList<>();
        list.add(p2PHKSignature);
        transactionSignature.setP2PHKSignatures(list);
        tx.setTransactionSignature(transactionSignature.serialize());
    }

    public static BigInteger getCumulativeAmounts(BigInteger[] amounts) {
        BigInteger result = BigInteger.ZERO;
        if (amounts == null) {
            return result;
        }
        for (BigInteger amount : amounts) {
            if (amount != null) {
                result = result.add(amount);
            }
        }
        return result;
    }

    public static BigInteger getCumulativeAmountsOfStableSwap(BigInteger[] amounts, int[] precisions) {
        BigInteger result = BigInteger.ZERO;
        if (amounts == null) {
            return result;
        }
        int length = amounts.length;
        BigInteger amount;
        int exponent;
        for (int i = 0; i < length; i++) {
            amount = amounts[i];
            exponent = 18 - precisions[i];
            if (amount != null) {
                if (exponent == 0) {
                    result = result.add(amount);
                } else {
                    result = result.add(amount.multiply(BigInteger.TEN.pow(exponent)));
                }
            }
        }
        return result;
    }

    public static BigInteger min(BigInteger[] amounts) {
        if (amounts == null) {
            return null;
        }
        BigInteger min = null;
        for (BigInteger amount : amounts) {
            if (min == null) {
                min = amount;
                continue;
            }
            if (min.compareTo(amount) > 0) {
                min = amount;
            }
        }
        return min;
    }

    public static BigInteger[] convertNegate(BigInteger[] amounts) {
        if (amounts == null) {
            return null;
        }
        int length = amounts.length;
        BigInteger[] nagates = new BigInteger[length];
        for (int i = 0; i < length; i++) {
            if (amounts[i] == null) {
                nagates[i] = BigInteger.ZERO;
            } else {
                nagates[i] = amounts[i].negate();
            }
        }
        return nagates;
    }

    public static BigInteger[] emptyFillZero(BigInteger[] amounts) {
        if (amounts == null) {
            return null;
        }
        int length = amounts.length;
        // 空值填充
        for (int i = 0; i < length; i++) {
            if (amounts[i] == null) {
                amounts[i] = BigInteger.ZERO;
            }
        }
        return amounts;
    }

    public static BigInteger[] cloneBigIntegerArray(BigInteger[] array) {
        if (array == null) {
            return null;
        }
        int length = array.length;
        BigInteger[] result = new BigInteger[length];
        // 填充
        for (int i = 0; i < length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static ErrorCode extractErrorCode(Exception e) {
        ErrorCode errorCode = null;
        if (e instanceof NulsException) {
            NulsException e1 = (NulsException) e;
            errorCode = e1.getErrorCode();
        } else if (e instanceof NulsRuntimeException) {
            NulsRuntimeException e1 = (NulsRuntimeException) e;
            errorCode = e1.getErrorCode();
        }
        if (errorCode == null) {
            errorCode = CommonCodeConstanst.DATA_ERROR;
        }
        return errorCode;
    }

    /**
     * token命名规则:只允许使用大、小写字母、数字、下划线（下划线不能在两端）1~20字节
     * @param name   token
     * @return       验证结果
     */
    public static boolean validTokenNameOrSymbol(String name, boolean isProtocol26) {
        if (!isProtocol26) {
            return FormatValidUtils.validTokenNameOrSymbol(name);
        }
        if (StringUtils.isBlank(name)) {
            return false;
        }

        if(name.equals(NULS)) {
            return false;
        }

        byte[] aliasBytes = name.getBytes(StandardCharsets.UTF_8);
        if (aliasBytes.length < 1 || aliasBytes.length > 20) {
            return false;
        }
        return name.matches("^([a-zA-Z0-9]+[a-zA-Z0-9_]*[a-zA-Z0-9]+)|[a-zA-Z0-9]+${1,20}");
    }
}
