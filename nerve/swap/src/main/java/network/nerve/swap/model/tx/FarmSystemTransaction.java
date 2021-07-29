package network.nerve.swap.model.tx;

import io.nuls.core.constant.TxType;

/**
 * @author Niels
 */
public class FarmSystemTransaction extends SystemBaseTransaction{

    public FarmSystemTransaction(String orginTxHash, long time) {
        super(orginTxHash);
        setTime(time);
        setTxType(TxType.FARM_SYSTEM_TX);
    }
}
