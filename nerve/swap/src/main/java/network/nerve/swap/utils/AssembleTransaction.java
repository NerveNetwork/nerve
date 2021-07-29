package network.nerve.swap.utils;

import network.nerve.swap.model.tx.BaseTransaction;

/**
 * @author Niels
 */
public class AssembleTransaction extends BaseTransaction {

    private byte[] realTxData;

    @Override
    protected BaseTransaction setTxData() {
        setTxData(realTxData);
        return this;
    }


    public AssembleTransaction(byte[] realTxData) {
        super();
        this.realTxData = realTxData;
        setTxData();
    }

    public void setTxType(int type) {
        super.setTxType(type);
    }

}
