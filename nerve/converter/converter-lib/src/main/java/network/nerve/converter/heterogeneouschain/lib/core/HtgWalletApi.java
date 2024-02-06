package network.nerve.converter.heterogeneouschain.lib.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgAccountHelper;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.utils.ConverterUtil;
import okhttp3.OkHttpClient;
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
import org.web3j.protocol.core.Response;
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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static okhttp3.ConnectionSpec.CLEARTEXT;

/**
 * @package com.bloex.wallet.util
 * @Author admin
 * @Date 2018/4/16
 * @project bloex-application
 */
public class HtgWalletApi implements WalletApi, BeanInitial {

    private static LoadingCache<TxKey, org.web3j.protocol.core.methods.response.Transaction> TX_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(50)
            .maximumSize(200)
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build(new CacheLoader<TxKey, org.web3j.protocol.core.methods.response.Transaction>() {
                @Override
                public org.web3j.protocol.core.methods.response.Transaction load(TxKey txKey) throws Exception {
                    return txKey.getHtgWalletApi().getTransactionByHashReal(txKey.getTxHash());
                }
            });

    private static LoadingCache<TxKey, TransactionReceipt> TX_RECEIPT_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(50)
            .maximumSize(200)
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build(new CacheLoader<TxKey, TransactionReceipt>() {
                @Override
                public TransactionReceipt load(TxKey txKey) throws Exception {
                    return txKey.getHtgWalletApi().getTxReceiptReal(txKey.getTxHash());
                }
            });

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
    private volatile boolean urlFromThirdParty = false;
    private volatile boolean urlFromThirdPartyForce = false;
    private Map<String, Integer> requestExceededMap = new HashMap<>();
    private long clearTimeOfRequestExceededMap = 0L;
    private int rpcVersion = -1;
    private boolean reSyncBlock = false;

    private Map<String, BigInteger> map = new HashMap<>();
    private ReentrantLock checkLock = new ReentrantLock();
    private ReentrantLock reSignLock = new ReentrantLock();

    private NulsLogger getLog() {
        return htgContext.logger();
    }

    public void init(String rpcAddress) throws NulsException {
        initialize();
        // Default initializedAPIWill be replaced by new onesAPIService coverage, when a node becomes a virtual bank, a new one will be initializedAPIservice
        if (web3j != null && htgContext.getConfig().getCommonRpcAddress().equals(this.rpcAddress) && !rpcAddress.equals(this.rpcAddress)) {
            resetWeb3j();
        }
        if (web3j == null) {
            web3j = newInstanceWeb3j(rpcAddress);
            this.rpcAddress = rpcAddress;
            getLog().info("initialization {} API URL: {}", symbol(), rpcAddress);
        }
    }

    public void checkApi(int order) throws NulsException {
        checkLock.lock();
        try {
            do {
                // Force updates from third-party systems rpc
                Map<Long, Map> rpcCheckMap = htgContext.getConverterCoreApi().HTG_RPC_CHECK_MAP();
                Map<String, Object> resultMap = rpcCheckMap.get(htgContext.getConfig().getChainIdOnHtgNetwork());
                if (resultMap == null) {
                    //getLog().warn("Empty resultMap! {} rpc check from third party, version: {}", symbol(), rpcVersion);
                    break;
                }
                Integer _version = (Integer) resultMap.get("rpcVersion");
                if (_version == null) {
                    //getLog().warn("Empty rpcVersion! {} rpc check from third party, version: {}", symbol(), rpcVersion);
                    break;
                }
                if (this.rpcVersion == -1) {
                    this.rpcVersion = _version.intValue();
                    getLog().info("initialization {} rpc check from third party, version: {}", symbol(), rpcVersion);
                    break;
                }
                if (this.rpcVersion == _version.intValue()){
                    //getLog().info("Same version {} rpc check from third party, version: {}", symbol(), rpcVersion);
                    break;
                }
                if (_version.intValue() > this.rpcVersion){
                    // findversionChange, switchrpc
                    Integer _index = (Integer) resultMap.get("index");
                    if (_index == null) {
                        getLog().warn("Empty index! {} rpc check from third party, version: {}", symbol(), rpcVersion);
                        break;
                    }
                    String apiUrl = (String) resultMap.get("extend" + (_index + 1));
                    if (StringUtils.isBlank(apiUrl)) {
                        getLog().warn("Empty apiUrl! {} rpc check from third party, version: {}", symbol(), rpcVersion);
                        break;
                    }
                    getLog().info("Checked that changes are neededRPCservice {} rpc check from third party, version: {}, url: {}", symbol(), _version.intValue(), apiUrl);
                    TX_CACHE.invalidateAll();
                    TX_RECEIPT_CACHE.invalidateAll();
                    this.changeApi(apiUrl);
                    this.rpcVersion = _version.intValue();
                    this.urlFromThirdPartyForce = true;
                    this.reSyncBlock = true;
                    return;
                }
            } while (false);

            if (this.urlFromThirdPartyForce) {
                getLog().info("[{}]Mandatory emergency responseAPI(ThirdParty)During the use period, no longer based onbank orderswitchAPI", symbol());
                return;
            }

            long now = System.currentTimeMillis();
            // If using emergencyAPIEmergency responseAPIDuring use, do not checkAPIswitch
            if (now < clearTimeOfRequestExceededMap) {
                if (htgContext.getConfig().getMainRpcAddress().equals(this.rpcAddress)) {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("emergencyAPIDuring use, do not checkAPIswitch, Expiration time: {}, remaining waiting time: {}", clearTimeOfRequestExceededMap, clearTimeOfRequestExceededMap - now);
                    }
                    return;
                }
                if (urlFromThirdParty) {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("emergencyAPI(ThirdParty)During use, do not checkAPIswitch, Expiration time: {}, remaining waiting time: {}", clearTimeOfRequestExceededMap, clearTimeOfRequestExceededMap - now);
                    }
                    return;
                }
            } else if (clearTimeOfRequestExceededMap != 0){
                getLog().info("Reset EmergencyAPI,{} API Ready to switch, currentlyAPI: {}", symbol(), this.rpcAddress);
                requestExceededMap.clear();
                clearTimeOfRequestExceededMap = 0L;
                urlFromThirdParty = false;
            }

