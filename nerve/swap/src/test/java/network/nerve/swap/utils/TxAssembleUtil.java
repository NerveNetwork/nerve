package network.nerve.swap.utils;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.ECKey;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.txdata.*;
import network.nerve.swap.model.txdata.stable.CreateStablePairData;
import network.nerve.swap.model.txdata.stable.StableAddLiquidityData;
import network.nerve.swap.model.txdata.stable.StableRemoveLiquidityData;
import network.nerve.swap.model.txdata.stable.StableSwapTradeData;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Niels
 */
public class TxAssembleUtil {

    public static Transaction asmbFarmCreate(NerveToken token1, NerveToken token2, byte[] prikey) throws IOException {
        FarmCreateData data = new FarmCreateData();
        data.setLockedTime(1);
        data.setStartBlockHeight(1);
        data.setStakeToken(token1);
        data.setSyrupToken(token2);
        data.setTotalSyrupAmount(BigInteger.valueOf(10000000000000000L));
        data.setSyrupPerBlock(BigInteger.valueOf(100000000L));

        AssembleTransaction aTx = new AssembleTransaction(data.serialize());



        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.FARM_CREATE);

        BigInteger amount = data.getTotalSyrupAmount();
        LedgerBalance balance = LedgerBalance.newInstance();
        balance.setBalance(amount);
        balance.setAddress(AddressTool.getAddress(ECKey.fromPrivate(prikey).getPubKey(), 9));
        balance.setAssetsId(token2.getAssetId());
        balance.setAssetsChainId(token2.getChainId());
        balance.setNonce(SwapConstant.DEFAULT_NONCE);
        balance.setFreeze(BigInteger.ZERO);

        aTx.newFrom().setFrom(balance, amount).endFrom();
        aTx.newTo().setToAddress(SwapUtils.getFarmAddress(9)).setToLockTime(0).setToAmount(amount).setToAssetsChainId(balance.getAssetsChainId()).setToAssetsId(balance.getAssetsId()).endTo();

