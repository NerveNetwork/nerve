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
package network.nerve.converter.heterogeneouschain.tbc.core;

import com.neemre.btcdcli4j.core.NodeProperties;
import com.neemre.btcdcli4j.core.common.Constants;
import com.neemre.btcdcli4j.core.common.Errors;
import com.neemre.btcdcli4j.core.domain.PubKeyScript;
import com.neemre.btcdcli4j.core.domain.RawOutput;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import com.neemre.btcdcli4j.core.domain.enums.ScriptTypes;
import com.neemre.btcdcli4j.core.http.HttpConstants;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.btc.txdata.FtUTXOData;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.heterogeneouschain.tbc.model.FtInfo;
import network.nerve.converter.heterogeneouschain.tbc.utils.TbcUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.bitcoinj.core.Transaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.*;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.EMPTY_STRING;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class TbcWalletApi extends BitCoinLibWalletApi {

    @Override
    public void additionalCheck(Map<String, Object> resultMap) {
        // like this: [wrapperApi,https://turingwallet.xyz,http://tbcdev.org:5000] update rpcMain and rpcTest
        // or this: [wrapperApi,https://turingwallet.xyz] only update rpcMain
        // or this: [wrapperApi,x,http://tbcdev.org:5000] only update rpcTest
        try {
            String extend3 = (String) resultMap.get("extend3");
            if (StringUtils.isBlank(extend3)) {
                return;
            }
            if (extend3.startsWith("wrapperApi,")) {
                String[] split = extend3.split(",");
                if (split.length > 2) {
                    String data1 = split[1].trim();
                    String data2 = split[2].trim();
                    rpcMain = data1.length() > 4 ? data1 : rpcMain;
                    rpcTest = data2.length() > 4 ? data2 : rpcTest;
                } else if (split.length > 1) {
                    String data1 = split[1].trim();
                    rpcMain = data1.length() > 4 ? data1 : rpcMain;
                }
            }
        } catch (Exception e) {
            htgContext.logger().error(e.getMessage());
        }
    }

    private String rpcMain = "https://turingwallet.xyz";
    private String rpcTest = "https://turingwallet.xyz";
    //private String rpcTest = "http://tbcdev.org:5000";

    public String rpc() {
        if (htgContext.getConverterCoreApi().isNerveMainnet()) {
            return rpcMain;
        }
        return rpcTest;
    }

    public String createNodeUri() throws URISyntaxException {
        Properties nodeConfig = this.getClient().getNodeConfig();
        // With port specified
        if (nodeConfig.containsKey(NodeProperties.RPC_PORT.getKey())) {
            return String.format("%s://%s:%s/",
                    nodeConfig.getProperty(NodeProperties.RPC_PROTOCOL.getKey()),
                    nodeConfig.getProperty(NodeProperties.RPC_HOST.getKey()),
                    nodeConfig.getProperty(NodeProperties.RPC_PORT.getKey()));
        } else {
            // Without port
            return String.format("%s://%s",
                    nodeConfig.getProperty(NodeProperties.RPC_PROTOCOL.getKey()),
                    nodeConfig.getProperty(NodeProperties.RPC_HOST.getKey()));
        }
    }

    public BasicHeader resolveAuthHeader(String authScheme) {
        if (authScheme.equals(HttpConstants.AUTH_SCHEME_NONE)) {
            return null;
        }
        if (authScheme.equals(HttpConstants.AUTH_SCHEME_BASIC)) {
            return new BasicHeader(HttpConstants.HEADER_AUTH, HttpConstants.AUTH_SCHEME_BASIC
                    + " " + getCredentials(HttpConstants.AUTH_SCHEME_BASIC));
        }
        return null;
    }

    private String getCredentials(String authScheme) {
        Properties nodeConfig = this.getClient().getNodeConfig();
        if (authScheme.equals(HttpConstants.AUTH_SCHEME_NONE)) {
            return Constants.STRING_EMPTY;
        } else if (authScheme.equals(HttpConstants.AUTH_SCHEME_BASIC)) {
            return Base64.encodeBase64String((nodeConfig.getProperty(NodeProperties.RPC_USER.getKey())
                    + ":" + nodeConfig.getProperty(NodeProperties.RPC_PASSWORD.getKey())).getBytes());
        }
        throw new IllegalArgumentException(Errors.ARGS_HTTP_AUTHSCHEME_UNSUPPORTED.getDescription());
    }

    @Override
    public long getFeeRate() {
        return 1;
    }

    public RawTransaction getTransactionByHash(String txHash) {
        try {
            String url = String.format("%s/v1/tbc/main/tx/hex/%s/decode", rpc(), txHash);
            String txJson = HttpClientUtil.get(url);
            RawTransaction rawTransaction = JSONUtils.json2pojo(txJson, RawTransaction.class);
            rawTransaction.getVOut().forEach((vout) -> {
                this.setVoutAddressToList(vout.getScriptPubKey());
            });
            return rawTransaction;
        } catch (Exception e) {
            getLog().error(e);
            return null;
        }
    }

    void setVoutAddressToList(PubKeyScript pubKeyScript) {
        if (pubKeyScript.getAddress() != null) {
            List<String> addresses = new ArrayList();
            addresses.add(pubKeyScript.getAddress());
            pubKeyScript.setAddresses(addresses);
        }

    }

    public String getOpReturnHex(RawTransaction tx) {
        if (tx == null) return EMPTY_STRING;
        List<RawOutput> vOut = tx.getVOut();
        if (HtgUtil.isEmptyList(vOut)) return EMPTY_STRING;
        String result = EMPTY_STRING;
        for (RawOutput output : vOut) {
            if (output.getValue().compareTo(BigDecimal.ZERO) > 0)
                continue;
            PubKeyScript script = output.getScriptPubKey();
            if (script.getType() != ScriptTypes.NULL_DATA)
                continue;
            String asm = script.getAsm();
            if (!asm.startsWith("0 OP_RETURN 88888888"))
                continue;
            String content = asm.replace("0 OP_RETURN 88888888", "");
            result += content;
        }
        return result;
    }

    public BigDecimal getBalance(String address) {
        String url = String.format("%s/v1/tbc/main/address/%s/get/balance", rpc(), address);
        try {
            String txJson = HttpClientUtil.get(url);
            Map map = JSONUtils.json2map(txJson);
            Map data = (Map) map.get("data");
            if (data == null) {
                return BigDecimal.ZERO;
            }
            Object balance = data.get("balance");
            if (balance == null) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(balance.toString());
        } catch (Exception e) {
            getLog().error(e);
            return BigDecimal.ZERO;
        }
    }

    public List<UTXOData> getAccountUTXOs(String address) {
        try {
            String url;
            if (address.startsWith("1")) {
                url = String.format("%s/v1/tbc/main/address/%s/unspent/", rpc(), address);
            } else {
                url = String.format("%s/v1/tbc/main/script/hash/%s/unspent/", rpc(), TbcUtil.getAddressHash(address));
            }
            String data = HttpClientUtil.get(url);
            List<Map> list = JSONUtils.json2list(data, Map.class);
            if (list.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            List<UTXOData> resultList = new ArrayList<>();
            for (Map utxo : list) {
                resultList.add(new UTXOData(
                        utxo.get("tx_hash").toString(),
                        Integer.parseInt(utxo.get("tx_pos").toString()),
                        new BigInteger(utxo.get("value").toString())
                ));
            }
            return resultList;
        } catch (Exception e) {
            getLog().error("get utxo error: " + e.getMessage());
            return Collections.EMPTY_LIST;
        }
    }

    public BigDecimal getTbc20Balance(String contractId, String address) {
        try {
            String url = String.format("%s/v1/tbc/main/ft/balance/address/%s/contract/%s", rpc(), address, contractId);
            String txJson = HttpClientUtil.get(url);
            Map data = JSONUtils.json2map(txJson);
            if (data == null) {
                return BigDecimal.ZERO;
            }
            Object balance = data.get("ftBalance");
            if (balance == null) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(balance.toString());
        } catch (Exception e) {
            getLog().error(e);
            return BigDecimal.ZERO;
        }
    }

    public FtInfo getTbc20Info(String contractId) {
        try {
            String url = String.format("%s/v1/tbc/main/ft/info/contract/id/%s", rpc(), contractId);
            String txJson = HttpClientUtil.get(url);
            return JSONUtils.json2pojo(txJson, FtInfo.class);
        } catch (Exception e) {
            getLog().error(e);
            return null;
        }
    }

    public String fetchTXraw(String txid) {
        try {
            String url = String.format("%s/v1/tbc/main/tx/hex/%s", rpc(), txid);
            String txraw = HttpClientUtil.get(url);
            if (txraw.charAt(0) == '"' && txraw.charAt(txraw.length() - 1) == '"') {
                txraw = txraw.substring(1, txraw.length() - 1);
            }
            return txraw;
        } catch (Exception e) {
            getLog().error(e);
            return null;
        }
    }

    public Map<String, String> fetchFtPrePreTx(String preTxHex, int preTxVout) {
        Map<String, String> prePreTxs = new HashMap<>();
        Transaction preTx = Transaction.read(ByteBuffer.wrap(HexUtil.decode(preTxHex)));
        byte[] scriptBytes = preTx.getOutputs().get(preTxVout + 1).getScriptBytes();
        String preTXtape = bytesToHex(scriptBytes, 3, 51);

        for (int i = preTXtape.length() - 16; i >= 0; i -= 16) {
            String chunk = preTXtape.substring(i, i + 16);
            if (!"0000000000000000".equals(chunk)) {
                int inputIndex = i / 16;
                String hash = preTx.getInputs().get(inputIndex).getOutpoint().hash().toString();
                prePreTxs.put(hash, this.fetchTXraw(hash));
            }
        }
        return prePreTxs;
    }

    private static String bytesToHex(byte[] bytes, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end && i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

    public List<FtUTXOData> getAccountTbc20UTXOs(String contractId, String address) {
        try {
            String url;
            if (address.startsWith("1")) {
                url = String.format("%s/v1/tbc/main/ft/utxo/address/%s/contract/%s", rpc(), address, contractId);
            } else {
                url = String.format("%s/v1/tbc/main/ft/utxo/combine/script/%s/contract/%s", rpc(), TbcUtil.getCombineHash(address), contractId);
            }

            String data = HttpClientUtil.get(url);
            Map map = JSONUtils.json2map(data);
            List<Map> list = (List<Map>) map.get("ftUtxoList");
            if (list == null || list.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            List<FtUTXOData> resultList = new ArrayList<>();
            for (Map utxo : list) {
                resultList.add(new FtUTXOData(
                        utxo.get("utxoId").toString(),
                        Integer.parseInt(utxo.get("utxoVout").toString()),
                        Long.parseLong(utxo.get("utxoBalance").toString()),
                        utxo.get("ftContractId").toString(),
                        new BigInteger(utxo.get("ftBalance").toString())
                ));
            }
            return resultList;
        } catch (Exception e) {
            getLog().error("get utxo error: " + e.getMessage());
            return Collections.EMPTY_LIST;
        }
    }

    public String broadcast(String txHex) {
        try {
            String url = String.format("%s/v1/tbc/main/broadcast/tx/raw", rpc());
            Map post = new HashMap();
            post.put("txHex", txHex);
            String result = HttpClientUtil.post(url, post);
            Map map = JSONUtils.json2map(result);
            Map error = (Map) map.get("error");
            if (error != null) {
                int code = Integer.parseInt(error.get("code").toString());
                if (code != 0) {
                    throw new RuntimeException(error.get("message").toString());
                }
            }
            return map.get("result").toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
