package nerve.network.pocbft.storage;
import io.nuls.base.data.NulsHash;
import nerve.network.pocbft.model.po.nonce.AgentDepositNoncePo;

public interface AgentDepositNonceService {
    /**
     * 节点追加/退出保证金的Nonce数据
     *
     * @param  agentHash    节点HASH
     * @param chainID    链ID/chain id
     * @return boolean
     * */
    boolean save(NulsHash agentHash, AgentDepositNoncePo po, int chainID);

    /**
     * 获取指定节点保证金Nonce相关信息
     *
     * @param  agentHash    节点HASH
     * @param chainID       链ID/chain id
     * */
    AgentDepositNoncePo get(NulsHash agentHash, int chainID);

    /**
     * 删除指定账户保证金Nonce信息
     *
     * @param agentHash    节点HASH
     * @param chainID      链ID/chain id
     * @return boolean
     * */
    boolean delete(NulsHash agentHash, int chainID);
}
