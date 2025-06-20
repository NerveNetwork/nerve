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

package network.nerve.quotation.constant;

/**
 * @author: Loki
 * @date: 2019/11/26
 */
public interface QuotationConstant {

    /** QuotedtxDataData type constant */
    byte QUOTE_TXDATA_TYPE = 1;

    String RPC_VERSION = "1.0";
    String SYS_FILE_ENCODING = "file.encoding";

    String PARAM_PASSWORD = "password";
    String PARAM_ADDRESS = "address";

    String QU_CONFIG_FILE = "quotation-config.json";
//    String QU_CONTRACT_CONFIG_FILE = "quotation-contract-config.json";
    String COLLECTOR_CONFIG_FILE = "collector-config.json";

    int INIT_CAPACITY_8 = 8;
    int INIT_CAPACITY_4 = 4;
    int INIT_CAPACITY_2 = 2;

    /** Keep decimal places when calculating prices */
    int SCALE = 8;

    /** Configuration Information Table Name */
    String DB_MODULE_CONGIF = "config";

    /** Store quotation and transaction data for each node */
    String DB_QUOTATION_NODE_PREFIX = "quotation_node_";

    /** Store quotation and transaction data for each node */
    String DB_INTRADAY_QUOTATION_NODE_PREFIX = "Intraday_quotation_node_";

    /** Store the final quotation after calculation */
    String DB_QUOTATION_FINAL_PREFIX = "final_quotation_final_";

    String DB_LAST_QUOTATION_PREFIX = "last_quotation_";


    /** Store the final quotation after calculation */
    String DB_CONFIRM_FINAL_QUOTATION_PREFIX = "confirm_final_quotation_";

    String DB_CONFIRM_LAST_FINAL_QUOTATION_PREFIX = "confirm_last_final_quotation_";

    /**
     * Update to overwrite the last confirmed final quotation, backup the current quotationkeyprefix（To prevent rollback）
     */

    /** Get quote thread*/
    String QU_COLLECTOR_THREAD = "quotationCollector";
    /** Calculate final quotation thread*/
    String QU_CALCULATOR_THREAD = "quotationCalculator";
    /** Quotation processingtask, Initial delay value(branch) */
    int QU_TASK_INITIALDELAY = 1;
    /** Quotation processingtask, Run cycle interval(branch) */
    int QUTASK_PERIOD = 3;


    String NULS_PRICE = "NULS_PRICE";
    String NULS_ANCHORTOKEN = "NULS-USDT";

    String NERVE_PRICE = "NERVE_PRICE";
    String NERVE_ANCHORTOKEN = "NVT-USDT";

    int TIMEOUT_MILLIS = 5000;

    String BASIC_QUERIER_THREAD = "basicQuerierThread";

    /**
     * Verification of protocol upgradekey
     */
    String ANCHOR_TOKEN_USDT = "USDT-USDT";
    String ANCHOR_TOKEN_DAI = "DAI-USDT";
    String ANCHOR_TOKEN_USDC = "USDC-USDT";
    String ANCHOR_TOKEN_PAX = "PAX-USDT";

    String ANCHOR_TOKEN_BNB = "BNB-USDT";
    String ANCHOR_TOKEN_HT = "HT-USDT";
    String ANCHOR_TOKEN_OKB = "OKB-USDT";
    String ANCHOR_TOKEN_OKT = "OKT-USDT";
    //oneMaticKcsHeight
    String ANCHOR_TOKEN_ONE = "ONE-USDT";
    String ANCHOR_TOKEN_MATIC = "MATIC-USDT";
    String ANCHOR_TOKEN_KCS = "KCS-USDT";
    //TRON
    String ANCHOR_TOKEN_TRX = "TRX-USDT";
    String ANCHOR_TOKEN_CRO = "CRO-USDT";
    String ANCHOR_TOKEN_AVAX = "AVAX-USDT";
    String ANCHOR_TOKEN_FTM = "FTM-USDT";

    String ANCHOR_TOKEN_METIS = "METIS-USDT";
    String ANCHOR_TOKEN_IOTX = "IOTX-USDT";
    String ANCHOR_TOKEN_KLAY = "KLAY-USDT";
    String ANCHOR_TOKEN_BCH = "BCH-USDT";

    String ANCHOR_TOKEN_KAVA = "KAVA-USDT";
    String ANCHOR_TOKEN_ETHW = "ETHW-USDT";
    String ANCHOR_TOKEN_REI = "REI-USDT";
    String ANCHOR_TOKEN_EOS = "EOS-USDT";
    String ANCHOR_TOKEN_CELO = "CELO-USDT";
    String ANCHOR_TOKEN_ETC = "ETC-USDT";
    String ANCHOR_TOKEN_BRISE = "BRISE-USDT";
    String ANCHOR_TOKEN_JNS = "JNS-USDT";
    String ANCHOR_TOKEN_DOGE = "DOGE-USDT";
    String ANCHOR_TOKEN_ZETA = "ZETA-USDT";
    String ANCHOR_TOKEN_FCH = "FCH-USDT";
    String ANCHOR_TOKEN_PLS = "PLS-USDT";
    String ANCHOR_TOKEN_TBC = "TBC-USDT";

    String QU_PROTOCOL_FILE = "qu-cfg-";
    String QU_CONTRACT_FILE = "quotation-contract-config-";
}
