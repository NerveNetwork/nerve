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
package network.nerve.converter.heterogeneouschain.eth.base;

import io.nuls.core.log.Log;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import org.junit.Before;
import org.junit.BeforeClass;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * @author: Mimi
 * @date: 2020-03-18
 */
public class Base {

    protected ETHWalletApi ethWalletApi;

    @BeforeClass
    public static void initClass() {
        Log.info("init");
    }

    @Before
    public void setUp() throws Exception {
        //String ethRpcAddress = "https://ropsten.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5";
        String ethRpcAddress = "https://ropsten.infura.io/v3/7e086d9f3bdc48e4996a3997b33b032f";
        ethWalletApi = new ETHWalletApi();
        EthContext.setLogger(Log.BASIC_LOGGER);
        Web3j web3j = Web3j.build(new HttpService(ethRpcAddress));
        ethWalletApi.setWeb3j(web3j);
        ethWalletApi.setEthRpcAddress(ethRpcAddress);
    }

    protected void setMain() {
        if(ethWalletApi.getWeb3j() != null) {
            ethWalletApi.getWeb3j().shutdown();
        }
        //String mainEthRpcAddress = "https://mainnet.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5";
        String mainEthRpcAddress = "http://geth.nerve.network?d=1111&s=2222&p=asds45fgvbcv";
        //String mainEthRpcAddress = "https://http-mainnet.hecochain.com";
        Web3j web3j = Web3j.build(new HttpService(mainEthRpcAddress));
        ethWalletApi.setWeb3j(web3j);
        ethWalletApi.setEthRpcAddress(mainEthRpcAddress);
    }

    protected void setRinkeby() {
        if(ethWalletApi.getWeb3j() != null) {
            ethWalletApi.getWeb3j().shutdown();
        }
        String mainEthRpcAddress = "https://rinkeby.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5";
        Web3j web3j = Web3j.build(new HttpService(mainEthRpcAddress));
        ethWalletApi.setWeb3j(web3j);
        ethWalletApi.setEthRpcAddress(mainEthRpcAddress);
    }

    protected void setLocalRpc() {
        if(ethWalletApi.getWeb3j() != null) {
            ethWalletApi.getWeb3j().shutdown();
        }
        String mainEthRpcAddress = "http://localhost:9898/jsonrpc";
        Web3j web3j = Web3j.build(new HttpService(mainEthRpcAddress));
        ethWalletApi.setWeb3j(web3j);
        ethWalletApi.setEthRpcAddress(mainEthRpcAddress);
    }
}
