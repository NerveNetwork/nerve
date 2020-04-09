package io.nuls.base.api.provider.consensus;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.consensus.facade.*;
import io.nuls.base.api.provider.transaction.facade.MultiSignTransferRes;

import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-11 11:43
 * @Description:
 * consensus provider
 */
public interface TestNetProvider {


    Result<Boolean> initNet(InitNet req);
    Result<Boolean> cleanNet(UpdateNet req);
    Result<Boolean> updateNet(UpdateNet req);


}
