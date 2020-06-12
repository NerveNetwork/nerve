package network.nerve.pocbft.storage;
import network.nerve.pocbft.model.po.VirtualAgentPo;

public interface VirtualAgentStorageService {
    /**
     * 保存虚拟银行节点
     *
     * @param  virtualAgentPo   虚拟银行节点对象
     * @param  height           高度
     * @return boolean
     * */
    boolean save(VirtualAgentPo virtualAgentPo, long height);

    /**
     * 查询指定高度对应虚拟银行信息
     * @param height  高度
     * @return VirtualAgentPo
     * */
    VirtualAgentPo get(long height);
}
