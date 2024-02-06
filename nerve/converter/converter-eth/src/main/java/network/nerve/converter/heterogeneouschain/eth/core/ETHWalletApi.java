package network.nerve.converter.heterogeneouschain.eth.core;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.helper.EthAccountHelper;
import network.nerve.converter.heterogeneouschain.eth.model.EthSendTransactionPo;
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

import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.ETH_GAS_LIMIT_OF_ETH;
import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.ETH_GAS_LIMIT_OF_USDT;


/**
 * @package com.bloex.wallet.util
 * @Author admin
 * @Date 2018/4/16
 * @project bloex-application
 */
@Component
public class ETHWalletApi implements WalletApi {

    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private AddressBloomFilter addressBloomFilter;
    @Autowired
    private EthAccountHelper ethAccountHelper;

    protected Web3j web3j;

    protected String ethRpcAddress;
    int switchStatus = 0;
    private boolean inited = false;
    private Map<String, Integer> requestExceededMap = new HashMap<>();
    private long clearTimeOfRequestExceededMap = 0L;

    private Map<String, BigInteger> map = new HashMap<>();
    private ReentrantLock checkLock = new ReentrantLock();
    private ReentrantLock resetLock = new ReentrantLock();
    private ReentrantLock reSignLock = new ReentrantLock();

    private NulsLogger getLog() {
        return EthContext.logger();
    }

    public void init(String ethRpcAddress) throws NulsException {
        initialize();
        // Default initializedAPIWill be replaced by new onesAPIService coverage, when a node becomes a virtual bank, a new one will be initializedAPIservice
        if (web3j != null && EthContext.getConfig().getCommonRpcAddress().equals(this.ethRpcAddress) && !ethRpcAddress.equals(this.ethRpcAddress)) {
            resetWeb3j();
        }
        if (web3j == null) {
            web3j = newInstanceWeb3j(ethRpcAddress);
            this.ethRpcAddress = ethRpcAddress;
            getLog().info("initialization ETH API URL: {}", ethRpcAddress);
        }
    }

