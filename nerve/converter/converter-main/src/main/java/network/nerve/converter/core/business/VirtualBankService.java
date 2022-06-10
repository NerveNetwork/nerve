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

package network.nerve.converter.core.business;

import io.nuls.core.exception.NulsException;
import network.nerve.converter.model.bo.AgentBasic;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.SignAccountDTO;

import java.util.List;

/**
 * @author: Loki
 * @date: 2020-03-13
 */
public interface VirtualBankService {

    /**
     * 根据最新高度获取当前共识节点列表，来维护最新的虚拟银行的变化情况
     * @param chain
     */
    void recordVirtualBankChanges(Chain chain);

    /**
     * 如果当前节点是虚拟银行成员 则返回当前成员的信息, 不是则返回null
     * @return
     */
    VirtualBankDirector getCurrentDirector(int chainId) throws NulsException;

    /**
     * 根据最新的排好序的共识列表 统计出最新的有虚拟银行资格的成员
     *
     * @param listAgent
     * @return
     */
     List<AgentBasic> calcNewestVirtualBank(Chain chain, List<AgentBasic> listAgent);


    /**
     * 根据虚拟银行成员的签名账户, 向异构链组件注册当前节点签名地址
     * @param chain
     * @param signAccountDTO 虚拟银行成员签名账户
     * @throws NulsException
     */
    void initLocalSignPriKeyToHeterogeneous(Chain chain, SignAccountDTO signAccountDTO) throws NulsException;



}
