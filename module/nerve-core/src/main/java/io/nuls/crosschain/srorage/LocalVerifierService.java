package io.nuls.crosschain.srorage;

import io.nuls.crosschain.model.po.LocalVerifierPO;

/**
 * Local validator list related operations
 * Local verifier list related operations
 *
 * @author  tag
 * 2020/4/26
 * */
public interface LocalVerifierService {
    /**
     * preserve
     * @param po                po
     * @param chainID           chainID
     * @return                  Whether the save was successful or not
     * */
    boolean save(LocalVerifierPO po, int chainID);

    /**
     * query
     * @param chainID   chainID
     * @return          Highly Corresponding TransactionsHashlist
     * */
    LocalVerifierPO get(int chainID);

    /**
     * delete
     * @param chainID   chainID
     * @return          Whether the deletion was successful or not
     * */
    boolean delete(int chainID);
}
