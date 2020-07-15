/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.api.model.po;

import java.math.BigInteger;
import java.util.List;

/**
 * @author Eva
 */
public class StatisticalInfo {

    /**
     * 快照时间戳，id
     * 时间戳为 3600 * 1000 的整数倍
     */
    private long time;

    /**
     * 天
     */
    private int date;

    /**
     * 月
     */
    private int month;

    private int year;

    /**
     * 这个快照时间区块的交易总数，统计指定区间内的值时应该取合计 $sum
     */
    private long txCount;

    /**
     * 这个快照时间区块的节点委托总数，统计指定区间内的值应该取最后一个值 $last
     */
    private BigInteger consensusLocked;

    /**
     * 这个快照时间所有的stacking抵押的总数，统计指定区间内的值应该取最后一个值 $last
     */
    private BigInteger stackingTotal;

    /**
     * 这个快照时间区块的节点总数，统计指定区间内的值应该取最后一个值 $last
     */
    private int nodeCount;

    /**
     * 这个快照时间区块的收益率，统计指定区间内的值应该取最后一个值 $last
     */
    private double annualizedReward;

    /**
     * 统计周期的截止高度
     */
    private long lastBlockHeight;

    private List<AssetSnapshotInfo> assetSnapshotList;

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getTxCount() {
        return txCount;
    }

    public void setTxCount(long txCount) {
        this.txCount = txCount;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public BigInteger getConsensusLocked() {
        return consensusLocked;
    }

    public void setConsensusLocked(BigInteger consensusLocked) {
        this.consensusLocked = consensusLocked;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public double getAnnualizedReward() {
        return annualizedReward;
    }

    public void setAnnualizedReward(double annualizedReward) {
        this.annualizedReward = annualizedReward;
    }

    public List<AssetSnapshotInfo> getAssetSnapshotList() {
        return assetSnapshotList;
    }

    public void setAssetSnapshotList(List<AssetSnapshotInfo> assetSnapshotList) {
        this.assetSnapshotList = assetSnapshotList;
    }

    public BigInteger getStackingTotal() {
        return stackingTotal;
    }

    public void setStackingTotal(BigInteger stackingTotal) {
        this.stackingTotal = stackingTotal;
    }

    public long getLastBlockHeight() {
        return lastBlockHeight;
    }

    public void setLastBlockHeight(long lastBlockHeight) {
        this.lastBlockHeight = lastBlockHeight;
    }
}
