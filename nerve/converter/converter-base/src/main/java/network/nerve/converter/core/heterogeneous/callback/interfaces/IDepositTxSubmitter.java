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
     * @param txHash          transactionhash
     * @param blockHeight     Transaction confirmation height
     * @param from            Transfer address
     * @param to              Transfer address
     * @param value           Transfer amount
     * @param txTime          Transaction time
     * @param decimals        Decimal places of assets
     * @param ifContractAsset Whether it is a contract asset
     * @param contractAddress Contract address
     * @param assetId         assetID
     * @param nerveAddress    NerveRecharge address
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
     * @param txHash          transactionhash
     * @param blockHeight     Transaction confirmation height
     * @param from            Transfer address
     * @param to              Transfer address
     * @param value           Transfer amount
     * @param txTime          Transaction time
     * @param decimals        Decimal places of assets
     * @param ifContractAsset Whether it is a contract asset
     * @param contractAddress Contract address
     * @param assetId         assetID
     * @param nerveAddress    NerveRecharge address
     */
    String pendingTxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger value, Long txTime,
                  Integer decimals, Boolean ifContractAsset, String contractAddress, Integer assetId, String nerveAddress) throws Exception;

    String pendingDepositIITxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger erc20Value, Long txTime,
                  Integer erc20Decimals, String erc20ContractAddress, Integer erc20AssetId, String nerveAddress, BigInteger mainAssetValue) throws Exception;

    String pendingOneClickCrossChainTxSubmit(String txHash, Long blockHeight, String from, String to, BigInteger erc20Value, Long txTime,
                                             Integer erc20Decimals, String erc20ContractAddress, Integer erc20AssetId, String nerveAddress, BigInteger mainAssetValue, BigInteger mainAssetFeeAmount, int desChainId, String desToAddress, BigInteger tipping, String tippingAddress, String desExtend) throws Exception;

    /**
     * Verify whether to recharge the transaction
     * @param hTxHash Heterogeneous Chain Tradinghash
     */
    Result validateDepositTx(String hTxHash);

    String depositTxSubmitOfBtcSys(String txHash, Long blockHeight, String from, String to, BigInteger value, Long txTime,
                                   Integer decimals, Boolean ifContractAsset, String contractAddress, Integer assetId, BigInteger fee, String feeTo, String extend) throws Exception;

    String depositIITxSubmitOfBtcSys(String txHash, Long blockHeight, String from, String to, BigInteger erc20Value, Long txTime,
                             Integer erc20Decimals, String erc20ContractAddress, Integer erc20AssetId, BigInteger mainAssetValue, BigInteger fee, String feeTo, String extend) throws Exception;
}
