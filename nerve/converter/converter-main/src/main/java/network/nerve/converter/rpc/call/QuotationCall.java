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

package network.nerve.converter.rpc.call;

import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.model.bo.Chain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static network.nerve.converter.constant.ConverterConstant.*;

/**
 * @author: Loki
 * @date: 2020/10/10
 */
public class QuotationCall {

    /**
     * 根据喂价key获取最终喂价
     * @param chain
     * @param oracleKey
     * @return
     */
    public static BigDecimal getPriceByOracleKey(Chain chain, String oracleKey) {
        /********************* 测试用 ***************************/
        /*if("ETH_PRICE".equals(oracleKey)){
            return new BigDecimal("4272.6417");
        } else if (ORACLE_KEY_NVT_PRICE.equals(oracleKey)){
            return new BigDecimal("0.0267");
        } else if ("BNB_PRICE".equals(oracleKey)){
            return new BigDecimal("580.3948");
        } else if ("HT_PRICE".equals(oracleKey)){
            return new BigDecimal("9.8641");
        } else if ("OKT_PRICE".equals(oracleKey)){
            return new BigDecimal("54.5144");
        } else if ("ONE_PRICE".equals(oracleKey)){
            return new BigDecimal("0.31331");
        } else if ("MATIC_PRICE".equals(oracleKey)){
            return new BigDecimal("1.6912");
        } else if ("KCS_PRICE".equals(oracleKey)){
            return new BigDecimal("23.9747");
        } else if ("TRX_PRICE".equals(oracleKey)){
            return new BigDecimal("0.10005");
        } else if ("CRO_PRICE".equals(oracleKey)){
            return new BigDecimal("0.8698");
        } else if ("AVAX_PRICE".equals(oracleKey)){
            return new BigDecimal("120.5");
        } else if ("FTM_PRICE".equals(oracleKey)){
            return new BigDecimal("2.2498");
        }*/
        /************************************************/
        try {
            Map<String, Object> params = new HashMap(ConverterConstant.INIT_CAPACITY_4);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("key", oracleKey);
            HashMap map = (HashMap) BaseCall.requestAndResponse(ModuleE.QU.abbr, "qu_final_quotation", params);
            String price = map.get("price").toString();
            if (StringUtils.isBlank(price)) {
                return null;
            }
            return new BigDecimal(price);
        } catch (NulsException e) {
            e.printStackTrace();
        }
        return null;
    }
}
