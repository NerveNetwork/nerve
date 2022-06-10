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

package network.nerve.converter.rpc.call;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.nuls.core.basic.Result;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.util.RpcCall;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.model.bo.AgentBasic;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.SignAccountDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Loki
 * @date: 2020-03-02
 */
public class ConsensusCall extends BaseCall {

    /**
     * 查询本节点是不是共识节点，如果是则返回，共识账户和密码
     * Query whether the node is a consensus node, if so, return, consensus account and password
     */
    public static SignAccountDTO getPackerInfo(Chain chain) {
        try {
            Map<String, Object> params = new HashMap(ConverterConstant.INIT_CAPACITY_4);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            HashMap map = (HashMap) requestAndResponse(ModuleE.CS.abbr, "cs_getPackerInfo", params);
            String address = (String) map.get("address");
            if (StringUtils.isBlank(address)) {
                return null;
            }
            String password = (String) map.get("password");
            if (StringUtils.isBlank(password)) {
                return null;
            }
            return new SignAccountDTO(address, password);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }

    /**
     * 获取出块节点列表
     *
     * @param chain
     * @return
     * @throws NulsException
     */
    public static List<AgentBasic> getAgentList(Chain chain) throws NulsException {
        return getAgentList(chain, null);
    }

    /**
     * 获取出块节点列表
     *
     * @param chain
     * @param height 根据高度获取共识列表,如果高度为0L, 则取当前最新共识列表
     * @return
     * @throws NulsException
     */
    public static List<AgentBasic> getAgentList(Chain chain, Long height) {
        try {
            Map<String, Object> params = new HashMap(ConverterConstant.INIT_CAPACITY_4);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            if (null != height) {
                params.put("height", height);
            }
            HashMap map = (HashMap) requestAndResponse(ModuleE.CS.abbr, "cs_getAgentBasicList", params);
            List<Map<String, Object>> listResult = (List<Map<String, Object>>) map.get("list");
            List<AgentBasic> list = new ArrayList<>();
            for (Map<String, Object> mapObj : listResult) {
                JSONUtils.getInstance().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                AgentBasic agentBasic = JSONUtils.map2pojo(mapObj, AgentBasic.class);
                if (agentBasic.getSeedNode()) {
                    // 如果是种子节点, 把节点地址和奖励地址都设为出块地址
                    agentBasic.setAgentAddress(agentBasic.getPackingAddress());
                    agentBasic.setRewardAddress(agentBasic.getPackingAddress());
                }
                list.add(agentBasic);
            }
            //按照委托金额从大到小排序, 如果金额相同,则用节点地址字符串排序
            list.sort((o1, o2) -> {
                int rs = o2.getDeposit().compareTo(o1.getDeposit());
                if (rs != 0) {
                    return rs;
                } else {
                    return o2.getAgentAddress().compareTo(o1.getAgentAddress());
                }

            });

            return list;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }

    public static long agentDelHeight(Chain chain, String agentHash) {
        HashMap<String, Object> hashMap = getAgentInfo(chain, agentHash);
        return Long.parseLong(hashMap.get("delHeight").toString());
    }

    public static HashMap<String, Object> getAgentInfo(Chain chain, String agentHash) {
        try {
            Map<String, Object> params = new HashMap(ConverterConstant.INIT_CAPACITY_4);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("agentHash", agentHash);
            return (HashMap) requestAndResponse(ModuleE.CS.abbr, "cs_getAgentInfo", params);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }

    public static Result<Map> getConsensusConfig(int chainId) {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        try {
            Map map = (Map) RpcCall.request(ModuleE.CS.abbr, "cs_getConsensusConfig", params);
            return Result.getSuccess(null).setData(map);
        } catch (NulsException e) {
            return Result.getFailed(e.getErrorCode());
        }
    }
    /**
     * 发送虚拟银行节点地址到共识
     * @param chain
     * @param height 当前高度
     */
    public static void sendVirtualBank(Chain chain, long height) {
        List<String> virtualAgentList = new ArrayList<>();
        for(VirtualBankDirector director : chain.getMapVirtualBank().values()){
            virtualAgentList.add(director.getAgentAddress());
        }
        Map<String, Object> params = new HashMap(ConverterConstant.INIT_CAPACITY_4);
        params.put(Constants.CHAIN_ID, chain.getChainId());
        params.put("height", height);
        params.put("virtualAgentList", virtualAgentList);
        try {
            requestAndResponse(ModuleE.CS.abbr, "cs_virtualAgentChange", params);
        } catch (NulsException e) {
            chain.getLogger().error(e);
        }
    }
}
