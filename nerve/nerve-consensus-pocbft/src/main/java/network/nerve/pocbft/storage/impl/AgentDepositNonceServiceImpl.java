package network.nerve.pocbft.storage.impl;
import network.nerve.pocbft.model.po.nonce.AgentDepositNoncePo;
import network.nerve.pocbft.storage.AgentDepositNonceService;
import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.pocbft.constant.ConsensusConstant;

@Component
public class AgentDepositNonceServiceImpl implements AgentDepositNonceService {

    @Override
    public boolean save(NulsHash agentHash, AgentDepositNoncePo po, int chainID) {
        try {
            byte[] value = po.serialize();
            return RocksDBService.put(ConsensusConstant.DB_NAME_AGENT_DEPOSIT_NONCE + ConsensusConstant.SEPARATOR  + chainID,agentHash.getBytes(),value);
        }catch (Exception e){
            Log.error(e);
            return  false;
        }
    }

    @Override
    public AgentDepositNoncePo get(NulsHash agentHash, int chainID) {
        try {
            byte[] value = RocksDBService.get(ConsensusConstant.DB_NAME_AGENT_DEPOSIT_NONCE+ConsensusConstant.SEPARATOR +chainID,agentHash.getBytes());
            if(value == null){
                return  null;
            }
            AgentDepositNoncePo po = new AgentDepositNoncePo();
            po.parse(value,0);
            return po;
        }catch (Exception e){
            Log.error(e);
            return  null;
        }
    }

    @Override
    public boolean delete(NulsHash agentHash, int chainID) {
        try {
            return RocksDBService.delete(ConsensusConstant.DB_NAME_AGENT_DEPOSIT_NONCE+ConsensusConstant.SEPARATOR +chainID,agentHash.getBytes());
        }catch (Exception e){
            Log.error(e);
            return  false;
        }
    }
}
