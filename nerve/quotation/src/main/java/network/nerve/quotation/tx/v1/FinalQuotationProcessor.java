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

package network.nerve.quotation.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.constant.QuotationErrorCode;
import network.nerve.quotation.manager.ChainManager;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.bo.QuotationActuator;
import network.nerve.quotation.model.bo.QuotationContractCfg;
import network.nerve.quotation.model.po.ConfirmFinalQuotationPO;
import network.nerve.quotation.model.txdata.Price;
import network.nerve.quotation.model.txdata.Prices;
import network.nerve.quotation.processor.impl.CalculatorProcessor;
import network.nerve.quotation.storage.ConfirmFinalQuotationStorageService;
import network.nerve.quotation.util.CommonUtil;
import network.nerve.quotation.util.LoggerUtil;
import network.nerve.quotation.util.TimeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.quotation.constant.QuotationConstant.*;
import static network.nerve.quotation.constant.QuotationContext.*;

/**
 * @author: Loki
 * @date: 2020-02-19
 */
@Component("FinalQuotationProcessorV1")
public class FinalQuotationProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private ConfirmFinalQuotationStorageService cfrFinalQuotationStorageService;
    @Autowired
    private CalculatorProcessor calculatorProcessor;

    @Override
    public int getType() {
        return TxType.FINAL_QUOTATION;
    }

    /**
     * Verification of final pricing transaction business
     * 0.The current block can only have one transaction of this transaction type
     * 0.5.CorrespondingkeyDoes it exist in the configuration
     * <p>
     * 1.Verify the correspondingkeyThere are no confirmed feed rate transactions on that day
     * Confirm that there is no presence in the business data key+Block date）
     * <p>
     * 2.Corresponding to the transaction to be verifiedkeyThe business data is the same as the final quotation transaction business data recalculated locally
     * （key+Block date+price equal）
     * <p>
     * 3.validatecoindataEmptynull/length==0;
     * <p>
     * 4.Verify that the signature is emptynull/length==0;
     *
     * @param chainId     chainId
     * @param txs         Type is{@link #getType()}All transaction sets for
     * @param txMap       Different transaction types and their corresponding transaction list key value pairs
     * @param blockHeader Block head
     * @return
     */
    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        //Business data verification
        Chain chain = null;
        Map<String, Object> result = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger log = chain.getLogger();
            String errorCode = null;
            result = new HashMap<>(QuotationConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();
            //There cannot be multiple final feed transactions within a block
            if (txs.size() > 1) {
                failsList.addAll(txs);
                log.error(QuotationErrorCode.TXDATA_EMPTY.getMsg());
                result.put("txList", failsList);
                result.put("errorCode", QuotationErrorCode.TXDATA_EMPTY.getCode());
                return result;
            }
            Transaction tx = txs.get(0);
            //Verify business data
            Prices prices = CommonUtil.getInstance(tx.getTxData(), Prices.class);
            List<Price> pricesList = prices.getPrices();
            if (null == pricesList || pricesList.isEmpty()) {
                failsList.add(tx);
                errorCode = QuotationErrorCode.TXDATA_EMPTY.getCode();
                log.error(QuotationErrorCode.TXDATA_EMPTY.getMsg());
            }

            for (Price price : pricesList) {
                if (StringUtils.isBlank(price.getKey()) || price.getValue() <= 0) {
                    failsList.add(tx);
                    errorCode = QuotationErrorCode.TXDATA_ERROR.getCode();
                    log.error(QuotationErrorCode.TXDATA_ERROR.getMsg());
                }
                if (tx.getCoinData() != null && tx.getCoinData().length != 0) {
                    failsList.add(tx);
                    errorCode = QuotationErrorCode.QUOTATION_COINDATA_NOT_EMPTY.getCode();
                    log.error(QuotationErrorCode.QUOTATION_COINDATA_NOT_EMPTY.getMsg());
                }
                if (tx.getTransactionSignature() != null && tx.getTransactionSignature().length != 0) {
                    // The final feed price transaction is a system transaction, Not forwarding through the network, Can only be packaged into blocks by packaging nodes, Without signature data
                    failsList.add(tx);
                    errorCode = QuotationErrorCode.FINAL_QUOTATION_SIGN_NOT_EMPTY.getCode();
                    log.error(QuotationErrorCode.FINAL_QUOTATION_SIGN_NOT_EMPTY.getMsg());
                }

                String key = price.getKey();
                long height = null == blockHeader ? chain.getLatestBasicBlock().getHeight() : blockHeader.getHeight();
                if (!validCfg(chain, key, height)) {
                    failsList.add(tx);
                    errorCode = QuotationErrorCode.QUOTATION_KEY_NOT_EXIST.getCode();
                    log.error(QuotationErrorCode.QUOTATION_KEY_NOT_EXIST.getMsg());
                }

                //The transaction may not have entered the block when calling the validator
                String date = null == blockHeader ? TimeUtil.nowUTCDate() : TimeUtil.toUTCDate(blockHeader.getTime());
                String dbKey = CommonUtil.assembleKey(date, key);
                //This datekeyHave you confirmed
                if (!validNoConfirmed(chain, dbKey)) {
                    failsList.add(tx);
                    errorCode = QuotationErrorCode.FINAL_QUOTATION_CONFIRMED.getCode();
                    log.error(QuotationErrorCode.FINAL_QUOTATION_CONFIRMED.getMsg());
                }

                //Is the corresponding quotation consistent with the statistics of each node's quotation
                /**
                 * 3. By block date andtxdataMiddlekeyObtain the confirmed quotes from each node and calculate the final price once
                 * If it is related to the transactiontxdataIf the prices in are consistent, the verification is successful
                 */
                Double calcPrice = calculatorProcessor.calcFinal(chain, key, date);
                if (price.getValue() != calcPrice.doubleValue()) {
                    failsList.add(tx);
                    errorCode = QuotationErrorCode.FINAL_QUOTATION_CALC_NOT_SAME.getCode();
                    log.error(QuotationErrorCode.FINAL_QUOTATION_CALC_NOT_SAME.getMsg());
                }
            }
            result.put("txList", failsList);
            result.put("errorCode", errorCode);
        } catch (Exception e) {
            errorLogProcess(chain, e);
            result.put("txList", txs);
            result.put("errorCode", QuotationErrorCode.SYS_UNKOWN_EXCEPTION.getCode());
        }
        return result;
    }

    /**
     * 0.Verify the correspondingkeyExists in the configuration
     */
    public boolean validCfg(Chain chain, String key, long height) {
        List<QuotationActuator> quteList = chain.getQuote();
        boolean cfgKey = false;
        for (QuotationActuator qa : quteList) {
            String anchorToken = qa.getAnchorToken();
            if (key.equals(anchorToken)) {
                cfgKey = true;
            }
        }
        if (cfgKey) {
            // Protocol upgrade special handling
            if (key.equals(ANCHOR_TOKEN_USDT)
                    || key.equals(ANCHOR_TOKEN_DAI)
                    || key.equals(ANCHOR_TOKEN_USDC)
                    || key.equals(ANCHOR_TOKEN_PAX)) {
                if (height < usdtDaiUsdcPaxKeyHeight) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }
            if (key.equals(ANCHOR_TOKEN_BNB)) {
                if (height < bnbKeyHeight) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }
            if(key.equals(ANCHOR_TOKEN_HT)
                || key.equals(ANCHOR_TOKEN_OKB)) {
                if (height < htOkbKeyHeight) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }

            if(key.equals(ANCHOR_TOKEN_ONE)
                    || key.equals(ANCHOR_TOKEN_MATIC)
                    || key.equals(ANCHOR_TOKEN_KCS)) {
                if (height < oneMaticKcsHeight) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }

            if (key.equals(ANCHOR_TOKEN_TRX)) {
                if (height < trxKeyHeight) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }

            if(key.equals(ANCHOR_TOKEN_CRO)
                    || key.equals(ANCHOR_TOKEN_AVAX)
                    || key.equals(ANCHOR_TOKEN_FTM)) {
                if (height < protocol16Height) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }

            if(key.equals(ANCHOR_TOKEN_METIS)
                    || key.equals(ANCHOR_TOKEN_IOTX)
                    || key.equals(ANCHOR_TOKEN_KLAY)
                    || key.equals(ANCHOR_TOKEN_BCH)) {
                if (height < protocol21Height) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }

            if(key.equals(ANCHOR_TOKEN_KAVA)
                    || key.equals(ANCHOR_TOKEN_ETHW)) {
                if (height < protocol22Height) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }

            if(key.equals(ANCHOR_TOKEN_REI)) {
                if (height < protocol24Height) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }

            if(key.equals(ANCHOR_TOKEN_EOS)) {
                if (height < protocol26Height) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }

            if(key.equals(ANCHOR_TOKEN_CELO)
                    || key.equals(ANCHOR_TOKEN_ETC)) {
                if (height < protocol27Height) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }

            if(key.equals(ANCHOR_TOKEN_BRISE)) {
                if (height < protocol29Height) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }

            if(key.equals(ANCHOR_TOKEN_JNS)) {
                if (height < protocol30Height) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }
            if(key.equals(ANCHOR_TOKEN_DOGE)
                    || key.equals(ANCHOR_TOKEN_ZETA)) {
                if (height < protocol31Height) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }
            if(key.equals(ANCHOR_TOKEN_FCH)
                    || key.equals(ANCHOR_TOKEN_PLS)) {
                if (height < protocol34Height) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }
            if(key.equals(ANCHOR_TOKEN_TBC)) {
                if (height < protocol40Height) {
                    chain.getLogger().error("Not reaching the protocol upgrade height, This transaction is not supported for quotation. {} , {}", key, height);
                    return false;
                }
            }

            return true;
        }
        for(QuotationContractCfg quContractCfg : chain.getContractQuote()){
            String anchorToken = quContractCfg.getAnchorToken();
            if (key.equals(anchorToken) && height >= quContractCfg.getEffectiveHeight()) {
                cfgKey = true;
            }
        }
        return cfgKey;
    }

    /**
     * 1.Verify the correspondingkeyThere are no confirmed feed rate transactions on that day(Check confirmed data)
     * Confirm that there is no presence in the business data key+Block date
     */
    public boolean validNoConfirmed(Chain chain, String dbKey) {
        ConfirmFinalQuotationPO cfrFinalQuotationPO = cfrFinalQuotationStorageService.getCfrFinalQuotation(chain, dbKey);
        if (null == cfrFinalQuotationPO) {
            return true;
        }
        return false;
    }


    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return commit(chainId, txs, blockHeader, syncStatus, true);
    }

    /**
     * Submit business data
     *
     * @param chainId
     * @param txs
     * @param blockHeader
     * @param failRollback Does the exception trigger a rollback
     * @return
     */
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            Transaction tx = txs.get(0);
            String date = TimeUtil.toUTCDate(blockHeader.getTime());
            //Verify business data
            Prices prices = CommonUtil.getInstance(tx.getTxData(), Prices.class);
            List<Price> pricesList = prices.getPrices();
            for (Price price : pricesList) {
                String anchorToken = price.getKey();
                ConfirmFinalQuotationPO cfrFinalQuotation = new ConfirmFinalQuotationPO();
                cfrFinalQuotation.setAnchorToken(anchorToken);
                cfrFinalQuotation.setPrice(price.getValue());
                cfrFinalQuotation.setDate(date);
                cfrFinalQuotation.setTxHash(tx.getHash().toHex());
                //yyyyMMdd-anchorToken
                String dbKey = CommonUtil.assembleKey(date, anchorToken);
                //Save the latest quotation（keyIncluding date）
                cfrFinalQuotationStorageService.saveCfrFinalQuotation(chain, dbKey, cfrFinalQuotation);
                //Cover the latest quotation（keyExcluding dates）
                cfrFinalQuotationStorageService.saveCfrFinalLastQuotation(chain, anchorToken, cfrFinalQuotation);
                //Add Today's Cache
                QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS.add(anchorToken);

                chain.getLogger().info("[commit] Confirm final quotation transaction Final-quotation anchorToken:{}, dbKey:{}, price:{}, hash:{}",
                        anchorToken, dbKey, price.getValue(), tx.getHash().toHex());
            }
            return true;
        } catch (Exception e) {
            errorLogProcess(chain, e);
            if (failRollback) {
                rollback(chainId, txs, blockHeader, false);
            }
            return false;
        }
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return rollback(chainId, txs, blockHeader, true);
    }

    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            Transaction tx = txs.get(0);
            String date = TimeUtil.toUTCDate(blockHeader.getTime());
            //The day before
            String theDayBefore = TimeUtil.theDayBeforeUTCDate(blockHeader.getTime());
            //Verify business data
            Prices prices = CommonUtil.getInstance(tx.getTxData(), Prices.class);
            List<Price> pricesList = prices.getPrices();
            for (Price price : pricesList) {
                String anchorToken = price.getKey();
                //yyyyMMdd-anchorToken
                String dbKey = CommonUtil.assembleKey(date, anchorToken);
                cfrFinalQuotationStorageService.deleteCfrFinalQuotationByKey(chain, dbKey);

                //Delete the current most recent quotation
                cfrFinalQuotationStorageService.deleteCfrFinalLastTimeQuotationByKey(chain, anchorToken);

                //Overlay the final quotation from the previous day with the latest quotation
                String theDayBeforeDbKey = CommonUtil.assembleKey(theDayBefore, anchorToken);
                ConfirmFinalQuotationPO cfrFinalQuotationPO = cfrFinalQuotationStorageService.getCfrFinalQuotation(chain, theDayBeforeDbKey);
                if (null == cfrFinalQuotationPO) {
                    return true;
                }
                cfrFinalQuotationStorageService.saveCfrFinalLastQuotation(chain, anchorToken, cfrFinalQuotationPO);
                //Clear from today's cache
                QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS.remove(anchorToken);
            }
            return true;
        } catch (Exception e) {
            errorLogProcess(chain, e);
            if (failCommit) {
                commit(chainId, txs, blockHeader, 0, false);
            }
            return false;
        }
    }

    private void errorLogProcess(Chain chain, Exception e) {
        if (chain == null) {
            LoggerUtil.LOG.error(e);
        } else {
            chain.getLogger().error(e);
        }
    }
}
