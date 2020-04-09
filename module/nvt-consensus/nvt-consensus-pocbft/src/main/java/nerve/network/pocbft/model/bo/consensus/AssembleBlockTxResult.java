package nerve.network.pocbft.model.bo.consensus;
import io.nuls.base.data.Transaction;

import java.util.List;

public class AssembleBlockTxResult {
    private List<Transaction> txList;
    private boolean stateRootIsNull;

    public AssembleBlockTxResult(List<Transaction> txList, boolean stateRootIsNull){
        this.txList = txList;
        this.stateRootIsNull = stateRootIsNull;
    }

    public List<Transaction> getTxList() {
        return txList;
    }

    public void setTxList(List<Transaction> txList) {
        this.txList = txList;
    }

    public boolean isStateRootIsNull() {
        return stateRootIsNull;
    }

    public void setStateRootIsNull(boolean stateRootIsNull) {
        this.stateRootIsNull = stateRootIsNull;
    }
}
