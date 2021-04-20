package network.nerve.dex.rpc.call;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.context.DexRpcConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountCall {

    /**
     * 账户验证
     * account validate
     *
     * @param chainId
     * @param address
     * @param password
     * @return validate result
     */
    public static String getPriKey(int chainId, String address, String password) throws NulsException {
        try {
            Map<String, Object> callParams = new HashMap<>(4);
            callParams.put(Constants.CHAIN_ID, chainId);
            callParams.put("address", address);
            callParams.put("password", password);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, DexRpcConstant.GET_PRIKEY, callParams);
            if (!cmdResp.isSuccess()) {
                throw new NulsException(DexErrorCode.ACCOUNT_VALID_ERROR);
            }
            HashMap callResult = (HashMap) ((HashMap) cmdResp.getResponseData()).get(DexRpcConstant.GET_PRIKEY);
            return (String) callResult.get("priKey");
        } catch (NulsException e) {
            throw e;
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    public static HashMap getAccountBalance(int chainId,int assetChainId, int assetId, String address) throws NulsException {

        try {
            Map<String, Object> params = new HashMap<>(4);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("address", address);
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, DexRpcConstant.GET_BALANCE_NONCE, params);
            if (!cmdResp.isSuccess()) {
                throw new NulsException(DexErrorCode.ACCOUNT_VALID_ERROR);
            }
            HashMap callResult = (HashMap) ((HashMap) cmdResp.getResponseData()).get(DexRpcConstant.GET_BALANCE_NONCE);
            return callResult;
        } catch (NulsException e) {
            throw e;
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    public static void txSignature(int chainId, String address, String password, String priKey, Transaction tx) throws NulsException {
        try {
            P2PHKSignature p2PHKSignature = new P2PHKSignature();
            if (!StringUtils.isBlank(priKey)) {
                p2PHKSignature = SignatureUtil.createSignatureByPriKey(tx, priKey);
            } else {
                Map<String, Object> callParams = new HashMap<>(4);
                callParams.put(Constants.CHAIN_ID, chainId);
                callParams.put("address", address);
                callParams.put("password", password);
                callParams.put("data", RPCUtil.encode(tx.getHash().getBytes()));
                Response signResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_signDigest", callParams);
                if (!signResp.isSuccess()) {
                    throw new NulsException(DexErrorCode.SIGNATURE_ERROR);
                }
                HashMap signResult = (HashMap) ((HashMap) signResp.getResponseData()).get("ac_signDigest");
                p2PHKSignature.parse(RPCUtil.decode((String) signResult.get("signature")), 0);
            }
            TransactionSignature signature = new TransactionSignature();
            List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
            p2PHKSignatures.add(p2PHKSignature);
            signature.setP2PHKSignatures(p2PHKSignatures);
            tx.setTransactionSignature(signature.serialize());
        } catch (NulsException e) {
            throw e;
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }
}
