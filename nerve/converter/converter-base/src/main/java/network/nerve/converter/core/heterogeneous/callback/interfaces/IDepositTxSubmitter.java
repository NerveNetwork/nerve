/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
package network.nerve.converter.core.heterogeneous.callback.interfaces;

import io.nuls.core.basic.Result;

import java.math.BigInteger;

/**
 * @author: Mimi
 * @date: 2020-02-17
 */
public interface IDepositTxSubmitter {

    /**
     * @param txHash          交易hash
     * @param blockHeight     交易确认高度
     * @param from            转出地址
     * @param to              转入地址
     * @param value           转账金额
     * @param txTime          交易时间
     * @param decimals        资产小数位数
     * @param ifContractAsset 是否为合约资产
     * @param contractAddress 合约地址
     * @param assetId         资产ID
     * @param nerveAddress    Nerve充值地址
     * @param extend
     */
    String txSubmit(String txHash, Long blockHeight, String from, String to, BigInteger value, Long txTime,
                  Integer decimals, Boolean ifContractAsset, String contractAddress, Integer assetId, String nerveAddress, String extend) throws Exception;

    String depositIITxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger erc20Value, Long txTime,
                                    Integer erc20Decimals, String erc20ContractAddress, Integer erc20AssetId, String nerveAddress, BigInteger mainAssetValue, String extend) throws Exception;
    String oneClickCrossChainTxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger erc20Value, Long txTime,
                                    Integer erc20Decimals, String erc20ContractAddress, Integer erc20AssetId, String nerveAddress, BigInteger mainAssetValue, BigInteger mainAssetFeeAmount, int desChainId, String desToAddress, BigInteger tipping, String tippingAddress, String desExtend) throws Exception;

    String addFeeCrossChainTxSubmit(String txHash, Long blockHeight, String from, String to, Long txTime,
                                    String nerveAddress, BigInteger mainAssetValue, String nerveTxHash, String subExtend) throws Exception;

    /**
     * @param txHash          交易hash
     * @param blockHeight     交易确认高度
     * @param from            转出地址
     * @param to              转入地址
     * @param value           转账金额
     * @param txTime          交易时间
     * @param decimals        资产小数位数
     * @param ifContractAsset 是否为合约资产
     * @param contractAddress 合约地址
     * @param assetId         资产ID
     * @param nerveAddress    Nerve充值地址
     */
    String pendingTxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger value, Long txTime,
                  Integer decimals, Boolean ifContractAsset, String contractAddress, Integer assetId, String nerveAddress) throws Exception;

    String pendingDepositIITxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger erc20Value, Long txTime,
                  Integer erc20Decimals, String erc20ContractAddress, Integer erc20AssetId, String nerveAddress, BigInteger mainAssetValue) throws Exception;

    String pendingOneClickCrossChainTxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger erc20Value, Long txTime,
                                             Integer erc20Decimals, String erc20ContractAddress, Integer erc20AssetId, String nerveAddress, BigInteger mainAssetValue, BigInteger mainAssetFeeAmount, int desChainId, String desToAddress, BigInteger tipping, String tippingAddress, String desExtend) throws Exception;

    /**
     * 验证是否充值交易
     * @param hTxHash 异构链交易hash
     */
    Result validateDepositTx(String hTxHash) ;
}
