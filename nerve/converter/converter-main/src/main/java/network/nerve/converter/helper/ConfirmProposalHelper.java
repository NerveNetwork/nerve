/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
package network.nerve.converter.helper;

import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.txdata.ConfirmProposalTxData;
import network.nerve.converter.tx.v1.proposal.interfaces.IConfirmProposal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Mimi
 * @date: 2020-05-19
 */
@Component
public class ConfirmProposalHelper {

    private Map<Byte, IConfirmProposal> confirmProposalMap = new ConcurrentHashMap<>();
    private IConfirmProposal defaultConfirmProposal = new IConfirmProposal() {
        @Override
        public Byte proposalType() {return 0;}
        @Override
        public void validate(Chain chain, Transaction tx, ConfirmProposalTxData txData) throws NulsException {}
    };

    public IConfirmProposal getConfirmProposal(Byte type) {
        IConfirmProposal proposal = confirmProposalMap.get(type);
        if(proposal == null) {
            return defaultConfirmProposal;
        }
        return proposal;
    }

    public void register(Byte type, IConfirmProposal proposal) {
        confirmProposalMap.put(type, proposal);
    }

}
