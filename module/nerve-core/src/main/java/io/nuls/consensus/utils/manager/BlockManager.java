package io.nuls.consensus.utils.manager;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.utils.ConsensusAwardUtil;
import io.nuls.consensus.utils.compare.BlockHeaderComparator;
import io.nuls.consensus.v1.RoundController;

import java.util.Iterator;
import java.util.List;

/**
 * Chain blockchain management
 * Chain Block Management Class
 *
 * @author tag
 * 2018/12/20
 */
@Component
public class BlockManager {

    @Autowired
    private PunishManager punishManager;

    @Autowired
    private AgentManager agentManager;

    private Long lastHeight;

    /**
     * Received the latest block header and updated the blockchain cache data
     * Receive the latest block header, update the chain block cache entity
     *
     * @param chain       chain info
     * @param blockHeader block header
     */
    public void addNewBlock(Chain chain, BlockHeader blockHeader, int download) {

        if (null != lastHeight && lastHeight >= blockHeader.getHeight()) {
            return;
        }
        lastHeight = blockHeader.getHeight();

        //Save consensus reward settlement information
        ConsensusAwardUtil.saveAndSwitchSettleRecord(chain, blockHeader);
        /*
        If there is a round change in the newly added block, delete the minimum round block
         */
        BlockHeader newestHeader = chain.getBestHeader();
        BlockExtendsData newestExtendsData = newestHeader.getExtendsData();
        BlockExtendsData receiveExtendsData = blockHeader.getExtendsData();
        long receiveRoundIndex = receiveExtendsData.getRoundIndex();
        if (chain.getBlockHeaderList().size() > 0) {
            BlockExtendsData lastExtendsData = chain.getBlockHeaderList().get(0).getExtendsData();
            long lastRoundIndex = lastExtendsData.getRoundIndex();
            if (receiveRoundIndex > newestExtendsData.getRoundIndex() && (receiveRoundIndex - ConsensusConstant.INIT_BLOCK_HEADER_COUNT > lastRoundIndex)) {
                Iterator<BlockHeader> iterator = chain.getBlockHeaderList().iterator();
                while (iterator.hasNext()) {
                    lastExtendsData = iterator.next().getExtendsData();
                    if (lastExtendsData.getRoundIndex() == lastRoundIndex) {
                        iterator.remove();
                    } else if (lastExtendsData.getRoundIndex() > lastRoundIndex) {
                        break;
                    }
                }
                //Cleaning the round cache
                punishManager.clear(chain);
            }
        }

        //Switch rounds here
        RoundController roundController = SpringLiteContext.getBean(RoundController.class);
        roundController.switchPackingIndex(receiveExtendsData.getRoundIndex(), receiveExtendsData.getRoundStartTime(),
                receiveExtendsData.getPackingIndexOfRound() + 1, blockHeader.getTime() + chain.getConfig().getPackingInterval());

        chain.getBlockHeaderList().add(blockHeader);
        chain.setBestHeader(blockHeader);
        chain.getLogger().info("Block save, with a height of：" + blockHeader.getHeight() + " , txCount: " + blockHeader.getTxCount() + ",The latest local block height is：" + chain.getBestHeader().getHeight() + ", Round:" + receiveExtendsData.getRoundIndex() + "\n\n");


        //If there is a node that has not saved its public key, save the node's public key
        if (!chain.getUnBlockAgentList().isEmpty()) {
            agentManager.setPubkey(chain, blockHeader.getBlockSignature().getPublicKey());
        }

// Prepare to modify the trigger for consensus reward calculation
//        chain.getConsensusCache().getBlockHeaderQueue().offer(blockHeader);

    }

    /**
     * Chain fork, block rollback
     * Chain bifurcation, block rollback
     *
     * @param chain  chain info
     * @param height block height
     */
    public void chainRollBack(Chain chain, int height) {
        if (height > 0) {
            throw new NulsRuntimeException(ConsensusErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        chain.getLogger().info("The height at which the block starts rolling back：" + height);
        List<BlockHeader> headerList = chain.getBlockHeaderList();
        headerList.sort(new BlockHeaderComparator());
        BlockHeader originalBlocHeader = chain.getBestHeader();
        BlockExtendsData originalExtendsData = originalBlocHeader.getExtendsData();
        long originalRound = originalExtendsData.getRoundIndex();
        BlockHeader rollBackHeader;
        for (int index = headerList.size() - 1; index >= 0; index--) {
            if (headerList.get(index).getHeight() >= height) {
                rollBackHeader = headerList.remove(index);
                ConsensusAwardUtil.rollbackAndSwitchSettleRecord(chain, rollBackHeader, headerList.get(index - 1));
            } else {
                break;
            }
        }
        chain.setBlockHeaderList(headerList);
        chain.setBestHeader(headerList.get(headerList.size() - 1));
        BlockHeader newestBlocHeader = chain.getBestHeader();
        BlockExtendsData bestExtendsData = newestBlocHeader.getExtendsData();
        long currentRound = bestExtendsData.getRoundIndex();
        //If there is a round change, after rolling back, if the local block is not enough for the specified round of blocks, it is necessary to obtain blocks from the block to make up for it and roll back the local block
        if (currentRound != originalRound) {
            BlockHeader lastestBlocHeader = chain.getBlockHeaderList().get(0);
            BlockExtendsData lastestExtendsData = lastestBlocHeader.getExtendsData();
            long minRound = lastestExtendsData.getRoundIndex();
            int localRoundCount = (int) (currentRound - minRound + 1);
            int diffRoundCount = ConsensusConstant.INIT_BLOCK_HEADER_COUNT - localRoundCount;
            if (diffRoundCount > 0) {
                try {
                    CallMethodUtils.getRoundBlockHeaders(chain, diffRoundCount, lastestBlocHeader.getHeight());
                } catch (Exception e) {
                    chain.getLogger().error(e);
                }
            }
            long roundIndex;
            //Rollback round
            if (bestExtendsData.getPackingIndexOfRound() > 1) {
                roundIndex = bestExtendsData.getRoundIndex();
            } else {
                roundIndex = bestExtendsData.getRoundIndex() - 1;
            }
//            roundManager.rollBackRound(chain, roundIndex);
        }
        chain.getLogger().info("Block rollback successful, rolled back to a height of：" + height + ",The latest local block height is：" + chain.getBestHeader().getHeight());
    }
}
