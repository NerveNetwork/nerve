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

import static network.nerve.converter.constant.ConverterConstant.ORACLE_KEY_NVT_PRICE;

/**
 * @author: Loki
 * @date: 2020/10/10
 */
public class QuotationCall {

    /**
     * According to the feeding pricekeyObtain final feed price
     * @param chain
     * @param oracleKey
     * @return
     */
    public static BigDecimal getPriceByOracleKey(Chain chain, String oracleKey) {
        /********************* For testing purposes ***************************/
        //TODO pierre test code
        /*if("ETH_PRICE".equals(oracleKey)){
            return new BigDecimal("3417.30564502");
        } else if (ORACLE_KEY_NVT_PRICE.equals(oracleKey)){
            return new BigDecimal("0.00848648");
        } else if ("BNB_PRICE".equals(oracleKey)){
            return new BigDecimal("416.3948");
        } else if ("HT_PRICE".equals(oracleKey)){
            return new BigDecimal("7.0541");
        } else if ("OKT_PRICE".equals(oracleKey)){
            return new BigDecimal("24.6444");
        } else if ("ONE_PRICE".equals(oracleKey)){
            return new BigDecimal("0.3431331");
        } else if ("MATIC_PRICE".equals(oracleKey)){
            return new BigDecimal("0.7412");
        } else if ("KCS_PRICE".equals(oracleKey)){
            return new BigDecimal("14.2687");
        } else if ("TRX_PRICE".equals(oracleKey)){
            return new BigDecimal("0.07095");
        } else if ("CRO_PRICE".equals(oracleKey)){
            return new BigDecimal("0.1991");
        } else if ("AVAX_PRICE".equals(oracleKey)){
            return new BigDecimal("34.43");
        } else if ("FTM_PRICE".equals(oracleKey)){
            return new BigDecimal("0.3648");
        } else if ("METIS_PRICE".equals(oracleKey)){
            return new BigDecimal("29.8957");
        } else if ("IOTX_PRICE".equals(oracleKey)){
            return new BigDecimal("0.03262");
        } else if ("KLAY_PRICE".equals(oracleKey)){
            return new BigDecimal("0.4146");
        } else if ("BCH_PRICE".equals(oracleKey)){
            return new BigDecimal("351.26");
        } else if ("NULS_PRICE".equals(oracleKey)){
            return new BigDecimal("0.2035");
        } else if ("KAVA_PRICE".equals(oracleKey)){
            return new BigDecimal("0.752");
        } else if ("ETHW_PRICE".equals(oracleKey)){
            return new BigDecimal("3.409");
        } else if ("LGCY_PRICE".equals(oracleKey)){
            return new BigDecimal("0.0001815");
        } else if ("REI_PRICE".equals(oracleKey)){
            return new BigDecimal("0.1993");
        } else if ("EOS_PRICE".equals(oracleKey)){
            return new BigDecimal("0.7366");
        } else if ("CELO_PRICE".equals(oracleKey)){
            return new BigDecimal("0.5106");
        } else if ("ETC_PRICE".equals(oracleKey)){
            return new BigDecimal("18.0031");
        } else if ("BRISE_PRICE".equals(oracleKey)){
            return new BigDecimal("0.0000001382");
        } else if ("JNS_PRICE".equals(oracleKey)){
            return new BigDecimal("0.07283");
        } else if ("FCH_PRICE".equals(oracleKey)){
            return new BigDecimal("0.01");
        } else if ("BTC_PRICE".equals(oracleKey)){
            return new BigDecimal("66249.56");
        } else if ("OKB_PRICE".equals(oracleKey)){
            return new BigDecimal("64.6444");
        } else if ("FCH_PRICE".equals(oracleKey)){
            return new BigDecimal("0.00307");
        } else if ("ZETA_PRICE".equals(oracleKey)){
            return new BigDecimal("0.01993");
        } else if ("PLS_PRICE".equals(oracleKey)){
            return new BigDecimal("0.0001142");
        } else if ("TBC_PRICE".equals(oracleKey)){
            return new BigDecimal("1.1666");
        }
        if (chain.getChainId() == 5 && "SHM_PRICE".equals(oracleKey)){
            return new BigDecimal("0.01993");
        }*/
        /************************************************/
        BigDecimal quotation = quFinalQuotation(chain, oracleKey);
        if (chain.getChainId() == 5 && "TBC_PRICE".equals(oracleKey) && (quotation == null || quotation.compareTo(BigDecimal.ZERO) == 0)){
            return new BigDecimal("0.95");
        }
        return quotation;
    }

    private static BigDecimal quFinalQuotation(Chain chain, String oracleKey) {
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
            //e.printStackTrace();
            chain.getLogger().error(e);
        }
        return null;
    }
}
