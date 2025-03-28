package network.nerve.converter.heterogeneouschain.trx.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.ByteString;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.ExceptionFunction;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.core.WalletApi;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant;
import network.nerve.converter.heterogeneouschain.trx.context.TrxContext;
import network.nerve.converter.heterogeneouschain.trx.model.TrxEstimateSun;
import network.nerve.converter.heterogeneouschain.trx.model.TrxSendTransactionPo;
import network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil;
import network.nerve.converter.utils.ConverterUtil;
import org.tron.trident.abi.FunctionEncoder;
import org.tron.trident.abi.FunctionReturnDecoder;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.Type;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.api.GrpcAPI;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.core.transaction.TransactionBuilder;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Numeric;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant.*;
import static org.tron.trident.core.ApiWrapper.parseAddress;
import static org.tron.trident.core.ApiWrapper.parseHex;

/**
 * @author: Mimi
 * @date: 2021/7/27
 */
public class TrxWalletApi implements WalletApi, BeanInitial {

    private static LoadingCache<TxKey, Chain.Transaction> TX_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(50)
            .maximumSize(200)
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .build(new CacheLoader<TxKey, Chain.Transaction>() {
                @Override
                public Chain.Transaction load(TxKey txKey) throws Exception {
                    return txKey.getTrxWalletApi().getTransactionByHashReal(txKey.getTxHash());
                }
            });

    private static LoadingCache<TxKey, Response.TransactionInfo> TX_RECEIPT_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(50)
            .maximumSize(200)
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .build(new CacheLoader<TxKey, Response.TransactionInfo>() {
                @Override
                public Response.TransactionInfo load(TxKey txKey) throws Exception {
                    return txKey.getTrxWalletApi().getTransactionReceiptReal(txKey.getTxHash());
                }
            });
    
    private HtgContext htgContext;

    private String symbol() {
        return htgContext.getConfig().getSymbol();
    }

    protected ApiWrapper wrapper;

    protected String rpcAddress;
    int switchStatus = 0;
    String tempKey = "3333333333333333333333333333333333333333333333333333333333333333";
    private boolean inited = false;
    private boolean urlFromThirdParty = false;
    private boolean urlFromThirdPartyForce = false;
    private Map<String, Integer> requestExceededMap = new HashMap<>();
    private long clearTimeOfRequestExceededMap = 0L;
    private int rpcVersion = -1;
    private boolean reSyncBlock = false;

    private Map<String, BigInteger> map = new HashMap<>();
    private ReentrantLock checkLock = new ReentrantLock();

    private NulsLogger getLog() {
        return htgContext.logger();
    }

    public void init(String rpcAddress) throws NulsException {
        // Default initializedAPIWill be replaced by new onesAPIService coverage, when a node becomes a virtual bank, a new one will be initializedAPIservice
        if (StringUtils.isNotBlank(rpcAddress) && htgContext.getConfig().getCommonRpcAddress().equals(this.rpcAddress) && !rpcAddress.equals(this.rpcAddress)) {
            resetApiWrapper();
        }
        if (wrapper == null) {
            wrapper = newInstanceApiWrapper(rpcAddress);
            this.rpcAddress = StringUtils.isBlank(rpcAddress) ? EMPTY_STRING : rpcAddress;
            getLog().info("initialization {} API URL: {}", symbol(), rpcAddress);
        }
    }

