/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.swap.service.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.handler.SwapInvoker;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.*;
import network.nerve.swap.manager.stable.StableSwapTempPairManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.NonceBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.txdata.AddLiquidityData;
import network.nerve.swap.model.txdata.CreatePairData;
import network.nerve.swap.model.txdata.RemoveLiquidityData;
import network.nerve.swap.model.txdata.SwapTradeData;
import network.nerve.swap.model.txdata.stable.CreateStablePairData;
import network.nerve.swap.model.txdata.stable.StableAddLiquidityData;
import network.nerve.swap.model.txdata.stable.StableRemoveLiquidityData;
import network.nerve.swap.model.txdata.stable.StableSwapTradeData;
import network.nerve.swap.rpc.call.AccountCall;
import network.nerve.swap.rpc.call.TransactionCall;
import network.nerve.swap.service.SwapService;
import network.nerve.swap.tx.v1.helpers.converter.LedgerService;
import network.nerve.swap.utils.AssembleTransaction;
import network.nerve.swap.utils.CoinDataMaker;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static network.nerve.swap.constant.SwapConstant.INITIAL_STATE_ROOT;
import static network.nerve.swap.constant.SwapErrorCode.INVALID_PATH;

/**
 * @author: PierreLuo
 * @date: 2021/4/15
 */
@Component
public class SwapServiceImpl implements SwapService {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private SwapInvoker swapInvoker;
    @Autowired
    private LedgerService ledgerService;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;
    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private SwapHelper swapHelper;

    @Override
    public Result begin(int chainId, long blockHeight, long blockTime, String preStateRoot) {
        Chain chain = chainManager.getChain(chainId);
        BatchInfo batchInfo = new BatchInfo();
        if (StringUtils.isBlank(preStateRoot)) {
            preStateRoot = INITIAL_STATE_ROOT;
        }
        batchInfo.setPreStateRoot(preStateRoot);
        // Initialize batch execution basic data
        chain.setBatchInfo(batchInfo);
        // Prepare temporary balance
        LedgerTempBalanceManager tempBalanceManager = LedgerTempBalanceManager.newInstance(chainId);
        batchInfo.setLedgerTempBalanceManager(tempBalanceManager);
        // Prepare the current block header
        BlockHeader tempHeader = new BlockHeader();
        tempHeader.setHeight(blockHeight);
        tempHeader.setTime(blockTime);
        batchInfo.setCurrentBlockHeader(tempHeader);
        // Prepare for temporary transactions
        SwapTempPairManager tempPairManager = SwapTempPairManager.newInstance(chainId);
        batchInfo.setSwapTempPairManager(tempPairManager);
        // prepareStableTemporary transaction pairs
        StableSwapTempPairManager stableSwapTempPairManager = StableSwapTempPairManager.newInstance(chainId);
        batchInfo.setStableSwapTempPairManager(stableSwapTempPairManager);
        //Prepare for temporaryFarmManager
        FarmTempManager farmTempManager = new FarmTempManager();
        batchInfo.setFarmTempManager(farmTempManager);
        //Prepare for temporaryFarmUserInfoManager
        FarmUserInfoTempManager farmUserInfoTempManager = new FarmUserInfoTempManager();
        batchInfo.setFarmUserTempManager(farmUserInfoTempManager);
        return Result.getSuccess(null);
    }

    @Override
    public Result invokeOneByOne(int chainId, long blockHeight, long blockTime, Transaction tx) {
        try {
            SwapResult swapResult = swapInvoker.invoke(chainId, tx, blockHeight, blockTime);
            SwapContext.logger.info("[{}]Call result: {}", blockHeight, swapResult.toString());
            Map<String, Object> _result = new HashMap<>();
            _result.put("success", swapResult.isSuccess());
            if (null != swapResult.getSubTxStr()) {
                _result.put("txList", Arrays.asList(swapResult.getSubTxStr()));
            }
            return Result.getSuccess(_result);
        } catch (NulsException e) {
            SwapContext.logger.error(e.format(), e);
            return Result.getFailed(e.getErrorCode());
        }
    }

