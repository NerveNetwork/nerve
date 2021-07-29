package network.nerve.converter.heterogeneouschain.lib.core;

import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgAccountHelper;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.utils.ConverterUtil;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @package com.bloex.wallet.util
 * @Author admin
 * @Date 2018/4/16
 * @project bloex-application
 */
public class HtgWalletApi implements WalletApi, BeanInitial {

    private ConverterConfig converterConfig;
    private HtgAccountHelper htgAccountHelper;
    private HtgContext htgContext;

    private String symbol() {
        return htgContext.getConfig().getSymbol();
    }

    protected Web3j web3j;

    protected String rpcAddress;
    int switchStatus = 0;
    private boolean inited = false;
    private Map<String, Integer> requestExceededMap = new HashMap<>();
    private long clearTimeOfRequestExceededMap = 0L;

    private Map<String, BigInteger> map = new HashMap<>();
    private ReentrantLock checkLock = new ReentrantLock();
    private ReentrantLock resetLock = new ReentrantLock();
    private ReentrantLock reSignLock = new ReentrantLock();

    private NulsLogger getLog() {
        return htgContext.logger();
    }

    public void init(String rpcAddress) throws NulsException {
        initialize();
        // 默认初始化的API会被新的API服务覆盖，当节点成为虚拟银行时，会初始化新的API服务
        if (web3j != null && htgContext.getConfig().getCommonRpcAddress().equals(this.rpcAddress) && !rpcAddress.equals(this.rpcAddress)) {
            resetWeb3j();
        }
        if (web3j == null) {
            web3j = newInstanceWeb3j(rpcAddress);
            this.rpcAddress = rpcAddress;
            getLog().info("初始化 {} API URL: {}", symbol(), rpcAddress);
        }
    }

