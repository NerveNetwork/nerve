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
package network.nerve.converter.heterogeneouschain.trx.base;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant;
import network.nerve.converter.heterogeneouschain.trx.context.TrxContext;
import network.nerve.converter.heterogeneouschain.trx.core.BeanUtilTest;
import network.nerve.converter.heterogeneouschain.trx.core.TrxWalletApi;
import network.nerve.converter.heterogeneouschain.trx.model.TrxEstimateSun;
import network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.LoggerFactory;
import org.tron.trident.abi.FunctionEncoder;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.core.transaction.TransactionBuilder;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;
import org.tron.trident.proto.Response;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant.TRX_100;
import static network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil.getCreateOrSignWithdrawFunction;


/**
 * @author: Mimi
 * @date: 2020-03-18
 */
public class Base {

    protected String address = "";
    protected String priKey = "";
    protected String multySignContractAddress = "";
    protected byte VERSION = 3;
    protected List<String> list;
    protected ApiWrapper wrapper;
    protected TrxWalletApi walletApi;
    protected TrxContext context;

    @BeforeClass
    public static void initClass() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger("io.grpc");
        logger.setAdditive(false);
        logger.setLevel(Level.INFO);
        Log.info("init");
    }

    @Before
    public void setUp() throws Exception {
        walletApi = new TrxWalletApi();
        context = new TrxContext();
        context.setLogger(Log.BASIC_LOGGER);
        HeterogeneousCfg cfg = new HeterogeneousCfg();
        cfg.setChainIdOnHtgNetwork(100000001);
        cfg.setSymbol("TRX");
        context.setConfig(cfg);
        context.SET_VERSION(VERSION);
        BeanUtilTest.setBean(walletApi, "htgContext", context);
        wrapper = ApiWrapper.ofShasta("3333333333333333333333333333333333333333333333333333333333333333");
        walletApi.setWrapper(wrapper);
        walletApi.setRpcAddress(EMPTY_STRING);
    }

    protected void setMain() {
        if (walletApi.getWrapper() != null) {
            walletApi.getWrapper().close();
        }
        wrapper = new ApiWrapper("tron.nerve.network:50051", "tron.nerve.network:50061", "3333333333333333333333333333333333333333333333333333333333333333");
        //wrapper = ApiWrapper.ofMainnet("3333333333333333333333333333333333333333333333333333333333333333", "76f3c2b5-357a-4e6c-aced-9e1c42179717");
        walletApi.setWrapper(wrapper);
        walletApi.setRpcAddress("endpoint:tron.nerve.network");
        context.config.setChainIdOnHtgNetwork(100000002);
    }

    protected void setNile() {
        if (walletApi.getWrapper() != null) {
            walletApi.getWrapper().close();
        }
        wrapper = ApiWrapper.ofNile("3333333333333333333333333333333333333333333333333333333333333333");
        walletApi.setWrapper(wrapper);
        walletApi.setRpcAddress(EMPTY_STRING);
    }

    protected String sendMainAssetWithdraw(String txKey, String toAddress, String value, int signCount) throws Exception {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(6)).toBigInteger();
        String vHash = TrxUtil.encoderWithdraw(context, txKey, toAddress, bValue, false, TrxConstant.ZERO_ADDRESS, VERSION);
        String signData = this.ethSign(vHash, signCount);
        Function function = getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, false, TrxConstant.ZERO_ADDRESS, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }

    protected String sendTRC20Withdraw(String txKey, String toAddress, String value, String erc20, int tokenDecimals, int signCount) throws Exception {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(tokenDecimals)).toBigInteger();
        String vHash = TrxUtil.encoderWithdraw(context, txKey, toAddress, bValue, true, erc20, VERSION);
        String signData = this.ethSign(vHash, signCount);
        Function function =  TrxUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, true, erc20, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }

    protected String sendTRC20WithdrawBySignData(String txKey, String toAddress, String value, String erc20, int tokenDecimals, String signData) throws Exception {
        BigInteger bValue = new BigDecimal(value).multiply(BigDecimal.TEN.pow(tokenDecimals)).toBigInteger();
        Function function =  TrxUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, true, erc20, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.WITHDRAW);
    }

    protected String sendChange(String txKey, String[] adds, int count, String[] removes, int signCount) throws Exception {
        String vHash = TrxUtil.encoderChange(context, txKey, adds, count, removes, VERSION);
        String signData = this.ethSign(vHash, signCount);
        List<Address> addList = Arrays.asList(adds).stream().map(a -> new Address(a)).collect(Collectors.toList());
        List<Address> removeList = Arrays.asList(removes).stream().map(r -> new Address(r)).collect(Collectors.toList());
        Function function = TrxUtil.getCreateOrSignManagerChangeFunction(txKey, addList, removeList, count, signData);
        return this.sendTx(address, priKey, function, HeterogeneousChainTxType.CHANGE);
    }

    protected String ethSign(String hashStr, int signCount) {
        String result = "";
        for (int i = 0; i < signCount; i++) {
            String prikey = list.get(i);
            String signedHex = TrxUtil.dataSign(hashStr, prikey);
            result += signedHex;
        }
        System.out.println(String.format("signatures: 0x%s", result));
        return result;
    }

    protected String sendTx(String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        return this.sendTx(fromAddress, priKey, txFunction, txType, null, multySignContractAddress);
    }

    protected String sendTx(String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType, BigInteger value, String contract) throws Exception {
        // 估算feeLimit
        System.out.println(String.format("%s, %s, %s, %s", fromAddress, contract, FunctionEncoder.encode(txFunction), value));
        TrxEstimateSun estimateSun = walletApi.estimateSunUsed(fromAddress, contract, txFunction, value);
        if (estimateSun.isReverted()) {
            System.err.println(String.format("[%s]交易验证失败，原因: %s", txType, estimateSun.getRevertReason()));
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, estimateSun.getRevertReason());
        }
        BigInteger feeLimit = TRX_100.multiply(BigInteger.valueOf(5));
        //if (estimateSun.getSunUsed() > 0) {
        //    feeLimit = BigInteger.valueOf(estimateSun.getSunUsed());
        //}
        System.out.println(String.format("交易类型: %s, 估算的feeLimit: %s", txType, TrxUtil.convertSunToTrx(feeLimit).toPlainString()));
        value = value == null ? BigInteger.ZERO : value;
        String encodedHex = FunctionEncoder.encode(txFunction);
        Contract.TriggerSmartContract trigger =
                Contract.TriggerSmartContract.newBuilder()
                        .setOwnerAddress(ApiWrapper.parseAddress(fromAddress))
                        .setContractAddress(ApiWrapper.parseAddress(contract))
                        .setData(ApiWrapper.parseHex(encodedHex))
                        .setCallValue(value.longValue())
                        .build();

        Response.TransactionExtention txnExt = wrapper.blockingStub.triggerContract(trigger);
        TransactionBuilder builder = new TransactionBuilder(txnExt.getTransaction());
        builder.setFeeLimit(feeLimit.longValue());

        Chain.Transaction signedTxn = wrapper.signTransaction(builder.build(), new KeyPair(priKey));
        String txHash = TrxUtil.calcTxHash(signedTxn);
        System.out.println("txHash => " + txHash);
        Response.TransactionReturn ret = wrapper.blockingStub.broadcastTransaction(signedTxn);
        System.out.println(String.format("[%s]======== Result: %s ", txType, ret.toString()));
        return txHash;
    }
}
