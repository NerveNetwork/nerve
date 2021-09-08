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
package network.nerve.swap.handler.impl.stable;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.FormatValidUtils;
import io.nuls.core.model.StringUtils;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.stable.StableSwapTempPairManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.txdata.stable.CreateStablePairData;

import java.util.HashSet;
import java.util.Set;

import static network.nerve.swap.constant.SwapErrorCode.IDENTICAL_TOKEN;
import static network.nerve.swap.constant.SwapErrorCode.PAIR_ALREADY_EXISTS;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class CreateStablePairHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;

    @Override
    public Integer txType() {
        return TxType.CREATE_SWAP_PAIR_STABLE_COIN;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        NulsHash txHash = tx.getHash();
        Chain chain = chainManager.getChain(chainId);
        NulsLogger logger = chain.getLogger();
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        try {
            StableSwapTempPairManager stableSwapTempPairManager = batchInfo.getStableSwapTempPairManager();
            // 提取业务参数
            CreateStablePairData txData = new CreateStablePairData();
            txData.parse(tx.getTxData(), 0);
            NerveToken[] coins = txData.getCoins();
            int length = coins.length;
            if (length < 2) {
                logger.error("INVALID_COINS! hash-{}", txHash.toHex());
                throw new NulsException(SwapErrorCode.INVALID_COINS);
            }
            String symbol = txData.getSymbol();
            if (StringUtils.isNotBlank(symbol) && !FormatValidUtils.validTokenNameOrSymbol(symbol)) {
                logger.error("INVALID_SYMBOL! hash-{}", txHash.toHex());
                throw new NulsException(SwapErrorCode.INVALID_SYMBOL);
            }
            Set<NerveToken> coinSet = new HashSet<>();
            for (int i = 0; i < length; i++) {
                NerveToken token = coins[i];
                if (!coinSet.add(token)) {
                    throw new NulsException(IDENTICAL_TOKEN);
                }
                LedgerAssetDTO asset = ledgerAssetCache.getLedgerAsset(chainId, token);
                if (asset == null) {
                    logger.error("Ledger asset not exist! hash-{}", txHash.toHex());
                    throw new NulsException(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
                }
                if (asset.getDecimalPlace() > 18) {
                    logger.error("coin_decimal_exceeded! hash-{}", txHash.toHex());
                    throw new NulsException(SwapErrorCode.COIN_DECIMAL_EXCEEDED);
                }
            }

            byte[] stablePairAddressBytes = AddressTool.getAddress(txHash.getBytes(), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
            String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
            // 检查交易对地址是否重复
            if (stableSwapTempPairManager.isExist(stablePairAddress)) {
                throw new NulsException(PAIR_ALREADY_EXISTS);
            }
            // 保存临时创建的交易对
            stableSwapTempPairManager.add(stablePairAddress);
            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);

        } catch (Exception e) {
            Log.error(e);
            // 装填失败的执行结果
            result.setTxType(txType());
            result.setSuccess(false);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setErrorMessage(e instanceof NulsException ? ((NulsException) e).format() : e.getMessage());
        }
        batchInfo.getSwapResultMap().put(tx.getHash().toHex(), result);
        return result;
    }


}
