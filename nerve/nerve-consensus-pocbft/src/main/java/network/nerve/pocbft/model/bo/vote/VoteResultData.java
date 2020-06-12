package network.nerve.pocbft.model.bo.vote;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VoteResultData extends BaseNulsData {
    private List<VoteResultItem> voteResultItemList = new ArrayList<>();

    /***
     * 验证是否成功
     * true表示投票成功，false表示进入下一轮投票（在第二阶段投票时才需要该字段）
     */
    private boolean resultSuccess;

    /**
     * 确认的是否为空块
     * */
    private boolean confirmedEmpty;
    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        int itemCount = voteResultItemList == null ? 0 : voteResultItemList.size();
        stream.writeVarInt(itemCount);
        if(voteResultItemList != null){
            for (VoteResultItem voteResultItem:voteResultItemList) {
                stream.writeNulsData(voteResultItem);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        int voteItemCount = (int) byteBuffer.readVarInt();
        if(voteItemCount > 0){
            List<VoteResultItem> voteResultItemList = new ArrayList<>();
            for (int i = 0; i < voteItemCount; i++) {
                voteResultItemList.add(byteBuffer.readNulsData(new VoteResultItem()));
            }
            this.voteResultItemList = voteResultItemList;
        }
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfVarInt(voteResultItemList == null ? 0 : voteResultItemList.size());
        if (null != voteResultItemList) {
            for (VoteResultItem voteResultItem:voteResultItemList) {
                size += SerializeUtils.sizeOfNulsData(voteResultItem);
            }
        }
        return size;
    }

    public List<VoteResultItem> getVoteResultItemList() {
        return voteResultItemList;
    }

    public void setVoteResultItemList(List<VoteResultItem> voteResultItemList) {
        this.voteResultItemList = voteResultItemList;
    }

    public boolean isResultSuccess() {
        return resultSuccess;
    }

    public void setResultSuccess(boolean resultSuccess) {
        this.resultSuccess = resultSuccess;
    }

    public boolean isConfirmedEmpty() {
        return confirmedEmpty;
    }

    public void setConfirmedEmpty(boolean confirmedEmpty) {
        this.confirmedEmpty = confirmedEmpty;
    }

    public VoteResultItem getVoteResultItem(){
        return  voteResultItemList.get(0);
    }
}
