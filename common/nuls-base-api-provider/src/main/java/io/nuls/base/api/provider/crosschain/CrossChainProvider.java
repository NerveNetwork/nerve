package io.nuls.base.api.provider.crosschain;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.crosschain.facade.*;

import java.util.List;

/**
 * @Author: zhoulijun
 * @Time: 2019-05-06 17:01
 * @Description: Function Description
 */
public interface CrossChainProvider {

    /**
     * Create a cross chain transaction
     * @param req
     * @return
     */
    Result<String> createCrossTx(CreateCrossTxReq req);


    /**
     * Query the processing status of cross chain transactions in other chains
     * @param req
     * @return
     */
    Result<Integer> getCrossTxState(GetCrossTxStateReq req);

    Result<List> getRegisteredChainInfoList();

    /**
     * Send a signal to the entire network to re validate the designated cross chain transactions through the Byzantine Court
     * @param req
     * @return
     */
    Result<String> rehandleCtx(RehandleCtxReq req);
}
