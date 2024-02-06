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

package network.nerve.quotation.task;

import io.nuls.base.data.Transaction;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.constant.QuotationErrorCode;
import network.nerve.quotation.heterogeneouschain.BNBWalletApi;
import network.nerve.quotation.heterogeneouschain.ETHWalletApi;
import network.nerve.quotation.heterogeneouschain.HECOWalletApi;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.bo.QuotationActuator;
import network.nerve.quotation.model.bo.QuotationContractCfg;
import network.nerve.quotation.model.dto.QuoteDTO;
import network.nerve.quotation.model.txdata.Quotation;
import network.nerve.quotation.processor.Collector;
import network.nerve.quotation.rpc.call.QuotationCall;
import network.nerve.quotation.service.QuotationService;
import network.nerve.quotation.storage.QuotationIntradayStorageService;
import network.nerve.quotation.util.CommonUtil;
import network.nerve.quotation.util.NerveSwapUtil;
import network.nerve.quotation.util.TimeUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.quotation.constant.QuotationConstant.*;
import static network.nerve.quotation.constant.QuotationContext.*;
import static network.nerve.quotation.heterogeneouschain.constant.BnbConstant.BSC_CHAIN;
import static network.nerve.quotation.heterogeneouschain.constant.EthConstant.ETH_CHAIN;
import static network.nerve.quotation.heterogeneouschain.constant.HtConstant.HECO_CHAIN;

/**
 * Obtain third-party prices Trading
 * Price Collect Process
 *
 * @author: Loki
 * @date: 2019/12/4
 */
public class CollectorTask implements Runnable {

    private Chain chain;

    private QuotationService quotationService = SpringLiteContext.getBean(QuotationService.class);
    private QuotationIntradayStorageService quotationIntradayStorageService = SpringLiteContext.getBean(QuotationIntradayStorageService.class);
    private ETHWalletApi ethWalletApi = SpringLiteContext.getBean(ETHWalletApi.class);
    private BNBWalletApi bnbWalletApi = SpringLiteContext.getBean(BNBWalletApi.class);
    private HECOWalletApi hecoWalletApi = SpringLiteContext.getBean(HECOWalletApi.class);

