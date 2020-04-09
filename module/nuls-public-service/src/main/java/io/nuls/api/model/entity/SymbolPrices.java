package io.nuls.api.model.entity;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static io.nuls.api.constant.ApiConstant.SPACE;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-06 14:28
 * @Description: 功能描述
 */
public class SymbolPrices extends BaseNulsData {

    public static class Price extends BaseNulsData {

        private String symbol;

        private String currency;

        private BigDecimal price;

        @Override
        protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
            throw new RuntimeException("error call");
        }

        @Override
        public void parse(NulsByteBuffer byteBuffer) throws NulsException {
            String key = byteBuffer.readString();
            String[] ary = key.split(SPACE);
            if(ary.length != 2){
                throw new RuntimeException("喂价Price对象key格式不正确:"+key);
            }
            this.symbol = ary[0];
            this.currency = ary[1];
            this.price = BigDecimal.valueOf(byteBuffer.readDouble());
        }

        @Override
        public int size() {
            int size = 0;
            size += SerializeUtils.sizeOfString(this.symbol + SPACE + this.currency);
            size += SerializeUtils.sizeOfDouble(this.price.doubleValue());
            return size;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getCurrency() {
            return currency;
        }

        public BigDecimal getPrice() {
            return price;
        }
    }

    private List<Price> prices;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        throw new RuntimeException("error call");
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        int length = (int) byteBuffer.readVarInt();
        if (0 < length) {
            List<Price> list = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                list.add(byteBuffer.readNulsData(new Price()));
            }
            this.prices = list;
        }
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfVarInt(this.prices == null ? 0 : this.prices.size());
        if (null != this.prices) {
            for (Price price : this.prices) {
                size += SerializeUtils.sizeOfNulsData(price);
            }
        }
        return size;
    }

    public List<Price> getPrices() {
        return prices;
    }
}
