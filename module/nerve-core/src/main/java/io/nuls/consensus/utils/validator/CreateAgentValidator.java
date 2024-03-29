package io.nuls.consensus.utils.validator;

import io.nuls.base.data.BlockHeader;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.consensus.utils.ConsensusNetUtil;
import io.nuls.consensus.utils.manager.PunishManager;
import io.nuls.consensus.utils.validator.base.BaseValidator;
import io.nuls.consensus.utils.manager.AgentManager;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.consensus.constant.ConsensusErrorCode;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Create a node transaction validator
 * @author  tag
 * */
@Component
public class CreateAgentValidator extends BaseValidator {
    @Autowired
    private PunishManager punishManager;
    @Autowired
    private AgentManager agentManager;
    @Override
    public Result validate(Chain chain, Transaction tx, BlockHeader blockHeader){
        try {
            if (tx.getTxData() == null) {
                chain.getLogger().error("CreateAgent -- TxData is null");
                return Result.getFailed(ConsensusErrorCode.AGENT_NOT_EXIST);
            }
            Agent agent = new Agent();
            agent.parse(tx.getTxData(), 0);
            //Node basic verification
            Result rs = createAgentBasicValid(chain, tx, agent);
            if (rs.isFailed()) {
                return rs;
            }
            //Node address verification
            rs = createAgentAddrValid(chain, tx, agent);
            if (rs.isFailed()) {
                return rs;
            }
            //coinDatavalidate
            CoinData coinData = new CoinData();
            coinData.parse(tx.getCoinData(), 0);
            rs = appendDepositCoinDataValid(chain, agent.getDeposit(), coinData, agent.getAgentAddress());
            if (rs.isFailed()) {
                return rs;
            }
            //Verify if the handling fee is sufficient
            rs = validFee(chain, coinData, tx);
            if (rs.isFailed()) {
                return rs;
            }
        }catch (NulsException | IOException e){
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.SERIALIZE_ERROR);
        }
        return ConsensusNetUtil.getSuccess();
    }

    /**
     * Create node transaction basic verification
     * Create agent transaction base validation
     *
     * @param chain chainID/chain id
     * @param tx    Create node transactions/create transaction
     * @param agent node/agent
     * @return Result
     */
    private Result createAgentBasicValid(Chain chain, Transaction tx, Agent agent){
        if (!AddressTool.validNormalAddress(agent.getPackingAddress(), chain.getConfig().getChainId())) {
            chain.getLogger().error("CreateAgent -- PackingAddress error");
            return Result.getFailed(ConsensusErrorCode.ADDRESS_ERROR);
        }
        if (Arrays.equals(agent.getAgentAddress(), agent.getPackingAddress())) {
            chain.getLogger().error("CreateAgent -- AgentAddress is the same as  packingAddress");
            return Result.getFailed(ConsensusErrorCode.AGENTADDR_AND_PACKING_SAME);
        }
        if (Arrays.equals(agent.getRewardAddress(), agent.getPackingAddress())) {
            chain.getLogger().error("CreateAgent -- RewardAddress is the same as  packingAddress");
            return Result.getFailed(ConsensusErrorCode.REWARDADDR_AND_PACKING_SAME);
        }
        if (tx.getTime() <= 0) {
            chain.getLogger().error("CreateAgent -- Transaction creation time error");
            return Result.getFailed(ConsensusErrorCode.DATA_ERROR);
        }
        BigInteger deposit = agent.getDeposit();
        if (deposit.compareTo(chain.getConfig().getDepositMin()) < 0 || deposit.compareTo(chain.getConfig().getDepositMax()) > 0) {
            chain.getLogger().error("CreateAgent -- The mortgage exceeds the boundary value");
            return Result.getFailed(ConsensusErrorCode.DEPOSIT_OUT_OF_RANGE);
        }
        return ConsensusNetUtil.getSuccess();
    }

    /**
     * Create node transaction node address and block out address verification
     * address validate
     *
     * @param chain chainID/chain id
     * @param tx    Create node transactions/create transaction
     * @param agent node/agent
     * @return boolean
     */
    private Result createAgentAddrValid(Chain chain, Transaction tx, Agent agent) {
        if (!chain.getSeedAddressList().isEmpty()) {
            byte[] nodeAddressBytes;
            //Node address and block address cannot be seed nodes
            for (String nodeAddress : chain.getSeedAddressList()) {
                nodeAddressBytes = AddressTool.getAddress(nodeAddress);
                if (Arrays.equals(nodeAddressBytes, agent.getAgentAddress())) {
                    chain.getLogger().error("CreateAgent -- AgentAddress is seedNode address");
                    return Result.getFailed(ConsensusErrorCode.AGENT_EXIST);
                }
                if (Arrays.equals(nodeAddressBytes, agent.getPackingAddress())) {
                    chain.getLogger().error("CreateAgent -- PackingAddress is seedNode address");
                    return Result.getFailed(ConsensusErrorCode.AGENT_PACKING_EXIST);
                }
            }
        }
        //Node address and block address cannot be duplicated
        List<Agent> agentList = agentManager.getAgentList(chain, chain.getBestHeader().getHeight());
        if (agentList != null && agentList.size() > 0) {
            Set<String> set = new HashSet<>();
            for (Agent agentTemp : agentList) {
                if (agentTemp.getTxHash().equals(tx.getHash())) {
                    chain.getLogger().error("CreateAgent -- Transaction already exists");
                    return Result.getFailed(ConsensusErrorCode.TRANSACTION_REPEATED);
                }
                set.add(HexUtil.encode(agentTemp.getAgentAddress()));
                set.add(HexUtil.encode(agentTemp.getPackingAddress()));
            }
            boolean b = set.contains(HexUtil.encode(agent.getAgentAddress()));
            if (b) {
                chain.getLogger().error("CreateAgent -- AgentAddress address is already in use");
                return Result.getFailed(ConsensusErrorCode.AGENT_EXIST);
            }
            b = set.contains(HexUtil.encode(agent.getPackingAddress()));
            if (b) {
                chain.getLogger().error("CreateAgent -- Agent packaging address is already in use");
                return Result.getFailed(ConsensusErrorCode.AGENT_PACKING_EXIST);
            }
        }
        long count = punishManager.getRedPunishCount(chain, agent.getAgentAddress());
        if (count > 0) {
            chain.getLogger().error("CreateAgent -- Red card address cannot create node again");
            return Result.getFailed(ConsensusErrorCode.LACK_OF_CREDIT);
        }
        return ConsensusNetUtil.getSuccess();
    }
}
