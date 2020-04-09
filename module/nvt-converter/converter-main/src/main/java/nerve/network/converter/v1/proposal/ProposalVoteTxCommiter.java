package nerve.network.converter.v1.proposal;

import nerve.network.converter.constant.ProposalConstant;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.txdata.ProposalTxData;

/**
 * @author Niels
 */
public class ProposalVoteTxCommiter {

    public static boolean commit(ProposalTxData txData, Chain chain) {
        switch (txData.getType()) {
            case ProposalConstant.TYPE_WORKORDER_RETURN:
                return workorderReturn(txData, chain);
            case ProposalConstant.TYPE_WORKORDER_FORWARD:
                return workorderForward(txData, chain);
            case ProposalConstant.TYPE_FROZEN_ACCOUNT:
                return frozenAccount(txData, chain);
            case ProposalConstant.TYPE_THAW_ACCOUNT:
                return thwaAccount(txData, chain);
            case ProposalConstant.TYPE_DISQUALIFICATION_OF_BANK:
                return disqualificateBank(txData, chain);
            default:
                //普通提案什么也不做
                return true;

        }
    }

    private static boolean disqualificateBank(ProposalTxData txData, Chain chain) {
        //todo
        return false;
    }

    private static boolean thwaAccount(ProposalTxData txData, Chain chain) {
        //todo 需要调用账本接口（账本接口内应该确认不会解冻原始黑名单中的地址）
        return false;
    }

    private static boolean frozenAccount(ProposalTxData txData, Chain chain) {
        //todo 需要调用账本接口
        return false;
    }

    private static boolean workorderForward(ProposalTxData txData, Chain chain) {
        //todo 调用对应组件
        //一个充值交易，只能进行一次处理，之后再有提案，则不进行处理
        return false;
    }

    private static boolean workorderReturn(ProposalTxData txData, Chain chain) {
        //todo 调用对应组件
        //一个充值交易，只能进行一次处理，之后再有提案，则不进行处理
        return false;
    }
}
