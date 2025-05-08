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
package network.nerve.converter.heterogeneouschain.tbc.helper;

import com.neemre.btcdcli4j.core.domain.RawInput;
import com.neemre.btcdcli4j.core.domain.RawOutput;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import com.neemre.btcdcli4j.core.domain.SignatureScript;
import com.neemre.btcdcli4j.core.http.HttpConstants;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.BchUtxoUtil;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.BitCoinLibAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.AnalysisTxInfo;
import network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.heterogeneouschain.tbc.core.TbcWalletApi;
import network.nerve.converter.heterogeneouschain.tbc.model.FtTransfer;
import network.nerve.converter.heterogeneouschain.tbc.model.FtTransferInfo;
import network.nerve.converter.heterogeneouschain.tbc.model.TbcRawTransaction;
import network.nerve.converter.heterogeneouschain.tbc.utils.TbcUtil;
import network.nerve.converter.utils.jsonrpc.JsonRpcUtil;
import network.nerve.converter.utils.jsonrpc.RpcResult;
import org.apache.http.message.BasicHeader;

import java.util.*;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.EMPTY_STRING;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
public class TbcAnalysisTxHelper extends BitCoinLibAnalysisTxHelper {

    private TbcWalletApi tbcWalletApi;

    static class RawInputRequest {
        private TbcRawTransaction tx;
        private String hash;
        private RawInput input;

        public RawInputRequest(TbcRawTransaction tx, String hash, RawInput input) {
            this.tx = tx;
            this.hash = hash;
            this.input = input;
        }
    }
    @Override
    public List fetchVinInfoOfMultiSign(List txs) throws Exception {
        List<TbcRawTransaction> resultList = new ArrayList<>();
        List<RawInputRequest> requestFtList = new ArrayList<>();
        List<RawInputRequest> requestList = new ArrayList<>();
        // Traverse the transaction input. When the script starts with 00, it means it is a multi-signature address transaction.
        for (Object _txInfo : txs) {
            RawTransaction txInfo = (RawTransaction) _txInfo;
            TbcRawTransaction tx = new TbcRawTransaction(txInfo);
            resultList.add(tx);
            List<RawInput> inputList = txInfo.getVIn();
            if (HtgUtil.isEmptyList(inputList)) {
                continue;
            }
            for (RawInput input : inputList) {
                SignatureScript scriptSig = input.getScriptSig();
                if (scriptSig == null || StringUtils.isBlank(scriptSig.getHex())) {
                    continue;
                }
                String hex = scriptSig.getHex();
                if (hex.startsWith("00")) {
                    requestList.add(new RawInputRequest(tx, txInfo.getTxId(), input));
                    continue;
                }
                String asm = scriptSig.getAsm();
                if (asm.startsWith("1 ")) {
                    requestFtList.add(new RawInputRequest(tx, txInfo.getTxId(), input));
                    continue;
                }
            }
        }
        if (!requestList.isEmpty()) {
            // Assemble parameters and request in batches to get preTxUTXO
            List methods = new ArrayList();
            List params = new ArrayList();
            String url = tbcWalletApi.createNodeUri();
            List<BasicHeader> headers = List.of(tbcWalletApi.resolveAuthHeader(HttpConstants.AUTH_SCHEME_BASIC));
            Set<String> hashSet = new HashSet<>();
            for (RawInputRequest request : requestList) {
                hashSet.add(request.input.getTxId());
            }
            for (String hash : hashSet) {
                methods.add("getrawtransaction");
                params.add(List.of(hash, true));
            }
            // The result is then assigned to TbcRawTransaction
            Map<String, Map> preTxMap = new HashMap<>();
            List<RpcResult> list = JsonRpcUtil.batchRequest(url, methods, params, headers);
            for (RpcResult result : list) {
                Map data = (Map) result.getResult();
                if (data == null) {
                    continue;
                }
                String txid = data.get("txid").toString();
                preTxMap.put(txid, data);
            }
            for (RawInputRequest request : requestList) {
                Map txMap = preTxMap.get(request.input.getTxId());
                if (txMap == null) {
                    continue;
                }
                request.tx.getPreTxMap().put(
                        request.input.getTxId(), txMap);
            }
        }

        if (!requestFtList.isEmpty()) {
            String ftUrl = "%s/v1/tbc/main/ft/decode/tx/history/%s";
            Set<String> hashSet = new HashSet<>();
            for (RawInputRequest request : requestFtList) {
                hashSet.add(request.hash);
            }
            Map<String, FtTransferInfo> ftMap = new HashMap<>();
            for (String hash : hashSet) {
                String data = HttpClientUtil.get(String.format(ftUrl, tbcWalletApi.rpc(), hash));
                if (StringUtils.isNotBlank(data)) {
                    if (data.contains("Internal Server Error")) {
                        continue;
                    }
                    FtTransferInfo ftTransferInfo = JSONUtils.json2pojo(data, FtTransferInfo.class);
                    ftMap.put(hash, ftTransferInfo);
                }
            }
            // The result is then assigned to TbcRawTransaction
            for (RawInputRequest request : requestFtList) {
                FtTransferInfo ftTransferInfo = ftMap.get(request.hash);
                if (ftTransferInfo == null) {
                    continue;
                }
                request.tx.setFtTransferInfo(ftTransferInfo);
            }
        }
        return resultList;
    }

