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

package network.nerve.converter.tx.v1.proposal;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.ConfirmProposalHelper;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.ConfirmProposalTxData;
import network.nerve.converter.model.txdata.ProposalExeBusinessData;
import network.nerve.converter.rpc.call.SwapCall;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.tx.v1.proposal.interfaces.IConfirmProposal;
import network.nerve.converter.utils.ConverterUtil;

/**
 * @author: PierreLuo
 * @date: 2022/1/4
 */
@Component("ConfirmAddStablePairForSwapTradeProposal")
public class ConfirmAddStablePairForSwapTradeProposal implements IConfirmProposal, InitializingBean {

    @Autowired
    private ConfirmProposalHelper confirmProposalHelper;
    @Autowired
    private ProposalStorageService proposalStorageService;

    @Override
    public void afterPropertiesSet() throws NulsException {
        confirmProposalHelper.register(proposalType(), this);
    }

    @Override
    public Byte proposalType() {
        return ProposalTypeEnum.MANAGE_STABLE_PAIR_FOR_SWAP_TRADE.value();
    }

    @Override
    public void validate(Chain chain, Transaction tx, ConfirmProposalTxData txData) throws NulsException {
        ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
        ProposalPO proposalPO = proposalStorageService.find(chain, businessData.getProposalTxHash());
        if (null == proposalPO) {
            chain.getLogger().error("[ConfirmAddStablePairForSwapTradeProposal] Proposal does not exist proposalHash:{}", businessData.getProposalTxHash().toHex());
            throw new NulsException(ConverterErrorCode.PROPOSAL_NOT_EXIST);
        }

        byte[] stablePairAddressBytes = proposalPO.getAddress();
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
        boolean legalStable = SwapCall.isLegalStable(chain.getChainId(), stablePairAddress);
        if (!legalStable) {
            chain.getLogger().error("[Proposal management for stablecoin trading] Transaction is illegal. stablePairAddress: {}", stablePairAddress);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

}
