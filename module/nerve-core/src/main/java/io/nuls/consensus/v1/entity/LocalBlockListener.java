package io.nuls.consensus.v1.entity;

import io.nuls.base.data.BlockHeader;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.v1.VoteController;

/**
 * @author Eva
 */
public class LocalBlockListener extends BasicObject {

    private VoteController voteController;

    public LocalBlockListener(Chain chain, VoteController voteController) {
        super(chain);
        this.voteController = voteController;
    }

    public void onChange(BlockHeader header) {
        try {
            //During the first change, a vote should be taken, and subsequent changes should not be taken into account
//            log.info("Block voting：{}-{}", header.getHeight(), header.getHash().toHex());
            this.voteController.doVote(header);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }
}
