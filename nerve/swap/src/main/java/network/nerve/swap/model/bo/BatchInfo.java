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
package network.nerve.swap.model.bo;

import io.nuls.base.data.BlockHeader;
import network.nerve.swap.manager.FarmTempManager;
import network.nerve.swap.manager.FarmUserInfoTempManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.manager.SwapTempPairManager;
import network.nerve.swap.manager.stable.StableSwapTempPairManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2021/4/6
 */
public class BatchInfo {

    /**
     * Temporary balance
     */
    private LedgerTempBalanceManager ledgerTempBalanceManager;

    /**
     * Transaction pair temporary cache
     */
    private SwapTempPairManager swapTempPairManager;
    /**
     * Stablecoin trading pair temporary cache
     */
    private StableSwapTempPairManager stableSwapTempPairManager;

    private FarmTempManager farmTempManager;
    private FarmUserInfoTempManager farmUserTempManager;

    /**
     * The block header currently being packaged
     */
    private BlockHeader currentBlockHeader;

    /**
     * The previous block'sstateRoot
     */
    private String preStateRoot;
    /**
     * Transaction execution results
     */
    private Map<String, SwapResult> swapResultMap = new HashMap<>();

    public Map<String, SwapResult> getSwapResultMap() {
        return swapResultMap;
    }

    public LedgerTempBalanceManager getLedgerTempBalanceManager() {
        return ledgerTempBalanceManager;
    }

    public void setLedgerTempBalanceManager(LedgerTempBalanceManager ledgerTempBalanceManager) {
        this.ledgerTempBalanceManager = ledgerTempBalanceManager;
    }

    public BlockHeader getCurrentBlockHeader() {
        return currentBlockHeader;
    }

    public void setCurrentBlockHeader(BlockHeader currentBlockHeader) {
        this.currentBlockHeader = currentBlockHeader;
    }

    public SwapTempPairManager getSwapTempPairManager() {
        return swapTempPairManager;
    }

    public void setSwapTempPairManager(SwapTempPairManager swapTempPairManager) {
        this.swapTempPairManager = swapTempPairManager;
    }

    public StableSwapTempPairManager getStableSwapTempPairManager() {
        return stableSwapTempPairManager;
    }

    public void setStableSwapTempPairManager(StableSwapTempPairManager stableSwapTempPairManager) {
        this.stableSwapTempPairManager = stableSwapTempPairManager;
    }

    public String getPreStateRoot() {
        return preStateRoot;
    }

    public void setPreStateRoot(String preStateRoot) {
        this.preStateRoot = preStateRoot;
    }

    public FarmTempManager getFarmTempManager() {
        return farmTempManager;
    }

    public void setFarmTempManager(FarmTempManager farmTempManager) {
        this.farmTempManager = farmTempManager;
    }

    public FarmUserInfoTempManager getFarmUserTempManager() {
        return farmUserTempManager;
    }

    public void setFarmUserTempManager(FarmUserInfoTempManager farmUserTempManager) {
        this.farmUserTempManager = farmUserTempManager;
    }
}
