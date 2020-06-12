package network.nerve.pocbft.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.pocbft.constant.ConsensusConstant;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PubKeyPo extends BaseNulsData {
    private Map<String, byte[]> packAddressPubKeyMap;

    public  PubKeyPo(){
        packAddressPubKeyMap = new HashMap<>();
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        int count = packAddressPubKeyMap == null ? 0 : packAddressPubKeyMap.size();
        stream.writeVarInt(count);
        if(count > 0){
            for (Map.Entry<String,byte[]> entry : packAddressPubKeyMap.entrySet()) {
                stream.writeString(entry.getKey());
                stream.write(entry.getValue().length);
                stream.write(entry.getValue());
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        int count = (int) byteBuffer.readVarInt();
        if(count > 0){
            Map<String, byte[]> packAddressPubKeyMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_16);
            for (int i = 0; i < count; i++) {
                String key = byteBuffer.readString();
                int length = byteBuffer.readByte();
                byte[] value = byteBuffer.readBytes(length);
                packAddressPubKeyMap.put(key, value);
            }
            this.packAddressPubKeyMap = packAddressPubKeyMap;
        }
    }

    @Override
    public int size() {
        int count = packAddressPubKeyMap == null ? 0 : packAddressPubKeyMap.size();
        int size = SerializeUtils.sizeOfVarInt(count);
        if(count > 0){
            for (Map.Entry<String,byte[]> entry : packAddressPubKeyMap.entrySet()) {
                size += SerializeUtils.sizeOfString(entry.getKey());
                size += 1;
                size += entry.getValue().length;
            }
        }
        return size;
    }

    public Map<String, byte[]> getPackAddressPubKeyMap() {
        return packAddressPubKeyMap;
    }

    public void setPackAddressPubKeyMap(Map<String, byte[]> packAddressPubKeyMap) {
        this.packAddressPubKeyMap = packAddressPubKeyMap;
    }
}
