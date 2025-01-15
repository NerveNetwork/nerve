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

package network.nerve.converter.core.api.interfaces;

import io.nuls.core.exception.NulsException;
import network.nerve.converter.btc.txdata.*;
import network.nerve.converter.enums.AssetName;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2024/2/29
 */
public interface IBitCoinApi {

    List<UTXOData> getUTXOs(String address);

    long getFeeRate();

    boolean isEnoughFeeOfWithdraw(String nerveTxHash, AssetName feeAssetName, BigDecimal fee, BigInteger transfer);

    String signWithdraw(String txHash, String toAddress, BigInteger value, Integer assetId) throws Exception;

    Boolean verifySignWithdraw(String signAddress, String txHash, String toAddress, BigInteger amount, int assetId, String signature) throws Exception;

    String createOrSignWithdrawTx(String txHash, String toAddress, BigInteger value, Integer assetId, String signed, boolean checkOrder) throws Exception;

    List<String> getMultiSignAddressPubs(String address);

    // a check function to see whether the available public key of the current bitSys'chain is greater than total * (2/3) + 1
    // Used to determine whether cfmTask needs to send a withdrawal UTXO transaction, used to generate a new multi-signature address, and transfer assets from the current multi-signature address to the new multi-signature address.
    boolean checkEnoughAvailablePubs(String address);

    String createOrSignManagerChangesTx(String txHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData, boolean checkOrder) throws Exception;
    String signManagerChanges(String txHash, String[] addAddresses, String[] removeAddresses, int orginTxCount) throws Exception;
    Boolean verifySignManagerChanges(String signAddress, String txHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signed) throws Exception;
    boolean validateManagerChangesTx(String txHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData) throws Exception;

    boolean hasRecordFeePayment(String htgTxHash) throws NulsException;
    void recordFeePayment(long blockHeight, String blockHash, String htgTxHash, long fee, boolean recharge, Boolean nerveInner) throws Exception;
    BigInteger getChainWithdrawalFee();
    WithdrawalFeeLog getWithdrawalFeeLogFromDB(String htgTxHash) throws Exception;
    WithdrawalFeeLog takeWithdrawalFeeLogFromTxParse(String htgTxHash, boolean nerveInner) throws Exception;

    void saveWithdrawalUTXOs(WithdrawalUTXOTxData txData) throws Exception;
    boolean isLockedUTXO(String txid, int vout);
    String getNerveHashByLockedUTXO(String txid, int vout);
    WithdrawalUTXOTxData checkLockedUTXO(String nerveTxHash, List<UsedUTXOData> usedUTXOs) throws Exception;
    WithdrawalUTXOTxData takeWithdrawalUTXOs(String nerveTxhash);
    void saveWithdrawalUTXORebuildPO(String nerveTxHash, WithdrawalUTXORebuildPO po) throws Exception;
    WithdrawalUTXORebuildPO getWithdrawalUTXORebuildPO(String nerveTxHash) throws Exception;

    long getWithdrawalFeeSize(long fromTotal, long transfer, long feeRate, int inputNum);
    long getChangeFeeSize(int inputNum);

    long convertMainAssetByFee(AssetName feeAssetName, BigDecimal fee);

    Map<String, String> getMinimumFeeOfWithdrawal(String nerveTxHash) throws NulsException;

    void recordFeePaymentByNerveInner(String nerveTxHash);

    List<String> getNerveHashOfLockedUTXOList(List<UTXOData> utxoDataList) throws Exception;

    void saveSplitGranularity(long splitGranularity) throws Exception;
    Long getSplitGranularity();
}