            String rpc = this.calRpcBySwitchStatus(order, switchStatus);
            if (!rpc.equals(this.rpcAddress)) {
                getLog().info("Detected a change in sequence,{} API Ready to switch, currentlyAPI: {}", symbol(), this.rpcAddress);
                changeApi(rpc);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            checkLock.unlock();
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
                getLog().info("Enter re signing");
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
        checkLock.lock();
        try {
            if (urlFromThirdParty || urlFromThirdPartyForce) {
                getLog().info("emergencyAPI(ThirdParty)During use, do not checkAPIswitch");
                return;
            }
            getLog().info("{} API Ready to switch, currentlyAPI: {}", symbol(), oldRpc);
            // Unequal, indicating that it has been switched
            if (!oldRpc.equals(this.rpcAddress)) {
                getLog().info("{} API Switched to: {}", symbol(), this.rpcAddress);
                return;
            }
            int expectSwitchStatus = (switchStatus + 1) % 2;
            int order = htgContext.getConverterCoreApi().getVirtualBankOrder();
            String rpc = this.calRpcBySwitchStatus(order, expectSwitchStatus);
            // Check the configurationAPIIs it excessive
            if (unavailableRpc(oldRpc) && unavailableRpc(rpc)) {
                String mainRpcAddress = htgContext.getConfig().getMainRpcAddress();
                getLog().info("{} API Not available: {} - {}, Prepare to switch to emergency modeAPI: {}, ", symbol(), oldRpc, rpc, mainRpcAddress);
                changeApi(mainRpcAddress);
                if (mainRpcAddress.equals(this.rpcAddress)) {
                    clearTimeOfRequestExceededMap = System.currentTimeMillis() + HtgConstant.HOURS_3;
                }
                return;
            }
            // Normal switchingAPI
            changeApi(rpc);
            // Equal, indicating successful switching
            if (rpc.equals(this.rpcAddress)) {
                switchStatus = expectSwitchStatus;
            }
        } catch (NulsException e) {
            throw e;
        } finally {
            checkLock.unlock();
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

    public void initialize() {
    }

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
            getLog().info("restartAPIservice");
            resetWeb3j();
            web3j = newInstanceWeb3j(rpcAddress);
        }
    }