    @Override
    public Result end(int chainId, long blockHeight) {
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        // Cache height must be consistent
        if (blockHeight != batchInfo.getCurrentBlockHeader().getHeight()) {
            return Result.getFailed(SwapErrorCode.BLOCK_HEIGHT_INCONSISTENCY);
        }
        Map<String, SwapResult> resultMap = batchInfo.getSwapResultMap();
        Set<Map.Entry<String, SwapResult>> entries = resultMap.entrySet();
        byte[] bytes = new byte[32 * (entries.size() + 1)];
        int i = 0;
        for (Map.Entry<String, SwapResult> entry : entries) {
            Log.info("subTxStr: {}", entry.getValue().getSubTxStr());
            byte[] modelSerialize = SwapDBUtil.getModelSerialize(entry.getValue());
            byte[] hash = Sha256Hash.hash(modelSerialize);
            System.arraycopy(hash, 0, bytes, i * 32, 32);
            i++;
        }
        String preStateRoot = batchInfo.getPreStateRoot();
        System.arraycopy(HexUtil.decode(preStateRoot), 0, bytes, i * 32, 32);
        Map<String, Object> result = new HashMap<>();
        result.put("stateRoot", HexUtil.encode(Sha256Hash.hash(bytes)));
        return Result.getSuccess(result);
    }

