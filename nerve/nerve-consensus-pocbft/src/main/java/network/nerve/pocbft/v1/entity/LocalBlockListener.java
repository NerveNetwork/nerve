package network.nerve.pocbft.v1.entity;

import io.nuls.base.data.BlockHeader;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.v1.VoteController;

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
            //第一次变化时，进行投票，之后变化应该不用管
//            log.info("区块投票：{}-{}", header.getHeight(), header.getHash().toHex());
            this.voteController.doVote(header);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }
}
