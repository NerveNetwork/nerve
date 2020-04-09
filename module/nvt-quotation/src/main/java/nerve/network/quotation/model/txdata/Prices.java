/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package nerve.network.quotation.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Chino
 * @date: 2020/03/25
 */
public class Prices extends BaseNulsData {

    private List<Price> prices;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        int length = prices == null ? 0 : prices.size();
        stream.writeVarInt(length);
        if (null != prices) {
            for (Price price : prices) {
                stream.writeNulsData(price);
            }
        }
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

    public Prices() {
    }

    public Prices(List<Price> prices) {
        this.prices = prices;
    }

    public List<Price> getPrices() {
        return prices;
    }

    public void setPrices(List<Price> prices) {
        this.prices = prices;
    }

    @Override
    public String toString() {
        if(null == this.prices){
            return null;
        }
        String lineSeparator = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        for (Price price : prices) {
            builder.append(String.format("\t\tkey: %s", price.getKey())).append(", ");
            builder.append(String.format("value: %s", price.getValue())).append(lineSeparator);
        }
        return builder.toString();
    }
}
