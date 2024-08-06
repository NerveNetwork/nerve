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
package network.nerve.converter.heterogeneouschain.bchutxo.utils.addr;

/**
 * @author: PierreLuo
 * @date: 2024/7/16
 */
public class TestNet4ParamsForAddr implements NetworkParameters {

    private static TestNet4ParamsForAddr instance = new TestNet4ParamsForAddr();
    protected int[] acceptableAddressCodes;
    protected int addressHeader;
    protected int p2shHeader;
    protected String cashAddrPrefix;

    public static TestNet4ParamsForAddr get() {
        return instance;
    }

    private TestNet4ParamsForAddr() {
        addressHeader = 111;
        p2shHeader = 196;
        acceptableAddressCodes = new int[]{addressHeader, p2shHeader};
        cashAddrPrefix = "bchtest";
    }

    @Override
    public int[] getAcceptableAddressCodes() {
        return acceptableAddressCodes;
    }

    @Override
    public String getCashAddrPrefix() {
        return cashAddrPrefix;
    }

    @Override
    public int getAddressHeader() {
        return addressHeader;
    }

    @Override
    public int getP2SHHeader() {
        return p2shHeader;
    }
}
