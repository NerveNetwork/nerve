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
package network.nerve.converter.heterogeneouschain.ht.base;

import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
import network.nerve.converter.heterogeneouschain.ht.core.BeanUtilTest;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousCfg;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * @author: Mimi
 * @date: 2020-03-18
 */
public class Base {

    protected String address = "";
    protected String priKey = "";
    protected String multySignContractAddress = "";
    protected byte VERSION = 3;
    protected HtgWalletApi htgWalletApi;
    protected List<String> list;
    protected HtContext htgContext;

    @BeforeClass
    public static void initClass() {
        Log.info("init");
    }

    @Before
    public void setUp() throws Exception {
        String ethRpcAddress = "https://http-testnet.hecochain.com/";
        htgWalletApi = new HtgWalletApi();
        Web3j web3j = Web3j.build(new HttpService(ethRpcAddress));
        htgWalletApi.setWeb3j(web3j);
        htgWalletApi.setEthRpcAddress(ethRpcAddress);
        htgContext = new HtContext();
        htgContext.setLogger(Log.BASIC_LOGGER);
        HeterogeneousCfg cfg = new HeterogeneousCfg();
        cfg.setChainIdOnHtgNetwork(256);
        cfg.setDecimals(18);
        htgContext.setConfig(cfg);
        BeanUtilTest.setBean(htgWalletApi, "htgContext", htgContext);
    }

    protected void setMain() {
        if(htgWalletApi.getWeb3j() != null) {
            htgWalletApi.getWeb3j().shutdown();
        }
        //String mainEthRpcAddress = "https://http-mainnet.hecochain.com/";
        String mainEthRpcAddress = "http://heco.nerve.network/";
        //String mainEthRpcAddress = "http://heco.nerve.network?d=1111&s=2222&p=asds45fgvbcv";
        //String mainEthRpcAddress = "http://heco.nerve.network";

        //OkHttpClient okHttpClient = new OkHttpClient.Builder()
        //        .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
        //        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        //        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        //        .build();
        //Web3j web3j = Web3j.build(new HttpService(mainEthRpcAddress, okHttpClient));
        Web3j web3j = Web3j.build(new HttpService(mainEthRpcAddress));

        htgWalletApi.setWeb3j(web3j);
        htgWalletApi.setEthRpcAddress(mainEthRpcAddress);
        htgContext.config.setChainIdOnHtgNetwork(128);
    }

    private static final int CONNECT_TIMEOUT = 5;

    private static final int READ_TIMEOUT = 7;

    private static SSLSocketFactory getUnsafeSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new X509TrustManager[]{getUnsafeTrustManager()}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create unsafe SSLSocketFactory", e);
        }
    }

    private static X509TrustManager getUnsafeTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

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
            Log.error("[{}]Transaction verification failed, reason: estimateGasLimitfail", txType);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, "estimateGasLimitfail");
            //estimateGas = BigInteger.valueOf(100000L);
        }
        BigInteger gasLimit = estimateGas;
        HtgSendTransactionPo htSendTransactionPo = htgWalletApi.callContract(fromAddress, priKey, contract, gasLimit, txFunction, value, null, null);
        String ethTxHash = htSendTransactionPo.getTxHash();
        return ethTxHash;
    }

    protected String sendMainAssetWithdraw(String txKey, String toAddress, String value, int signCount) throws Exception {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(18)).toBigInteger();
        String vHash = HtgUtil.encoderWithdraw(htgContext, txKey, toAddress, bValue, false, HtgConstant.ZERO_ADDRESS, VERSION);
        String signData = this.ethSign(vHash, signCount);
        //signData += "1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111";
        Function function = HtgUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, false, HtgConstant.ZERO_ADDRESS, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }

    protected String sendERC20Withdraw(String txKey, String toAddress, String value, String erc20, int tokenDecimals, int signCount) throws Exception {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(tokenDecimals)).toBigInteger();
        String vHash = HtgUtil.encoderWithdraw(htgContext, txKey, toAddress, bValue, true, erc20, VERSION);
        String signData = this.ethSign(vHash, signCount);
        Function function =  HtgUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, true, erc20, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }
    protected String signDataForERC20Withdraw(String txKey, String toAddress, String value, String erc20, int tokenDecimals, int signCount) {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(tokenDecimals)).toBigInteger();
        String vHash = HtgUtil.encoderWithdraw(htgContext, txKey, toAddress, bValue, true, erc20, VERSION);
        String signData = this.ethSign(vHash, signCount);
        return signData;
    }
    protected String sendERC20WithdrawBySignData(String txKey, String toAddress, String value, String erc20, int tokenDecimals, String signData) throws Exception {
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(tokenDecimals)).toBigInteger();
        Function function =  HtgUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, true, erc20, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }
    protected String sendChange(String txKey, String[] adds, int count, String[] removes, int signCount) throws Exception {
        String vHash = HtgUtil.encoderChange(htgContext, txKey, adds, count, removes, VERSION);
        String signData = this.ethSign(vHash, signCount);
        List<Address> addList = Arrays.asList(adds).stream().map(a -> new Address(a)).collect(Collectors.toList());
        List<Address> removeList = Arrays.asList(removes).stream().map(r -> new Address(r)).collect(Collectors.toList());
        Function function = HtgUtil.getCreateOrSignManagerChangeFunction(txKey, addList, removeList, count, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.CHANGE);
    }

    protected String sendUpgrade(String txKey, String upgradeContract, int signCount) throws Exception {
        String vHash = HtgUtil.encoderUpgrade(htgContext, txKey, upgradeContract, VERSION);
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