    public void checkApi(int order) throws NulsException {
        long now = System.currentTimeMillis();
        // 如果使用的是应急API，应急API使用时间内，不检查API切换
        if (now < clearTimeOfRequestExceededMap) {
            if (htgContext.getConfig().getMainRpcAddress().equals(this.rpcAddress)) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("应急API使用时间内，不检查API切换, 到期时间: {}，剩余等待时间: {}", clearTimeOfRequestExceededMap, clearTimeOfRequestExceededMap - now);
                }
                return;
            }
        } else if (clearTimeOfRequestExceededMap != 0){
            getLog().info("重置应急API，{} API 准备切换，当前API: {}", symbol(), this.rpcAddress);
            requestExceededMap.clear();
            clearTimeOfRequestExceededMap = 0L;
        }

        String rpc = this.calRpcBySwitchStatus(order, switchStatus);
        if (!rpc.equals(this.rpcAddress)) {
            checkLock.lock();
            try {
                if (!rpc.equals(this.rpcAddress)) {
                    getLog().info("检测到顺序变化，{} API 准备切换，当前API: {}", symbol(), this.rpcAddress);
                    changeApi(rpc);
                }
            } catch (NulsException e) {
                throw e;
            } finally {
                checkLock.unlock();
            }
        }
    }

    private String calRpcBySwitchStatus(int order, int tempSwitchStatus) throws NulsException {
        String rpc;
        List<String> rpcAddressList = htgContext.RPC_ADDRESS_LIST();
        if(tempSwitchStatus == 1) {
            rpcAddressList = htgContext.STANDBY_RPC_ADDRESS_LIST();
        }
        int rpcSize = rpcAddressList.size();
        if(rpcSize == 0) {
            throw new NulsException(ConverterErrorCode.DATA_ERROR, "No available rpc address.");
        } else if(rpcSize == 1) {
            rpc = rpcAddressList.get(0);
        } else {
            int index = this.calRpcIndex(order, rpcSize);
            rpc = rpcAddressList.get(index);
        }
        return rpc;
    }

    private void changeApi(String rpc) throws NulsException {
        resetWeb3j();
        init(rpc);
    }

    private void reSignMainAPI() throws NulsException {
        this.rpcAddress = null;
        reSignLock.lock();
        try {
            if (this.rpcAddress == null) {
                getLog().info("进入重签");
                changeApi(htgContext.getConfig().getMainRpcAddress());
            }
        } catch (NulsException e) {
            throw e;
        } finally {
            reSignLock.unlock();
        }

    }

    private int calRpcIndex(int order, int rpcSize) {
        if (order == 0) {
            order++;
        }
        int mod = order % rpcSize;
        int index = mod != 0 ? mod : rpcSize;
        index--;
        return index;
    }

    private boolean unavailableRpc(String rpc) {
        Integer count = requestExceededMap.get(rpc);
        return (count != null && count > 5);
    }

    private void switchStandbyAPI(String oldRpc) throws NulsException {
        getLog().info("{} API 准备切换，当前API: {}", symbol(), oldRpc);
        resetLock.lock();
        try {
            // 不相等，说明已经被切换
            if (!oldRpc.equals(this.rpcAddress)) {
                getLog().info("{} API 已切换至: {}", symbol(), this.rpcAddress);
                return;
            }
            int expectSwitchStatus = (switchStatus + 1) % 2;
            int order = htgContext.getConverterCoreApi().getVirtualBankOrder();
            String rpc = this.calRpcBySwitchStatus(order, expectSwitchStatus);
            // 检查配置的API是否超额
            if (unavailableRpc(oldRpc) && unavailableRpc(rpc)) {
                String mainRpcAddress = htgContext.getConfig().getMainRpcAddress();
                getLog().info("{} API 不可用: {} - {}, 准备切换至应急API: {}, ", symbol(), oldRpc, rpc, mainRpcAddress);
                changeApi(mainRpcAddress);
                if (mainRpcAddress.equals(this.rpcAddress)) {
                    clearTimeOfRequestExceededMap = System.currentTimeMillis() + HtgConstant.HOURS_3;
                }
                return;
            }
            // 正常切换API
            changeApi(rpc);
            // 相等，说明切换成功
            if (rpc.equals(this.rpcAddress)) {
                switchStatus = expectSwitchStatus;
            }
        } catch (NulsException e) {
            throw e;
        } finally {
            resetLock.unlock();
        }
    }

    private void resetWeb3j() {
        if (web3j != null) {
            web3j.shutdown();
            web3j = null;
        }
    }

    public boolean isInited() {
        return inited;
    }

    public void initedDone() {
        this.inited = true;
    }

    @Override
    public void initialize() {
    }

    @Override
    public long getBlockHeight() throws Exception {
        BigInteger blockHeight = this.timeOutWrapperFunction("getBlockHeight", null, args -> {
            return web3j.ethBlockNumber().send().getBlockNumber();
        });
        if (blockHeight != null) {
            return blockHeight.longValue();
        }
        return 0L;
    }

    protected void checkIfResetWeb3j(int times) throws NulsException {
        int mod = times % 6;
        if (mod == 5 && web3j != null && rpcAddress != null) {
            getLog().info("重启API服务");
            resetWeb3j();
            web3j = newInstanceWeb3j(rpcAddress);
        }
    }

    private Web3j newInstanceWeb3j(String rpcAddress) throws NulsException {
        Web3j web3j = Web3j.build(new HttpService(rpcAddress));
        return web3j;
    }

    /**
     * Method:getBlock
     * Description: 获取区块信息
     * Author: xinjl
     * Date: 2018/4/16 15:23
     */
    @Override
    public Block getBlock(long height) throws Exception {
        EthBlock.Block block = getBlockByHeight(height);
        return createBlock(block);
    }

    @Override
    public Block getBlock(String hash) throws Exception {
        EthBlock.Block block = this.timeOutWrapperFunction("getBlock", hash, args -> {
            return web3j.ethGetBlockByHash(args, true).send().getBlock();
        });
        if(block == null) {
            return null;
        }
        return createBlock(block);
    }

    private Block createBlock(EthBlock.Block block) {
        Block simpleBlock = new Block();
        simpleBlock.setHeight(block.getNumber().longValue());
        simpleBlock.setHash(block.getHash());
        List<EthBlock.TransactionResult> transactions = block.getTransactions();
        if (transactions != null && transactions.size() > 0) {
            List<Transaction> list = new ArrayList<>();
            for (int i = 0; i < transactions.size(); i++) {
                org.web3j.protocol.core.methods.response.Transaction transaction = getTransaction(block.getNumber().longValue(), i);
                Transaction transferTransaction = new Transaction();
                transferTransaction.setFromAddress(transaction.getFrom());
                transferTransaction.setToAddress(transaction.getTo());
                BigDecimal value = convertWeiToMainAsset(transaction.getValue());
                transferTransaction.setAmount(value);
                transferTransaction.setTxHash(transaction.getHash());
                list.add(transferTransaction);
            }
            simpleBlock.setTransactions(list);
        }
        return simpleBlock;
    }

    /**
     * Method:getBlockByHeight
     * Description: 根据高度获取区块
     * Author: xinjl
     * Date: 2018/4/16 15:23
     */
    public EthBlock.Block getBlockByHeight(Long height) throws Exception {
        EthBlock.Block block = this.timeOutWrapperFunction("getBlockByHeight", height, args -> {
            return web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(args), true).send().getBlock();
        });
        if (block == null) {
            getLog().error("获取区块为空");
        }
        return block;
    }

    /**
     * 根据高度获取区块头
     */
    public EthBlock.Block getBlockHeaderByHeight(Long height) throws Exception {
        EthBlock.Block header = this.timeOutWrapperFunction("getBlockHeaderByHeight", height, args -> {
            return web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(args), false).send().getBlock();
        });
        if (header == null) {
            getLog().error("获取区块头为空");
        }
        return header;
    }

    /**
     * Method:获取链上交易
     * Description:
     * Author: xinjl
     * Date: 2018/4/16 15:23
     */
    public org.web3j.protocol.core.methods.response.Transaction getTransaction(Long height, Integer index) {
        Request<?, EthTransaction> ethTransactionRequest = web3j.ethGetTransactionByBlockNumberAndIndex(new DefaultBlockParameterNumber(height), new BigInteger(index.toString()));
        org.web3j.protocol.core.methods.response.Transaction transaction = null;
        try {
            EthTransaction send = ethTransactionRequest.send();
            if (send.getTransaction().isPresent()) {
                transaction = send.getTransaction().get();
            } else {
                getLog().error("交易详情获取失败:" + transaction + ",height:" + height + ",index:" + index);
            }
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
        }
        return transaction;
    }

    /**
     * Method:getHtBalance
     * Description: 获取ht余额
     * Author: xinjl
     * Date: 2018/4/16 15:22
     */
    @Override
    public BigDecimal getBalance(String address) throws Exception {
        BigDecimal balance = this.timeOutWrapperFunction("getBalance", address, args -> {
            EthGetBalance send = web3j.ethGetBalance(args, DefaultBlockParameterName.LATEST).send();
            if (send != null) {
                return new BigDecimal(send.getBalance());
            } else {
                return BigDecimal.ZERO;
            }
        });
        return balance;
    }

    /**
     * 获取交易详情
     */
    public org.web3j.protocol.core.methods.response.Transaction getTransactionByHash(String txHash) throws Exception {
        return this.timeOutWrapperFunction("getTransactionByHash", txHash, args -> {
            org.web3j.protocol.core.methods.response.Transaction transaction = null;
            EthTransaction send = web3j.ethGetTransactionByHash(args).send();
            if (send.getTransaction().isPresent()) {
                transaction = send.getTransaction().get();
            }
            return transaction;
        });
    }

    @Override
    public boolean canTransferBatch() {
        return false;
    }

    @Override
    public void confirmUnspent(Block block) {
    }

    @Override
    public EthSendTransaction sendTransaction(String fromAddress, String secretKey, Map<String, BigDecimal> accounts) {
        return null;
    }

    @Override
    public String sendTransaction(String toAddress, String fromAddress, String secretKey, BigDecimal amount) {
        String result = null;
        //发送eth
        if (toAddress.length() != 42) {
            return null;
        }
        if (secretKey == null) {
            getLog().error("账户私钥不存在!");
        }
        try {
            result = sendMainAsset(fromAddress, secretKey, toAddress, amount, HtgConstant.GAS_LIMIT_OF_MAIN_ASSET, htgContext.getEthGasPrice());
        } catch (Exception e) {
            getLog().error("send fail", e);
        }
        return result;
    }

    /**
     * Method:send
     * Description: 发送交易
     * Author: xinjl
     * Date: 2018/4/16 15:22
     */
    public EthSendTransaction send(String hexValue) {
        EthSendTransaction ethSendTransaction = null;
        try {
            ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
        }
        return ethSendTransaction;
    }

    /**
     * Method:sendMainAsset
     * Description: 发送HT
     * Author: xinjl
     * Date: 2018/4/16 14:55
     */
    public String sendMainAsset(String fromAddress, String privateKey, String toAddress, BigDecimal value, BigInteger gasLimit, BigInteger gasPrice) throws Exception {
        BigDecimal htBalance = getBalance(fromAddress);
        if (htBalance == null) {
            getLog().error("获取当前地址{}余额失败!", symbol());
            return "501";
        }
        //getLog().info(fromAddress + "===账户金额" + convertWeiToEth(ethBalance.toBigInteger()));
        BigInteger bigIntegerValue = convertMainAssetToWei(value);
        if (htBalance.toBigInteger().compareTo(bigIntegerValue.add(gasLimit.multiply(gasPrice))) < 0) {
            //余额小于转账金额与手续费之和
            getLog().error("账户金额小于转账金额与手续费之和!");
            return "502";
        }
        BigInteger nonce = getNonce(fromAddress);
        if (nonce == null) {
            getLog().error("获取当前地址nonce失败!");
            return "503";
        }
        //getLog().info("nonce======>" + nonce);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            getLog().error(e.getMessage(), e);
        }
        RawTransaction etherTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, bigIntegerValue);
        //交易签名
        Credentials credentials = Credentials.create(privateKey);
        byte[] signedMessage = TransactionEncoder.signMessage(etherTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        //发送广播
        EthSendTransaction send = send(hexValue);
        if (send == null || send.getResult() == null) {
            return null;
        }
        if (send.getResult().equals("nonce too low")) {
            sendMainAsset(fromAddress, privateKey, toAddress, value, gasLimit, gasPrice);
        }
        return send.getTransactionHash();
    }

    public String sendMainAssetWithNonce(String fromAddress, String privateKey, String toAddress, BigDecimal value, BigInteger gasLimit, BigInteger gasPrice, BigInteger nonce) throws Exception {
        String hash = this.timeOutWrapperFunction("sendMainAssetWithNonce", List.of(fromAddress, privateKey, toAddress, value, gasLimit, gasPrice, nonce), args -> {
            int i = 0;
            String _fromAddress = (String) args.get(i++);
            String _privateKey = (String) args.get(i++);
            String _toAddress = (String) args.get(i++);
            BigDecimal _value = (BigDecimal) args.get(i++);
            BigInteger _gasLimit = (BigInteger) args.get(i++);
            BigInteger _gasPrice = (BigInteger) args.get(i++);
            BigInteger _nonce = (BigInteger) args.get(i++);
            BigInteger bigIntegerValue = convertMainAssetToWei(_value);
            RawTransaction etherTransaction = RawTransaction.createEtherTransaction(_nonce, _gasPrice, _gasLimit, _toAddress, bigIntegerValue);
            //交易签名
            Credentials credentials = Credentials.create(_privateKey);
            byte[] signedMessage = TransactionEncoder.signMessage(etherTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
            String hexValue = Numeric.toHexString(signedMessage);
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            if (ethSendTransaction == null || ethSendTransaction.getResult() == null) {
                if (ethSendTransaction != null && ethSendTransaction.getError() != null) {
                    getLog().error("Failed to transfer, error: {}", ethSendTransaction.getError().getMessage());
                } else {
                    getLog().error("Failed to transfer");
                }
                return null;
            }
            return ethSendTransaction.getTransactionHash();
        });
        return hash;
    }

    /**
     * 获取nonce，Pending模式 适用于连续转账
     *
     * @param from
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public BigInteger getNonce(String from) throws Exception {
        BigInteger nonce = this.timeOutWrapperFunction("getNonce", from, args -> {
            EthGetTransactionCount transactionCount = web3j.ethGetTransactionCount(args, DefaultBlockParameterName.PENDING).sendAsync().get();
            return transactionCount.getTransactionCount();
        });
        return nonce;
    }

    /**
     * 获取nonce，Latest模式
     *
     * @param from
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public BigInteger getLatestNonce(String from) throws Exception {
        BigInteger nonce = this.timeOutWrapperFunction("getLatestNonce", from, args -> {
            EthGetTransactionCount transactionCount = web3j.ethGetTransactionCount(args, DefaultBlockParameterName.LATEST).sendAsync().get();
            return transactionCount.getTransactionCount();
        });
        return nonce;
    }


    /**
     * 获取eth网络当前gasPrice
     */
    public BigInteger getCurrentGasPrice() throws Exception {
        BigInteger nonce = this.timeOutWrapperFunction("getCurrentGasPrice", null, args -> {
            EthGasPrice send = web3j.ethGasPrice().send();
            if (send == null) {
                return null;
            }
            return send.getGasPrice();
        });
        return nonce;
    }

    /**
     * ERC-20Token交易
     *
     * @param from
     * @param to
     * @param value
     * @param privateKey
     * @return
     * @throws Exception
     */
    public EthSendTransaction transferERC20Token(String from,
                                                 String to,
                                                 BigInteger value,
                                                 String privateKey,
                                                 String contractAddress) throws Exception {
        //加载转账所需的凭证，用私钥
        Credentials credentials = Credentials.create(privateKey);
        //获取nonce，交易笔数
        BigInteger nonce = getNonce(from);

        //创建RawTransaction交易对象
        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(value)),
                Arrays.asList(new TypeReference<Type>() {
                }));

        String encodedFunction = FunctionEncoder.encode(function);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                htgContext.getEthGasPrice(),
                HtgConstant.GAS_LIMIT_OF_ERC20,
                contractAddress, encodedFunction
        );
        //签名Transaction，这里要对交易做签名
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //发送交易
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        return ethSendTransaction;
    }

    @Override
    public EthSendTransaction sendTransaction(String toAddress, String fromAddress, String secretKey, BigDecimal amount, String contractAddress) throws Exception {
        //发送token
        if (toAddress.length() != 42) {
            return null;
        }
        if (secretKey == null) {
            getLog().error("账户私钥不存在!");
        }
        EthSendTransaction result = transferERC20Token(fromAddress, toAddress, amount.toBigInteger(), secretKey, contractAddress);
        return result;
    }

    @Override
    public String convertToNewAddress(String address) {
        return address;
    }

    public BigInteger convertMainAssetToWei(BigDecimal value) {
        BigDecimal cardinalNumber = new BigDecimal("1000000000000000000");
        value = value.multiply(cardinalNumber);
        return value.toBigInteger();
    }

    public static BigDecimal convertWeiToMainAsset(BigInteger balance) {
        BigDecimal cardinalNumber = new BigDecimal("1000000000000000000");
        BigDecimal decimalBalance = new BigDecimal(balance);
        BigDecimal value = decimalBalance.divide(cardinalNumber, 18, RoundingMode.DOWN);
        return value;
    }

    public TransactionReceipt getTxReceipt(String txHash) throws Exception {
        return this.timeOutWrapperFunction("getTxReceipt", txHash, args -> {
            Optional<TransactionReceipt> result = web3j.ethGetTransactionReceipt(args).send().getTransactionReceipt();
            if (result == null || result.isEmpty()) {
                return null;
            }
            return result.get();
        });
    }

    /**
     * 调用合约的view/constant函数
     */
    public List<Type> callViewFunction(String contractAddress, Function function) throws Exception {
        return this.callViewFunction(contractAddress, function, false);
    }

    public List<Type> callViewFunction(String contractAddress, Function function, boolean latest) throws Exception {
        String encode = FunctionEncoder.encode(function);

        List<Type> typeList = this.timeOutWrapperFunction("callViewFunction", List.of(contractAddress, encode, latest), args -> {
            String _contractAddress = (String) args.get(0);
            String _encode = (String) args.get(1);
            boolean _latest = (Boolean) args.get(2);
            org.web3j.protocol.core.methods.request.Transaction ethCallTransaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, _contractAddress, _encode);
            DefaultBlockParameterName parameterName = DefaultBlockParameterName.PENDING;
            if (!htgContext.supportPendingCall() || _latest) {
                parameterName = DefaultBlockParameterName.LATEST;
            }
            EthCall ethCall = web3j.ethCall(ethCallTransaction, parameterName).sendAsync().get();
            String value = ethCall.getResult();
            if (StringUtils.isBlank(value)) {
                return null;
            }
            return FunctionReturnDecoder.decode(value, function.getOutputParameters());
        });
        return typeList;
    }

    public HtgSendTransactionPo callContractRaw(String privateKey, HtgSendTransactionPo resendTxPo) throws Exception {
        HtgSendTransactionPo txPo = this.timeOutWrapperFunction("callContract", List.of(privateKey, resendTxPo), args -> {
            String _privateKey = args.get(0).toString();
            HtgSendTransactionPo _currentTxPo = (HtgSendTransactionPo) args.get(1);
            String from = _currentTxPo.getFrom();
            BigInteger gasLimit = _currentTxPo.getGasLimit();
            String contractAddress = _currentTxPo.getTo();
            String encodedFunction = _currentTxPo.getData();
            Credentials credentials = Credentials.create(_privateKey);
            BigInteger nonce = _currentTxPo.getNonce();
            BigInteger gasPrice = _currentTxPo.getGasPrice();
            BigInteger value = _currentTxPo.getValue();
            value = value == null ? BigInteger.ZERO : value;
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    contractAddress,
                    value,
                    encodedFunction
            );
            //签名Transaction，这里要对交易做签名
            byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
            String hexValue = Numeric.toHexString(signMessage);
            //发送交易
            EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
            if (send == null) {
                throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD, String.format("%s request error", symbol()));
            }
            if (send.hasError()) {
                throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD, send.getError().getMessage());
            }
            return new HtgSendTransactionPo(send.getTransactionHash(), from, rawTransaction);
        });
        return txPo;
    }

    public HtgSendTransactionPo callContract(String from, String privateKey, String contractAddress, BigInteger gasLimit, Function function) throws Exception {
        return this.callContract(from, privateKey, contractAddress, gasLimit, function, null, null);
    }

    public HtgSendTransactionPo callContract(String from, String privateKey, String contractAddress, BigInteger gasLimit, Function function, BigInteger value, BigInteger gasPrice) throws Exception {
        value = value == null ? BigInteger.ZERO : value;
        gasPrice = gasPrice == null || gasPrice.compareTo(BigInteger.ZERO) == 0 ? htgContext.getEthGasPrice() : gasPrice;
        String encodedFunction = FunctionEncoder.encode(function);

        HtgSendTransactionPo txPo = this.timeOutWrapperFunction("callContract", List.of(from, privateKey, contractAddress, gasLimit, encodedFunction, value, gasPrice), args -> {
            int i =0;
            String _from = args.get(i++).toString();
            String _privateKey = args.get(i++).toString();
            String _contractAddress = args.get(i++).toString();
            BigInteger _gasLimit = (BigInteger) args.get(i++);
            String _encodedFunction = args.get(i++).toString();
            BigInteger _value = (BigInteger) args.get(i++);
            BigInteger _gasPrice = (BigInteger) args.get(i++);
            Credentials credentials = Credentials.create(_privateKey);
            BigInteger nonce = this.getNonce(_from);
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    _gasPrice,
                    _gasLimit,
                    _contractAddress,
                    _value,
                    _encodedFunction
            );
            //签名Transaction，这里要对交易做签名
            byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
            String hexValue = Numeric.toHexString(signMessage);
            //发送交易
            EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
            if (send == null) {
                throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD, String.format("%s request error", symbol()));
            }
            if (send.hasError()) {
                throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD, send.getError().getMessage());
            }
            return new HtgSendTransactionPo(send.getTransactionHash(), _from, rawTransaction);
        });
        return txPo;
    }

    public EthCall validateContractCall(String from, String contractAddress, Function function) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        return this.validateContractCall(from, contractAddress, encodedFunction, null);
    }

    public EthCall validateContractCall(String from, String contractAddress, Function function, BigInteger value) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        return this.validateContractCall(from, contractAddress, encodedFunction, value);
    }

    public EthCall validateContractCall(String from, String contractAddress, String encodedFunction) throws Exception {
        return this.validateContractCall(from, contractAddress, encodedFunction, null);
    }

    public EthCall validateContractCall(String from, String contractAddress, String encodedFunction, BigInteger value) throws Exception {
        value = value == null ? BigInteger.ZERO : value;
        EthCall ethCall = this.timeOutWrapperFunction("validateContractCall", List.of(from, contractAddress, encodedFunction, value), args -> {
            String _from = args.get(0).toString();
            String _contractAddress = args.get(1).toString();
            String _encodedFunction = (String) args.get(2);
            BigInteger _value = (BigInteger) args.get(3);

            org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                    _from,
                    null,
                    null,
                    null,
                    _contractAddress,
                    _value,
                    _encodedFunction
            );

            EthCall _ethCall = web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send();
            return _ethCall;
        });
        return ethCall;
    }

    public EthEstimateGas ethEstimateGas(String from, String contractAddress, Function function) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        return this.ethEstimateGas(from, contractAddress, encodedFunction, null);
    }

    public EthEstimateGas ethEstimateGas(String from, String contractAddress, Function function, BigInteger value) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        return this.ethEstimateGas(from, contractAddress, encodedFunction, value);
    }

    public EthEstimateGas ethEstimateGas(String from, String contractAddress, String encodedFunction, BigInteger value) throws Exception {
        value = value == null ? BigInteger.ZERO : value;
        EthEstimateGas gas = this.timeOutWrapperFunction("ethEstimateGas", List.of(from, contractAddress, encodedFunction, value), args -> {
            String _from = args.get(0).toString();
            String _contractAddress = args.get(1).toString();
            String _encodedFunction = (String) args.get(2);
            BigInteger _value = (BigInteger) args.get(3);

            org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                    _from,
                    null,
                    null,
                    null,
                    _contractAddress,
                    _value,
                    _encodedFunction
            );
            EthEstimateGas estimateGas = web3j.ethEstimateGas(tx).send();
            if(StringUtils.isBlank(estimateGas.getResult())) {
                if (estimateGas.getError() != null) {
                    getLog().error("Failed to estimate gas, error: {}", estimateGas.getError().getMessage());
                } else {
                    getLog().error("Failed to estimate gas");
                }
                //return BigInteger.ZERO;
            }
            return estimateGas;
        });
        return gas;
    }


    private <T, R> R timeOutWrapperFunction(String functionName, T arg, ExceptionFunction<T, R> fucntion) throws Exception {
        return this.timeOutWrapperFunctionReal(functionName, fucntion, 0, arg);
    }

    private <T, R> R timeOutWrapperFunctionReal(String functionName, ExceptionFunction<T, R> fucntion, int times, T arg) throws Exception {
        try {
            this.checkIfResetWeb3j(times);
            return fucntion.apply(arg);
        } catch (Exception e) {
            // 当API连接异常时，重置API连接，使用备用API 异常: ClientConnectionException
            if (e instanceof ClientConnectionException) {
                getLog().warn("API连接异常时，重置API连接，使用备用API");
                if (ConverterUtil.isRequestExpired(e.getMessage()) && htgContext.getConfig().getMainRpcAddress().equals(this.rpcAddress)) {
                    getLog().info("重新签名应急API: {}", this.rpcAddress);
                    reSignMainAPI();
                    throw e;
                }
                if (ConverterUtil.isRequestDenied(e.getMessage()) && htgContext.getConfig().getMainRpcAddress().equals(this.rpcAddress)) {
                    getLog().info("重置应急API，{} API 准备切换，当前API: {}", symbol(), this.rpcAddress);
                    requestExceededMap.clear();
                    clearTimeOfRequestExceededMap = 0L;
                    switchStandbyAPI(this.rpcAddress);
                    throw e;
                }
                Integer count = requestExceededMap.computeIfAbsent(this.rpcAddress, r -> 0);
                requestExceededMap.put(this.rpcAddress, count + 1);
                switchStandbyAPI(this.rpcAddress);
                throw e;
            }
            // 若遭遇网络连接异常
            if (e instanceof ConnectException || e instanceof UnknownHostException) {
                // 应急API重置，切换到普通API
                if (htgContext.getConfig().getMainRpcAddress().equals(this.rpcAddress)) {
                    getLog().info("重置应急API，{} API 准备切换，当前API: {}", symbol(), this.rpcAddress);
                    requestExceededMap.clear();
                    clearTimeOfRequestExceededMap = 0L;
                    switchStandbyAPI(this.rpcAddress);
                    throw e;
                }
                // 普通API记录异常次数
                Integer count = requestExceededMap.getOrDefault(this.rpcAddress, 0);
                requestExceededMap.put(this.rpcAddress, count + 1);
                switchStandbyAPI(this.rpcAddress);
                throw e;
            }
            String message = e.getMessage();
            boolean isTimeOut = ConverterUtil.isTimeOutError(message);
            if (isTimeOut) {
                getLog().error("{}: {} function [{}] time out", e.getClass().getName(), symbol(), functionName);
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                return timeOutWrapperFunctionReal(functionName, fucntion, times + 1, arg);
            }
            if (e instanceof SSLHandshakeException || e instanceof SSLException) {
                changeApi(this.rpcAddress);
            }
            getLog().error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Method:getTransactionReceipt
     * Description: 获取合约交易信息
     * Author: xinjl
     * Date: 2018/4/16 15:22
     */
    protected Transaction getContractTransaction(String txHash, String input) throws Exception {
        TransactionReceipt transactionReceipt = this.getTxReceipt(txHash);
        if (transactionReceipt == null || !transactionReceipt.isStatusOK()) {
            return null;
        }
        List<Log> logs = transactionReceipt.getLogs();
        if (logs != null && logs.size() > 0) {
            List<String> topics = logs.get(0).getTopics();
            if (topics.get(0).substring(0, 10).equals("0xa9059cbb") || input.substring(0, 10).equals("0xa9059cbb")) {
                //为转账
                String fromAddress = "0x" + topics.get(1).substring(26, topics.get(1).length()).toString();
                String toAddress = "0x" + topics.get(2).substring(26, topics.get(1).length()).toString();
                String data;
                if (topics.size() == 3) {
                    data = logs.get(0).getData();
                } else {
                    data = topics.get(3);
                }
                String[] v = data.split("x");
                BigDecimal amount = new BigDecimal(new BigInteger(v[1], 16));
                Transaction transferTransaction = new Transaction();
                transferTransaction.setFromAddress(fromAddress);
                transferTransaction.setToAddress(toAddress);
                transferTransaction.setTxHash(txHash);
                transferTransaction.setAmount(amount);
                return transferTransaction;
            }
        } else {
            if (input != null && input.length() > 10 && input.substring(0, 10).equals("0xa9059cbb")) {
                //0xa9059cbb000000000000000000000000240149e9b5f611a6784f663e56234dc021e5a999000000000000000000000000000000000000000000000049b9ca9a6943400000
                String address = input.substring(34, 74);
                String to = "0x" + address;
                String value = input.substring(75, 138);
                while (value.startsWith("0")) {
                    value = value.substring(1, value.length());
                }
                if (StringUtils.isBlank(value)) {
                    getLog().error("tx value is null : " + txHash);
                    return null;
                }
                BigDecimal amount = new BigDecimal(new BigInteger(value, 16));
                Transaction transferTransaction = new Transaction();
                transferTransaction.setFromAddress(transactionReceipt.getFrom());
                transferTransaction.setToAddress(to);
                transferTransaction.setTxHash(txHash);
                transferTransaction.setAmount(amount);
                return transferTransaction;
            }
        }
        return null;
    }

    public void setWeb3j(Web3j web3j) {
        this.web3j = web3j;
    }

    public void setEthRpcAddress(String rpcAddress) {
        this.rpcAddress = rpcAddress;
    }

    public Web3j getWeb3j() {
        return web3j;
    }


    /**
     * 获取ERC-20 token指定地址余额
     *
     * @param address         查询地址
     * @param contractAddress 合约地址
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public BigInteger getERC20Balance(String address, String contractAddress) throws Exception {
        return this.getERC20BalanceReal(address, contractAddress, DefaultBlockParameterName.LATEST, 0);
    }

    private BigInteger getERC20BalanceReal(String address, String contractAddress, DefaultBlockParameterName status, int times) throws Exception {
        try {
            this.checkIfResetWeb3j(times);
            Function function = new Function("balanceOf",
                    Arrays.asList(new Address(address)),
                    Arrays.asList(new TypeReference<Address>() {
                    }));

            String encode = FunctionEncoder.encode(function);
            org.web3j.protocol.core.methods.request.Transaction ethCallTransaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(address, contractAddress, encode);
            EthCall ethCall = web3j.ethCall(ethCallTransaction, status).sendAsync().get();
            String value = ethCall.getResult();
            BigInteger balance = new BigInteger(value.substring(2), 16);
            return balance;
        } catch (Exception e) {
            String message = e.getMessage();
            boolean isTimeOut = ConverterUtil.isTimeOutError(message);
            if (isTimeOut) {
                getLog().error("{} ERC20 balance time out", symbol());
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                return getERC20BalanceReal(address, contractAddress, status, times + 1);
            } else {
                getLog().error(e.getMessage(), e);
                throw e;
            }
        }

    }

}
