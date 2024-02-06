package io.nuls.consensus.service.impl;

import io.nuls.base.data.NulsHash;
import io.nuls.consensus.utils.manager.BlockManager;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.consensus.v1.message.GetVoteResultMessage;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.dto.input.ValidBlockDTO;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.rpc.call.NetWorkCall;
import io.nuls.consensus.service.BlockService;
import io.nuls.consensus.utils.ConsensusAwardUtil;
import io.nuls.consensus.utils.validator.BlockValidator;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Block;
import io.nuls.base.data.BlockHeader;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.consensus.constant.CommandConstant;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.v1.message.VoteResultMessage;
import org.bouncycastle.util.encoders.Hex;

import static io.nuls.consensus.constant.ParameterConstant.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consensus moduleRPCInterface implementation class
 * Consensus Module RPC Interface Implementation Class
 *
 * @author tag
 * 2018/11/7
 */
@Component
public class BlockServiceImpl implements BlockService {

    @Autowired
    private ChainManager chainManager;

    @Autowired
    private BlockManager blockManager;

    @Autowired
    private BlockValidator blockValidator;

    /**
     * Cache the latest block
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result addBlock(Map<String, Object> params) {
        if (params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_BLOCK_HEADER) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            String headerHex = (String) params.get(PARAM_BLOCK_HEADER);
            BlockHeader header = new BlockHeader();
            header.parse(RPCUtil.decode(headerHex), 0);
            int download = (Integer) params.get(PARAM_DOWN_LOAD);
            blockManager.addNewBlock(chain, header, download);
            Map<String, Object> validResult = new HashMap<>(2);
            validResult.put(PARAM_RESULT_VALUE, true);
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(validResult);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        }
    }

    /**
     * Chain fork block rollback
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result chainRollBack(Map<String, Object> params) {
        if (params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_HEIGHT) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        int height = (Integer) params.get(PARAM_HEIGHT);
        blockManager.chainRollBack(chain, height);
        Map<String, Object> validResult = new HashMap<>(2);
        validResult.put(PARAM_RESULT_VALUE, true);
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(validResult);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result receiveHeaderList(Map<String, Object> params) {
        if (params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_HEADER_LIST) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            List<String> headerList = (List<String>) params.get(PARAM_HEADER_LIST);
            List<BlockHeader> blockHeaderList = new ArrayList<>();
            for (String header : headerList) {
                BlockHeader blockHeader = new BlockHeader();
                blockHeader.parse(RPCUtil.decode(header), 0);
                blockHeaderList.add(blockHeader);
            }
            List<BlockHeader> localBlockHeaders = chain.getBlockHeaderList();
            localBlockHeaders.addAll(0, blockHeaderList);
            Map<String, Object> validResult = new HashMap<>(2);
            validResult.put(PARAM_RESULT_VALUE, true);
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(validResult);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        }
    }

    /**
     * Verify block correctness
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result validBlock(Map<String, Object> params) {
        if (params == null) {
            Log.info("Parameter error：param_error");
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }

        ValidBlockDTO dto = JSONUtils.map2pojo(params, ValidBlockDTO.class);
        if (dto.getChainId() <= MIN_VALUE || dto.getBlock() == null) {
            Log.info("Parameter error：param_error");
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }

        int chainId = dto.getChainId();
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        /*
         * 0In block downloading,1Received the latest block
         * */
        String blockHex = dto.getBlock();
        Map<String, Object> validResult = new HashMap<>(2);
        validResult.put(PARAM_RESULT_VALUE, false);
        Block block = new Block();
        try {
            block.parse(new NulsByteBuffer(RPCUtil.decode(blockHex)));
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode()).setData(validResult);
        }

