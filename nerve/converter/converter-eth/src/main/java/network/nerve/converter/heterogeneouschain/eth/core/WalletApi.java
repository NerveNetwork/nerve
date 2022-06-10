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

package network.nerve.converter.heterogeneouschain.eth.core;

import org.web3j.protocol.core.methods.response.EthSendTransaction;

import java.math.BigDecimal;
import java.util.Map;

/**
 * FileName: WalletApi
 * Description:
 * Author: ZhangYX
 * Date:  2018/4/13
 */
public interface WalletApi {

    void initialize();

    long getBlockHeight() throws Exception;

    Block getBlock(String hash) throws Exception;

    Block getBlock(long height) throws Exception;

    BigDecimal getBalance(String address) throws Exception;

    boolean canTransferBatch();

    void confirmUnspent(Block block);

    EthSendTransaction sendTransaction(String fromAddress, String secretKey, Map<String, BigDecimal> transferRequests);

    String sendTransaction(String toAddress, String fromAddress, String secretKey, BigDecimal amount);

    EthSendTransaction sendTransaction(String toAddress, String fromAddress, String secretKey, BigDecimal amount, String contractAddress) throws Exception;

    String convertToNewAddress(String address);

}
