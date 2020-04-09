/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.rpc.call;

import com.fasterxml.jackson.databind.DeserializationFeature;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.model.bo.AgentBasic;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.dto.SignAccountDTO;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Chino
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
     * @param chain
     * @return
     * @throws NulsException
     */
    public static List<AgentBasic> getAgentInfo(Chain chain) throws NulsException {
        return getAgentInfo(chain, null);
    }

    /**
     * 获取出块节点列表
     * @param chain
     * @param height
     * @return
     * @throws NulsException
     */
    public static List<AgentBasic> getAgentInfo(Chain chain, Long height) {
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
            for(Map<String, Object> mapObj : listResult){
                JSONUtils.getInstance().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                AgentBasic agentBasic = JSONUtils.map2pojo(mapObj, AgentBasic.class);
                list.add(agentBasic);
            }
            //按照委托金额从大到小排序
            list.sort((AgentBasic o1, AgentBasic o2) -> o2.getDeposit().compareTo(o1.getDeposit()));
            return list;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }
}
