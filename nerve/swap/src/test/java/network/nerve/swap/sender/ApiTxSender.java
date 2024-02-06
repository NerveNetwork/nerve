package network.nerve.swap.sender;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.utils.HttpClientUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
public abstract class ApiTxSender {

    protected boolean broadcastTx(int chainId,Transaction tx) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("jsonrpc", "2.0");
        paramMap.put("method", "broadcastTx");
        paramMap.put("params", List.of(chainId, HexUtil.encode(tx.serialize())));
        paramMap.put("id", "1234");
        String response = HttpClientUtil.post(getApiUrl(), paramMap);
        if (StringUtils.isBlank(response)) {
            System.out.println("Failed to obtain return data");
            return false;
        }
        Map<String, Object> map = JSONUtils.json2map(response);
        Map<String, Object> resultMap = (Map<String, Object>) map.get("result");
        if (null == resultMap) {
            System.out.println(map.get("error"));
            return false;
        }
        return true;
    }

    protected LedgerBalance getLedgerBalance(int chainId, String address, int syrupAssetChainId, int syrupAssetId) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("jsonrpc", "2.0");
        paramMap.put("method", "getAccountBalance");
        paramMap.put("params", List.of(chainId, syrupAssetChainId, syrupAssetId, address));
        paramMap.put("id", "1234");
        String response = HttpClientUtil.post(getApiUrl(), paramMap);
        if (StringUtils.isBlank(response)) {
            return null;
        }
        Map<String, Object> map = JSONUtils.json2map(response);
        Map<String, Object> resultMap = (Map<String, Object>) map.get("result");
        if (null == resultMap) {
            return null;
        }
        LedgerBalance balance = LedgerBalance.newInstance();
        balance.setAssetsId(syrupAssetId);
        balance.setNonce(HexUtil.decode(resultMap.get("nonce") + ""));
        balance.setBalance(new BigInteger(resultMap.get("balance") + ""));
        balance.setFreeze(new BigInteger(resultMap.get("freeze") + ""));
        balance.setAssetsChainId(syrupAssetChainId);
        balance.setAddress(AddressTool.getAddress(address));
        return balance;
    }


    public abstract String getApiUrl();
}
