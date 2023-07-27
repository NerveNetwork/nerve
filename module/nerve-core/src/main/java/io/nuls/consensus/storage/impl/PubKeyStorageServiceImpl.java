package io.nuls.consensus.storage.impl;

import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.po.PubKeyPo;
import io.nuls.consensus.storage.PubKeyStorageService;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.consensus.constant.ConsensusConstant;

@Component
public class PubKeyStorageServiceImpl implements PubKeyStorageService {

    @Override
    public boolean save(PubKeyPo po, Chain chain) {
        try {
            return RocksDBService.put(ConsensusConstant.DB_NAME_PUB_KEY, ByteUtils.intToBytes(chain.getChainId()), po.serialize());
        }catch (Exception e){
            chain.getLogger().error(e);
        }
        return false;
    }

    @Override
    public PubKeyPo get(Chain chain) {
        byte[] value = RocksDBService.get(ConsensusConstant.DB_NAME_PUB_KEY,ByteUtils.intToBytes(chain.getChainId()));
        PubKeyPo po = new PubKeyPo();
        try {
            po.parse(value,0);
        }catch (Exception e){
            chain.getLogger().error(e);
        }
        return po;
    }
}
