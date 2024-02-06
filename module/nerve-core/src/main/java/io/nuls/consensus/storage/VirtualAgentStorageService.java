package io.nuls.consensus.storage;
import io.nuls.consensus.model.po.VirtualAgentPo;

public interface VirtualAgentStorageService {
    /**
     * Save Virtual Bank Node
     *
     * @param  virtualAgentPo   Virtual Bank Node Object
     * @param  height           height
     * @return boolean
     * */
    boolean save(VirtualAgentPo virtualAgentPo, long height);

    /**
     * Query virtual bank information corresponding to the specified height
     * @param height  height
     * @return VirtualAgentPo
     * */
    VirtualAgentPo get(long height);
}
