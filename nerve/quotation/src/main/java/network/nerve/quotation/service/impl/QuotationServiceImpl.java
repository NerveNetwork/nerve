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

package network.nerve.quotation.service.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationErrorCode;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.dto.QuoteDTO;
import network.nerve.quotation.model.txdata.Price;
import network.nerve.quotation.model.txdata.Prices;
import network.nerve.quotation.model.txdata.Quotation;
import network.nerve.quotation.rpc.call.QuotationCall;
import network.nerve.quotation.service.QuotationService;
import network.nerve.quotation.util.TimeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Loki
 * @date: 2019/11/25
 */
@Component
public class QuotationServiceImpl implements QuotationService {


    @Override
    public Transaction quote(Chain chain, QuoteDTO quoteDTO) throws NulsException {
        byte[] txData = assemblyQuotationTxData(quoteDTO.getAddress(), quoteDTO.getPricesMap());
        Transaction tx = createQuotationTransaction(txData, quoteDTO.getAddress(), quoteDTO.getPassword());
        QuotationCall.newTx(chain, tx);
        return tx;
    }

    public Prices getPrices(Map<String, Double> pricesMap) throws NulsException {
        if (null == pricesMap || pricesMap.isEmpty()) {
            throw new NulsException(QuotationErrorCode.TXDATA_EMPTY);
        }
        List<Price> list = new ArrayList<>();
        pricesMap.forEach((key, value) -> {
            Price price = new Price(key, value);
            list.add(price);
        });
        return new Prices(list);
    }

    @Override
    public byte[] assemblyQuotationTxData(String address, Map<String, Double> pricesMap) throws NulsException {
        Prices prices = getPrices(pricesMap);
        try {
            Quotation quotation = new Quotation(AddressTool.getAddress(address), QuotationConstant.QUOTE_TXDATA_TYPE, prices.serialize());
            return quotation.serialize();
        } catch (IOException e) {
            e.printStackTrace();
            throw new NulsException(QuotationErrorCode.SERIALIZE_ERROR);
        }
    }

    @Override
    public Transaction createQuotationTransaction(byte[] txData, String address, String password) throws NulsException {
        if (null == txData || txData.length == 0) {
            throw new NulsException(QuotationErrorCode.TXDATA_EMPTY);
        }
        Transaction tx = new Transaction(TxType.QUOTATION);
        tx.setTxData(txData);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        //签名
        sign(tx, address, password);
        return tx;
    }

    @Override
    public Transaction createFinalQuotationTransaction(Map<String, Double> pricesMap) throws NulsException {
        Prices prices = getPrices(pricesMap);
        return createFinalQuotationTransaction(prices);
    }

    @Override
    public Transaction createFinalQuotationTransaction(Prices prices) throws NulsException {
        if (null == prices) {
            throw new NulsException(QuotationErrorCode.TXDATA_EMPTY);
        }
        Transaction tx = new Transaction(TxType.FINAL_QUOTATION);
        try {
            tx.setTxData(prices.serialize());
        } catch (IOException e) {
            throw new NulsException(QuotationErrorCode.SERIALIZE_ERROR);
        }
        long zeroTimeMillis = TimeUtil.getUTCZeroTimeMillisOfTheDay(NulsDateUtils.getCurrentTimeMillis());
        tx.setTime(zeroTimeMillis / 1000);
        return tx;
    }

    private void sign(Transaction tx, String address, String password) throws NulsException {
        if (StringUtils.isBlank(address) || StringUtils.isBlank(password)) {
            //非共识节点
            throw new NulsException(QuotationErrorCode.NULL_PARAMETER);
        }
        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
        TransactionSignature transactionSignature = new TransactionSignature();
        Map<String, Object> extendMap = new HashMap<>();
        extendMap.put("method", "quotationSign");
        try {
            extendMap.put("txHex", HexUtil.encode(tx.serialize()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        P2PHKSignature p2PHKSignature = QuotationCall.signDigest(address, password, tx.getHash().getBytes(),extendMap);
        p2PHKSignatures.add(p2PHKSignature);
        transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        try {
            tx.setTransactionSignature(transactionSignature.serialize());
        } catch (IOException e) {
            e.printStackTrace();
            throw new NulsException(QuotationErrorCode.SERIALIZE_ERROR);
        }
    }
}
