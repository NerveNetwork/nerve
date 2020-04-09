package nerve.network.converter.v1.proposal;

import io.nuls.base.basic.AddressTool;
import nerve.network.converter.config.ConverterContext;
import nerve.network.converter.constant.ProposalConstant;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.po.ProposalPo;
import nerve.network.converter.model.po.VoteProposalPo;
import nerve.network.converter.model.txdata.VoteProposalTxData;
import nerve.network.converter.storage.VoteProposalStorageService;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rpc.util.NulsDateUtils;

/**
 * @author Niels
 */
public class VoteValidater {

    public static boolean validate(ProposalPo po, VoteProposalTxData txData, byte[] address, Chain chain, VoteProposalStorageService service) {
        NulsLogger log = chain.getLogger();
        // 提案存在
        if (null == po) {
            log.warn("Proposal is not exist.");
            return false;
        }
        if (txData.getProposalTxHash() == null || txData.getProposalTxHash().isBlank()) {
            log.warn("Proposal tx hash can't be null.");
            return false;
        }

        if (txData.getChoice() < 0 || txData.getChoice() > 3) {
            log.warn("Vote data error: unknown chioce.");
            return false;
        }
        if (po.getTime() - NulsDateUtils.getCurrentTimeSeconds() > ConverterContext.PROPOSAL_TIMEOUT_SECONDS) {
            //提案要保证没有超时
            log.warn("The proposal is timeout.");
            return false;
        }
        //本人有投票权利
        if (po.getType() < ProposalConstant.TYPE_GENERAL && !chain.isVirtualBankByAgentAddr(AddressTool.getStringAddressByBytes(address))) {
            //必须是银行节点
            log.warn("Voter is not a virtual bank account");
            return false;
        }
        //本人没有重复投票（一经确认，不可修改）
        VoteProposalPo vote = service.find(chain, txData.getProposalTxHash(), address);
        if (vote != null) {
            log.warn("Do not vote again.");
            return false;
        }
        if (po.getStatus() != ProposalConstant.ProposalStatus.VOTING) {
            log.warn("The proposal was done.");
            return false;
        }
        return true;
    }
}
