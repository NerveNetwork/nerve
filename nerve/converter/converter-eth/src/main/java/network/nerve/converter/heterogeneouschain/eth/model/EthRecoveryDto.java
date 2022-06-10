/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
package network.nerve.converter.heterogeneouschain.eth.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author: Mimi
 * @date: 2020-02-26
 */
public class EthRecoveryDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private String realNerveTxHash;
    private String[] seedManagers;
    private String[] allManagers;

    public EthRecoveryDto(String realNerveTxHash, String[] seedManagers, String[] allManagers) {
        this.realNerveTxHash = realNerveTxHash;
        this.seedManagers = seedManagers;
        this.allManagers = allManagers;
    }

    public String getRealNerveTxHash() {
        return realNerveTxHash;
    }

    public void setRealNerveTxHash(String realNerveTxHash) {
        this.realNerveTxHash = realNerveTxHash;
    }

    public String[] getSeedManagers() {
        return seedManagers;
    }

    public void setSeedManagers(String[] seedManagers) {
        this.seedManagers = seedManagers;
    }

    public String[] getAllManagers() {
        return allManagers;
    }

    public void setAllManagers(String[] allManagers) {
        this.allManagers = allManagers;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"realNerveTxHash\":")
                .append('\"').append(realNerveTxHash).append('\"');
        sb.append(",\"seedManagers\":")
                .append(Arrays.toString(seedManagers));
        sb.append(",\"allManagers\":")
                .append(Arrays.toString(allManagers));
        sb.append('}');
        return sb.toString();
    }
}