    public void checkApi(int order) throws NulsException {
        checkLock.lock();
        try {
            do {
                // Force updates from third-party systemsrpc
                Map<Long, Map> rpcCheckMap = htgContext.getConverterCoreApi().HTG_RPC_CHECK_MAP();
                Map<String, Object> resultMap = rpcCheckMap.get(htgContext.getConfig().getChainIdOnHtgNetwork());
                if (resultMap == null) {
                    break;
                }
                Integer _version = (Integer) resultMap.get("rpcVersion");
                if (_version == null) {
                    break;
                }
                if (this.rpcVersion == -1) {
                    this.rpcVersion = _version.intValue();
                    break;
                }
                if (this.rpcVersion == _version.intValue()){
                    break;
                }
                if (_version.intValue() > this.rpcVersion){
                    // findversionChange, switchrpc
                    Integer _index = (Integer) resultMap.get("index");
                    if (_index == null) {
                        break;
                    }
                    String apiUrl = (String) resultMap.get("extend" + (_index + 1));
                    if (StringUtils.isBlank(apiUrl)) {
                        break;
                    }
                    getLog().info("Checked that changes are neededRPCservice {} rpc check from third party, version: {}, url: {}", symbol(), _version.intValue(), apiUrl);
                    TX_CACHE.invalidateAll();
                    TX_RECEIPT_CACHE.invalidateAll();
                    this.changeApi(apiUrl);
                    this.rpcVersion = _version.intValue();
                    urlFromThirdPartyForce = true;
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
        } catch (NulsException e) {
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
        if(rpcSize == 1) {
            rpc = rpcAddressList.get(0);
        } else if(rpcSize > 1) {
            int index = this.calRpcIndex(order, rpcSize);
            rpc = rpcAddressList.get(index);
        } else {
            rpc = EMPTY_STRING;
        }
        return rpc;
    }

    private void changeApi(String rpc) throws NulsException {
        resetApiWrapper();
        init(rpc);
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
                mainRpcAddress = StringUtils.isBlank(mainRpcAddress) ? EMPTY_STRING : mainRpcAddress;
                getLog().info("{} API Not available: {} - {}, Prepare to switch to emergency modeAPI: {}, ", symbol(), oldRpc, rpc, mainRpcAddress);
                changeApi(mainRpcAddress);
                if (mainRpcAddress.equals(this.rpcAddress)) {
                    clearTimeOfRequestExceededMap = System.currentTimeMillis() + HtgConstant.HOURS_3;
                }
                return;
            }
            // Normal switchingAPI
            if (!rpc.equals(this.rpcAddress)) {
                changeApi(rpc);
                // Equal, indicating successful switching
                if (rpc.equals(this.rpcAddress)) {
                    switchStatus = expectSwitchStatus;
                }
            }
        } catch (NulsException e) {
            throw e;
        } finally {
            checkLock.unlock();
        }
    }

    private void resetApiWrapper() {
        if (wrapper != null) {
            wrapper.close();
            wrapper = null;
        }
    }

    public boolean isInited() {
        return inited;
    }

    public void initedDone() {
        this.inited = true;
    }


    protected void checkIfResetApiWrapper(int times) throws NulsException {
        int mod = times % 6;
        if (mod == 5 && wrapper != null && rpcAddress != null) {
            getLog().info("restartAPIservice");
            resetApiWrapper();
            wrapper = newInstanceApiWrapper(rpcAddress);
        }
    }

    private ApiWrapper newInstanceApiWrapper(String rpcAddress) throws NulsException {
        if (StringUtils.isBlank(rpcAddress)) {
            return ApiWrapper.ofShasta(tempKey);
        } else if (rpcAddress.startsWith("endpoint")) {
            String[] split = rpcAddress.split(":");
            String rpc = split[1].trim();
            return new ApiWrapper(rpc + ":50051", rpc + ":50061", tempKey);
        } else {
            return ApiWrapper.ofMainnet(tempKey, rpcAddress);
        }
    }


    public Response.TransactionInfo getTransactionReceipt(String txHash) throws Exception {
        try {
            return TX_RECEIPT_CACHE.get(new TxKey(txHash, this));
        } catch (Exception e) {
            getLog().warn("[{}] Transaction Receipt[{}] Cache error: {}", htgContext.getConfig().getSymbol(), txHash, e.getMessage());
            return null;
        }
    }

    public void refreshCache(String txHash) {
        TX_CACHE.refresh(new TxKey(txHash, this));
        TX_RECEIPT_CACHE.refresh(new TxKey(txHash, this));
    }

    private Response.TransactionInfo getTransactionReceiptReal(String txHash) throws Exception {
        if (StringUtils.isBlank(txHash)) {
            return null;
        }
        txHash = Numeric.cleanHexPrefix(txHash);
        return this.timeOutWrapperFunction("getTxReceipt", txHash, args -> {
            ByteString bsTxid = parseAddress(args);
            GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder()
                    .setValue(bsTxid)
                    .build();
            Response.TransactionInfo transactionInfo = wrapper.blockingStub.getTransactionInfoById(request);

            if(transactionInfo.getBlockTimeStamp() == 0){
                return null;
            }
            return transactionInfo;
        });
    }

    private <T, R> R timeOutWrapperFunction(String functionName, T arg, ExceptionFunction<T, R> fucntion) throws Exception {
        return this.timeOutWrapperFunctionReal(functionName, fucntion, 0, arg);
    }

    private <T, R> R timeOutWrapperFunctionReal(String functionName, ExceptionFunction<T, R> fucntion, int times, T arg) throws Exception {
        try {
            this.checkIfResetApiWrapper(times);
            return fucntion.apply(arg);
        } catch (Exception e) {
            String message = e.getMessage();
            Throwable cause = e.getCause();
            if (cause != null) {
                String causeMessage = cause.getMessage();
                if (causeMessage != null) {
                    message += causeMessage;
                }
            }
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
            String availableRpc = this.getAvailableRpcFromThirdParty(htgContext.getConfig().getChainIdOnHtgNetwork());
            if (StringUtils.isNotBlank(availableRpc) && !availableRpc.equals(this.rpcAddress)) {
                clearTimeOfRequestExceededMap = System.currentTimeMillis() + HtgConstant.HOURS_3;
                urlFromThirdParty = true;
                urlFromThirdPartyForce = false;
                changeApi(availableRpc);
                throw e;
            }
            // WhenAPIReset when there is an abnormal connectionAPIConnection, using backupAPI
            if (e instanceof StatusException || e instanceof StatusRuntimeException) {
                getLog().warn("APIReset when there is an abnormal connectionAPIConnection, using backupAPI");
                if (ConverterUtil.isRequestExpired(e.getMessage()) && htgContext.getConfig().getMainRpcAddress().equals(this.rpcAddress)) {
                    getLog().info("Re sign EmergencyAPI: {}", this.rpcAddress);
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
     * Get transaction details
     */
    public Chain.Transaction getTransactionByHash(String txHash) throws Exception {
        try {
            return TX_CACHE.get(new TxKey(txHash, this));
        } catch (Exception e) {
            getLog().error("[{}] Transaction[{}] Cache error: {}", htgContext.getConfig().getSymbol(), txHash, e.getMessage());
            return null;
        }
    }

    private Chain.Transaction getTransactionByHashReal(String txHash) throws Exception {
        if (StringUtils.isBlank(txHash)) {
            return null;
        }
        txHash = Numeric.cleanHexPrefix(txHash);
        return this.timeOutWrapperFunction("getTransactionById", txHash, args -> {
            ByteString bsTxid = parseAddress(args);
            GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder().setValue(bsTxid).build();
            Chain.Transaction transaction = wrapper.blockingStub.getTransactionById(request);
            if (transaction.getRetCount() == 0) {
                return null;
            } else {
                return transaction;
            }
        });
    }

    /**
     * Invoke contractview/constantfunction
     */
    public List<Type> callViewFunction(String contractAddress, Function function) throws Exception {
        List<Type> typeList = this.timeOutWrapperFunction("callViewFunction", List.of(contractAddress, function), args -> {
            String _contractAddress = (String) args.get(0);
            Function _function = (Function) args.get(1);
            Response.TransactionExtention call = wrapper.constantCall(TrxConstant.ZERO_ADDRESS_TRX, _contractAddress, _function);
            if (call.getConstantResultCount() == 0) {
                return null;
            }
            byte[] bytes = call.getConstantResult(0).toByteArray();
            return FunctionReturnDecoder.decode(Numeric.toHexString(bytes), _function.getOutputParameters());
        });
        return typeList;
    }


    public TrxEstimateSun estimateSunUsed(String from, String contractAddress, Function function) throws Exception {
        return this.estimateSunUsed(from, contractAddress, function, null);
    }

    public TrxEstimateSun estimateSunUsed(String from, String contractAddress, Function function, BigInteger value) throws Exception {
        value = value == null ? BigInteger.ZERO : value;
        TrxEstimateSun estimateEnergy = this.timeOutWrapperFunction("estimateEnergyUsed", List.of(from, contractAddress, function, value), args -> {
            BigInteger _value = (BigInteger) args.get(3);
            String encodedHex = FunctionEncoder.encode(function);
            Contract.TriggerSmartContract trigger = Contract.TriggerSmartContract.newBuilder()
                    .setOwnerAddress(parseAddress(from))
                    .setContractAddress(parseAddress(contractAddress))
                    .setData(parseHex(encodedHex))
                    .setCallValue(_value.longValue())
                    .build();
            Response.TransactionExtention call = wrapper.blockingStub.triggerConstantContract(trigger);

            if (call.getConstantResultCount() == 0) {
                if (call.getResult() != null && !call.getResult().getResult()) {
                    ByteString message = call.getResult().getMessage();
                    if (message != null) {
                        return TrxEstimateSun.FAILED("Execute failed: " + message.toStringUtf8());
                    }
                }
                return TrxEstimateSun.FAILED("Execute failed: empty result.");
            }
            String result = Numeric.toHexString(call.getConstantResult(0).toByteArray());
            if (TrxUtil.isErrorInResult(result)) {
                return TrxEstimateSun.FAILED(TrxUtil.getRevertReason(result));
            }
            long sunUsed = 0;
            long energyUsed = call.getEnergyUsed();
            if (energyUsed != 0) {
                TrxContext trxContext = (TrxContext) htgContext;
                sunUsed = BigInteger.valueOf(energyUsed).multiply(trxContext.SUN_PER_ENERGY).longValue();
            }
            return TrxEstimateSun.SUCCESS(sunUsed, energyUsed);
        });
        return estimateEnergy;
    }

    public long getBlockHeight() throws Exception {
        long blockHeight = this.timeOutWrapperFunction("getBlockHeight", null, args -> {
            Chain.Block nowBlock = wrapper.getNowBlock();
            return nowBlock.getBlockHeader().getRawDataOrBuilder().getNumber();
        });
        return blockHeight;
    }


    /**
     * Obtain blocks based on height
     */
    public Response.BlockExtention getBlockByHeight(Long height) throws Exception {
        Response.BlockExtention block = this.timeOutWrapperFunction("getBlockByHeight", height, args -> {
            long blockNum = args;
            GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
            builder.setNum(blockNum);
            return wrapper.blockingStub.getBlockByNum2(builder.build());
        });
        if (block == null) {
            getLog().error("Get block empty");
        }
        return block;
    }

    /**
     * obtainhtbalance
     */
    public BigDecimal getBalance(String address) throws Exception {
        BigDecimal balance = this.timeOutWrapperFunction("getAccountBalance", address, args -> {
            long accountBalance = wrapper.getAccountBalance(args);
            return new BigDecimal(accountBalance);
        });
        return balance;
    }

    public TrxSendTransactionPo callContract(String from, String privateKey, String contractAddress, BigInteger feeLimit, Function function) throws Exception {
        return this.callContract(from, privateKey, contractAddress, feeLimit, function, null);
    }

    public TrxSendTransactionPo callContract(String from, String privateKey, String contractAddress, BigInteger feeLimit, Function function, BigInteger value) throws Exception {
        value = value == null ? BigInteger.ZERO : value;
        String encodedFunction = FunctionEncoder.encode(function);

        TrxSendTransactionPo txPo = this.timeOutWrapperFunction("callContract", List.of(from, privateKey, contractAddress, feeLimit, encodedFunction, value), args -> {
            int i =0;
            String _from = args.get(i++).toString();
            String _privateKey = args.get(i++).toString();
            String _contractAddress = args.get(i++).toString();
            BigInteger _feeLimit = (BigInteger) args.get(i++);
            String _encodedFunction = args.get(i++).toString();
            BigInteger _value = (BigInteger) args.get(i++);

            Contract.TriggerSmartContract trigger =
                    Contract.TriggerSmartContract.newBuilder()
                            .setOwnerAddress(parseAddress(_from))
                            .setContractAddress(parseAddress(_contractAddress))
                            .setData(parseHex(_encodedFunction))
                            .setCallValue(_value.longValue())
                            .build();
            Response.TransactionExtention txnExt = wrapper.blockingStub.triggerContract(trigger);
            TransactionBuilder builder = new TransactionBuilder(txnExt.getTransaction());
            builder.setFeeLimit(_feeLimit.longValue());

            Chain.Transaction txn = builder.build();
            Chain.Transaction signedTxn;
            if (htgContext.getConverterCoreApi() != null && !htgContext.getConverterCoreApi().isLocalSign()) {
                String txStr = Numeric.toHexStringNoPrefix(txn.toByteArray());
                String signData = htgContext.getConverterCoreApi().signTronRawTransactionByMachine(htgContext.ADMIN_ADDRESS_PUBLIC_KEY(), txStr);
                signedTxn = txn.toBuilder().addSignature(ByteString.copyFrom(Numeric.hexStringToByteArray(signData))).build();
            } else {
                signedTxn = wrapper.signTransaction(txn, new KeyPair(_privateKey));
            }
            Response.TransactionReturn ret = wrapper.blockingStub.broadcastTransaction(signedTxn);
            if (!ret.getResult()) {
                getLog().error("[{}]Call to contract transaction broadcast failed, reason: {}", symbol(), ret.getMessage().toStringUtf8());
                return null;
            }

            return new TrxSendTransactionPo(TrxUtil.calcTxHash(signedTxn), _from, _contractAddress, _value, _encodedFunction, _feeLimit);
        });
        return txPo;
    }

    public void setRpcAddress(String rpcAddress) {
        this.rpcAddress = rpcAddress;
    }

    public ApiWrapper getWrapper() {
        return wrapper;
    }

    public void setWrapper(ApiWrapper wrapper) {
        this.wrapper = wrapper;
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
        return this.getERC20BalanceReal(address, contractAddress, 0);
    }

    private BigInteger getERC20BalanceReal(String address, String contractAddress, int times) throws Exception {
        try {
            this.checkIfResetApiWrapper(times);
            Function function = new Function("balanceOf",
                    Arrays.asList(new Address(address)),
                    Arrays.asList(new TypeReference<Uint256>() {
                    }));
            List<Type> types = callViewFunction(contractAddress, function);
            if (types == null || types.isEmpty()) {
                return BigInteger.ZERO;
            }
            return (BigInteger) types.get(0).getValue();
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
                return getERC20BalanceReal(address, contractAddress, times + 1);
            } else {
                getLog().error(e.getMessage(), e);
                throw e;
            }
        }

    }

    public TrxSendTransactionPo transferTRC20Token(String from, String to, BigInteger value, String privateKey, String contractAddress) throws Exception {
        return this.transferTRC20Token(from, to, value, privateKey, contractAddress, null);
    }

    public TrxSendTransactionPo transferTRC20Token(String from, String to, BigInteger value, String privateKey, String contractAddress, BigInteger feeLimit) throws Exception {
        feeLimit = feeLimit == null ? TRX_20 : feeLimit;
        //establishRawTransactionTrading partner
        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(value)),
                Arrays.asList(new TypeReference<Type>() {}));
        TrxSendTransactionPo callContract = this.callContract(from, privateKey, contractAddress, feeLimit, function);
        return callContract;
    }

    public TrxSendTransactionPo transferTrxForTestcase(String from, String to, BigInteger value, String privateKey) throws Exception {
        TrxSendTransactionPo transferTrx = this.timeOutWrapperFunction("transferTrx", List.of(from, to, value, privateKey), args -> {
            Response.TransactionExtention txnExt = wrapper.transfer(from, to, value.longValue());

            TransactionBuilder builder = new TransactionBuilder(txnExt.getTransaction());
            builder.setFeeLimit(TRX_2.longValue());

            Chain.Transaction signedTxn = wrapper.signTransaction(builder.build(), new KeyPair(privateKey));
            Response.TransactionReturn ret = wrapper.blockingStub.broadcastTransaction(signedTxn);
            if (!ret.getResult()) {
                getLog().error("[{}]Transfer transaction broadcast failed, reason: {}", symbol(), ret.getMessage().toStringUtf8());
                return null;
            }
            return new TrxSendTransactionPo(TrxUtil.calcTxHash(signedTxn), from, to, value, null, TRX_2);
        });
        return transferTrx;
    }

    public BigInteger getCurrentGasPrice() throws Exception {
        String result = HttpClientUtil.get(String.format(chainParamsURL));
        if (StringUtils.isBlank(result)) {
            return null;
        }
        Map<String, Object> map0 = JSONUtils.json2map(result);
        List<Map> chainParameters = (List<Map>) map0.get("chainParameter");
        if (chainParameters == null) {
            return null;
        }
        BigInteger energyFee = null;
        for (Map map : chainParameters) {
            if ("getEnergyFee".equalsIgnoreCase((String) map.get("key"))) {
                energyFee = new BigInteger(map.get("value").toString());
                break;
            }
        }
        return energyFee;
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
        private TrxWalletApi trxWalletApi;

        public TxKey(String txHash, TrxWalletApi trxWalletApi) {
            this.txHash = txHash;
            this.trxWalletApi = trxWalletApi;
            this.nativeId = trxWalletApi.htgContext.getConfig().getChainIdOnHtgNetwork();
        }

        public String getTxHash() {
            return txHash;
        }

        public void setTxHash(String txHash) {
            this.txHash = txHash;
        }

        public TrxWalletApi getTrxWalletApi() {
            return trxWalletApi;
        }

        public void setTrxWalletApi(TrxWalletApi trxWalletApi) {
            this.trxWalletApi = trxWalletApi;
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
