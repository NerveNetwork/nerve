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
     * Obtain the current consensus node list based on the latest height to maintain the latest changes in virtual banking
     * @param chain
     */
    void recordVirtualBankChanges(Chain chain);

    /**
     * If the current node is a virtual bank member Then return the information of the current member, If not, returnnull
     * @return
     */
    VirtualBankDirector getCurrentDirector(int chainId) throws NulsException;

    /**
     * Based on the latest sorted consensus list Count the latest members with virtual banking qualifications
     *
     * @param listAgent
     * @return
     */
     List<AgentBasic> calcNewestVirtualBank(Chain chain, List<AgentBasic> listAgent);


    /**
     * Based on the signed accounts of virtual bank members, Register the current node signature address with heterogeneous chain components
     * @param chain
     * @param signAccountDTO Virtual bank member signature account
     * @throws NulsException
     */
    void initLocalSignPriKeyToHeterogeneous(Chain chain, SignAccountDTO signAccountDTO) throws NulsException;



}
