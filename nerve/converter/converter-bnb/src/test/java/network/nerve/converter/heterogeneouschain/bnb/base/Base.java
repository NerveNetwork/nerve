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
package network.nerve.converter.heterogeneouschain.bnb.base;

import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.bnb.core.BeanUtilTest;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.core.WalletApi;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static okhttp3.ConnectionSpec.CLEARTEXT;


/**
 * @author: Mimi
 * @date: 2020-03-18
 */
public class Base {

    protected String address = "";
    protected String priKey = "";
    protected String multySignContractAddress = "";
    protected HtgWalletApi htgWalletApi;
    protected List<String> list;
    protected BnbContext htgContext;

    String testEthRpcAddress = "https://bsc-testnet.public.blastapi.io";
    //String testEthRpcAddress = "https://data-seed-prebsc-1-s1.binance.org:8545/";
    int testChainId = 97;
    //String mainEthRpcAddress = "https://bsc-dataseed.binance.org/";
    String mainEthRpcAddress = "https://bsc-dataseed1.defibit.io/";//1
    //String mainEthRpcAddress = "https://bsc-dataseed1.binance.org/";
    //String mainEthRpcAddress = "https://bsc-dataseed4.defibit.io/";//1
    //String mainEthRpcAddress = "https://bsc-dataseed2.binance.org/";
    //String mainEthRpcAddress = "https://bsc-dataseed3.ninicoin.io/";//1
    //String mainEthRpcAddress = "https://bsc-dataseed3.binance.org/";
    //String mainEthRpcAddress = "https://bsc-dataseed4.ninicoin.io/";//1
    int mainChainId = 56;

    @BeforeClass
    public static void initClass() {
        Log.info("init");
    }

    @Before
    public void setUp() throws Exception {
        htgWalletApi = new HtgWalletApi();
        Web3j web3j = Web3j.build(new HttpService(testEthRpcAddress));
        htgWalletApi.setWeb3j(web3j);
        htgWalletApi.setEthRpcAddress(testEthRpcAddress);
        htgContext = new BnbContext();
        htgContext.setLogger(Log.BASIC_LOGGER);
        htgContext.SET_VERSION((byte) 3);
        HeterogeneousCfg cfg = new HeterogeneousCfg();
        cfg.setChainIdOnHtgNetwork(testChainId);
        htgContext.setConfig(cfg);
        BeanUtilTest.setBean(htgWalletApi, "htgContext", htgContext);
    }

