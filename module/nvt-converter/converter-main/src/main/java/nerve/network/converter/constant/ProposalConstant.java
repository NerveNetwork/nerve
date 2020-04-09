package nerve.network.converter.constant;

/**
 * 提案相关常量
 *
 * @author Niels
 */
public interface ProposalConstant {

    /**
     * 充值失败，原路退回
     */
    byte TYPE_WORKORDER_RETURN = 1;

    /**
     * 充值处理，转到其他地址
     */
    byte TYPE_WORKORDER_FORWARD = 2;

    /**
     * 冻结账户
     */
    byte TYPE_FROZEN_ACCOUNT = 3;

    /**
     * 解冻账户
     */
    byte TYPE_THAW_ACCOUNT = 4;

    /**
     * 取消银行资格
     */
    byte TYPE_DISQUALIFICATION_OF_BANK = 5;

    /**
     * 通用提案
     */
    byte TYPE_GENERAL = 6;

    /**
     * 提案的状态，底层专用（应用端在ps上管理状态）
     */
    interface ProposalStatus{
        byte VOTING = 1;
        byte VOTED= 2;
        byte FINISHED = 3;
        byte TIMEOUT = 4;
    }

    /**
     * 投票范围常量
     */
    interface VoteRangeType {
        /**
         * 所有持币账户
         */
        int ALL_ACCOUNTS = 1;

        /**
         * 所有共识节点的创建账户
         */
        int CONSENSUS_ACCOUNTS = 2;

        /**
         * 所有银行账户
         */
        int BANK_ACCOUNTS = 3;
    }

    interface VoteChoice {

        /**
         * 同意
         */
        int favor = 1;

        /**
         * 反对&弃权
         */
        int against = 2;

    }
}