//        chain.getLogger().info("{}-basic:{},pbft:{}", block.getHeader().getHeight(), dto.isBasicVerify(), dto.isByzantineVerify());

        if (dto.isBasicVerify()) {
            boolean settleConsensusAward = ConsensusAwardUtil.settleConsensusAward(chain, block.getHeader().getTime());
            try {
                //chain.getLogger().debug("Received block verification message,hash:{}", block.getHeader().getHash());
                blockValidator.validate(chain, block, settleConsensusAward);
                //chain.getLogger().debug("The basic verification of the block is completed, and the verification of the block transaction begins,hash:{}", block.getHeader().getHash());
                Response response = CallMethodUtils.verify(chainId, block.getTxs(), block.getHeader(), chain.getBestHeader(), chain.getLogger());
                chain.getLogger().info("Block transaction verification completed,hash:{}", block.getHeader().getHash().toHex());
                if (response != null && response.isSuccess()) {
                    //If the block verification is successful, the voting letter for this round will be sent
                    if (dto.getDownload() == 1) {
                        chain.getConsensusCache().getBestBlocksVotingContainer().addBlock(chain,block.getHeader());
                    }
                } else {
                    if (settleConsensusAward) {
                        ConsensusAwardUtil.clearSettleDetails();
                    }
                    chain.getLogger().info("Block transaction validation failed!");
                    return Result.getFailed(ConsensusErrorCode.FAILED).setData(validResult);
                }
            } catch (NulsException e) {
                if (settleConsensusAward) {
                    ConsensusAwardUtil.clearSettleDetails();
                }
                chain.getLogger().error(e);
                return Result.getFailed(e.getErrorCode()).setData(validResult);
            } catch (IOException e) {
                if (settleConsensusAward) {
                    ConsensusAwardUtil.clearSettleDetails();
                }
                chain.getLogger().error(e);
                return Result.getFailed(ConsensusErrorCode.SERIALIZE_ERROR).setData(validResult);
            }
        }
        if (dto.isByzantineVerify()) {

            //chain.getLogger().debug("Perform Byzantine verification on the new block and move towards the node{}Obtain block voting information", dto.getNodeId());
            BlockHeader blockHeader = block.getHeader();

            //Retrieve the voting results from memory first, and if there are any, you need to
            VoteResultMessage result = chain.getConsensusCache().getVoteResult(blockHeader.getHash());
            if (null == result) {
                //Block Byzantine Verification
                GetVoteResultMessage getVoteResultMessage = new GetVoteResultMessage(blockHeader.getHash());
                chain.getLogger().info("Obtain voting results：" + dto.getNodeId() + ", height={},hash={}", blockHeader.getHeight(), blockHeader.getHash().toHex());
                NetWorkCall.sendToNode(chainId, getVoteResultMessage, dto.getNodeId(), CommandConstant.MESSAGE_GET_VOTE_RESULT);
            } else {
                //Notification block module, Byzantine completed
//                CallMethodUtils.noticeByzantineResult(chain, result.getHeight(), false, result.getBlockHash(), null);
                validResult.put("bzt_value", result != null);
            }
        }
        validResult.put(PARAM_RESULT_VALUE, true);

        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(validResult);
    }


    /**
     * Obtain voting results
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result getVoteResult(Map<String, Object> params) {
        int chainId = (int) params.get(PARAM_CHAIN_ID);
        Chain chain = chainManager.getChainMap().get(chainId);
        Map<String, Object> result = new HashMap<>(2);
        String hashHex = (String) params.get(PARAM_BLOCK_HASH);
        if (null == hashHex) {
            return Result.getSuccess(ConsensusErrorCode.FAILED).setData(result);
        }
        NulsHash blockHash = NulsHash.fromHex(hashHex);

        VoteResultMessage voteResultMessage = chain.getConsensusCache().getVoteResult(blockHash);

        try {
            if (null != voteResultMessage) {
                result.put("voteResult", HexUtil.encode(voteResultMessage.serialize()));
            }
        } catch (IOException e) {
            chain.getLogger().error(e);
            return Result.getSuccess(ConsensusErrorCode.FAILED).setData(result);
        }

        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
    }

    /**
     * Push block voting results
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result noticeVoteResult(Map<String, Object> params) {
        int chainId = (int) params.get(PARAM_CHAIN_ID);
        Chain chain = chainManager.getChainMap().get(chainId);
        Map<String, Object> result = new HashMap<>(2);
        String voteResultHex = (String) params.get("voteResult");
        if (null == voteResultHex) {
            return Result.getSuccess(ConsensusErrorCode.FAILED).setData(result);
        }
        VoteResultMessage voteResultMessage = new VoteResultMessage();
        try {
            voteResultMessage.parse(Hex.decode(voteResultHex), 0);
            chain.getConsensusCache().getVoteResultQueue().offer(voteResultMessage);
            result.put("result", true);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return Result.getSuccess(ConsensusErrorCode.FAILED).setData(result);
        }

        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
    }
}
