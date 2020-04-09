package nerve.network.quotation.model.bo;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

public class ConfigBean extends BaseNulsData {
    private int chainId;
    private int assetId;


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(chainId);
        stream.writeUint16(assetId);
//        stream.writeString(dataPath);
//        stream.writeString(language);
//        stream.writeUint16(effectiveQuotation);
//        stream.writeString(quoteStartHm);
//        stream.writeString(quoteEndHm);
//        stream.writeByte(nerveBasedNuls);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.chainId = byteBuffer.readUint16();
        this.assetId = byteBuffer.readUint16();
//        this.dataPath = byteBuffer.readString();
//        this.language = byteBuffer.readString();
//        this.effectiveQuotation = byteBuffer.readUint16();
//        this.quoteStartHm = byteBuffer.readString();
//        this.quoteEndHm = byteBuffer.readString();
//        this.nerveBasedNuls = byteBuffer.readByte();
    }

    @Override
    public int size() {
        int size = 2 * SerializeUtils.sizeOfUint16();
//        size += SerializeUtils.sizeOfString(this.dataPath);
//        size += SerializeUtils.sizeOfString(this.language);
//        size += SerializeUtils.sizeOfUint16();
//        size += SerializeUtils.sizeOfString(this.quoteStartHm);
//        size += SerializeUtils.sizeOfString(this.quoteEndHm);
//        size += 1;
        return  size;
    }


    public ConfigBean() {
    }

    public ConfigBean(int chainId, int assetId) {
        this.chainId = chainId;
        this.assetId = assetId;
    }
    //    public ConfigBean(int chainId, int assetId, String dataPath, String language, int effectiveQuotation, String quoteStartHm, String quoteEndHm) {
//        this.chainId = chainId;
//        this.assetId = assetId;
//        this.dataPath = dataPath;
//        this.language = language;
//        this.effectiveQuotation = effectiveQuotation;
//        this.quoteStartHm = quoteStartHm;
//        this.quoteEndHm = quoteEndHm;
//    }

//    public int getEffectiveQuotation() {
//        return effectiveQuotation;
//    }
//
//    public void setEffectiveQuotation(int effectiveQuotation) {
//        this.effectiveQuotation = effectiveQuotation;
//    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

//    public String getDataPath() {
//        return dataPath;
//    }
//
//    public void setDataPath(String dataPath) {
//        this.dataPath = dataPath;
//    }
//
//    public void setQuoteStartHm(String quoteStartHm) {
//        this.quoteStartHm = quoteStartHm;
//    }
//
//    public void setQuoteEndHm(String quoteEndHm) {
//        this.quoteEndHm = quoteEndHm;
//    }
//
//    public String getQuoteStartHm() {
//        return quoteStartHm;
//    }
//
//    public String getQuoteEndHm() {
//        return quoteEndHm;
//    }
//
//    public byte getNerveBasedNuls() {
//        return nerveBasedNuls;
//    }
//
//    public void setNerveBasedNuls(byte nerveBasedNuls) {
//        this.nerveBasedNuls = nerveBasedNuls;
//    }
//
//    public String getLanguage() {
//        return language;
//    }
//
//    public void setLanguage(String language) {
//        this.language = language;
//    }
}
