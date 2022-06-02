/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
package network.nerve.converter.heterogeneouschain.trx.model;

import io.nuls.core.model.StringUtils;

/**
 * @author: PierreLuo
 * @date: 2021/8/4
 */
public class TrxEstimateSun {
    private static final String DEFAULT_ERROR = "unkown error";

    private boolean reverted;
    private String revertReason;
    private long sunUsed;
    private long energyUsed;

    public TrxEstimateSun(boolean reverted, String revertReason, long sunUsed, long energyUsed) {
        this.reverted = reverted;
        this.revertReason = revertReason;
        this.sunUsed = sunUsed;
        this.energyUsed = energyUsed;
    }

    public static TrxEstimateSun SUCCESS(long sunUsed, long energyUsed) {
        return new TrxEstimateSun(false, null, sunUsed, energyUsed);
    }

    public static TrxEstimateSun FAILED(String error) {
        if (StringUtils.isBlank(error)) {
            error = DEFAULT_ERROR;
        }
        return new TrxEstimateSun(true, error, 0, 0);
    }

    public boolean isReverted() {
        return reverted;
    }

    public String getRevertReason() {
        return revertReason;
    }

    public long getSunUsed() {
        return sunUsed;
    }

    public long getEnergyUsed() {
        return energyUsed;
    }
}
