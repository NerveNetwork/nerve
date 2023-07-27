package io.nuls.block.utils;

import io.nuls.base.data.NulsHash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
public class VoteResultCache {

    private List<NulsHash> hashList = new ArrayList<>();
    private Map<NulsHash, byte[]> map = new HashMap<>();

    public void cache(NulsHash blockHash, byte[] voteResult) {
        map.put(blockHash,voteResult);
        hashList.add(blockHash);
        while (hashList.size()>50){
            NulsHash hash = hashList.remove(0);
            map.remove(hash);
        }
    }

    public byte[] get(NulsHash blockHash){
        return map.get(blockHash);
    }
}
