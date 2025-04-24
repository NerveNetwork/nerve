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
package network.nerve.hetool.rpc.cmd;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.hetool.context.HeToolContext;
import network.nerve.hetool.model.TbcUTXO;
import network.nerve.hetool.txdata.FtUTXOData;
import network.nerve.hetool.txdata.UTXOData;
import network.nerve.hetool.txdata.WithdrawalUTXOTxData;
import network.nerve.hetool.util.TbcTxUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.hetool.constant.Constant.*;

/**
 * @author: PierreLuo
 * @date: 2025/4/1
 */
@Component
public class HeToolCmd extends BaseCmd {

    private NulsLogger logger() {
        return HeToolContext.logger;
    }

    @CmdAnnotation(cmd = BUILD_TBC_WITHDRAW_TBC, version = 1.0, description = "BUILD_TBC_WITHDRAW_TBC")
    public Response buildTbcWithdrawTbc(Map<String, Object> params) {
        try {

            String withdrawalUTXO = (String) params.get("withdrawalUTXO");
            String from = (String) params.get("from");
            String to = (String) params.get("to");
            String amount = (String) params.get("amount");
            String opReturn = (String) params.get("opReturn");

            WithdrawalUTXOTxData utxoTxData = new WithdrawalUTXOTxData();
            utxoTxData.parse(HexUtil.decode(withdrawalUTXO), 0);
            String script = utxoTxData.getScript();
            List<UTXOData> inputs = utxoTxData.getUtxoDataList();
            List<TbcUTXO> utxoList = inputs.stream().map(u -> new TbcUTXO(u.getTxid(), u.getVout(), script, u.getAmount().longValue())).toList();
            Map<String, Object> map = TbcTxUtil.buildMultiSigTransaction_sendTBC(HeToolContext.context, from, to, amount, utxoList, opReturn);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", map);
            return success(resultData);
        } catch (Exception e) {
            try {
                logger().error("call params: {}", JSONUtils.obj2json(params));
            } catch (Exception e1) {
                logger().error(e1);
            }
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = FINISH_TBC_WITHDRAW_TBC, version = 1.0, description = "FINISH_TBC_WITHDRAW_TBC")
    public Response finishTbcWithdrawTbc(Map<String, Object> params) {
        try {

            String txraw = (String) params.get("txraw");
            List sigs = (List) params.get("sigs");
            List pubKeys = (List) params.get("pubKeys");

            String signedTx = TbcTxUtil.finishMultiSigTransaction_sendTBC(HeToolContext.context, txraw, sigs, pubKeys);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", signedTx);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = BUILD_TBC_WITHDRAW_FT, version = 1.0, description = "BUILD_TBC_WITHDRAW_FT")
    public Response buildTbcWithdrawFt(Map<String, Object> params) {
        try {

            String withdrawalUTXO = (String) params.get("withdrawalUTXO");
            String from = (String) params.get("from");
            String fromHash = (String) params.get("fromHash");
            String to = (String) params.get("to");
            Map tokenInfo = (Map) params.get("tokenInfo");
            String amount = (String) params.get("amount");
            List preTxs = (List) params.get("preTXs");
            List prepreTxDatas = (List) params.get("prepreTxDatas");
            String contractTx = (String) params.get("contractTx");

            String opReturn = (String) params.get("opReturn");

            WithdrawalUTXOTxData utxoTxData = new WithdrawalUTXOTxData();
            utxoTxData.parse(HexUtil.decode(withdrawalUTXO), 0);
            String tokenId = utxoTxData.getFtAddress();
            String script = utxoTxData.getScript();
            String ftTransferCode = HexUtil.encode(TbcTxUtil.buildFTtransferCode(script, fromHash));
            List<UTXOData> inputs = utxoTxData.getUtxoDataList();
            List<TbcUTXO> utxoList = inputs.stream().map(u -> new TbcUTXO(u.getTxid(), u.getVout(), script, u.getAmount().longValue())).toList();
            List<FtUTXOData> ftUtxoDataList = utxoTxData.getFtUtxoDataList();
            ftUtxoDataList.forEach(f -> f.setScript(ftTransferCode));

            Map<String, Object> map = TbcTxUtil.buildMultiSigTransaction_transferFT(
                    HeToolContext.context, from, to, tokenId, tokenInfo, amount, utxoList.get(0), ftUtxoDataList, preTxs, prepreTxDatas, contractTx, "cQx11rCDpvCGZx2W6Fqdg55ZSnbKf8DhazsNxfVTDGCQQtvJWccL", opReturn);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", map);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = FINISH_TBC_WITHDRAW_FT, version = 1.0, description = "FINISH_TBC_WITHDRAW_FT")
    public Response finishTbcWithdrawFt(Map<String, Object> params) {
        try {

            String txraw = (String) params.get("txraw");
            List sigs = (List) params.get("sigs");
            List pubKeys = (List) params.get("pubKeys");

            String signedTx = TbcTxUtil.finishMultiSigTransaction_transferFT(HeToolContext.context, txraw, sigs, pubKeys);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", signedTx);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = FETCH_FT_PREPRETXDATA, version = 1.0, description = "FETCH_FT_PREPRETXDATA")
    public Response fetchFtPrePreTxData(Map<String, Object> params) {
        try {

            String preTXHex = (String) params.get("preTXHex");
            int preTxVout = Integer.parseInt(params.get("preTxVout").toString());
            Map prePreTxs = (Map) params.get("prePreTxs");

            String ftPrePreTxData = TbcTxUtil.fetchFtPrePreTxData(HeToolContext.context, preTXHex, preTxVout, prePreTxs);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("value", ftPrePreTxData);
            return success(resultData);
        } catch (Exception e) {
            logger().error(e);
            return failed(e.getMessage());
        }
    }

}