    protected void setTestProxy() {
        if(htgWalletApi.getWeb3j() != null) {
            htgWalletApi.getWeb3j().shutdown();
        }
        final OkHttpClient.Builder builder =
                new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1087))).connectionSpecs(Arrays.asList(WalletApi.INFURA_CIPHER_SUITE_SPEC, CLEARTEXT));
        OkHttpClient okHttpClient = builder.build();
        Web3j web3j = Web3j.build(new HttpService(testEthRpcAddress, okHttpClient));
        htgWalletApi.setWeb3j(web3j);
        htgWalletApi.setEthRpcAddress(testEthRpcAddress);
        htgContext.getConfig().setChainIdOnHtgNetwork(testChainId);
    }

    protected void setMain() {
        if(htgWalletApi.getWeb3j() != null) {
            htgWalletApi.getWeb3j().shutdown();
        }
        Web3j web3j = Web3j.build(new HttpService(mainEthRpcAddress));
        htgWalletApi.setWeb3j(web3j);
        htgWalletApi.setEthRpcAddress(mainEthRpcAddress);
        htgContext.getConfig().setChainIdOnHtgNetwork(mainChainId);
    }

    protected void setMainProxy() {
        if(htgWalletApi.getWeb3j() != null) {
            htgWalletApi.getWeb3j().shutdown();
        }
        final OkHttpClient.Builder builder =
                new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1087))).connectionSpecs(Arrays.asList(WalletApi.INFURA_CIPHER_SUITE_SPEC, CLEARTEXT));
        OkHttpClient okHttpClient = builder.build();
        Web3j web3j = Web3j.build(new HttpService(mainEthRpcAddress, okHttpClient));
        htgWalletApi.setWeb3j(web3j);
        htgWalletApi.setEthRpcAddress(mainEthRpcAddress);
        htgContext.getConfig().setChainIdOnHtgNetwork(mainChainId);
    }


    /*@Before
    public void setUp() throws Exception {
        *//*
         "commonRpcAddress": "https://data-seed-prebsc-2-s3.binance.org:8545/",
         "mainRpcAddress": "http://data-seed-prebsc-1-s2.binance.org:8545/",
         "orderRpcAddresses": "https://data-seed-prebsc-1-s1.binance.org:8545/,https://data-seed-prebsc-2-s1.binance.org:8545/",
         "standbyRpcAddresses": "https://data-seed-prebsc-1-s3.binance.org:8545/,https://data-seed-prebsc-2-s3.binance.org:8545/",
         *//*
        // delete
        // 1-s2, 2-s1, 2-s2
        //String ethRpcAddress = "https://data-seed-prebsc-1-s1.binance.org:8545/";

        //final OkHttpClient.Builder builder =
        //        new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1087))).connectionSpecs(Arrays.asList(WalletApi.INFURA_CIPHER_SUITE_SPEC, CLEARTEXT));
        //OkHttpClient okHttpClient = builder.build();
        //Web3j web3j = Web3j.build(new HttpService(ethRpcAddress, okHttpClient));
        //String ethRpcAddress = "https://endpoints.omniatech.io/v1/bsc/testnet/public";// https://bsctestapi.terminet.io/rpc https://bsc-testnet.public.blastapi.io
        String ethRpcAddress = "https://bsc-testnet.public.blastapi.io";// https://bsctestapi.terminet.io/rpc https://bsc-testnet.public.blastapi.io
        //String ethRpcAddress = "https://bsc-testnet.publicnode.com";
        //String ethRpcAddress = "https://endpoints.omniatech.io/v1/bsc/testnet/public";
        Web3j web3j = Web3j.build(new HttpService(ethRpcAddress));
        htgWalletApi = new HtgWalletApi();
        htgWalletApi.setWeb3j(web3j);
        htgWalletApi.setEthRpcAddress(ethRpcAddress);
        htgContext = new BnbContext();
        htgContext.SET_VERSION((byte) 3);
        htgContext.setLogger(Log.BASIC_LOGGER);
        HeterogeneousCfg cfg = new HeterogeneousCfg();
        cfg.setChainIdOnHtgNetwork(97);
        htgContext.setConfig(cfg);
        BeanUtilTest.setBean(htgWalletApi, "htgContext", htgContext);
    }

    protected void setMain() {
        if(htgWalletApi.getWeb3j() != null) {
            htgWalletApi.getWeb3j().shutdown();
        }
        *//*
         "commonRpcAddress": "https://bsc-dataseed.binance.org/",
         "mainRpcAddress": "https://bsc-dataseed1.binance.org/",
         "orderRpcAddresses": "https://bsc-dataseed.binance.org/,https://bsc-dataseed1.binance.org/,https://bsc-dataseed2.binance.org/,https://bsc-dataseed3.binance.org/",
         "standbyRpcAddresses": "https://bsc-dataseed1.defibit.io/,https://bsc-dataseed4.defibit.io/,https://bsc-dataseed3.ninicoin.io/,https://bsc-dataseed4.ninicoin.io/",
0xfaf436543661419de222536e518833ce5a1ed4dc
         *//*
        //String mainEthRpcAddress = "https://bsc-dataseed.binance.org/";
        String mainEthRpcAddress = "https://bsc-dataseed1.defibit.io/";//1
        //String mainEthRpcAddress = "https://bsc-dataseed1.binance.org/";
        //String mainEthRpcAddress = "https://bsc-dataseed4.defibit.io/";//1
        //String mainEthRpcAddress = "https://bsc-dataseed2.binance.org/";
        //String mainEthRpcAddress = "https://bsc-dataseed3.ninicoin.io/";//1
        //String mainEthRpcAddress = "https://bsc-dataseed3.binance.org/";
        //String mainEthRpcAddress = "https://bsc-dataseed4.ninicoin.io/";//1
        //final OkHttpClient.Builder builder =
        //        new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1087))).connectionSpecs(Arrays.asList(INFURA_CIPHER_SUITE_SPEC, CLEARTEXT));
        //OkHttpClient okHttpClient = builder.build();
        //Web3j web3j = Web3j.build(new HttpService(mainEthRpcAddress, okHttpClient));
        Web3j web3j = Web3j.build(new HttpService(mainEthRpcAddress));
        htgWalletApi.setWeb3j(web3j);
        htgWalletApi.setEthRpcAddress(mainEthRpcAddress);
        htgContext.config.setChainIdOnHtgNetwork(56);
    }*/

    protected String sendTx(String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        return this.sendTx(fromAddress, priKey, txFunction, txType, null, multySignContractAddress);
    }

    protected String sendTx(String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType, BigInteger value, String contract) throws Exception {
        // Verify the legality of contract transactions
        EthCall ethCall = htgWalletApi.validateContractCall(fromAddress, contract, txFunction, value);
        if (ethCall.isReverted()) {
            Log.error("[{}]Transaction verification failed, reason: {}", txType, ethCall.getRevertReason());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, ethCall.getRevertReason());
        }
        // estimateGasLimit
        EthEstimateGas estimateGasObj = htgWalletApi.ethEstimateGas(fromAddress, contract, txFunction, value);
        BigInteger estimateGas = estimateGasObj.getAmountUsed();

        Log.info("Transaction type: {}, EstimatedGasLimit: {}", txType, estimateGas);
        if (estimateGas.compareTo(BigInteger.ZERO) == 0) {
            Log.error("[{}]Transaction verification failed, reason: estimateGasLimitfail, {}", txType, estimateGasObj.getError().getMessage());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, "estimateGasLimitfail, " + estimateGasObj.getError().getMessage());
            //estimateGas = BigInteger.valueOf(100000L);
        }
        BigInteger gasLimit = estimateGas;
        HtgSendTransactionPo htSendTransactionPo = htgWalletApi.callContract(fromAddress, priKey, contract, gasLimit, txFunction, value, null, null);
        String ethTxHash = htSendTransactionPo.getTxHash();
        return ethTxHash;
    }

    protected String sendMainAssetWithdraw(String txKey, String toAddress, String value, int signCount) throws Exception {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(18)).toBigInteger();
        String vHash = HtgUtil.encoderWithdraw(htgContext, txKey, toAddress, bValue, false, HtgConstant.ZERO_ADDRESS, htgContext.VERSION());
        String signData = this.ethSign(vHash, signCount);
        //signData += "1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111";
        Function function = HtgUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, false, HtgConstant.ZERO_ADDRESS, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }

    protected String sendERC20Withdraw(String txKey, String toAddress, String value, String erc20, int tokenDecimals, int signCount) throws Exception {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(tokenDecimals)).toBigInteger();
        String vHash = HtgUtil.encoderWithdraw(htgContext, txKey, toAddress, bValue, true, erc20, htgContext.VERSION());
        String signData = this.ethSign(vHash, signCount);
        Function function =  HtgUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, true, erc20, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }

    protected String signDataForMainAssetWithdraw(String txKey, String toAddress, String value, int signCount) {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(18)).toBigInteger();
        String vHash = HtgUtil.encoderWithdraw(htgContext, txKey, toAddress, bValue, false, HtgConstant.ZERO_ADDRESS, htgContext.VERSION());
        String signData = this.ethSign(vHash, signCount);
        return signData;
    }

    protected String sendMainAssetWithdrawBySignData(String txKey, String toAddress, String value, String signData) throws Exception {
        BigInteger bValue = new BigDecimal(value).movePointRight(18).toBigInteger();
        Function function = HtgUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, false, HtgConstant.ZERO_ADDRESS, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }

    protected String signDataForERC20Withdraw(String txKey, String toAddress, String value, String erc20, int tokenDecimals, int signCount) {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(tokenDecimals)).toBigInteger();
        String vHash = HtgUtil.encoderWithdraw(htgContext, txKey, toAddress, bValue, true, erc20, htgContext.VERSION());
        String signData = this.ethSign(vHash, signCount);
        return signData;
    }
    protected String sendERC20WithdrawBySignData(String txKey, String toAddress, String value, String erc20, int tokenDecimals, String signData) throws Exception {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(tokenDecimals)).toBigInteger();
        Function function =  HtgUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, true, erc20, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }
    protected String sendChange(String txKey, String[] adds, int count, String[] removes, int signCount) throws Exception {
        String vHash = HtgUtil.encoderChange(htgContext, txKey, adds, count, removes, htgContext.VERSION());
        String signData = this.ethSign(vHash, signCount);
        List<Address> addList = Arrays.asList(adds).stream().map(a -> new Address(a)).collect(Collectors.toList());
        List<Address> removeList = Arrays.asList(removes).stream().map(r -> new Address(r)).collect(Collectors.toList());
        Function function = HtgUtil.getCreateOrSignManagerChangeFunction(txKey, addList, removeList, count, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.CHANGE);
    }

    protected String sendUpgrade(String txKey, String upgradeContract, int signCount) throws Exception {
        String vHash = HtgUtil.encoderUpgrade(htgContext, txKey, upgradeContract, htgContext.VERSION());
        String signData = this.ethSign(vHash, signCount);
        Function function =  HtgUtil.getCreateOrSignUpgradeFunction(txKey, upgradeContract, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.UPGRADE);
    }

    protected String ethSign(String hashStr, int signCount) {
        String result = "";
        List<String> addressList = new ArrayList<>();
        byte[] hash = Numeric.hexStringToByteArray(hashStr);
        for (int i = 0; i < signCount; i++) {
            String prikey = list.get(i);
            Credentials credentials = Credentials.create(prikey);
            String address = credentials.getAddress();
            Sign.SignatureData signMessage = Sign.signMessage(hash, credentials.getEcKeyPair(), false);
            //Sign.SignatureData signMessage = Sign.signPrefixedMessage(hash, credentials.getEcKeyPair());
            byte[] signed = new byte[65];
            System.arraycopy(signMessage.getR(), 0, signed, 0, 32);
            System.arraycopy(signMessage.getS(), 0, signed, 32, 32);
            System.arraycopy(signMessage.getV(), 0, signed, 64, 1);
            String signedHex = Numeric.toHexStringNoPrefix(signed);
            result += signedHex;
            addressList.add(address);
        }
        System.out.println(Arrays.toString(addressList.toArray()));
        return result;
    }

    protected Function getERC20TransferFromFunction(String sender, String recipient,BigInteger value) {
        return new Function(
                "transferFrom",
                List.of(new Address(sender),
                        new Address(recipient),
                        new Uint256(value)),
                List.of(new TypeReference<Type>() {
                })
        );
    }

    protected Function getERC20ApproveFunction(String spender, BigInteger value) {
        return new Function(
                "approve",
                List.of(new Address(spender),
                        new Uint256(value)),
                List.of(new TypeReference<Type>() {
                })
        );
    }
}
