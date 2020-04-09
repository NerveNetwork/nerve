package nerve.network.converter.v1.proposal;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import nerve.network.converter.config.ConverterContext;
import nerve.network.converter.constant.ProposalConstant;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.txdata.ProposalTxData;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.model.StringUtils;

/**
 * 提案交易的单交易验证器
 *
 * @author Niels
 */
public class ProposalValidater {

    public static boolean validate(ProposalTxData txData, CoinData coinData, Chain chain) {
        NulsLogger log = chain.getLogger();
        if (StringUtils.isBlank(txData.getContent())) {
            log.warn("the content of the proposal can't be null.");
            return false;
        }

        if (coinData.getTo() == null || coinData.getTo().isEmpty()) {
            log.warn("proposal tx fee is wrong.");
            return false;
        }
        boolean feeOk = false;
        for (CoinTo to : coinData.getTo()) {
            if (ArraysTool.arrayEquals(to.getAddress(), AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId())) && ConverterContext.PROPOSAL_PRICE.compareTo(to.getAmount()) <= 0) {
                feeOk = true;
                break;
            }
        }
        if (!feeOk) {
            log.warn("proposal tx fee is wrong.");
            return false;
        }
        if (txData.getType() == ProposalConstant.TYPE_WORKORDER_RETURN && StringUtils.isBlank(txData.getHeterogeneousTxHash())) {
            // 如果是充值退回类型，必须保证输入了原始充值交易的hash
            log.warn("the heterogeneous tx hash can't be null.");
            return false;
        }

        // 充值失败转发类型
        if (txData.getType() == ProposalConstant.TYPE_WORKORDER_FORWARD) {
            boolean addressRight = AddressTool.validNormalAddress(txData.getAddress(), chain.getChainId());
//            如果转发地址不是本链、普通地址，则不通过
            if (!addressRight) {
                log.warn("the address is not right.");
                return false;
            }
//            必须有原始充值交易的hash
            if (StringUtils.isBlank(txData.getHeterogeneousTxHash())) {
                log.warn("the heterogeneous tx hash can't be null.");
                return false;
            }
        }
//        冻结账户类型的交易
        if (txData.getType() == ProposalConstant.TYPE_FROZEN_ACCOUNT || txData.getType() == ProposalConstant.TYPE_THAW_ACCOUNT) {
            boolean addressRight = isLocalAccount(txData.getAddress(), chain.getChainId());
//            如果冻结地址不是本链地址，则不通过
            if (!addressRight) {
                log.warn("the address is not right.");
                return false;
            }
        }
        if (txData.getType() == ProposalConstant.TYPE_DISQUALIFICATION_OF_BANK) {
            //地址必须是银行地址
            String agentAddress = AddressTool.getStringAddressByBytes(txData.getAddress());
            // 判断不是虚拟银行成员
            if (!chain.isVirtualBankByAgentAddr(agentAddress)) {
                log.warn("the address is not right.");
                return false;
            }
        }
        return true;
    }

    private static boolean isLocalAccount(byte[] bytes, int chainId) {
        return AddressTool.validAddress(bytes, chainId, (byte) 0);
    }
}
