package io.nuls.crosschain.srorage.imp;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.crosschain.base.model.bo.txdata.RegisteredChainMessage;
import io.nuls.crosschain.constant.NulsCrossChainConstant;
import io.nuls.crosschain.srorage.RegisteredCrossChainService;

/**
 * Registered cross chain transaction database operation implementation class
 * Registered Cross-Chain Transaction Database Operations Implementation Class
 *
 * @author  tag
 * 2019/5/30
 * */
@Component
public class RegisteredCrossChainServiceImpl implements RegisteredCrossChainService {
    private final byte[] key = NulsCrossChainConstant.DB_NAME_REGISTERED_CHAIN.getBytes();

    @Override
    public boolean save(RegisteredChainMessage registeredChainMessage) {
        try {
            return RocksDBService.put(NulsCrossChainConstant.DB_NAME_REGISTERED_CHAIN, key,registeredChainMessage.serialize());
        }catch (Exception e){
            Log.error(e);
        }
        return false;
    }

    @Override
    public RegisteredChainMessage get() {
        try {
            byte[] messageBytes = RocksDBService.get(NulsCrossChainConstant.DB_NAME_REGISTERED_CHAIN,key);
            if(messageBytes == null){
                return null;
            }
            RegisteredChainMessage registeredChainMessage = new RegisteredChainMessage();
            registeredChainMessage.parse(messageBytes,0);
            return registeredChainMessage;
        }catch (Exception e){
            Log.error(e);
        }
        return null;
    }
}
