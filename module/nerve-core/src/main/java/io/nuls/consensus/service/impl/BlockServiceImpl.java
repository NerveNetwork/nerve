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
 * 共识模块RPC接口实现类
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
     * 缓存最新区块
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
     * 链分叉区块回滚
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
     * 验证区块正确性
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result validBlock(Map<String, Object> params) {
        if (params == null) {
            Log.info("参数错误：param_error");
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }

        ValidBlockDTO dto = JSONUtils.map2pojo(params, ValidBlockDTO.class);
        if (dto.getChainId() <= MIN_VALUE || dto.getBlock() == null) {
            Log.info("参数错误：param_error");
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }

        int chainId = dto.getChainId();
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        /*
         * 0区块下载中，1接收到最新区块
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
                //chain.getLogger().debug("接收到区块验证消息，hash:{}", block.getHeader().getHash());
                blockValidator.validate(chain, block, settleConsensusAward);
                //chain.getLogger().debug("区块基础验证完成，开始验证区块交易，hash:{}", block.getHeader().getHash());
                Response response = CallMethodUtils.verify(chainId, block.getTxs(), block.getHeader(), chain.getBestHeader(), chain.getLogger());
                chain.getLogger().info("区块交易验证完成，hash:{}", block.getHeader().getHash().toHex());
                if (response != null && response.isSuccess()) {
                    //区块验证成功，则将本轮次投票信
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

            //chain.getLogger().debug("对新区块做拜占庭验证，向节点{}获取区块投票信息", dto.getNodeId());
            BlockHeader blockHeader = block.getHeader();

            //从内存先获取投票结果，如果有，要
            VoteResultMessage result = chain.getConsensusCache().getVoteResult(blockHeader.getHash());
            if (null == result) {
                //区块拜占庭验证
                GetVoteResultMessage getVoteResultMessage = new GetVoteResultMessage(blockHeader.getHash());
                chain.getLogger().info("获取投票结果：" + dto.getNodeId() + ", height={},hash={}", blockHeader.getHeight(), blockHeader.getHash().toHex());
                NetWorkCall.sendToNode(chainId, getVoteResultMessage, dto.getNodeId(), CommandConstant.MESSAGE_GET_VOTE_RESULT);
            } else {
                //通知区块模块，拜占庭完成
//                CallMethodUtils.noticeByzantineResult(chain, result.getHeight(), false, result.getBlockHash(), null);
                validResult.put("bzt_value", result != null);
            }
        }
        validResult.put(PARAM_RESULT_VALUE, true);

        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(validResult);
    }


    /**
     * 获取投票结果
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
     * 推送区块投票结果
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
