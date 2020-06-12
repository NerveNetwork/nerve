package network.nerve.pocbft.model.bo.vote;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;

public class VoteResult {
    /***
     * 验证是否成功
     * true表示投票成功，false表示进入下一轮投票（在第二阶段投票时才需要该字段）
     */
    private boolean resultSuccess;

    /**
     * 确认的是否为空块
     * */
    private boolean confirmedEmpty;

    /**
     * 是否分叉
     * */
    private boolean bifurcate;

    /**
     * 确认的区块HASH
     * */
    private NulsHash blockHash;

    /**
     * 分叉区块头1
     * */
    private BlockHeader firstHeader;

    /**
     * 分叉区块头2
     * */
    private BlockHeader secondHeader;
}
