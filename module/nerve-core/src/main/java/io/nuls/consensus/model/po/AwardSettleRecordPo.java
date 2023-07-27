package io.nuls.consensus.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AwardSettleRecordPo extends BaseNulsData {
    /**
     * 已结算的共识奖励记录已结算的共识奖励记录
     */
    private List<AwardSettlePo> recordList;
    /**
     * 当前待结算的共识奖励结算信息
     */
    private AwardSettlePo lastestSettleResult;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        int count = recordList == null ? 0 : recordList.size();
        stream.writeVarInt(count);
        if (null != recordList) {
            for (AwardSettlePo awardPo : recordList) {
                stream.writeNulsData(awardPo);
            }
        }
        stream.writeNulsData(lastestSettleResult);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        int count = (int) byteBuffer.readVarInt();
        if (0 < count) {
            List<AwardSettlePo> recordList = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                recordList.add(byteBuffer.readNulsData(new AwardSettlePo()));
            }
            this.recordList = recordList;
        }
        lastestSettleResult = byteBuffer.readNulsData(new AwardSettlePo());
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfVarInt(recordList == null ? 0 : recordList.size());
        if (null != recordList) {
            for (AwardSettlePo awardPo : recordList) {
                size += SerializeUtils.sizeOfNulsData(awardPo);
            }
        }
        size += SerializeUtils.sizeOfNulsData(lastestSettleResult);
        return size;
    }

    public List<AwardSettlePo> getRecordList() {
        return recordList;
    }

    public void setRecordList(List<AwardSettlePo> recordList) {
        this.recordList = recordList;
    }

    public AwardSettlePo getLastestSettleResult() {
        return lastestSettleResult;
    }

    public void setLastestSettleResult(AwardSettlePo lastestSettleResult) {
        this.lastestSettleResult = lastestSettleResult;
    }


}
