package io.nuls.consensus.storage;

import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.po.PubKeyPo;

public interface PubKeyStorageService {
    /**
     * preserve
     * @param po     Public key data
     * @param chain  Chain information
     * @return  Whether the save was successful
     * */
    boolean save(PubKeyPo po, Chain chain);

    /**
     * Obtain chain public key information
     * @param chain Chain information
     * @return      List of public keys in this chain
     * */
    PubKeyPo get(Chain chain);
}
