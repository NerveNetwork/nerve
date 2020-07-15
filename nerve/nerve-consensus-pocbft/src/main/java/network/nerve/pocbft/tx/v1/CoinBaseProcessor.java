package network.nerve.pocbft.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.pocbft.service.impl.RandomSeedService;
import network.nerve.pocbft.utils.manager.ChainManager;

import java.util.List;
import java.util.Map;

/**
 * CoinBase交易处理器
 * @author tag
 * @date 2019/6/1
 */
@Component("CoinBaseProcessorV1")
public class CoinBaseProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private RandomSeedService randomSeedService;

    @Override
    public int getType() {
        return TxType.COIN_BASE;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        return null;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return true;
    }
}
