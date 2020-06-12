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

package network.nerve.quotation.rpc.cmd;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.ObjectUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.Parameter;
import io.nuls.core.rpc.model.Parameters;
import io.nuls.core.rpc.model.TypeDescriptor;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.constant.QuotationErrorCode;
import network.nerve.quotation.manager.ChainManager;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.bo.QuotationActuator;
import network.nerve.quotation.model.dto.QuoteDTO;
import network.nerve.quotation.model.po.ConfirmFinalQuotationPO;
import network.nerve.quotation.service.QuotationService;
import network.nerve.quotation.storage.ConfirmFinalQuotationStorageService;
import network.nerve.quotation.util.CommonUtil;
import network.nerve.quotation.util.TimeUtil;
import network.nerve.quotation.util.LoggerUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Loki
 * @date: 2019/11/26
 */
@Component
public class QuotationCmd extends BaseCmd {

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private ConfirmFinalQuotationStorageService confirmFinalQuotationStorageService;

    @Autowired
    private ChainManager chainManager;

    @CmdAnnotation(cmd = "qu_quote", version = 1.0, description = "创建节点报价交易 测试")
    public Response quote(Map params) {
        Chain chain = null;
        try {
            JSONUtils.getInstance().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            QuoteDTO quoteDTO = JSONUtils.map2pojo(params, QuoteDTO.class);
            chain = chainManager.getChain(quoteDTO.getChainId());
            if (null == chain) {
                throw new NulsException(QuotationErrorCode.CHAIN_NOT_FOUND);
            }
            Transaction tx = quotationService.quote(chain, quoteDTO);
            Map<String, String> map = new HashMap<>(QuotationConstant.INIT_CAPACITY_2);
            map.put("value", tx.getHash().toHex());
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(QuotationErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = "qu_final_quotation", version = 1.0, description = "获取最终报价")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "key", parameterType = "String", parameterDes = "key"),
            @Parameter(parameterName = "date", parameterType = "String", parameterDes = "获取指定UTC日期的报价, yyyyMMdd")
    })
    public Response intradayFinalQuotation(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), QuotationErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("key"), QuotationErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(QuotationErrorCode.CHAIN_NOT_FOUND);
            }
            String key = (String) params.get("key");
            String date = (String) params.get("date");
            if (StringUtils.isBlank(date)) {
                date = TimeUtil.nowUTCDate();
            }
            ConfirmFinalQuotationPO confirmFinalQuotationPO = null;
            boolean keyExist = false;
            List<QuotationActuator> quteList = chain.getQuote();
            for (QuotationActuator qa : quteList) {
                if (key.equals(qa.getKey())) {
                    keyExist = true;
                    String anchorToken = qa.getAnchorToken();
                    String dbKey = CommonUtil.assembleKey(date, anchorToken);
                    confirmFinalQuotationPO = confirmFinalQuotationStorageService.getCfrFinalQuotation(chain, dbKey);
                    if (null == confirmFinalQuotationPO) {
                        // 带日期的查不到就查最近一次的(不带日期key)
                        confirmFinalQuotationPO = confirmFinalQuotationStorageService.getCfrFinalLastTimeQuotation(chain, anchorToken);
                    }
                    break;
                }
            }
            if (null == confirmFinalQuotationPO) {
                //如果当前需要获取nerve的价格，并且
                if (key.equals(QuotationConstant.NERVE_PRICE)
                        && QuotationContext.nerveBasedNuls != QuotationConstant.NERVE_BASED_NULS) {
                    confirmFinalQuotationPO = getNervePriceSpecial(chain, date);
                } else if (!keyExist) {
                    return failed(QuotationErrorCode.QUOTATION_KEY_NOT_EXIST);
                } else {
                    confirmFinalQuotationPO = new ConfirmFinalQuotationPO();
                    confirmFinalQuotationPO.setPrice(0.0);
                    chain.getLogger().warn("暂无任何最终报价. key:{}", key);
                }
            }
            chain.getLogger().debug("取: " + JSON.toJSONString(confirmFinalQuotationPO));
            return success(confirmFinalQuotationPO);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(QuotationErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    /**
     * 如果不存在nerve的价格，则取nuls价格的十分之一
     *
     * @param chain
     * @param date
     * @return
     */
    private ConfirmFinalQuotationPO getNervePriceSpecial(Chain chain, String date) {
        String dbKey = CommonUtil.assembleKey(date, QuotationConstant.NULS_ANCHORTOKEN);
        ConfirmFinalQuotationPO confirmFinalQuotationPO = confirmFinalQuotationStorageService.getCfrFinalQuotation(chain, dbKey);
        if (null == confirmFinalQuotationPO) {
            // 带日期的查不到就查最近一次的(不带日期key)
            confirmFinalQuotationPO = confirmFinalQuotationStorageService.getCfrFinalLastTimeQuotation(chain, QuotationConstant.NULS_ANCHORTOKEN);
            if(null == confirmFinalQuotationPO){
                chain.getLogger().warn("暂无任何最终报价. 基于NULS key:{}", QuotationConstant.NULS_ANCHORTOKEN);
                return null;
            }
        }
        BigDecimal p1 = new BigDecimal(String.valueOf(confirmFinalQuotationPO.getPrice()));
        BigDecimal p2 = new BigDecimal("10.0");
        double nerve = p1.divide(p2, 6, RoundingMode.HALF_UP).doubleValue();
        confirmFinalQuotationPO.setPrice(nerve);
        confirmFinalQuotationPO.setAnchorToken(QuotationConstant.NERVE_ANCHORTOKEN);
        return confirmFinalQuotationPO;
    }

    @CmdAnnotation(cmd = "test_final", version = 1.0, description = "测试接口")
    public Response testFinal(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), QuotationErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("token"), QuotationErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("price"), QuotationErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(QuotationErrorCode.CHAIN_NOT_FOUND);
            }
            String token = (String) params.get("token");
            String date = (String) params.get("date");
            double price = Double.parseDouble((String) params.get("price"));
            if (StringUtils.isBlank(date)) {
                date = TimeUtil.nowUTCDate();
            }
            String key = CommonUtil.assembleKey(date, token);
            ConfirmFinalQuotationPO finalQuotation = new ConfirmFinalQuotationPO();
            finalQuotation.setAnchorToken(token);
            finalQuotation.setPrice(price);
            finalQuotation.setDate(date);
            confirmFinalQuotationStorageService.saveCfrFinalQuotation(chain, key, finalQuotation);
            System.out.println("test: " + JSON.toJSONString(finalQuotation));
            return success(finalQuotation);

        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(QuotationErrorCode.SYS_UNKOWN_EXCEPTION);
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
