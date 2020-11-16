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
package network.nerve.converter.heterogeneouschain.bnb.base;

import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.bnb.core.BNBWalletApi;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbSendTransactionPo;
import network.nerve.converter.heterogeneouschain.bnb.utils.BnbUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static network.nerve.converter.heterogeneouschain.bnb.utils.BnbUtil.leftPadding;


/**
 * @author: Mimi
 * @date: 2020-03-18
 */
public class Base {

    protected String address = "";
    protected String priKey = "";
    protected String multySignContractAddress = "";
    protected byte VERSION = 2;
    protected BNBWalletApi bnbWalletApi;
    protected List<String> list;

    @BeforeClass
    public static void initClass() {
        Log.info("init");
    }

    @Before
    public void setUp() throws Exception {
        String ethRpcAddress = "https://data-seed-prebsc-2-s1.binance.org:8545/";
        bnbWalletApi = new BNBWalletApi();
        BnbContext.setLogger(Log.BASIC_LOGGER);
        Web3j web3j = Web3j.build(new HttpService(ethRpcAddress));
        bnbWalletApi.setWeb3j(web3j);
        bnbWalletApi.setEthRpcAddress(ethRpcAddress);
    }

    protected void setMain() {
        if(bnbWalletApi.getWeb3j() != null) {
            bnbWalletApi.getWeb3j().shutdown();
        }
        //String mainEthRpcAddress = "https://bsc-dataseed.binance.org/";
        String mainEthRpcAddress = "http://bsc.nerve.network";
        Web3j web3j = Web3j.build(new HttpService(mainEthRpcAddress));
        bnbWalletApi.setWeb3j(web3j);
        bnbWalletApi.setEthRpcAddress(mainEthRpcAddress);
    }

    protected String sendTx(String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        return this.sendTx(fromAddress, priKey, txFunction, txType, null, multySignContractAddress);
    }

    protected String sendTx(String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType, BigInteger value, String contract) throws Exception {
        // 验证合约交易合法性
        EthCall ethCall = bnbWalletApi.validateContractCall(fromAddress, contract, txFunction, value);
        if (ethCall.isReverted()) {
            Log.error("[{}]交易验证失败，原因: {}", txType, ethCall.getRevertReason());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, ethCall.getRevertReason());
        }
        // 估算GasLimit
        BigInteger estimateGas = bnbWalletApi.ethEstimateGas(fromAddress, contract, txFunction, value);
        Log.info("交易类型: {}, 估算的GasLimit: {}", txType, estimateGas);
        if (estimateGas.compareTo(BigInteger.ZERO) == 0) {
            Log.error("[{}]交易验证失败，原因: 估算GasLimit失败", txType);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, "估算GasLimit失败");
            //estimateGas = BigInteger.valueOf(100000L);
        }
        BigInteger gasLimit = estimateGas;
        BnbSendTransactionPo bnbSendTransactionPo = bnbWalletApi.callContract(fromAddress, priKey, contract, gasLimit, txFunction, value, null);
        String ethTxHash = bnbSendTransactionPo.getTxHash();
        return ethTxHash;
    }

    protected String sendBNBWithdraw(String txKey, String toAddress, String value, int signCount) throws Exception {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(18)).toBigInteger();
        String vHash = this.encoderWithdraw(txKey, toAddress, bValue, false, BnbConstant.ZERO_ADDRESS, VERSION);
        String signData = this.ethSign(vHash, signCount);
        //signData += "1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111";
        Function function = BnbUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, false, BnbConstant.ZERO_ADDRESS, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }

    protected String sendERC20Withdraw(String txKey, String toAddress, String value, String erc20, int tokenDecimals, int signCount) throws Exception {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(tokenDecimals)).toBigInteger();
        String vHash = this.encoderWithdraw(txKey, toAddress, bValue, true, erc20, VERSION);
        String signData = this.ethSign(vHash, signCount);
        Function function =  BnbUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, true, erc20, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }
    protected String sendChange(String txKey, String[] adds, int count, String[] removes, int signCount) throws Exception {
        String vHash = this.encoderChange(txKey, adds, count, removes, VERSION);
        String signData = this.ethSign(vHash, signCount);
        List<Address> addList = Arrays.asList(adds).stream().map(a -> new Address(a)).collect(Collectors.toList());
        List<Address> removeList = Arrays.asList(removes).stream().map(r -> new Address(r)).collect(Collectors.toList());
        Function function = BnbUtil.getCreateOrSignManagerChangeFunction(txKey, addList, removeList, count, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.CHANGE);
    }

    protected String sendUpgrade(String txKey, String upgradeContract, int signCount) throws Exception {
        String vHash = this.encoderUpgrade(txKey, upgradeContract, VERSION);
        String signData = this.ethSign(vHash, signCount);
        Function function =  BnbUtil.getCreateOrSignUpgradeFunction(txKey, upgradeContract, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.UPGRADE);
    }

    protected String encoderWithdraw(String txKey, String toAddress, BigInteger value, Boolean isContractAsset, String erc20, byte version) {
        StringBuilder sb = new StringBuilder();
        sb.append(Numeric.toHexString(txKey.getBytes(StandardCharsets.UTF_8)));
        sb.append(Numeric.cleanHexPrefix(toAddress));
        sb.append(leftPadding(value.toString(16), "0", 64));
        sb.append(isContractAsset ? "01" : "00");
        sb.append(Numeric.cleanHexPrefix(erc20));
        sb.append(String.format("%02x", version & 255));
        byte[] hash = Hash.sha3(Numeric.hexStringToByteArray(sb.toString()));
        System.out.println(String.format("data: %s", sb.toString()));
        System.out.println(String.format("vHash: %s", Numeric.toHexString(hash)));
        return Numeric.toHexString(hash);
    }

    protected String encoderChange(String txKey, String[] adds, int count, String[] removes, byte version) {
        StringBuilder sb = new StringBuilder();
        sb.append(Numeric.toHexString(txKey.getBytes(StandardCharsets.UTF_8)));
        for (String add : adds) {
            sb.append(leftPadding(Numeric.cleanHexPrefix(add), "0", 64));
        }
        sb.append(leftPadding(Integer.toHexString(count), "0", 2));
        for (String remove : removes) {
            sb.append(leftPadding(Numeric.cleanHexPrefix(remove), "0", 64));
        }
        sb.append(String.format("%02x", version & 255));
        byte[] hash = Hash.sha3(Numeric.hexStringToByteArray(sb.toString()));
        System.out.println(String.format("data: %s", sb.toString()));
        System.out.println(String.format("vHash: %s", Numeric.toHexString(hash)));
        return Numeric.toHexString(hash);
    }

    protected String encoderUpgrade(String txKey, String upgradeContract, byte version) {
        StringBuilder sb = new StringBuilder();
        sb.append(Numeric.toHexString(txKey.getBytes(StandardCharsets.UTF_8)));
        sb.append(Numeric.cleanHexPrefix(upgradeContract));
        sb.append(String.format("%02x", version & 255));
        byte[] hash = Hash.sha3(Numeric.hexStringToByteArray(sb.toString()));
        System.out.println(String.format("data: %s", sb.toString()));
        System.out.println(String.format("vHash: %s", Numeric.toHexString(hash)));
        return Numeric.toHexString(hash);
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
        System.out.println(String.format("signatures: 0x%s", result));
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