    @Override
    public AnalysisTxInfo analysisTxTypeInfo(Object _txInfo, long txTime, String blockHash) {
        String multiAddressCombineHash = (String) htgContext.dynamicCache().get("combineHash");
        TbcRawTransaction tbcTxInfo = (TbcRawTransaction) _txInfo;
        if (tbcTxInfo == null) {
            htgContext.logger().warn("Transaction does not exist");
            return null;
        }
        Map<String, Map> preTxMap = tbcTxInfo.getPreTxMap();
        FtTransferInfo ftTransferInfo = tbcTxInfo.getFtTransferInfo();
        RawTransaction txInfo = tbcTxInfo.getTx();
        if (txInfo.getTime() == null || txInfo.getBlockTime() == null) {
            txInfo.setTime(txTime);
            txInfo.setBlockTime(txTime);
        }
        if (txInfo.getBlockHash() == null) {
            txInfo.setBlockHash(blockHash);
        }
        if (HtgUtil.isEmptyList(txInfo.getVOut())) {
            return null;
        }
        if (HtgUtil.isEmptyList(txInfo.getVIn())) {
            return null;
        }
        List<RawOutput> outputList = txInfo.getVOut();
        List<RawInput> inputList = txInfo.getVIn();
        HeterogeneousChainTxType txType = null;
        OUT:
        do {
            // 发起者是多签地址，资产是TBC
            for (RawInput input : inputList) {
                SignatureScript scriptSig = input.getScriptSig();
                if (scriptSig == null || StringUtils.isBlank(scriptSig.getHex())) {
                    continue;
                }
                String hex = scriptSig.getHex();
                if (hex.startsWith("00")) {
                    Map preTx = preTxMap.get(input.getTxId());
                    //RawOutput preOutput = preVoutMap.get(input.getTxId() + "-" + input.getVOut());
                    if (preTx == null) {
                        continue;
                    }
                    List<Map> voutList = (List<Map>) preTx.get("vout");
                    RawOutput preOutput = JSONUtils.map2pojo(voutList.get(input.getVOut()), RawOutput.class);

                    String inputAddress = TbcUtil.convertP2MSScriptToMSAddress(preOutput.getScriptPubKey().getAsm());
                    if (htgListener.isListeningAddress(inputAddress)) {
                        txType = HeterogeneousChainTxType.WITHDRAW;
                        break OUT;
                    }
                    continue;
                }
            }
            if (ftTransferInfo != null) {
                // 发起者是多签地址，转账的是ft token
                List<FtTransfer> ftInput = ftTransferInfo.getInput();
                for (FtTransfer transfer : ftInput) {
                    String address = transfer.getAddress();
                    if (htgListener.isListeningAddress(address) || address.contains(multiAddressCombineHash)) {
                        txType = HeterogeneousChainTxType.WITHDRAW;
                        break OUT;
                    }
                }
            }

            // 接收者是多签地址，资产是TBC
            for (int i = 0; i < outputList.size(); i++) {
                RawOutput output = outputList.get(i);
                String asm = output.getScriptPubKey().getAsm();
                if (asm.endsWith("OP_CHECKMULTISIG")) {
                    String outputAddress = TbcUtil.convertP2MSScriptToMSAddress(asm);
                    if (htgListener.isListeningAddress(outputAddress)) {
                        txType = HeterogeneousChainTxType.DEPOSIT;
                        break OUT;
                    }
                }
            }
            if (ftTransferInfo == null) {
                break OUT;
            }
            // 接收者是多签地址，转账的是ft token
            List<FtTransfer> output = ftTransferInfo.getOutput();
            for (FtTransfer transfer : output) {
                String address = transfer.getAddress();
                if (htgListener.isListeningAddress(address) || address.contains(multiAddressCombineHash)) {
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    break OUT;
                }
            }
        } while (false);
        if (txType == null) {
            return null;
        }
        return new AnalysisTxInfo(txType, _txInfo);
    }

    @Override
    public long calcTxFee(Object _txInfo, BitCoinLibWalletApi htgWalletApi) {
        TbcRawTransaction tbcTxInfo = (TbcRawTransaction) _txInfo;
        RawTransaction txInfo = tbcTxInfo.getTx();
        Map<String, Map> preTxMap = tbcTxInfo.getPreTxMap();
        Map<String, RawTransaction> preTxCache = new HashMap<>();
        List<RawInput> inputList = txInfo.getVIn();
        long fromTotal = 0;
        for (RawInput input : inputList) {
            String txId = input.getTxId();
            Integer vOut = input.getVOut();
            RawTransaction prevTx;
            prevTx = preTxCache.get(txId);
            if (prevTx == null) {
                Map prevTx_ = preTxMap.get(txId);
                if (prevTx_ == null) {
                    prevTx = htgWalletApi.getTransactionByHash(txId);
                } else {
                    prevTx = JSONUtils.map2pojo(prevTx_, RawTransaction.class);
                }
                preTxCache.put(txId, prevTx);
            }
            fromTotal += prevTx.getVOut().get(vOut).getValue().movePointRight(htgContext.ASSET_NAME().decimals()).longValue();
        }
        List<RawOutput> outputList = txInfo.getVOut();
        long toTotal = 0;
        for (RawOutput output : outputList) {
            toTotal += output.getValue().movePointRight(htgContext.ASSET_NAME().decimals()).longValue();
        }
        long fee = fromTotal - toTotal;
        return fee;
    }

    @Override
    protected String fetchTxHash(Object _txInfo) {
        TbcRawTransaction tbcTxInfo = (TbcRawTransaction) _txInfo;
        RawTransaction txInfo = tbcTxInfo.getTx();
        return txInfo.getTxId();
    }
}
