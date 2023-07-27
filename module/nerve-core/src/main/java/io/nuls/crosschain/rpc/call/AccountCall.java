package io.nuls.crosschain.rpc.call;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.crosschain.utils.CommonUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nuls.crosschain.constant.NulsCrossChainConstant.INIT_CAPACITY_8;
import static io.nuls.crosschain.constant.NulsCrossChainConstant.RPC_VERSION;

/**
 * 与账户模块交互类
 * Interaction class with account module
 *
 * @author tag
 * 2019/4/10
 */
public class AccountCall {

    /**
     * 通过账户模块对数据进行签名
     *
     * @param address
     * @param password
     * @param data     待签名的数据
     * @return P2PHKSignature
     * @throws NulsException
     */
    public static P2PHKSignature signDigest(String address, String password, byte[] data, Map<String, Object> extendMap) throws NulsException {
        try {
            int chainId = AddressTool.getChainIdByAddress(address);
            Map<String, Object> params = new HashMap<>(INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("address", address);
            params.put("password", password);
            params.put("data", RPCUtil.encode(data));
            params.put("extend", extendMap);
            HashMap result = (HashMap) CommonCall.request(ModuleE.AC.abbr, "ac_signature", params);
            String signatureStr = (String) result.get("signature");
            return CommonUtil.getInstanceRpcStr(signatureStr, P2PHKSignature.class);
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    /**
     * 将链地址前缀发送给账户模块
     *
     * @param prefixList 链前缀列表
     * @throws NulsException
     */
    public static void addAddressPrefix(List<Map<String, Object>> prefixList) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(2);
            params.put("prefixList", prefixList);
            CommonCall.request(ModuleE.AC.abbr, "ac_addAddressPrefix", params);
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }
}
