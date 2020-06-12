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
package network.nerve.converter.heterogeneouschain.eth.thirdpartyapi;

import network.nerve.converter.heterogeneouschain.eth.base.Base;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Mimi
 * @date: 2020-03-24
 */
public class ThirdPartyAPITest extends Base {

    private String address;

    @Before
    public void before() {

    }

    @Test
    public void test() throws Exception {
        this.address = "0x08aa275fe3523bdf2177421c74ec502c91cec364"; // rinkeby
        //this.address = "0x06012c8cf97bead5deae237070f9587f8e7a266d";// mainnet
        String url = String.format("https://web3api.io/api/v2/addresses/%s/information", address);
        List<BasicHeader> headers = new ArrayList<>();
        //headers.add(new BasicHeader("x-amberdata-blockchain-id", "ethereum-ropsten"));// 不支持
        //headers.add(new BasicHeader("x-amberdata-blockchain-id", "ethereum-mainnet"));
        headers.add(new BasicHeader("x-amberdata-blockchain-id", "ethereum-rinkeby"));
        headers.add(new BasicHeader("x-api-key", "UAK7d32cfddcdd4b3d95e9e31ecf8e8747c"));
        String result = HttpClientUtil.get(url, headers);
        System.out.println(result);
    }
}
