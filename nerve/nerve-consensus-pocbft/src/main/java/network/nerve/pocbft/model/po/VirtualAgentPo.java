package network.nerve.pocbft.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VirtualAgentPo extends BaseNulsData {
    private long height;
    private List<String> virtualAgentList;

    public VirtualAgentPo(){ }

    public VirtualAgentPo(long height, List<String> virtualAgentList){
        this.height = height;
        this.virtualAgentList = virtualAgentList;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeVarInt(height);
        int count = virtualAgentList == null ? 0 : virtualAgentList.size();
        stream.writeVarInt(count);
        if(virtualAgentList != null){
            for (String virtualAgent:virtualAgentList) {
                stream.writeString(virtualAgent);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.height = byteBuffer.readVarInt();
        int count = (int) byteBuffer.readVarInt();
        if(count > 0){
            List<String> virtualAgentList = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                virtualAgentList.add(byteBuffer.readString());
            }
            this.virtualAgentList = virtualAgentList;
        }
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfVarInt(height);
        size += SerializeUtils.sizeOfVarInt(virtualAgentList == null ? 0 : virtualAgentList.size());
        if(virtualAgentList != null){
            for (String virtualAgent:virtualAgentList) {
                size += SerializeUtils.sizeOfString(virtualAgent);
            }
        }
        return size;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public List<String> getVirtualAgentList() {
        return virtualAgentList;
    }

    public void setVirtualAgentList(List<String> virtualAgentList) {
        this.virtualAgentList = virtualAgentList;
    }
}