    private Result<String> checkAccount(int chainId, String address, String password) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(SwapErrorCode.CHAIN_NOT_EXIST);
        }
        NulsLogger logger = chain.getLogger();
        //Account verification
        String prikeyHex;
        try {
            prikeyHex = AccountCall.getAccountPrikey(chainId, address, password);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }

        if (StringUtils.isBlank(prikeyHex)) {
            return Result.getFailed(SwapErrorCode.ACCOUNT_VALID_ERROR);
        }
        return Result.getSuccess(prikeyHex);
    }

    private Result<String> newTx(int chainId, String prikeyHex, Transaction tx, NulsLogger logger) {
        byte[] prikey = HexUtil.decode(prikeyHex);
        try {
            SwapUtils.signTx(tx, prikey);
            boolean result = TransactionCall.newTx(chainId, HexUtil.encode(tx.serialize()));
            if (result) {
                return Result.getSuccess(tx.getHash().toHex());
            } else {
                return Result.getFailed(SwapErrorCode.SYS_UNKOWN_EXCEPTION);
            }
        } catch (IOException e) {
            logger.error(e);
            return Result.getFailed(SwapErrorCode.IO_ERROR);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
    }

    @Override
    public Result<String> swapCreatePair(int chainId, String address, String password, String tokenA, String tokenB) {
        // Check account
        Result<String> checkResult = this.checkAccount(chainId, address, password);
        if (checkResult.isFailed()) {
            return checkResult;
        }
        String prikeyHex = checkResult.getData();
        NulsLogger logger = chainManager.getChainMap().get(chainId).getLogger();
        // Simple inspection of transaction business
        NerveToken _tokenA = SwapUtils.parseTokenStr(tokenA);
        NerveToken _tokenB = SwapUtils.parseTokenStr(tokenB);
        if (ledgerAssetCache.getLedgerAsset(chainId, _tokenA) == null || ledgerAssetCache.getLedgerAsset(chainId, _tokenB) == null) {
            logger.warn("Incorrect asset type");
            return Result.getFailed(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
        }
        // Assembly transaction
        Transaction tx = new Transaction();
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        tx.setType(TxType.CREATE_SWAP_PAIR);
        CreatePairData data = new CreatePairData();
        data.setToken0(_tokenA);
        data.setToken1(_tokenB);
        tx.setTxData(SwapUtils.nulsData2HexBytes(data));

        NerveToken mainToken = new NerveToken(chainId, 1);
        NonceBalance balance;
        try {
            balance = ledgerService.getBalanceNonce(chainId, mainToken.getChainId(), mainToken.getAssetId(), address);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
        byte[] addressBytes = AddressTool.getAddress(address);
        CoinData coinData = new CoinDataMaker()
                .addFrom(addressBytes, mainToken, BigInteger.ZERO, balance.getNonce(), (byte) 0)
                .addTo(addressBytes, mainToken, BigInteger.ZERO, 0)
                .getCoinData();
        tx.setCoinData(SwapUtils.nulsData2HexBytes(coinData));
        // Broadcasting transactions
        return this.newTx(chainId, prikeyHex, tx, logger);
    }

    @Override
    public Result<String> swapAddLiquidity(int chainId, String address, String password, BigInteger amountA, BigInteger amountB,
                                           String tokenA, String tokenB, BigInteger amountAMin, BigInteger amountBMin,
                                           long deadline, String to) {
        // Check account
        Result<String> checkResult = this.checkAccount(chainId, address, password);
        if (checkResult.isFailed()) {
            return checkResult;
        }
        String prikeyHex = checkResult.getData();
        Chain chain = chainManager.getChainMap().get(chainId);
        NulsLogger logger = chain.getLogger();
        // Simple inspection of transaction business
        if (chain.getLatestBasicBlock().getTime() > deadline) {
            return Result.getFailed(SwapErrorCode.EXPIRED);
        }
        NerveToken _tokenA = SwapUtils.parseTokenStr(tokenA);
        NerveToken _tokenB = SwapUtils.parseTokenStr(tokenB);
        if (ledgerAssetCache.getLedgerAsset(chainId, _tokenA) == null || ledgerAssetCache.getLedgerAsset(chainId, _tokenB) == null) {
            logger.warn("Incorrect asset type");
            return Result.getFailed(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
        }
        byte[] pairAddress = SwapUtils.getPairAddress(chainId, _tokenA, _tokenB);
        if (!swapPairCache.isExist(AddressTool.getStringAddressByBytes(pairAddress))) {
            logger.warn("Transaction pair address does not exist");
            return Result.getFailed(SwapErrorCode.PAIR_NOT_EXIST);
        }
        // Assembly transaction
        AddLiquidityData data = new AddLiquidityData();
        data.setTokenA(_tokenA);
        data.setTokenB(_tokenB);
        data.setTo(AddressTool.getAddress(to));
        data.setDeadline(deadline);
        data.setAmountAMin(amountAMin);
        data.setAmountBMin(amountBMin);
        AssembleTransaction aTx = new AssembleTransaction(SwapUtils.nulsData2HexBytes(data));
        aTx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        aTx.setTxType(TxType.SWAP_ADD_LIQUIDITY);

        try {
            LedgerBalance balanceA = ledgerService.getLedgerBalance(chainId, _tokenA.getChainId(), _tokenA.getAssetId(), address);
            LedgerBalance balanceB = ledgerService.getLedgerBalance(chainId, _tokenB.getChainId(), _tokenB.getAssetId(), address);
            aTx.newFrom().setFrom(balanceA, amountA).endFrom();
            aTx.newFrom().setFrom(balanceB, amountB).endFrom();
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }

        aTx.newTo().setToAddress(pairAddress)
                .setToAssetsChainId(_tokenA.getChainId())
                .setToAssetsId(_tokenA.getAssetId())
                .setToAmount(amountA).endTo();
        aTx.newTo().setToAddress(pairAddress)
                .setToAssetsChainId(_tokenB.getChainId())
                .setToAssetsId(_tokenB.getAssetId())
                .setToAmount(amountB).endTo();
        Transaction tx = aTx.build();
        // Broadcasting transactions
        return this.newTx(chainId, prikeyHex, tx, logger);
    }

    @Override
    public Result<String> swapRemoveLiquidity(int chainId, String address, String password, BigInteger amountLP, String tokenLP,
                                              String tokenA, String tokenB, BigInteger amountAMin, BigInteger amountBMin,
                                              long deadline, String to) {
        // Check account
        Result<String> checkResult = this.checkAccount(chainId, address, password);
        if (checkResult.isFailed()) {
            return checkResult;
        }
        String prikeyHex = checkResult.getData();
        Chain chain = chainManager.getChainMap().get(chainId);
        NulsLogger logger = chain.getLogger();
        // Simple inspection of transaction business
        if (chain.getLatestBasicBlock().getTime() > deadline) {
            return Result.getFailed(SwapErrorCode.EXPIRED);
        }
        NerveToken _tokenA = SwapUtils.parseTokenStr(tokenA);
        NerveToken _tokenB = SwapUtils.parseTokenStr(tokenB);
        NerveToken _tokenLP = SwapUtils.parseTokenStr(tokenLP);
        if (ledgerAssetCache.getLedgerAsset(chainId, _tokenA) == null || ledgerAssetCache.getLedgerAsset(chainId, _tokenB) == null || ledgerAssetCache.getLedgerAsset(chainId, _tokenLP) == null) {
            logger.warn("Incorrect asset type");
            return Result.getFailed(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
        }
        byte[] pairAddress = SwapUtils.getPairAddress(chainId, _tokenA, _tokenB);
        if (!swapPairCache.isExist(AddressTool.getStringAddressByBytes(pairAddress))) {
            logger.warn("Transaction pair address does not exist");
            return Result.getFailed(SwapErrorCode.PAIR_NOT_EXIST);
        }
        // Assembly transaction
        RemoveLiquidityData data = new RemoveLiquidityData();
        data.setTokenA(_tokenA);
        data.setTokenB(_tokenB);
        data.setTo(AddressTool.getAddress(to));
        data.setDeadline(deadline);
        data.setAmountAMin(amountAMin);
        data.setAmountBMin(amountBMin);

        AssembleTransaction aTx = new AssembleTransaction(SwapUtils.nulsData2HexBytes(data));

        aTx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        aTx.setTxType(TxType.SWAP_REMOVE_LIQUIDITY);

        try {
            LedgerBalance balanceLP = ledgerService.getLedgerBalance(chainId, _tokenLP.getChainId(), _tokenLP.getAssetId(), address);
            aTx.newFrom().setFrom(balanceLP, amountLP).endFrom();
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }

        aTx.newTo().setToAddress(pairAddress)
                .setToAssetsChainId(_tokenLP.getChainId())
                .setToAssetsId(_tokenLP.getAssetId())
                .setToAmount(amountLP).endTo();
        Transaction tx = aTx.build();
        // Broadcasting transactions
        return this.newTx(chainId, prikeyHex, tx, logger);
    }

    @Override
    public Result<String> swapTokenTrade(int chainId, String address, String password, BigInteger amountIn,
                                         BigInteger amountOutMin, String[] tokenPath, String feeTo, long deadline, String to) {
        // Check account
        Result<String> checkResult = this.checkAccount(chainId, address, password);
        if (checkResult.isFailed()) {
            return checkResult;
        }
        String prikeyHex = checkResult.getData();
        Chain chain = chainManager.getChainMap().get(chainId);
        NulsLogger logger = chain.getLogger();
        // Simple inspection of transaction business
        if (chain.getLatestBasicBlock().getTime() > deadline) {
            return Result.getFailed(SwapErrorCode.EXPIRED);
        }
        int length = tokenPath.length;
        if (length < 2) {
            return Result.getFailed(INVALID_PATH);
        }
        NerveToken[] _tokenPath = new NerveToken[length];
        for (int i = 0; i < length; i++) {
            _tokenPath[i] = SwapUtils.parseTokenStr(tokenPath[i]);
        }
        for (NerveToken token : _tokenPath) {
            if (ledgerAssetCache.getLedgerAsset(chainId, token) == null) {
                logger.warn("Incorrect asset type");
                return Result.getFailed(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
            }
        }
        byte[] firstPairAddress = SwapUtils.getPairAddress(chainId, _tokenPath[0], _tokenPath[1]);
        if (!swapPairCache.isExist(AddressTool.getStringAddressByBytes(firstPairAddress))) {
            logger.warn("Transaction pair address does not exist");
            return Result.getFailed(SwapErrorCode.PAIR_NOT_EXIST);
        }

        // Assembly transaction
        NerveToken tokenIn = _tokenPath[0];
        SwapTradeData data = new SwapTradeData();
        data.setAmountOutMin(amountOutMin);
        data.setTo(AddressTool.getAddress(to));
        data.setFeeTo(feeTo != null ? AddressTool.getAddress(feeTo) : null);
        data.setDeadline(deadline);
        data.setPath(_tokenPath);

        AssembleTransaction aTx = new AssembleTransaction(SwapUtils.nulsData2HexBytes(data));

        aTx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        aTx.setTxType(TxType.SWAP_TRADE);

        try {
            LedgerBalance balanceIn = ledgerService.getLedgerBalance(chainId, tokenIn.getChainId(), tokenIn.getAssetId(), address);
            aTx.newFrom().setFrom(balanceIn, amountIn).endFrom();
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
        aTx.newTo().setToAddress(firstPairAddress)
                .setToAssetsChainId(tokenIn.getChainId())
                .setToAssetsId(tokenIn.getAssetId())
                .setToAmount(amountIn).endTo();
        Transaction tx = aTx.build();
        // Broadcasting transactions
        return this.newTx(chainId, prikeyHex, tx, logger);
    }

    @Override
    public Result<String> stableSwapCreatePair(int chainId, String address, String password, String[] coins, String symbol) {
        // Check account
        Result<String> checkResult = this.checkAccount(chainId, address, password);
        if (checkResult.isFailed()) {
            return checkResult;
        }
        String prikeyHex = checkResult.getData();
        NulsLogger logger = chainManager.getChainMap().get(chainId).getLogger();
        // Simple inspection of transaction business
        int length = coins.length;
        NerveToken[] _coins = new NerveToken[length];
        for (int i = 0; i < length; i++) {
            _coins[i] = SwapUtils.parseTokenStr(coins[i]);
        }
        for (NerveToken token : _coins) {
            if (ledgerAssetCache.getLedgerAsset(chainId, token) == null) {
                logger.warn("Incorrect asset type");
                return Result.getFailed(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
            }
        }
        if (StringUtils.isNotBlank(symbol) && !SwapUtils.validTokenNameOrSymbol(symbol, swapHelper.isSupportProtocol26())) {
            logger.error("INVALID_SYMBOL!");
            return Result.getFailed(SwapErrorCode.INVALID_SYMBOL);
        }
        // Assembly transaction
        CreateStablePairData data = new CreateStablePairData();
        data.setCoins(_coins);
        data.setSymbol(symbol);

        Transaction tx = new Transaction();
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        tx.setType(TxType.CREATE_SWAP_PAIR_STABLE_COIN);
        tx.setTxData(SwapUtils.nulsData2HexBytes(data));

        NerveToken mainToken = new NerveToken(chainId, 1);
        NonceBalance balance;
        try {
            balance = ledgerService.getBalanceNonce(chainId, mainToken.getChainId(), mainToken.getAssetId(), address);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
        byte[] addressBytes = AddressTool.getAddress(address);
        CoinData coinData = new CoinDataMaker()
                .addFrom(addressBytes, mainToken, BigInteger.ZERO, balance.getNonce(), (byte) 0)
                .addTo(addressBytes, mainToken, BigInteger.ZERO, 0)
                .getCoinData();
        tx.setCoinData(SwapUtils.nulsData2HexBytes(coinData));
        // Broadcasting transactions
        return this.newTx(chainId, prikeyHex, tx, logger);
    }

    @Override
    public Result<String> stableSwapAddLiquidity(int chainId, String address, String password, BigInteger[] amounts, String[] tokens,
                                                 String pairAddress, long deadline, String to) {
        // Check account
        Result<String> checkResult = this.checkAccount(chainId, address, password);
        if (checkResult.isFailed()) {
            return checkResult;
        }
        String prikeyHex = checkResult.getData();
        Chain chain = chainManager.getChainMap().get(chainId);
        NulsLogger logger = chain.getLogger();
        // Simple inspection of transaction business
        if (chain.getLatestBasicBlock().getTime() > deadline) {
            return Result.getFailed(SwapErrorCode.EXPIRED);
        }
        int length = tokens.length;
        NerveToken[] _tokens = new NerveToken[length];
        for (int i = 0; i < length; i++) {
            _tokens[i] = SwapUtils.parseTokenStr(tokens[i]);
        }
        for (NerveToken token : _tokens) {
            if (ledgerAssetCache.getLedgerAsset(chainId, token) == null) {
                logger.warn("Incorrect asset type");
                return Result.getFailed(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
            }
        }

        byte[] pairAddressBytes = AddressTool.getAddress(pairAddress);
        if (!stableSwapPairCache.isExist(pairAddress)) {
            logger.warn("Transaction pair address does not exist");
            return Result.getFailed(SwapErrorCode.PAIR_NOT_EXIST);
        }
        // Assembly transaction
        StableAddLiquidityData data = new StableAddLiquidityData();
        data.setTo(AddressTool.getAddress(to));
        AssembleTransaction aTx = new AssembleTransaction(SwapUtils.nulsData2HexBytes(data));
        aTx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        aTx.setTxType(TxType.SWAP_ADD_LIQUIDITY_STABLE_COIN);

        try {
            for (int i = 0; i < length; i++) {
                NerveToken token = _tokens[i];
                BigInteger amount = amounts[i];
                LedgerBalance balance = ledgerService.getLedgerBalance(chainId, token.getChainId(), token.getAssetId(), address);
                aTx.newFrom().setFrom(balance, amount).endFrom();
                aTx.newTo().setToAddress(pairAddressBytes)
                        .setToAssetsChainId(token.getChainId())
                        .setToAssetsId(token.getAssetId())
                        .setToAmount(amount).endTo();
            }
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
        Transaction tx = aTx.build();
        // Broadcasting transactions
        return this.newTx(chainId, prikeyHex, tx, logger);
    }

    @Override
    public Result<String> stableSwapRemoveLiquidity(int chainId, String address, String password, BigInteger amountLP, String tokenLP,
                                                    Integer[] receiveOrderIndexs, String pairAddress, long deadline, String to) {
        // Check account
        Result<String> checkResult = this.checkAccount(chainId, address, password);
        if (checkResult.isFailed()) {
            return checkResult;
        }
        String prikeyHex = checkResult.getData();
        Chain chain = chainManager.getChainMap().get(chainId);
        NulsLogger logger = chain.getLogger();
        // Simple inspection of transaction business
        if (chain.getLatestBasicBlock().getTime() > deadline) {
            return Result.getFailed(SwapErrorCode.EXPIRED);
        }
        NerveToken _tokenLP = SwapUtils.parseTokenStr(tokenLP);
        if (ledgerAssetCache.getLedgerAsset(chainId, _tokenLP) == null) {
            logger.warn("Incorrect asset type");
            return Result.getFailed(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
        }
        byte[] pairAddressBytes = AddressTool.getAddress(pairAddress);
        if (!stableSwapPairCache.isExist(pairAddress)) {
            logger.warn("Transaction pair address does not exist");
            return Result.getFailed(SwapErrorCode.PAIR_NOT_EXIST);
        }
        // Assembly transaction
        int length = receiveOrderIndexs.length;
        byte[] indexs = new byte[length];
        for (int i = 0; i < length; i++) {
            indexs[i] = receiveOrderIndexs[i].byteValue();
        }
        StableRemoveLiquidityData data = new StableRemoveLiquidityData();
        data.setIndexs(indexs);
        data.setTo(AddressTool.getAddress(to));

        AssembleTransaction aTx = new AssembleTransaction(SwapUtils.nulsData2HexBytes(data));

        aTx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        aTx.setTxType(TxType.SWAP_REMOVE_LIQUIDITY_STABLE_COIN);

        try {
            LedgerBalance balanceLP = ledgerService.getLedgerBalance(chainId, _tokenLP.getChainId(), _tokenLP.getAssetId(), address);
            aTx.newFrom().setFrom(balanceLP, amountLP).endFrom();
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }

        aTx.newTo().setToAddress(pairAddressBytes)
                .setToAssetsChainId(_tokenLP.getChainId())
                .setToAssetsId(_tokenLP.getAssetId())
                .setToAmount(amountLP).endTo();
        Transaction tx = aTx.build();
        // Broadcasting transactions
        return this.newTx(chainId, prikeyHex, tx, logger);
    }

    @Override
    public Result<String> stableSwapTokenTrade(int chainId, String address, String password, BigInteger[] amountsIn,
                                               String[] tokensIn, int tokenOutIndex, String feeTo,
                                               String pairAddress, long deadline, String to, String feeTokenStr, String feeAmountStr) {
        // Check account
        Result<String> checkResult = this.checkAccount(chainId, address, password);
        if (checkResult.isFailed()) {
            return checkResult;
        }
        String prikeyHex = checkResult.getData();
        Chain chain = chainManager.getChainMap().get(chainId);
        NulsLogger logger = chain.getLogger();
        // Simple inspection of transaction business
        if (chain.getLatestBasicBlock().getTime() > deadline) {
            return Result.getFailed(SwapErrorCode.EXPIRED);
        }
        boolean hasFee = false;
        byte[] feeToBytes = feeTo != null ? AddressTool.getAddress(feeTo) : null;
        NerveToken feeToken = null;
        BigInteger feeAmount = null;
        if (StringUtils.isNotBlank(feeTo) && StringUtils.isNotBlank(feeTokenStr) && StringUtils.isNotBlank(feeAmountStr)) {
            hasFee = true;
            feeToken = SwapUtils.parseTokenStr(feeTokenStr);
            feeAmount = new BigInteger(feeAmountStr);
        }
        int length = tokensIn.length;
        NerveToken[] _tokensIn = new NerveToken[length];
        for (int i = 0; i < length; i++) {
            _tokensIn[i] = SwapUtils.parseTokenStr(tokensIn[i]);
        }
        for (NerveToken tokenIn : _tokensIn) {
            if (ledgerAssetCache.getLedgerAsset(chainId, tokenIn) == null) {
                logger.warn("Incorrect asset type");
                return Result.getFailed(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
            }
        }

        byte[] pairAddressBytes = AddressTool.getAddress(pairAddress);
        if (!stableSwapPairCache.isExist(pairAddress)) {
            logger.warn("Transaction pair address does not exist");
            return Result.getFailed(SwapErrorCode.PAIR_NOT_EXIST);
        }

        // Assembly transaction
        StableSwapTradeData data = new StableSwapTradeData();
        data.setTo(AddressTool.getAddress(to));
        data.setTokenOutIndex((byte) tokenOutIndex);
        data.setFeeTo(feeToBytes);

        AssembleTransaction aTx = new AssembleTransaction(SwapUtils.nulsData2HexBytes(data));

        aTx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        aTx.setTxType(TxType.SWAP_TRADE_STABLE_COIN);

        try {
            for (int i = 0; i < length; i++) {
                NerveToken token = _tokensIn[i];
                LedgerBalance balance = ledgerService.getLedgerBalance(chainId, token.getChainId(), token.getAssetId(), address);
                BigInteger amount = amountsIn[i];
                aTx.newFrom().setFrom(balance, amount).endFrom();
                if (hasFee && token.equals(feeToken)) {
                    amount = amount.subtract(feeAmount);
                    aTx.newTo().setToAddress(feeToBytes)
                            .setToAssetsChainId(token.getChainId())
                            .setToAssetsId(token.getAssetId())
                            .setToAmount(feeAmount).endTo();
                }
                aTx.newTo().setToAddress(pairAddressBytes)
                        .setToAssetsChainId(token.getChainId())
                        .setToAssetsId(token.getAssetId())
                        .setToAmount(amount).endTo();
            }
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
        Transaction tx = aTx.build();
        // Broadcasting transactions
        return this.newTx(chainId, prikeyHex, tx, logger);
    }

    @Override
    public Result<String> getPairAddressByTokenLP(int chainId, String tokenLP) {
        String address = swapPairCache.getPairAddressByTokenLP(chainId, SwapUtils.parseTokenStr(tokenLP));
        if (StringUtils.isBlank(address)) {
            address = stableSwapPairCache.getPairAddressByTokenLP(chainId, SwapUtils.parseTokenStr(tokenLP));
            if (StringUtils.isBlank(address)) {
                return Result.getFailed(SwapErrorCode.DATA_NOT_FOUND);
            }
        }
        return Result.getSuccess(address);
    }

}