    private Web3j newInstanceWeb3j(String rpcAddress) throws NulsException {
        if (htgContext.getConfig().isProxy()) {
            String httpProxy = htgContext.getConfig().getHttpProxy();
            String[] split = httpProxy.split(":");
            final OkHttpClient.Builder builder =
                    new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(split[0], Integer.parseInt(split[1])))).connectionSpecs(Arrays.asList(INFURA_CIPHER_SUITE_SPEC, CLEARTEXT));
            OkHttpClient okHttpClient = builder.build();
            Web3j web3j = Web3j.build(new HttpService(rpcAddress, okHttpClient));
            return web3j;
        }
        Web3j web3j = Web3j.build(new HttpService(rpcAddress));
        return web3j;
    }

    /**
     * Method:getBlockByHeight
     * Description: Obtain blocks based on height
     * Author: xinjl
     * Date: 2018/4/16 15:23
     */
    public EthBlock.Block getBlockByHeight(Long height) throws Exception {
        EthBlock.Block block = this.timeOutWrapperFunction("getBlockByHeight", height, args -> {
            EthBlock send = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(args), true).send();
            Response.Error error = send.getError();
            if (error != null) {
                getLog().error("Block query exception,errorMsg: {}, errorData: {}-{}", error.getMessage(), error.getCode(), error.getData());
            }
            return send.getBlock();
        });
        if (block == null) {
            getLog().error("Get block empty, height: {}, rpc: {}", height, this.rpcAddress);
        }
        return block;
    }

    /**
     * Obtain block heads based on height
     */
    public EthBlock.Block getBlockHeaderByHeight(Long height) throws Exception {
        EthBlock.Block header = this.timeOutWrapperFunction("getBlockHeaderByHeight", height, args -> {
            EthBlock send = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(args), false).send();
            Response.Error error = send.getError();
            if (error != null) {
                getLog().error("Block header query exception,errorMsg: {}, errorData: {}-{}", error.getMessage(), error.getCode(), error.getData());
            }
            return send.getBlock();
        });
        if (header == null) {
            getLog().error("Get block header empty, height: {}, rpc: {}", height, this.rpcAddress);
        }
        return header;
    }

    /**
     * Method:getHtBalance
     * Description: obtainhtbalance
     * Author: xinjl
     * Date: 2018/4/16 15:22
     */
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
        try {
            if (htgContext.HTG_CHAIN_ID() == 106) {
                return this.getTransactionByHashReal(txHash);
            } else {
                return TX_CACHE.get(new TxKey(txHash, this));
            }
        } catch (Exception e) {
            htgContext.logger().error("[{}] Transaction[{}] Cache error: {}", htgContext.getConfig().getSymbol(), txHash, e.getMessage());
            return null;
        }
    }

    private org.web3j.protocol.core.methods.response.Transaction getTransactionByHashReal(String txHash) throws Exception {
        htgContext.logger().debug("[{}]real request network for getTransactionByHash: {}", htgContext.getConfig().getSymbol(), txHash);
        return this.timeOutWrapperFunction("getTransactionByHash", txHash, args -> {
            org.web3j.protocol.core.methods.response.Transaction transaction = null;
            EthTransaction send = web3j.ethGetTransactionByHash(args).send();
            if (send.getTransaction().isPresent()) {
                transaction = send.getTransaction().get();
            }
            return transaction;
        });
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
     * Method:sendMainAsset
     * Description: sendHT
     * Author: xinjl
     * Date: 2018/4/16 14:55
     */
    public String sendMainAssetForTestCase(String fromAddress, String privateKey, String toAddress, BigDecimal value, BigInteger gasLimit, BigInteger gasPrice) throws Exception {
        BigDecimal htBalance = getBalance(fromAddress);
        if (htBalance == null) {
            getLog().error("Get the current address{}Balance failed!", symbol());
            return "501";
        }
        //getLog().info(fromAddress + "===Account amount" + convertWeiToEth(ethBalance.toBigInteger()));
        BigInteger bigIntegerValue = convertMainAssetToWei(value);
        if (htBalance.toBigInteger().compareTo(bigIntegerValue.add(gasLimit.multiply(gasPrice))) < 0) {
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
        byte[] signedMessage = TransactionEncoder.signMessage(etherTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        //Send broadcast
        EthSendTransaction send = send(hexValue);
        if (send == null || send.getResult() == null) {
            getLog().error(JSONUtils.obj2json(send));
            return null;
        }
        if (send.getResult().equals("nonce too low")) {
            sendMainAssetForTestCase(fromAddress, privateKey, toAddress, value, gasLimit, gasPrice);
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
            String hexValue;
            if (htgContext.getConverterCoreApi() != null && !htgContext.getConverterCoreApi().isLocalSign()) {
                hexValue = htgContext.getConverterCoreApi().signRawTransactionByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(), htgContext.ADMIN_ADDRESS_PUBLIC_KEY(), _fromAddress, _nonce, _gasPrice, _gasLimit, _toAddress, bigIntegerValue, null);
            } else {
                RawTransaction etherTransaction = RawTransaction.createEtherTransaction(_nonce, _gasPrice, _gasLimit, _toAddress, bigIntegerValue);
                //Transaction signature
                Credentials credentials = Credentials.create(_privateKey);
                byte[] signedMessage = TransactionEncoder.signMessage(etherTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
                hexValue = Numeric.toHexString(signedMessage);
            }
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


    /**
     * obtainethNetwork currentgasPrice
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
     * ERC-20Tokentransaction
     *
     * @param from
     * @param to
     * @param value
     * @param privateKey
     * @return
     * @throws Exception
     */
    public EthSendTransaction transferERC20TokenForTestCase(String from,
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
                htgContext.getEthGasPrice(),
                htgContext.GAS_LIMIT_OF_ERC20(),
                contractAddress, encodedFunction
        );
        //autographTransactionHere, we need to sign the transaction
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //Send transaction
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        return ethSendTransaction;
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
        try {
            if (htgContext.HTG_CHAIN_ID() == 106) {
                return this.getTxReceiptReal(txHash);
            } else {
                return TX_RECEIPT_CACHE.get(new TxKey(txHash, this));
            }
        } catch (Exception e) {
            htgContext.logger().warn("[{}] Transaction Receipt[{}] Cache error: {}", htgContext.getConfig().getSymbol(), txHash, e.getMessage());
            return null;
        }
    }

    public void refreshCache(String txHash) {
        TX_CACHE.refresh(new TxKey(txHash, this));
        TX_RECEIPT_CACHE.refresh(new TxKey(txHash, this));
    }

    public void clearCache() {
        TX_CACHE.invalidateAll();
        TX_RECEIPT_CACHE.invalidateAll();
    }

    private TransactionReceipt getTxReceiptReal(String txHash) throws Exception {
        htgContext.logger().debug("[{}]real request network for getTxReceipt: {}", htgContext.getConfig().getSymbol(), txHash);
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
                    encodedFunction);
            String hexValue;
            if (htgContext.getConverterCoreApi() != null && !htgContext.getConverterCoreApi().isLocalSign()) {
                hexValue = htgContext.getConverterCoreApi().signRawTransactionByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(), htgContext.ADMIN_ADDRESS_PUBLIC_KEY(), from, nonce, gasPrice, gasLimit, contractAddress, value, encodedFunction);
            } else {
                Credentials credentials = Credentials.create(_privateKey);
                //autographTransactionHere, we need to sign the transaction
                byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
                hexValue = Numeric.toHexString(signMessage);
            }
            //Send transaction
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

    public HtgSendTransactionPo callContract(String from, String privateKey, String contractAddress, BigInteger gasLimit, Function function, BigInteger value, BigInteger gasPrice, BigInteger nonce) throws Exception {
        value = value == null ? BigInteger.ZERO : value;
        gasPrice = gasPrice == null || gasPrice.compareTo(BigInteger.ZERO) == 0 ? htgContext.getEthGasPrice() : gasPrice;
        nonce = nonce == null ? this.getNonce(from) : nonce;
        String encodedFunction = FunctionEncoder.encode(function);

        HtgSendTransactionPo txPo = this.timeOutWrapperFunction("callContract", List.of(from, privateKey, contractAddress, gasLimit, encodedFunction, value, gasPrice, nonce), args -> {
            int i =0;
            String _from = args.get(i++).toString();
            String _privateKey = args.get(i++).toString();
            String _contractAddress = args.get(i++).toString();
            BigInteger _gasLimit = (BigInteger) args.get(i++);
            String _encodedFunction = args.get(i++).toString();
            BigInteger _value = (BigInteger) args.get(i++);
            BigInteger _gasPrice = (BigInteger) args.get(i++);
            BigInteger _nonce = (BigInteger) args.get(i++);
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    _nonce,
                    _gasPrice,
                    _gasLimit,
                    _contractAddress,
                    _value,
                    _encodedFunction);
            String hexValue;
            if (htgContext.getConverterCoreApi() != null && !htgContext.getConverterCoreApi().isLocalSign()) {
                hexValue = htgContext.getConverterCoreApi().signRawTransactionByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(), htgContext.ADMIN_ADDRESS_PUBLIC_KEY(),
                        _from, _nonce, _gasPrice, _gasLimit, _contractAddress, _value, _encodedFunction);
            } else {
                Credentials credentials = Credentials.create(_privateKey);
                //autographTransactionHere, we need to sign the transaction
                byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
                hexValue = Numeric.toHexString(signMessage);
            }

            //Send transaction
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
    private boolean timeOutExceed = false;
    private <T, R> R timeOutWrapperFunctionReal(String functionName, ExceptionFunction<T, R> fucntion, int times, T arg) throws Exception {
        try {
            this.checkIfResetWeb3j(times);
            return fucntion.apply(arg);
        } catch (Exception e) {
            String message = e.getMessage();
            boolean isTimeOut = ConverterUtil.isTimeOutError(message);
            if (isTimeOut) {
                getLog().error("{}: {} function [{}] time out", e.getClass().getName(), symbol(), functionName);
                if (timeOutExceed || times > 10) {
                    timeOutExceed = true;
                    Integer count = requestExceededMap.computeIfAbsent(this.rpcAddress, r -> 0);
                    requestExceededMap.put(this.rpcAddress, count + 1);
                    switchStandbyAPI(this.rpcAddress);
                    throw e;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                return timeOutWrapperFunctionReal(functionName, fucntion, times + 1, arg);
            }
            getLog().warn("APIConnection exception. ERROR: {}", message);
            String availableRpc = this.getAvailableRpcFromThirdParty(htgContext.getConfig().getChainIdOnHtgNetwork());
            if (StringUtils.isNotBlank(availableRpc) && !availableRpc.equals(this.rpcAddress)) {
                clearTimeOfRequestExceededMap = System.currentTimeMillis() + HtgConstant.HOURS_3;
                urlFromThirdParty = true;
                urlFromThirdPartyForce = false;
                changeApi(availableRpc);
                throw e;
            }
            // WhenAPIReset when there is an abnormal connectionAPIConnection, using backupAPI abnormal: ClientConnectionException
            if (e instanceof ClientConnectionException) {
                getLog().warn("APIReset when there is an abnormal connectionAPIConnection, using backupAPI.");
                if (ConverterUtil.isRequestExpired(e.getMessage()) && htgContext.getConfig().getMainRpcAddress().equals(this.rpcAddress)) {
                    getLog().info("Re sign EmergencyAPI: {}", this.rpcAddress);
                    reSignMainAPI();
                    throw e;
                }
                if (ConverterUtil.isRequestDenied(e.getMessage()) && htgContext.getConfig().getMainRpcAddress().equals(this.rpcAddress)) {
                    getLog().info("Reset EmergencyAPI,{} API Ready to switch, currentlyAPI: {}", symbol(), this.rpcAddress);
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
            // If encountering abnormal network connection
            if (e instanceof ConnectException || e instanceof UnknownHostException) {
                // emergencyAPIReset, switch to normalAPI
                if (htgContext.getConfig().getMainRpcAddress().equals(this.rpcAddress)) {
                    getLog().info("Reset EmergencyAPI,{} API Ready to switch, currentlyAPI: {}", symbol(), this.rpcAddress);
                    requestExceededMap.clear();
                    clearTimeOfRequestExceededMap = 0L;
                    switchStandbyAPI(this.rpcAddress);
                    throw e;
                }
                // ordinaryAPIRecord the number of exceptions
                Integer count = requestExceededMap.getOrDefault(this.rpcAddress, 0);
                requestExceededMap.put(this.rpcAddress, count + 1);
                switchStandbyAPI(this.rpcAddress);
                throw e;
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

    public void setEthRpcAddress(String rpcAddress) {
        this.rpcAddress = rpcAddress;
    }

    public Web3j getWeb3j() {
        return web3j;
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

    public EthSendTransaction ethSendRawTransaction(String hexValue) throws Exception {
        EthSendTransaction ethSendTransaction = this.timeOutWrapperFunction("getNonce", hexValue, args -> {
            EthSendTransaction send = web3j.ethSendRawTransaction(args).sendAsync().get();
            if (send == null) {
                throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD, String.format("%s request error", symbol()));
            }
            return send;
        });
        return ethSendTransaction;
    }

    public boolean isReSyncBlock() {
        return reSyncBlock;
    }

    public void setReSyncBlock(boolean reSyncBlock) {
        this.reSyncBlock = reSyncBlock;
    }

    static class TxKey {
        private String txHash;
        private long nativeId;
        private HtgWalletApi htgWalletApi;

        public TxKey(String txHash, HtgWalletApi htgWalletApi) {
            this.txHash = txHash;
            this.htgWalletApi = htgWalletApi;
            this.nativeId = htgWalletApi.htgContext.getConfig().getChainIdOnHtgNetwork();
        }

        public String getTxHash() {
            return txHash;
        }

        public void setTxHash(String txHash) {
            this.txHash = txHash;
        }

        public HtgWalletApi getHtgWalletApi() {
            return htgWalletApi;
        }

        public void setHtgWalletApi(HtgWalletApi htgWalletApi) {
            this.htgWalletApi = htgWalletApi;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TxKey txKey = (TxKey) o;

            if (nativeId != txKey.nativeId) return false;
            if (!txHash.equals(txKey.txHash)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = txHash.hashCode();
            result = 31 * result + (int) (nativeId ^ (nativeId >>> 32));
            return result;
        }
    }
}
