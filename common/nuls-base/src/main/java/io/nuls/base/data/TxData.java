package io.nuls.base.data;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.core.exception.NulsException;

import java.io.IOException;

public class TxData extends BaseNulsData {
    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {

    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {

    }

    @Override
    public int size() {
        return 0;
    }
}
