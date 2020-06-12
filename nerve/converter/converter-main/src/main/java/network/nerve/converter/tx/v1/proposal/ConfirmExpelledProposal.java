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

package network.nerve.converter.tx.v1.proposal;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.ConfirmProposalHelper;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.ConfirmProposalTxData;
import network.nerve.converter.model.txdata.ProposalExeBusinessData;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.tx.v1.proposal.interfaces.IConfirmProposal;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;

/**
 * @author: Loki
 * @date: 2020/5/21
 */
@Component("ConfirmExpelledProposal")
public class ConfirmExpelledProposal implements IConfirmProposal, InitializingBean {
    @Autowired
    private ConfirmProposalHelper confirmProposalHelper;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private ProposalStorageService proposalStorageService;

    @Override
    public void afterPropertiesSet() throws NulsException {
        confirmProposalHelper.register(proposalType(), this);
    }

    @Override
    public Byte proposalType() {
        return ProposalTypeEnum.EXPELLED.value();
    }

    @Override
    public void validate(Chain chain, Transaction tx, ConfirmProposalTxData txData) throws NulsException {

        ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);

        ProposalPO proposalPO = proposalStorageService.find(chain, businessData.getProposalTxHash());
        if(null == proposalPO){
            chain.getLogger().error("[ConfirmExpelledProposal] 提案不存在 proposalHash:{}", businessData.getProposalTxHash().toHex());
            throw new NulsException(ConverterErrorCode.PROPOSAL_NOT_EXIST);
        }

        if(chain.isVirtualBankByAgentAddr(AddressTool.getStringAddressByBytes(proposalPO.getAddress()))){
            chain.getLogger().error("[ConfirmExpelledProposal] 提案执行未成功, 该节点仍是虚拟银行 txHash:{}, ProposalTxHash:{}",
                    tx.getHash().toHex(), businessData.getProposalTxHash().toHex());
            throw new NulsException(ConverterErrorCode.AGENT_IS_VIRTUAL_BANK);
        }

        HeterogeneousTransactionInfo txInfo = HeterogeneousUtil.getTxInfo(chain,
                businessData.getHeterogeneousChainId(),
                businessData.getHeterogeneousTxHash(),
                HeterogeneousTxTypeEnum.WITHDRAWAL,
                heterogeneousDockingManager);
        if(null == txInfo){
            chain.getLogger().error("[ConfirmExpelledProposal] 未查询到异构链交易 heterogeneousTxHash:{}", businessData.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
    }

}