        Transaction tx = aTx.build();
        if (null != prikey) {
            P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByEckey(tx.getHash(), ECKey.fromPrivate(prikey));
            TransactionSignature transactionSignature = new TransactionSignature();
            List<P2PHKSignature> list = new ArrayList<>();
            list.add(p2PHKSignature);
            transactionSignature.setP2PHKSignatures(list);
            tx.setTransactionSignature(transactionSignature.serialize());
        }
        return tx;
    }

    public static Transaction asmbFarmStake(NerveToken token, NulsHash farmHash, BigInteger amount, byte[] privKeyBytes) throws IOException {
        FarmStakeChangeData data = new FarmStakeChangeData();
        data.setFarmHash(farmHash);
        data.setAmount(amount);

        AssembleTransaction aTx = new AssembleTransaction(data.serialize());



        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.FARM_STAKE);


        LedgerBalance balance = LedgerBalance.newInstance();
        balance.setBalance(amount);
        balance.setAddress(AddressTool.getAddress(ECKey.fromPrivate(privKeyBytes).getPubKey(), 9));
        balance.setAssetsId(token.getAssetId());
        balance.setAssetsChainId(token.getChainId());
        balance.setNonce(SwapConstant.DEFAULT_NONCE);
        balance.setFreeze(BigInteger.ZERO);

        aTx.newFrom().setFrom(balance, amount).endFrom();
        aTx.newTo().setToAddress(SwapUtils.getFarmAddress(9)).setToLockTime(0).setToAmount(amount).setToAssetsChainId(balance.getAssetsChainId()).setToAssetsId(balance.getAssetsId()).endTo();

        Transaction tx = aTx.build();
        P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByEckey(tx.getHash(), ECKey.fromPrivate(privKeyBytes));
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> list = new ArrayList<>();
        list.add(p2PHKSignature);
        transactionSignature.setP2PHKSignatures(list);
        tx.setTransactionSignature(transactionSignature.serialize());

        return tx;
    }

    public static Transaction asmbFarmWithdraw(NulsHash farmHash, BigInteger amount, byte[] prikey) throws IOException {
        FarmStakeChangeData data = new FarmStakeChangeData();
        data.setFarmHash(farmHash);
        data.setAmount(amount);

        AssembleTransaction aTx = new AssembleTransaction(data.serialize());



        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.FARM_WITHDRAW);


        Transaction tx = aTx.build();
        P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByEckey(tx.getHash(), ECKey.fromPrivate(prikey));
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> list = new ArrayList<>();
        list.add(p2PHKSignature);
        transactionSignature.setP2PHKSignatures(list);
        tx.setTransactionSignature(transactionSignature.serialize());
        return tx;
    }

    public static Transaction asmbSwapPairCreate(int chainId, NerveToken token0, NerveToken token1) throws IOException {
        CreatePairData data = new CreatePairData();
        data.setToken0(token0);
        data.setToken1(token1);

        AssembleTransaction aTx = new AssembleTransaction(data.serialize());

        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.CREATE_SWAP_PAIR);

        Transaction tx = aTx.build();
        return tx;
    }

    public static Transaction asmbSwapAddLiquidity(int chainId, String from,
                                                   BigInteger amountA, BigInteger amountB,
                                                   NerveToken tokenA, NerveToken tokenB,
                                                   BigInteger amountAMin, BigInteger amountBMin,
                                                   long deadline, byte[] to, LedgerTempBalanceManager tempBalanceManager) throws IOException {
        AddLiquidityData data = new AddLiquidityData();
        data.setTokenA(tokenA);
        data.setTokenB(tokenB);
        data.setTo(to);
        data.setDeadline(deadline);
        data.setAmountAMin(amountAMin);
        data.setAmountBMin(amountBMin);

        AssembleTransaction aTx = new AssembleTransaction(data.serialize());

        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.SWAP_ADD_LIQUIDITY);

        byte[] pairAddress = SwapUtils.getPairAddress(chainId, tokenA, tokenB);
        byte[] fromAddress = AddressTool.getAddress(from);
        if (tempBalanceManager == null) {
            aTx.newFrom().setFromAddress(fromAddress)
                    .setFromNonce(SwapConstant.DEFAULT_NONCE)
                    .setFromAssetsChainId(tokenA.getChainId())
                    .setFromAssetsId(tokenA.getAssetId())
                    .setFromAmount(amountA).endFrom();
            aTx.newFrom().setFromAddress(fromAddress)
                    .setFromNonce(SwapConstant.DEFAULT_NONCE)
                    .setFromAssetsChainId(tokenB.getChainId())
                    .setFromAssetsId(tokenB.getAssetId())
                    .setFromAmount(amountB).endFrom();
        } else {
            LedgerBalance balanceA = tempBalanceManager.getBalance(fromAddress, tokenA.getChainId(), tokenA.getAssetId()).getData();
            LedgerBalance balanceB = tempBalanceManager.getBalance(fromAddress, tokenB.getChainId(), tokenB.getAssetId()).getData();
            aTx.newFrom().setFrom(balanceA, amountA).endFrom();
            aTx.newFrom().setFrom(balanceB, amountB).endFrom();
        }

        aTx.newTo().setToAddress(pairAddress)
                .setToAssetsChainId(tokenA.getChainId())
                .setToAssetsId(tokenA.getAssetId())
                .setToAmount(amountA).endTo();
        aTx.newTo().setToAddress(pairAddress)
                .setToAssetsChainId(tokenB.getChainId())
                .setToAssetsId(tokenB.getAssetId())
                .setToAmount(amountB).endTo();
        Transaction tx = aTx.build();
        return tx;
    }

    public static Transaction asmbSwapRemoveLiquidity(int chainId, String from,
                                                      BigInteger amountLP, NerveToken tokenLP,
                                                      NerveToken tokenA, NerveToken tokenB,
                                                      BigInteger amountAMin, BigInteger amountBMin,
                                                      long deadline, byte[] to, LedgerTempBalanceManager tempBalanceManager) throws IOException {
        RemoveLiquidityData data = new RemoveLiquidityData();
        data.setTokenA(tokenA);
        data.setTokenB(tokenB);
        data.setTo(to);
        data.setDeadline(deadline);
        data.setAmountAMin(amountAMin);
        data.setAmountBMin(amountBMin);

        AssembleTransaction aTx = new AssembleTransaction(data.serialize());

        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.SWAP_REMOVE_LIQUIDITY);

        byte[] pairAddress = SwapUtils.getPairAddress(chainId, tokenA, tokenB);
        byte[] fromAddress = AddressTool.getAddress(from);
        if (tempBalanceManager == null) {
            aTx.newFrom().setFromAddress(fromAddress)
                    .setFromNonce(SwapConstant.DEFAULT_NONCE)
                    .setFromAssetsChainId(tokenLP.getChainId())
                    .setFromAssetsId(tokenLP.getAssetId())
                    .setFromAmount(amountLP).endFrom();
        } else {
            LedgerBalance balanceLP = tempBalanceManager.getBalance(fromAddress, tokenLP.getChainId(), tokenLP.getAssetId()).getData();
            aTx.newFrom().setFrom(balanceLP, amountLP).endFrom();
        }
        aTx.newTo().setToAddress(pairAddress)
                .setToAssetsChainId(tokenLP.getChainId())
                .setToAssetsId(tokenLP.getAssetId())
                .setToAmount(amountLP).endTo();
        Transaction tx = aTx.build();
        return tx;
    }


    public static Transaction asmbSwapTrade(int chainId, String from,
                                            BigInteger amountIn,
                                            NerveToken tokenIn,
                                            BigInteger amountOutMin,
                                            NerveToken[] path, byte[] feeTo,
                                            long deadline, byte[] to, LedgerTempBalanceManager tempBalanceManager) throws IOException {
        SwapTradeData data = new SwapTradeData();
        data.setAmountOutMin(amountOutMin);
        data.setTo(to);
        data.setFeeTo(feeTo);
        data.setDeadline(deadline);
        data.setPath(path);

        AssembleTransaction aTx = new AssembleTransaction(data.serialize());

        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.SWAP_TRADE);

        byte[] firstPairAddress = SwapUtils.getPairAddress(chainId, path[0], path[1]);
        byte[] fromAddress = AddressTool.getAddress(from);
        if (tempBalanceManager == null) {
            aTx.newFrom().setFromAddress(fromAddress)
                    .setFromNonce(SwapConstant.DEFAULT_NONCE)
                    .setFromAssetsChainId(tokenIn.getChainId())
                    .setFromAssetsId(tokenIn.getAssetId())
                    .setFromAmount(amountIn).endFrom();
        } else {
            LedgerBalance balanceIn = tempBalanceManager.getBalance(fromAddress, tokenIn.getChainId(), tokenIn.getAssetId()).getData();
            aTx.newFrom().setFrom(balanceIn, amountIn).endFrom();
        }
        aTx.newTo().setToAddress(firstPairAddress)
                .setToAssetsChainId(tokenIn.getChainId())
                .setToAssetsId(tokenIn.getAssetId())
                .setToAmount(amountIn).endTo();
        Transaction tx = aTx.build();
        return tx;
    }


    public static Transaction asmbStableSwapPairCreate(int chainId, String symbol, NerveToken... coins) throws IOException {
        CreateStablePairData data = new CreateStablePairData();
        data.setCoins(coins);
        data.setSymbol(symbol);

        AssembleTransaction aTx = new AssembleTransaction(data.serialize());

        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.CREATE_SWAP_PAIR_STABLE_COIN);

        Transaction tx = aTx.build();
        return tx;
    }

    public static Transaction asmbStableSwapAddLiquidity(int chainId, String from,
                                                         BigInteger[] amounts,
                                                         NerveToken[] tokens,
                                                         byte[] pairAddress,
                                                         byte[] to, LedgerTempBalanceManager tempBalanceManager) throws IOException {
        StableAddLiquidityData data = new StableAddLiquidityData();
        data.setTo(to);

        AssembleTransaction aTx = new AssembleTransaction(data.serialize());

        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.SWAP_ADD_LIQUIDITY_STABLE_COIN);

        byte[] fromAddress = AddressTool.getAddress(from);
        int length = tokens.length;
        for (int i = 0; i < length; i++) {
            NerveToken token = tokens[i];
            BigInteger amount = amounts[i];
            if (tempBalanceManager == null) {
                aTx.newFrom().setFromAddress(fromAddress)
                        .setFromNonce(SwapConstant.DEFAULT_NONCE)
                        .setFromAssetsChainId(token.getChainId())
                        .setFromAssetsId(token.getAssetId())
                        .setFromAmount(amount).endFrom();
            } else {
                LedgerBalance balance = tempBalanceManager.getBalance(fromAddress, token.getChainId(), token.getAssetId()).getData();
                aTx.newFrom().setFrom(balance, amount).endFrom();
            }
            aTx.newTo().setToAddress(pairAddress)
                    .setToAssetsChainId(token.getChainId())
                    .setToAssetsId(token.getAssetId())
                    .setToAmount(amount).endTo();
        }
        Transaction tx = aTx.build();
        return tx;
    }

    public static Transaction asmbStableSwapRemoveLiquidity(int chainId, String from,
                                                            BigInteger amountLP, NerveToken tokenLP,
                                                            byte[] indexs,
                                                            byte[] pairAddress,
                                                            byte[] to, LedgerTempBalanceManager tempBalanceManager) throws IOException {
        StableRemoveLiquidityData data = new StableRemoveLiquidityData();
        data.setIndexs(indexs);
        data.setTo(to);

        AssembleTransaction aTx = new AssembleTransaction(data.serialize());

        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.SWAP_REMOVE_LIQUIDITY_STABLE_COIN);

        byte[] fromAddress = AddressTool.getAddress(from);
        if (tempBalanceManager == null) {
            aTx.newFrom().setFromAddress(fromAddress)
                    .setFromNonce(SwapConstant.DEFAULT_NONCE)
                    .setFromAssetsChainId(tokenLP.getChainId())
                    .setFromAssetsId(tokenLP.getAssetId())
                    .setFromAmount(amountLP).endFrom();
        } else {
            LedgerBalance balance = tempBalanceManager.getBalance(fromAddress, tokenLP.getChainId(), tokenLP.getAssetId()).getData();
            aTx.newFrom().setFrom(balance, amountLP).endFrom();
        }
        aTx.newTo().setToAddress(pairAddress)
                .setToAssetsChainId(tokenLP.getChainId())
                .setToAssetsId(tokenLP.getAssetId())
                .setToAmount(amountLP).endTo();
        Transaction tx = aTx.build();
        return tx;
    }


    public static Transaction asmbStableSwapTrade(int chainId, String from,
                                                  BigInteger[] amountsIn,
                                                  NerveToken[] tokensIn,
                                                  byte tokenOutIndex, byte[] feeTo,
                                                  byte[] pairAddress,
                                                  byte[] to, LedgerTempBalanceManager tempBalanceManager) throws IOException {
        StableSwapTradeData data = new StableSwapTradeData();
        data.setTo(to);
        data.setTokenOutIndex(tokenOutIndex);
        data.setFeeTo(feeTo);

        AssembleTransaction aTx = new AssembleTransaction(data.serialize());

        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.SWAP_TRADE_STABLE_COIN);

        byte[] fromAddress = AddressTool.getAddress(from);
        int length = tokensIn.length;
        for (int i = 0; i < length; i++) {
            NerveToken token = tokensIn[i];
            BigInteger amount = amountsIn[i];
            if (tempBalanceManager == null) {
                aTx.newFrom().setFromAddress(fromAddress)
                        .setFromNonce(SwapConstant.DEFAULT_NONCE)
                        .setFromAssetsChainId(token.getChainId())
                        .setFromAssetsId(token.getAssetId())
                        .setFromAmount(amount).endFrom();
            } else {
                LedgerBalance balance = tempBalanceManager.getBalance(fromAddress, token.getChainId(), token.getAssetId()).getData();
                aTx.newFrom().setFrom(balance, amount).endFrom();
            }
            aTx.newTo().setToAddress(pairAddress)
                    .setToAssetsChainId(token.getChainId())
                    .setToAssetsId(token.getAssetId())
                    .setToAmount(amount).endTo();
        }

        Transaction tx = aTx.build();
        return tx;
    }

}