    public void checkApi(int order) throws NulsException {
        long now = System.currentTimeMillis();
        // If using emergencyAPIEmergency responseAPIDuring use, do not checkAPIswitch
        if (now < clearTimeOfRequestExceededMap) {
            if (EthContext.getConfig().getMainRpcAddress().equals(this.ethRpcAddress)) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("emergencyAPIDuring use, do not checkAPIswitch, Expiration time: {}, remaining waiting time: {}", clearTimeOfRequestExceededMap, clearTimeOfRequestExceededMap - now);
                }
                return;
            }
        } else if (clearTimeOfRequestExceededMap != 0){
            getLog().info("Reset EmergencyAPI,ETH API Ready to switch, currentlyAPI: {}", this.ethRpcAddress);
            requestExceededMap.clear();
            clearTimeOfRequestExceededMap = 0L;
        }

        String rpc = this.calRpcBySwitchStatus(order, switchStatus);
        if (!rpc.equals(this.ethRpcAddress)) {
            checkLock.lock();
            try {
                if (!rpc.equals(this.ethRpcAddress)) {
                    getLog().info("Detected a change in sequence,ETH API Ready to switch, currentlyAPI: {}", this.ethRpcAddress);
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
        List<String> rpcAddressList = EthContext.RPC_ADDRESS_LIST;
        if(tempSwitchStatus == 1) {
            rpcAddressList = EthContext.STANDBY_RPC_ADDRESS_LIST;
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
        this.ethRpcAddress = null;
        reSignLock.lock();
        try {
            if (this.ethRpcAddress == null) {
                getLog().info("Enter re signing");
                changeApi(EthContext.getConfig().getMainRpcAddress());
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
        getLog().info("ETH API Ready to switch, currentlyAPI: {}", oldRpc);
        resetLock.lock();
        try {
            // Unequal, indicating that it has been switched
            if (!oldRpc.equals(this.ethRpcAddress)) {
                getLog().info("ETH API Switched to: {}", this.ethRpcAddress);
                return;
            }
            int expectSwitchStatus = (switchStatus + 1) % 2;
            int order = EthContext.getConverterCoreApi().getVirtualBankOrder();
            String rpc = this.calRpcBySwitchStatus(order, expectSwitchStatus);
            // Check the configurationAPIIs it excessive
            if (unavailableRpc(oldRpc) && unavailableRpc(rpc)) {
                String mainRpcAddress = EthContext.getConfig().getMainRpcAddress();
                getLog().info("ETH API Not available: {} - {}, Prepare to switch to emergency modeAPI: {}, ", oldRpc, rpc, mainRpcAddress);
                if (!mainRpcAddress.equals(this.ethRpcAddress)) {
                    changeApi(mainRpcAddress);
                    if (mainRpcAddress.equals(this.ethRpcAddress)) {
                        clearTimeOfRequestExceededMap = System.currentTimeMillis() + EthConstant.HOURS_3;
                    }
                }
                return;
            }
            // Normal switchingAPI
            if (!rpc.equals(this.ethRpcAddress)) {
                changeApi(rpc);
                // Equal, indicating successful switching
                if (rpc.equals(this.ethRpcAddress)) {
                    switchStatus = expectSwitchStatus;
                }
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
        if (mod == 5 && web3j != null && ethRpcAddress != null) {
            getLog().info("restartAPIservice");
            resetWeb3j();
            web3j = newInstanceWeb3j(ethRpcAddress);
        }
    }

    private Web3j newInstanceWeb3j(String ethRpcAddress) throws NulsException {
        //Web3j web3j;
        //if (EthContext.getConfig().getMainRpcAddress().equals(ethRpcAddress)) {
        //    String data = String.valueOf(System.currentTimeMillis());
        //    String sign = ethAccountHelper.sign(data);
        //    web3j = Web3j.build(new HttpService(ethRpcAddress + String.format("?d=%s&s=%s&p=%s", data, sign, EthContext.ADMIN_ADDRESS_PUBLIC_KEY)));
        //} else {
        //    web3j = Web3j.build(new HttpService(ethRpcAddress));
        //}
        Web3j web3j = Web3j.build(new HttpService(ethRpcAddress));
        return web3j;
    }

    /**
     * Method:getBlock
     * Description: Obtain block information
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
                BigDecimal value = convertWeiToEth(transaction.getValue());
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
     * Description: Obtain blocks based on height
     * Author: xinjl
     * Date: 2018/4/16 15:23
     */
    public EthBlock.Block getBlockByHeight(Long height) throws Exception {
        EthBlock.Block block = this.timeOutWrapperFunction("getBlockByHeight", height, args -> {
            return web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(args), true).send().getBlock();
        });
        if (block == null) {
            getLog().error("Get block empty");
        }
        return block;
    }

    /**
     * Obtain block heads based on height
     */
    public EthBlock.Block getBlockHeaderByHeight(Long height) throws Exception {
        EthBlock.Block header = this.timeOutWrapperFunction("getBlockHeaderByHeight", height, args -> {
            return web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(args), false).send().getBlock();
        });
        if (header == null) {
            getLog().error("Get block header empty");
        }
        return header;
    }

    /**
     * Method:Obtain on chain transactions
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
                getLog().error("Transaction details acquisition failed:" + transaction + ",height:" + height + ",index:" + index);
            }
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
        }
        return transaction;
    }

    /**
     * Method:getEthBalance
     * Description: obtainethbalance
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
     * Get transaction details
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
        //sendeth
        if (toAddress.length() != 42) {
            return null;
        }
        if (secretKey == null) {
            getLog().error("The account private key does not exist!");
        }
        try {
            result = sendETH(fromAddress, secretKey, toAddress, amount, ETH_GAS_LIMIT_OF_ETH, EthContext.getEthGasPrice());
        } catch (Exception e) {
            getLog().error("send fail", e);
        }
        return result;
    }

    /**
     * Method:send
     * Description: Send transaction
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
     * Method:sendETH
     * Description: Send Ethereum
     * Author: xinjl
     * Date: 2018/4/16 14:55
     */
    public String sendETH(String fromAddress, String privateKey, String toAddress, BigDecimal value, BigInteger gasLimit, BigInteger gasPrice) throws Exception {
        BigDecimal ethBalance = getBalance(fromAddress);
        if (ethBalance == null) {
            getLog().error("Get the current addressETHBalance failed!");
            return "501";
        }
        //getLog().info(fromAddress + "===Account amount" + convertWeiToEth(ethBalance.toBigInteger()));
        BigInteger bigIntegerValue = convertEthToWei(value);
        if (ethBalance.toBigInteger().compareTo(bigIntegerValue.add(gasLimit.multiply(gasPrice))) < 0) {
            //The balance is less than the sum of the transfer amount and handling fee
            getLog().error("The account amount is less than the sum of the transfer amount and handling fee!");
            return "502";
        }
        BigInteger nonce = getNonce(fromAddress);
        if (nonce == null) {
            getLog().error("Get the current addressnoncefail!");
            return "503";
        }
        //getLog().info("nonce======>" + nonce);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            getLog().error(e.getMessage(), e);
        }
        RawTransaction etherTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, bigIntegerValue);
        //Transaction signature
        Credentials credentials = Credentials.create(privateKey);
        byte[] signedMessage = TransactionEncoder.signMessage(etherTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        //Send broadcast
        EthSendTransaction send = send(hexValue);
        if (send == null || send.getResult() == null) {
            return null;
        }
        if (send.getResult().equals("nonce too low")) {
            sendETH(fromAddress, privateKey, toAddress, value, gasLimit, gasPrice);
        }
        return send.getTransactionHash();
    }

    /**
     * obtainnonce LATEST mode + Self maintenancenonce
     *
     * @param address
     * @return
     */
    public BigInteger getNonceLatestAndMemory(String address) {
        //obtainnonce
        if (map.size() > 0 && map.containsKey(address)) {
            BigInteger nonce = map.get(address);
            nonce = nonce.add(BigInteger.ONE);
            map.replace(address, nonce);
            return nonce.subtract(BigInteger.ONE);
        } else {
            EthGetTransactionCount ethGetTransactionCount = null;
            try {
                ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).sendAsync().get();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();
                nonce = nonce.add(BigInteger.ONE);
                map.put(address, nonce);
                return nonce.subtract(BigInteger.ONE);
            } catch (InterruptedException e) {
                getLog().error(e.getMessage(), e);
            } catch (ExecutionException e) {
                getLog().error(e.getMessage(), e);
            }
            return BigInteger.ZERO;

        }
    }

    public String sendETHWithNonce(String fromAddress, String privateKey, String toAddress, BigDecimal value, BigInteger gasLimit, BigInteger gasPrice, BigInteger nonce) throws Exception {
        String hash = this.timeOutWrapperFunction("sendETHWithNonce", List.of(fromAddress, privateKey, toAddress, value, gasLimit, gasPrice, nonce), args -> {
            int i = 0;
            String _fromAddress = (String) args.get(i++);
            String _privateKey = (String) args.get(i++);
            String _toAddress = (String) args.get(i++);
            BigDecimal _value = (BigDecimal) args.get(i++);
            BigInteger _gasLimit = (BigInteger) args.get(i++);
            BigInteger _gasPrice = (BigInteger) args.get(i++);
            BigInteger _nonce = (BigInteger) args.get(i++);
            BigInteger bigIntegerValue = convertEthToWei(_value);
            RawTransaction etherTransaction = RawTransaction.createEtherTransaction(_nonce, _gasPrice, _gasLimit, _toAddress, bigIntegerValue);
            //Transaction signature
            Credentials credentials = Credentials.create(_privateKey);
            byte[] signedMessage = TransactionEncoder.signMessage(etherTransaction, 1, credentials);
            String hexValue = Numeric.toHexString(signedMessage);
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            if (ethSendTransaction == null || ethSendTransaction.getResult() == null) {
                return null;
            }
            return ethSendTransaction.getTransactionHash();
        });
        return hash;
    }

    /**
     * obtainnonce,Pendingmode Suitable for continuous transfers
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
     * obtainnonce,Latestmode
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

    // test+
    /*private Web3j mainWeb3j;
    public Web3j getMainWeb3j() {
        if (mainWeb3j == null) {
            String mainEthRpcAddress = "https://mainnet.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5";
            mainWeb3j = Web3j.build(new HttpService(mainEthRpcAddress));
        }
        return mainWeb3j;
    }*/
    // test-

    /**
     * obtainethNetwork currentgasPrice
     */
    public BigInteger getCurrentGasPrice() throws Exception {
        BigInteger nonce = this.timeOutWrapperFunction("getCurrentGasPrice", null, args -> {
            EthGasPrice send = web3j.ethGasPrice().send();
            // test+
            //EthGasPrice send = getMainWeb3j().ethGasPrice().send();
            // test-
            if (send == null) {
                return null;
            }
            return send.getGasPrice();
        });
        return nonce;
    }

    /**
     * ERC-20Tokentransaction
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
        //Load the required credentials for the transfer using a private key
        Credentials credentials = Credentials.create(privateKey);
        //obtainnonceNumber of transactions
        BigInteger nonce = getNonce(from);

        //establishRawTransactionTrading partner
        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(value)),
                Arrays.asList(new TypeReference<Type>() {
                }));

        String encodedFunction = FunctionEncoder.encode(function);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                EthContext.getEthGasPrice(),
                ETH_GAS_LIMIT_OF_USDT,
                contractAddress, encodedFunction
        );
        //autographTransactionHere, we need to sign the transaction
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //Send transaction
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        return ethSendTransaction;
    }

    @Override
    public EthSendTransaction sendTransaction(String toAddress, String fromAddress, String secretKey, BigDecimal amount, String contractAddress) throws Exception {
        //sendtoken
        if (toAddress.length() != 42) {
            return null;
        }
        if (secretKey == null) {
            getLog().error("The account private key does not exist!");
        }
        EthSendTransaction result = transferERC20Token(fromAddress, toAddress, amount.toBigInteger(), secretKey, contractAddress);
        return result;
    }

    @Override
    public String convertToNewAddress(String address) {
        return address;
    }

    public BigInteger convertEthToWei(BigDecimal value) {
        BigDecimal cardinalNumber = new BigDecimal("1000000000000000000");
        value = value.multiply(cardinalNumber);
        return value.toBigInteger();
    }

    public static BigDecimal convertWeiToEth(BigInteger balance) {
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
     * Invoke contractview/constantfunction
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
            if (_latest) {
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

    public EthSendTransactionPo callContractRaw(String privateKey, EthSendTransactionPo resendTxPo) throws Exception {
        EthSendTransactionPo txPo = this.timeOutWrapperFunction("callContract", List.of(privateKey, resendTxPo), args -> {
            String _privateKey = args.get(0).toString();
            EthSendTransactionPo _currentTxPo = (EthSendTransactionPo) args.get(1);
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
            //autographTransactionHere, we need to sign the transaction
            byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signMessage);
            //Send transaction
            EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
            if (send == null) {
                throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD, "ETH request error");
            }
            if (send.hasError()) {
                throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD, send.getError().getMessage());
            }
            return new EthSendTransactionPo(send.getTransactionHash(), from, rawTransaction);
        });
        return txPo;
    }

    public EthSendTransactionPo callContract(String from, String privateKey, String contractAddress, BigInteger gasLimit, Function function) throws Exception {
        return this.callContract(from, privateKey, contractAddress, gasLimit, function, null, null);
    }

    public EthSendTransactionPo callContract(String from, String privateKey, String contractAddress, BigInteger gasLimit, Function function, BigInteger value, BigInteger gasPrice) throws Exception {
        value = value == null ? BigInteger.ZERO : value;
        gasPrice = gasPrice == null || gasPrice.compareTo(BigInteger.ZERO) == 0 ? EthContext.getEthGasPrice() : gasPrice;
        String encodedFunction = FunctionEncoder.encode(function);

        EthSendTransactionPo txPo = this.timeOutWrapperFunction("callContract", List.of(from, privateKey, contractAddress, gasLimit, encodedFunction, value, gasPrice), args -> {
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
            //autographTransactionHere, we need to sign the transaction
            byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signMessage);
            //Send transaction
            EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
            if (send == null) {
                throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD, "ETH request error");
            }
            if (send.hasError()) {
                throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD, send.getError().getMessage());
            }
            return new EthSendTransactionPo(send.getTransactionHash(), _from, rawTransaction);
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
                    BigInteger.ONE,
                    EthConstant.ETH_ESTIMATE_GAS,
                    _contractAddress,
                    _value,
                    _encodedFunction
            );

            EthCall _ethCall = web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send();
            return _ethCall;
        });
        return ethCall;
    }

    public BigInteger ethEstimateGas(String from, String contractAddress, Function function) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        return this.ethEstimateGas(from, contractAddress, encodedFunction, null);
    }

    public BigInteger ethEstimateGas(String from, String contractAddress, Function function, BigInteger value) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        return this.ethEstimateGas(from, contractAddress, encodedFunction, value);
    }

    public BigInteger ethEstimateGas(String from, String contractAddress, String encodedFunction, BigInteger value) throws Exception {
        value = value == null ? BigInteger.ZERO : value;
        BigInteger gas = this.timeOutWrapperFunction("ethEstimateGas", List.of(from, contractAddress, encodedFunction, value), args -> {
            String _from = args.get(0).toString();
            String _contractAddress = args.get(1).toString();
            String _encodedFunction = (String) args.get(2);
            BigInteger _value = (BigInteger) args.get(3);

            org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                    _from,
                    null,
                    BigInteger.ONE,
                    EthConstant.ETH_ESTIMATE_GAS,
                    _contractAddress,
                    _value,
                    _encodedFunction
            );
            EthEstimateGas estimateGas = web3j.ethEstimateGas(tx).send();
            if(StringUtils.isBlank(estimateGas.getResult())) {
                return BigInteger.ZERO;
            }
            return estimateGas.getAmountUsed();
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
            // WhenAPIReset when there is an abnormal connectionAPIConnection, using backupAPI abnormal: ClientConnectionException
            if (e instanceof ClientConnectionException) {
                getLog().warn("APIReset when there is an abnormal connectionAPIConnection, using backupAPI");
                if (ConverterUtil.isRequestExpired(e.getMessage()) && EthContext.getConfig().getMainRpcAddress().equals(this.ethRpcAddress)) {
                    getLog().info("Re sign EmergencyAPI: {}", this.ethRpcAddress);
                    reSignMainAPI();
                    throw e;
                }
                if (ConverterUtil.isRequestDenied(e.getMessage()) && EthContext.getConfig().getMainRpcAddress().equals(this.ethRpcAddress)) {
                    getLog().info("Reset EmergencyAPI,ETH API Ready to switch, currentlyAPI: {}", this.ethRpcAddress);
                    requestExceededMap.clear();
                    clearTimeOfRequestExceededMap = 0L;
                    switchStandbyAPI(this.ethRpcAddress);
                    throw e;
                }
                // daily request count exceeded
                if (ConverterUtil.isRequestExceeded(e.getMessage())) {
                    Integer count = requestExceededMap.computeIfAbsent(this.ethRpcAddress, r -> 0);
                    requestExceededMap.put(this.ethRpcAddress, count + 1);
                }
                switchStandbyAPI(this.ethRpcAddress);
                throw e;
            }
            // If encountering abnormal network connection
            if (e instanceof ConnectException || e instanceof UnknownHostException) {
                // emergencyAPIReset, switch to normalAPI
                if (EthContext.getConfig().getMainRpcAddress().equals(this.ethRpcAddress)) {
                    getLog().info("Reset EmergencyAPI,ETH API Ready to switch, currentlyAPI: {}", this.ethRpcAddress);
                    requestExceededMap.clear();
                    clearTimeOfRequestExceededMap = 0L;
                    switchStandbyAPI(this.ethRpcAddress);
                    throw e;
                }
                // ordinaryAPIRecord the number of exceptions
                Integer count = requestExceededMap.computeIfAbsent(this.ethRpcAddress, r -> 0);
                requestExceededMap.put(this.ethRpcAddress, count + 1);
                switchStandbyAPI(this.ethRpcAddress);
                throw e;
            }
            String message = e.getMessage();
            boolean isTimeOut = ConverterUtil.isTimeOutError(message);
            if (isTimeOut) {
                getLog().error("{}: eth function [{}] time out", e.getClass().getName(), functionName);
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                return timeOutWrapperFunctionReal(functionName, fucntion, times + 1, arg);
            }
            getLog().error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Method:getTransactionReceipt
     * Description: Obtain contract transaction information
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
                //For transfer
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

    public void setEthRpcAddress(String ethRpcAddress) {
        this.ethRpcAddress = ethRpcAddress;
    }

    public Web3j getWeb3j() {
        return web3j;
    }

    /**
     * Method:getBlock
     * Description: Get specifiedERC20Block transaction information of the contract
     * Author: xinjl
     * Date: 2018/4/16 15:23
     */
    public Block getERC20Block(long height, String contractAddress) throws Exception {
        EthBlock.Block ethBlock = getBlockByHeight(height);
        Block localBlock = new Block();
        localBlock.setHeight(height);
        localBlock.setHash(ethBlock.getHash());
        localBlock.setPreHash(ethBlock.getParentHash());
        localBlock.setTimestamp(ethBlock.getTimestamp().longValue());
        List<EthBlock.TransactionResult> ethTransactionResults = ethBlock.getTransactions();
        if (ethTransactionResults != null && ethTransactionResults.size() > 0) {
            List<Transaction> localTransactions = new ArrayList<>();
            for (int i = 0; i < ethTransactionResults.size(); i++) {
                org.web3j.protocol.core.methods.response.Transaction ethTransaction = (org.web3j.protocol.core.methods.response.Transaction) ethTransactionResults.get(i).get();
                if (ethTransaction != null && ethTransaction.getTo() != null && ethTransaction.getTo().equals(contractAddress)) {
                    Transaction contractTransaction = getContractTransaction(ethTransaction.getHash(), ethTransaction.getInput());
                    //User recharge transaction Collecting transactions Withdrawal transaction Write block
                    if (contractTransaction != null && contractTransaction.getToAddress() != null && addressBloomFilter.contains(contractTransaction.getToAddress())) {
                        if (getLog().isDebugEnabled()) {
                            getLog().debug("Eth Block Height is [{}], Detected contract transaction in ETH is {}", height, contractTransaction.toString());
                        }
                        contractTransaction.setFromAddress(ethTransaction.getFrom());
                        localTransactions.add(contractTransaction);
                    }
                }
            }
            localBlock.setTransactions(localTransactions);
        }
        return localBlock;
    }

    /**
     * obtainERC-20 tokenDesignated address balance
     *
     * @param address         Search address
     * @param contractAddress Contract address
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public BigInteger getERC20Balance(String address, String contractAddress) throws Exception {
        return this.getERC20BalanceReal(address, contractAddress, DefaultBlockParameterName.PENDING, 0);
    }

    public BigInteger getERC20Balance(String address, String contractAddress, DefaultBlockParameterName status) throws Exception {
        return this.getERC20BalanceReal(address, contractAddress, status, 0);
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
                getLog().error("eth ERC20 balance time out");
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