    public CollectorTask(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void run() {
        try {
//Used for testing            for (QuotationContractCfg quContractCfg : chain.getContractQuote()) {
//                if ("5-146-18".equals(quContractCfg.getTokenInfo())) {
//                    Double price = NerveSwapUtil.getPrice(chain, quContractCfg);
//                    chain.getLogger().info("==========price: {}", price);
//                }
//            }


            if (!TimeUtil.isNowDateTimeInRange(QuotationContext.quoteStartH, QuotationContext.quoteStartM, QuotationContext.quoteEndH, QuotationContext.quoteEndM)) {
                return;
            }
            if (!CommonUtil.isCurrentConsensusNode(QuotationCall.getPackerInfo(chain))) {
                return;
            }
            QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS.clear();
            List<QuotationActuator> quteList = chain.getQuote();
            Map<String, Double> pricesMap = new HashMap<>();
            long blockHeight = chain.getLatestBasicBlock().getHeight();
            for (QuotationActuator qa : quteList) {
                String anchorToken = qa.getAnchorToken();
                if (QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED.contains(anchorToken)) {
                    //This already exists on the same daykeyConfirmed quotation transaction for
                    continue;
                }
                if (QuotationContext.NODE_QUOTED_TX_TOKENS_TEMP.containsKey(anchorToken)) {
                    //If the unconfirmed quote transaction already exists in the cachekey, Then it is necessary to obtain whether the transaction has been confirmed from the transaction module
                    String txHash = QuotationContext.NODE_QUOTED_TX_TOKENS_TEMP.get(anchorToken);
                    boolean confirmed = QuotationCall.isConfirmed(chain, txHash);
                    chain.getLogger().info("[CollectorTask] Obtain whether the transaction has been confirmed：{}, hash:{}", confirmed, txHash);
                    if (confirmed) {
                        chain.getLogger().info("[CollectorTask] {}Quoted", anchorToken);
                        //Add Quoted Confirmation
                        QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED.add(anchorToken);
                        continue;
                    } else {
                        quotationIntradayStorageService.delete(chain, anchorToken);
                    }
                }
                BigDecimal price = null;

                if (ANCHOR_TOKEN_USDT.equals(anchorToken)) {
                    if (blockHeight >= usdtDaiUsdcPaxKeyHeight) {
                        price = BigDecimal.ONE;
                    } else {
                        continue;
                    }
                }/* else if (ANCHOR_TOKEN_OKT.equals(anchorToken)) {
                 *//**
                 * 2021-03-29 New additionOKTQuotation, usingOKBQuotation results.
                 *//*
                    if (blockHeight < oktKeyHeight) {
                        continue;
                    }
                    Double priceDouble = pricesMap.get(ANCHOR_TOKEN_OKB);
                    if (null != priceDouble) {
                        // Direct useOKBQuotation for
                        pricesMap.put(anchorToken, priceDouble);
                    } else {
                        // If notOKBQuote, then directly obtain the calculationOKBoffer
                        Collector collector = getCollector(qa.getCollector());
                        price = collector.enquiry(chain, ANCHOR_TOKEN_OKB);
                    }
                }*/
                else {
                    if (ANCHOR_TOKEN_DAI.equals(anchorToken)
                            || ANCHOR_TOKEN_USDC.equals(anchorToken)
                            || ANCHOR_TOKEN_PAX.equals(anchorToken)) {
                        if (blockHeight < usdtDaiUsdcPaxKeyHeight) {
                            continue;
                        }
                    }
                    if (ANCHOR_TOKEN_BNB.equals(anchorToken)) {
                        if (blockHeight < bnbKeyHeight) {
                            continue;
                        }
                    }
                    if (ANCHOR_TOKEN_HT.equals(anchorToken)
                            || ANCHOR_TOKEN_OKB.equals(anchorToken)) {
                        if (blockHeight < htOkbKeyHeight) {
                            continue;
                        }
                    }

                    if (ANCHOR_TOKEN_OKT.equals(anchorToken)) {
                        if (blockHeight < oktKeyHeight) {
                            continue;
                        }
                    }

                    if (ANCHOR_TOKEN_ONE.equals(anchorToken)
                            || ANCHOR_TOKEN_MATIC.equals(anchorToken)
                            || ANCHOR_TOKEN_KCS.equals(anchorToken)) {
                        if (blockHeight < oneMaticKcsHeight) {
                            continue;
                        }
                    }

                    if (ANCHOR_TOKEN_TRX.equals(anchorToken)) {
                        if (blockHeight < trxKeyHeight) {
                            continue;
                        }
                    }

                    if (ANCHOR_TOKEN_CRO.equals(anchorToken)
                            || ANCHOR_TOKEN_AVAX.equals(anchorToken)
                            || ANCHOR_TOKEN_FTM.equals(anchorToken)) {
                        if (blockHeight < protocol16Height) {
                            continue;
                        }
                    }

                    if (ANCHOR_TOKEN_METIS.equals(anchorToken)
                            || ANCHOR_TOKEN_IOTX.equals(anchorToken)
                            || ANCHOR_TOKEN_KLAY.equals(anchorToken)
                            || ANCHOR_TOKEN_BCH.equals(anchorToken)) {
                        if (blockHeight < protocol21Height) {
                            continue;
                        }
                    }

                    if (ANCHOR_TOKEN_KAVA.equals(anchorToken)
                            || ANCHOR_TOKEN_ETHW.equals(anchorToken)) {
                        if (blockHeight < protocol22Height) {
                            continue;
                        }
                    }

                    if (ANCHOR_TOKEN_REI.equals(anchorToken)) {
                        if (blockHeight < protocol24Height) {
                            continue;
                        }
                    }

                    if (ANCHOR_TOKEN_EOS.equals(anchorToken)) {
                        if (blockHeight < protocol26Height) {
                            continue;
                        }
                    }

                    if (ANCHOR_TOKEN_CELO.equals(anchorToken)
                            || ANCHOR_TOKEN_ETC.equals(anchorToken)) {
                        if (blockHeight < protocol27Height) {
                            continue;
                        }
                    }

                    if (ANCHOR_TOKEN_BRISE.equals(anchorToken)) {
                        if (blockHeight < protocol29Height) {
                            continue;
                        }
                    }

                    if (ANCHOR_TOKEN_JNS.equals(anchorToken)) {
                        if (blockHeight < protocol30Height) {
                            continue;
                        }
                    }
                    if (ANCHOR_TOKEN_DOGE.equals(anchorToken)
                            || ANCHOR_TOKEN_ZETA.equals(anchorToken)) {
                        if (blockHeight < protocol31Height) {
                            continue;
                        }
                    }

                    Collector collector = getCollector(qa.getCollector());
                    price = collector.enquiry(chain, anchorToken);
                }
                if (null == price || price.doubleValue() == 0) {
                    chain.getLogger().error("[CollectorTask] [{}]No average third-party quotation obtained", anchorToken);
                    continue;
                }
                chain.getLogger().info("[CollectorTask] [{}]The average third-party quotation is：{} \n\n", anchorToken, price.doubleValue());
                pricesMap.put(anchorToken, price.doubleValue());
            }

            // swapContract quotation
            for (QuotationContractCfg quContractCfg : chain.getContractQuote()) {
                if (blockHeight < quContractCfg.getEffectiveHeight()) {
                    continue;
                }
                String anchorToken = quContractCfg.getAnchorToken();
                if (QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED.contains(anchorToken)) {
                    //This already exists on the same daykeyConfirmed quotation transaction for
                    continue;
                }
                if (QuotationContext.NODE_QUOTED_TX_TOKENS_TEMP.containsKey(anchorToken)) {
                    //If the unconfirmed quote transaction already exists in the cachekey, Then it is necessary to obtain whether the transaction has been confirmed from the transaction module
                    String txHash = QuotationContext.NODE_QUOTED_TX_TOKENS_TEMP.get(anchorToken);
                    boolean confirmed = QuotationCall.isConfirmed(chain, txHash);
                    chain.getLogger().info("[CollectorTask] Obtain whether the transaction has been confirmed：{}, hash:{}", confirmed, txHash);
                    if (confirmed) {
                        chain.getLogger().info("[CollectorTask] {}Quoted", anchorToken);
                        //Add Quoted Confirmation
                        QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED.add(anchorToken);
                        continue;
                    } else {
                        quotationIntradayStorageService.delete(chain, anchorToken);
                    }
                }
                String baseAnchorToken = quContractCfg.getBaseAnchorToken();
                Double unitPrice = pricesMap.get(baseAnchorToken);
                if (null == unitPrice && !quContractCfg.getChain().equals("NERVE")) {
                    chain.getLogger().error("[CollectorTask] {}Cannot proceed with contractSWAPoffer, [{}]No third-party quotation obtained", quContractCfg.getKey(), baseAnchorToken);
                    continue;
                }
                Double resultPrice = null;
                switch (quContractCfg.getChain()) {
                    case ETH_CHAIN:
                        resultPrice = ethContractQuote(quContractCfg.getSwapTokenContractAddress(),
                                quContractCfg.getBaseTokenContractAddress(),
                                unitPrice.toString());
                        break;
                    case BSC_CHAIN:
                        resultPrice = bscContractQuote(quContractCfg.getSwapTokenContractAddress(),
                                quContractCfg.getBaseTokenContractAddress(),
                                unitPrice.toString());
                        break;
                    case HECO_CHAIN:
                        resultPrice = hecoContractQuote(quContractCfg.getSwapTokenContractAddress(),
                                quContractCfg.getBaseTokenContractAddress(),
                                unitPrice.toString());
                        break;
                    case "NERVE":
                        resultPrice = nerveSwapQuote(quContractCfg);
                        break;
                    default:
                }
                if (null != resultPrice) {
                    chain.getLogger().info("[CollectorTask] contractSWAPoffer, [{}]：{} \n\n", anchorToken, resultPrice);
                    pricesMap.put(anchorToken, resultPrice);
                }
            }

            Transaction tx = null;
            if (pricesMap.isEmpty() || null == (tx = createAndSendTx(chain, pricesMap))) {
                return;
            }
            chain.getLogger().info("[Quotation] Node executes quotation transactions txHash: {}", tx.getHash().toHex());
            chain.getLogger().info("{}", tx.format(Quotation.class));
            //Record successful quotation transactions senttoken (If it is recorded during the transaction confirmation of one's own node,Frequent occurrence of duplicate quotations)
            String txHash = tx.getHash().toHex();
            pricesMap.forEach((key, value) -> {
                QuotationContext.NODE_QUOTED_TX_TOKENS_TEMP.put(key, txHash);
                quotationIntradayStorageService.save(chain, key);
            });
        } catch (Exception e) {
            chain.getLogger().error("CollectorTask error", e);
        } catch (Throwable e) {
            chain.getLogger().error("CollectorTask error", e);
        }
    }

    /**
     * Assembly transaction, And broadcast it to the network
     *
     * @param chain
     * @param pricesMap
     * @return
     * @throws NulsException
     */
    private Transaction createAndSendTx(Chain chain, Map<String, Double> pricesMap) throws NulsException {
        QuoteDTO quoteDTO = new QuoteDTO();
        quoteDTO.setChainId(chain.getChainId());
        quoteDTO.setPricesMap(pricesMap);
        Map<String, String> map = QuotationCall.getPackerInfo(chain);
        String address = map.get("address");
        if (StringUtils.isBlank(address)) {
            throw new NulsException(QuotationErrorCode.PACKER_ADDRESS_NOT_FOUND);
        }
        String password = map.get("password");
        if (StringUtils.isBlank(password)) {
            throw new NulsException(QuotationErrorCode.PACKER_PASSWORD_NOT_FOUND);
        }
        quoteDTO.setAddress(map.get("address"));
        quoteDTO.setPassword(map.get("password"));
        return quotationService.quote(chain, quoteDTO);
    }


    public Collector getCollector(String clazz) throws Exception {
        Class<?> clasz = Class.forName(clazz);
        return (Collector) SpringLiteContext.getBean(clasz);
    }


    /**
     * ETHnetwork calculate UNI-V2 tokenprice
     *
     * @param swapTokenContractAddress swap UNI-V2Contract address
     * @param baseTokenContractAddress Benchmark for calculating pricestoken(Trading against one of themtokenAs a basis for calculation)
     * @param baseTokenUnitPrice       benchmarktokenUnit price of
     * @throws Exception
     */
    public Double ethContractQuote(String swapTokenContractAddress, String baseTokenContractAddress, String baseTokenUnitPrice) {
        try {
            // Chi Zi LiETHQuantity of
            chain.getLogger().debug("ETH,  V2-LP:{}, WETH:{}", swapTokenContractAddress, baseTokenContractAddress);
            BigInteger wethBalance = ethWalletApi.getERC20Balance(swapTokenContractAddress, baseTokenContractAddress);
            chain.getLogger().debug("wethBalance:" + wethBalance);
            int wethDecimals = ethWalletApi.getContractTokenDecimals(baseTokenContractAddress);
            chain.getLogger().debug("wethDecimals:" + wethDecimals);
            BigInteger wethTwice = wethBalance.multiply(new BigInteger("2"));
            chain.getLogger().debug("wethTwice:" + wethTwice);
            BigDecimal wethCount = new BigDecimal(wethTwice).movePointLeft(wethDecimals);
            chain.getLogger().debug("wethCount:{}, baseTokenUnitPrice:{}", wethCount, baseTokenUnitPrice);
            BigDecimal wethPrice = wethCount.multiply(new BigDecimal(baseTokenUnitPrice));
            chain.getLogger().debug("wethPrice:" + wethPrice);
            chain.getLogger().debug("");
            BigInteger totalSupplyV2 = ethWalletApi.totalSupply(swapTokenContractAddress);
            chain.getLogger().debug("totalSupplyV2:" + totalSupplyV2);
            int v2Decimals = ethWalletApi.getContractTokenDecimals(swapTokenContractAddress);
            chain.getLogger().debug("V2Decimals:" + v2Decimals);
            BigDecimal v2Count = new BigDecimal(totalSupplyV2).movePointLeft(v2Decimals);
            chain.getLogger().debug("V2Count:" + v2Count);
            BigDecimal uniV2Price = wethPrice.divide(v2Count, 6, RoundingMode.DOWN);
            chain.getLogger().debug("uniV2Price:" + uniV2Price);
            return uniV2Price.doubleValue();
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }


    /**
     * BSCnetwork calculate Cake_LP tokenprice
     *
     * @param swapTokenContractAddress swap UNI-V2Contract address
     * @param baseTokenContractAddress Benchmark for calculating pricestoken(Trading against one of themtokenAs a basis for calculation)
     * @param baseTokenUnitPrice       benchmarktokenUnit price of
     * @return
     * @throws Exception
     */
    public Double bscContractQuote(String swapTokenContractAddress, String baseTokenContractAddress, String baseTokenUnitPrice) {
        try {
            // calculateETHUnder the network CAKE_LP The price of cakeLp
            chain.getLogger().debug("BSC,  Cake-LP:{}, WBNB:{}", swapTokenContractAddress, baseTokenContractAddress);
            // Chi Zi LiETHQuantity of
            BigInteger wethBalance = bnbWalletApi.getERC20Balance(swapTokenContractAddress, baseTokenContractAddress);
            chain.getLogger().debug("wethBalance:" + wethBalance);
            int wethDecimals = bnbWalletApi.getContractTokenDecimals(baseTokenContractAddress);
            chain.getLogger().debug("wethDecimals:" + wethDecimals);
            BigInteger wethTwice = wethBalance.multiply(new BigInteger("2"));
            chain.getLogger().debug("wethTwice:" + wethTwice);
            BigDecimal wethCount = new BigDecimal(wethTwice).movePointLeft(wethDecimals);
            chain.getLogger().debug("wethCount:" + wethCount);
            BigDecimal wethPrice = wethCount.multiply(new BigDecimal(baseTokenUnitPrice));
            chain.getLogger().debug("wethPrice:" + wethPrice);
            chain.getLogger().debug("");
            BigInteger totalSupplyCakeLp = bnbWalletApi.totalSupply(swapTokenContractAddress);
            chain.getLogger().debug("totalSupplyCakeLp:" + totalSupplyCakeLp);
            int cakeLpDecimals = bnbWalletApi.getContractTokenDecimals(swapTokenContractAddress);
            chain.getLogger().debug("cakeLpDecimals:" + cakeLpDecimals);
            BigDecimal cakeLpCount = new BigDecimal(totalSupplyCakeLp).movePointLeft(cakeLpDecimals);
            chain.getLogger().debug("cakeLpCount:" + cakeLpCount);
            BigDecimal cakeLpPrice = wethPrice.divide(cakeLpCount, 6, RoundingMode.DOWN);
            chain.getLogger().debug("cakeLpPrice:" + cakeLpPrice);
            return cakeLpPrice.doubleValue();
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }

    /**
     * Heconetwork calculate HSwap LP tokenprice
     *
     * @param swapTokenContractAddress swap HSwap LPContract address
     * @param baseTokenContractAddress Benchmark for calculating pricestoken(Trading against one of themtokenAs a basis for calculation)
     * @param baseTokenUnitPrice       benchmarktokenUnit price of
     * @return
     * @throws Exception
     */
    public Double hecoContractQuote(String swapTokenContractAddress, String baseTokenContractAddress, String baseTokenUnitPrice) {
        try {
            // calculateETHUnder the network CAKE_LP The price of cakeLp
            chain.getLogger().debug("HECO,  Cake-LP:{}, HUSD:{}", swapTokenContractAddress, baseTokenContractAddress);
            // Chi Zi LiHUSDQuantity of
            BigInteger husdBalance = hecoWalletApi.getERC20Balance(swapTokenContractAddress, baseTokenContractAddress);
            chain.getLogger().debug("husdBalance:" + husdBalance);
            int husdDecimals = hecoWalletApi.getContractTokenDecimals(baseTokenContractAddress);
            chain.getLogger().debug("husdDecimals:" + husdDecimals);
            BigInteger husdTwice = husdBalance.multiply(new BigInteger("2"));
            chain.getLogger().debug("husdTwice:" + husdTwice);
            BigDecimal husdCount = new BigDecimal(husdTwice).movePointLeft(husdDecimals);
            chain.getLogger().debug("husdCount:" + husdCount);
            BigDecimal husdPrice = husdCount.multiply(new BigDecimal(baseTokenUnitPrice));
            chain.getLogger().debug("husdPrice:" + husdPrice);
            chain.getLogger().debug("");
            BigInteger totalSupplyCakeLp = hecoWalletApi.totalSupply(swapTokenContractAddress);
            chain.getLogger().debug("totalSupplyHSwapLP:" + totalSupplyCakeLp);
            int cakeLpDecimals = hecoWalletApi.getContractTokenDecimals(swapTokenContractAddress);
            chain.getLogger().debug("HSwapLpDecimals:" + cakeLpDecimals);
            BigDecimal cakeLpCount = new BigDecimal(totalSupplyCakeLp).movePointLeft(cakeLpDecimals);
            chain.getLogger().debug("HSwapLpCount:" + cakeLpCount);
            BigDecimal cakeLpPrice = husdPrice.divide(cakeLpCount, 6, RoundingMode.DOWN);
            chain.getLogger().debug("HSwapLpPrice:" + cakeLpPrice);
            return cakeLpPrice.doubleValue();
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }

    public Double nerveSwapQuote(QuotationContractCfg quContractCfg) {
        try {
            return NerveSwapUtil.getPrice(chain, quContractCfg);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }
}
