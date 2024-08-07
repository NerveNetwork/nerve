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
package network.nerve.swap.help;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.TokenAmount;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.vo.RouteVO;
import network.nerve.swap.model.vo.SwapPairVO;
import network.nerve.swap.storage.SwapPairStorageService;
import network.nerve.swap.utils.SwapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2021/5/8
 */
@Component
public class SwapHelper {

    private Chain nerveChain;
    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;
    @Autowired
    private SwapPairStorageService swapPairStorageService;

    public void setNerveChain(Chain nerveChain) {
        this.nerveChain = nerveChain;
    }

    public List<RouteVO> bestTradeExactIn(int chainId, List<String> pairs, TokenAmount tokenAmountIn, NerveToken out, int maxPairSize, String resultRule) throws NulsException {
        if (ledgerAssetCache.getLedgerAsset(chainId, tokenAmountIn.getToken()) == null || ledgerAssetCache.getLedgerAsset(chainId, out) == null) {
            throw new NulsException(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
        }
        List<SwapPairVO> swapPairs = new ArrayList<>();
        // pairsUse all addresses when empty
        if (pairs == null || pairs.isEmpty()) {
            Collection<SwapPairDTO> list = swapPairCache.getList();
            for (SwapPairDTO pairDTO : list) {
                swapPairs.add(new SwapPairVO(pairDTO));
            }
        } else {
            for (String pairAddress : pairs) {
                SwapPairDTO pairDTO = swapPairCache.get(pairAddress);
                if (pairDTO == null) {
                    throw new NulsException(SwapErrorCode.PAIR_NOT_EXIST);
                }
                swapPairs.add(new SwapPairVO(pairDTO));
            }
        }
        List<RouteVO> routes = SwapUtils.bestTradeExactIn(chainId, iPairFactory, swapPairs, tokenAmountIn, out, new LinkedHashSet<>(), new ArrayList<>(), tokenAmountIn, maxPairSize, resultRule);
        return routes;
    }

    public boolean isSupportProtocol15() {
        return nerveChain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_15_0;
    }

    public boolean isSupportProtocol17() {
        return nerveChain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_17_0;
    }

    public boolean isSupportProtocol21() {
        return nerveChain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_21_0;
    }

    public boolean isSupportProtocol22() {
        return nerveChain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_22_0;
    }
    public boolean isSupportProtocol24() {
        return nerveChain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_24_0;
    }
    public boolean isSupportProtocol26() {
        return nerveChain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_26_0;
    }
    public boolean isSupportProtocol28() {
        return nerveChain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_28_0;
    }
    public boolean isSupportProtocol31() {
        return nerveChain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_31_0;
    }
    public boolean isSupportProtocol35() {
        return nerveChain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_35_0;
    }

    public boolean isSupportProtocol36() {
        return nerveChain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_36_0;
    }

    public boolean updateSwapPairFeeRate(int chainId, String swapPairAddress, int feeRate) throws Exception {
        nerveChain.getLogger().info("[Commit SwapFeeRate] swapPairAddress: {}. feeRate: {}", swapPairAddress, feeRate);
        byte[] address = AddressTool.getAddress(swapPairAddress);
        SwapPairPO pair = swapPairStorageService.getPair(address);
        if (pair == null) {
            return false;
        }
        pair.setFeeRate(feeRate);
        swapPairStorageService.savePair(address, pair);
        // Update cache
        swapPairCache.reload(swapPairAddress);
        return true;
    }

    public boolean isLegalSwapFeeRate(Integer chainId, String swapPairAddress, Integer feeRate) {
        byte[] address = AddressTool.getAddress(swapPairAddress);
        SwapPairPO pair = swapPairStorageService.getPair(address);
        if (pair == null) {
            return false;
        }
        if (feeRate == null) {
            return false;
        }
        if (feeRate.intValue() <= 3 || feeRate.intValue() >= 1000) {
            nerveChain.getLogger().error("[CheckSwapFeeRate] error, swapPairAddress: {}. feeRate: {}", swapPairAddress, feeRate);
            return false;
        }
        return true;
    }
}
