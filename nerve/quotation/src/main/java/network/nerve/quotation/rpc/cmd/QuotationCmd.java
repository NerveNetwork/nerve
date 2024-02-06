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

package network.nerve.quotation.rpc.cmd;

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
import network.nerve.quotation.constant.QuotationErrorCode;
import network.nerve.quotation.manager.ChainManager;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.bo.QuotationActuator;
import network.nerve.quotation.model.bo.QuotationContractCfg;
import network.nerve.quotation.model.dto.QuoteDTO;
import network.nerve.quotation.model.po.ConfirmFinalQuotationPO;
import network.nerve.quotation.service.QuotationService;
import network.nerve.quotation.storage.ConfirmFinalQuotationStorageService;
import network.nerve.quotation.util.CommonUtil;
import network.nerve.quotation.util.LoggerUtil;
import network.nerve.quotation.util.TimeUtil;

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

    @CmdAnnotation(cmd = "qu_quote", version = 1.0, description = "Create node quotation transactions test")
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

    @CmdAnnotation(cmd = "qu_final_quotation", version = 1.0, description = "Obtain final quotation")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "key", parameterType = "String", parameterDes = "key"),
            @Parameter(parameterName = "date", parameterType = "String", parameterDes = "Get specifiedUTCQuotation for dates, yyyyMMdd")
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
                        // If you can't find the date, check the most recent one(Without datekey)
                        confirmFinalQuotationPO = confirmFinalQuotationStorageService.getCfrFinalLastTimeQuotation(chain, anchorToken);
                    }
                    break;
                }
            }
            // swapContract quotationkey
            for (QuotationContractCfg quContractCfg : chain.getContractQuote()) {
                if (key.equals(quContractCfg.getKey())) {
                    keyExist = true;
                    String anchorToken = quContractCfg.getAnchorToken();
                    String dbKey = CommonUtil.assembleKey(date, anchorToken);
                    confirmFinalQuotationPO = confirmFinalQuotationStorageService.getCfrFinalQuotation(chain, dbKey);
                    if (null == confirmFinalQuotationPO) {
                        // If you can't find the date, check the most recent one(Without datekey)
                        confirmFinalQuotationPO = confirmFinalQuotationStorageService.getCfrFinalLastTimeQuotation(chain, anchorToken);
                    }
                    break;
                }
            }

            if (null == confirmFinalQuotationPO) {
                if (!keyExist) {
                    return failed(QuotationErrorCode.QUOTATION_KEY_NOT_EXIST);
                } else {
                    confirmFinalQuotationPO = new ConfirmFinalQuotationPO();
                    confirmFinalQuotationPO.setPrice(0.0);
                    chain.getLogger().warn("There is currently no final quotation available. key:{}", key);
                }
            }
            chain.getLogger().debug("take: " + JSONUtils.obj2json(confirmFinalQuotationPO));
            return success(confirmFinalQuotationPO);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(QuotationErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = "test_final", version = 1.0, description = "Test interface")
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
            System.out.println("test: " + JSONUtils.obj2json(finalQuotation));
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
