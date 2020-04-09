/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package nerve.network.pocbft.utils.compare;


import nerve.network.pocbft.model.bo.tx.txdata.Agent;

import java.util.Comparator;

/**
 * @author Niels
 */
public class AgentComparatorRpc implements Comparator<Agent> {

    public static final int DEPOSIT = 0;
    public static final int CREDIT_VALUE = 1;
    public static final int COMPREHENSIVE = 2;

    private static final AgentComparatorRpc[] INSTANCE_ARRAY = new AgentComparatorRpc[]{
            new AgentComparatorRpc(DEPOSIT),
            new AgentComparatorRpc(CREDIT_VALUE),
            new AgentComparatorRpc(COMPREHENSIVE)
    };
    private final int sortType;

    public static AgentComparatorRpc getInstance(int sortType) {
        switch (sortType) {
            case DEPOSIT:
                return INSTANCE_ARRAY[DEPOSIT];
            case CREDIT_VALUE:
                return INSTANCE_ARRAY[CREDIT_VALUE];
            case COMPREHENSIVE:
                return INSTANCE_ARRAY[COMPREHENSIVE];
            default:
                return INSTANCE_ARRAY[CREDIT_VALUE];
        }
    }

    private AgentComparatorRpc(int sortType) {
        this.sortType = sortType;
    }

    @Override
    public int compare(Agent o1, Agent o2) {
        switch (sortType) {
            case DEPOSIT: {
                return  o2.getDeposit().compareTo(o1.getDeposit());
            }
            case CREDIT_VALUE: {
                if (o2.getCreditVal() < o1.getCreditVal()) {
                    return -1;
                } else if (o2.getCreditVal() == o1.getCreditVal()) {
                    return 0;
                }
                return 1;
            }
            default: {
                if (o2.getCreditVal() < o1.getCreditVal()) {
                    return -1;
                } else if (o2.getCreditVal() == o1.getCreditVal()) {
                    return 0;
                }
                return 1;
            }
        }
    }
}
