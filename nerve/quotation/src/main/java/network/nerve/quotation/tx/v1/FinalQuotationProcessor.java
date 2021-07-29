/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
     * 最终喂价交易业务验证
     * 0.当前块只能有一个该交易类型交易
     * 0.5.对应的key是否存在于配置的中
     * <p>
     * 1.验证对应的key当天没有已确认的喂价交易
     * 确认业务数据中不存在 key+区块日期）
     * <p>
     * 2.待验证交易中对应key的业务数据与本地重新计算的最终报价交易业务数据相同
     * （key+区块日期+价格 相等）
     * <p>
     * 3.验证coindata为空null/length==0;
     * <p>
     * 4.验证签名为空null/length==0;
     *
     * @param chainId     链Id
     * @param txs         类型为{@link #getType()}的所有交易集合
     * @param txMap       不同交易类型与其对应交易列表键值对
     * @param blockHeader 区块头
     * @return
     */
    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        //业务数据校验
        Chain chain = null;
        Map<String, Object> result = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger log = chain.getLogger();
            String errorCode = null;
            result = new HashMap<>(QuotationConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();
            //块里面不能有多个最终喂价交易
            if (txs.size() > 1) {
                failsList.addAll(txs);
                log.error(QuotationErrorCode.TXDATA_EMPTY.getMsg());
                result.put("txList", failsList);
                result.put("errorCode", QuotationErrorCode.TXDATA_EMPTY.getCode());
                return result;
            }
            Transaction tx = txs.get(0);
            //验证业务数据
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
                    // 最终喂价交易为系统交易, 不通过网络转发, 只能由打包节点打包到区块中, 不含签名数据
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

                //调验证器时交易可能没有打入区块
                String date = null == blockHeader ? TimeUtil.nowUTCDate() : TimeUtil.toUTCDate(blockHeader.getTime());
                String dbKey = CommonUtil.assembleKey(date, key);
                //该日期key是否确认过
                if (!validNoConfirmed(chain, dbKey)) {
                    failsList.add(tx);
                    errorCode = QuotationErrorCode.FINAL_QUOTATION_CONFIRMED.getCode();
                    log.error(QuotationErrorCode.FINAL_QUOTATION_CONFIRMED.getMsg());
                }

                //对应报价与各节点报价是统计出来的结果是否一致
                /**
                 * 3. 通过区块日期和txdata中的key，获取各节点已确认报价统计一次最终价格
                 * 如果与交易中txdata中的价格一致，则验证通过
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
     * 0.验证对应的key存在于配置的中
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
            // 协议升级特殊处理
            if (key.equals(ANCHOR_TOKEN_USDT)
                    || key.equals(ANCHOR_TOKEN_DAI)
                    || key.equals(ANCHOR_TOKEN_USDC)
                    || key.equals(ANCHOR_TOKEN_PAX)) {
                if (height < usdtDaiUsdcPaxKeyHeight) {
                    chain.getLogger().error("没达到协议升级高度, 不支持该交易对报价. {} , {}", key, height);
                    return false;
                }
            }
            if (key.equals(ANCHOR_TOKEN_BNB)) {
                if (height < bnbKeyHeight) {
                    chain.getLogger().error("没达到协议升级高度, 不支持该交易对报价. {} , {}", key, height);
                    return false;
                }
            }
            if(key.equals(ANCHOR_TOKEN_HT)
                || key.equals(ANCHOR_TOKEN_OKB)) {
                if (height < htOkbKeyHeight) {
                    chain.getLogger().error("没达到协议升级高度, 不支持该交易对报价. {} , {}", key, height);
                    return false;
                }
            }

            if(key.equals(ANCHOR_TOKEN_ONE)
                    || key.equals(ANCHOR_TOKEN_MATIC)
                    || key.equals(ANCHOR_TOKEN_KCS)) {
                if (height < oneMaticKcsHeight) {
                    chain.getLogger().error("没达到协议升级高度, 不支持该交易对报价. {} , {}", key, height);
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
     * 1.验证对应的key当天没有已确认的喂价交易(查已确认数据)
     * 确认业务数据中不存在 key+区块日期
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
     * 提交业务数据
     *
     * @param chainId
     * @param txs
     * @param blockHeader
     * @param failRollback 异常是否触发回滚
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
            //验证业务数据
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
                //保存最新报价（key含日期）
                cfrFinalQuotationStorageService.saveCfrFinalQuotation(chain, dbKey, cfrFinalQuotation);
                //覆盖最新报价（key不含日期）
                cfrFinalQuotationStorageService.saveCfrFinalLastQuotation(chain, anchorToken, cfrFinalQuotation);
                //加入当天缓存
                QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS.add(anchorToken);

                chain.getLogger().info("[commit] 确认最终报价交易 Final-quotation anchorToken:{}, dbKey:{}, price:{}, hash:{}",
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
            //前一天
            String theDayBefore = TimeUtil.theDayBeforeUTCDate(blockHeader.getTime());
            //验证业务数据
            Prices prices = CommonUtil.getInstance(tx.getTxData(), Prices.class);
            List<Price> pricesList = prices.getPrices();
            for (Price price : pricesList) {
                String anchorToken = price.getKey();
                //yyyyMMdd-anchorToken
                String dbKey = CommonUtil.assembleKey(date, anchorToken);
                cfrFinalQuotationStorageService.deleteCfrFinalQuotationByKey(chain, dbKey);

                //删除当前最近一次报价
                cfrFinalQuotationStorageService.deleteCfrFinalLastTimeQuotationByKey(chain, anchorToken);

                //将前一天的最终报价覆盖到最近一次报价中
                String theDayBeforeDbKey = CommonUtil.assembleKey(theDayBefore, anchorToken);
                ConfirmFinalQuotationPO cfrFinalQuotationPO = cfrFinalQuotationStorageService.getCfrFinalQuotation(chain, theDayBeforeDbKey);
                if (null == cfrFinalQuotationPO) {
                    return true;
                }
                cfrFinalQuotationStorageService.saveCfrFinalLastQuotation(chain, anchorToken, cfrFinalQuotationPO);
                //从当天缓存中清除
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
