package network.nerve.quotation.heterogeneouschain;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.heterogeneouschain.constant.EthConstant;
import network.nerve.quotation.heterogeneouschain.context.EthContext;
import network.nerve.quotation.heterogeneouschain.model.Block;
import network.nerve.quotation.heterogeneouschain.model.EthSendTransactionPo;
import network.nerve.quotation.heterogeneouschain.model.Transaction;
import network.nerve.quotation.heterogeneouschain.utils.Tools;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static network.nerve.quotation.heterogeneouschain.constant.EthConstant.ZERO_ADDRESS;
import static network.nerve.quotation.heterogeneouschain.context.EthContext.*;


/**
 * BSC API
 */
@Component
public class ETHWalletApi implements WalletApi {

    private NulsLogger getLog() {
        return QuotationContext.logger();
    }

    protected Web3j web3j;

    public void init() {
        initialize();
        if (web3j == null) {
            web3j = newInstanceWeb3j(rpcAddress);
        }
    }

    public void restartApi() {
        shutdownWeb3j();
        init();
    }

    private void shutdownWeb3j() {
        if (web3j != null) {
            web3j.shutdown();
            web3j = null;
        }
    }

    /**
     * ERC-20Token交易
     */
    public EthSendTransaction transferERC20Token(String from,
                                                 String to,
                                                 BigInteger value,
                                                 String privateKey,
                                                 String contractAddress,
                                                 BigInteger gasLimit,
                                                 BigInteger gasPrice) throws Exception {
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
                gasPrice,
                gasLimit,
                contractAddress, encodedFunction
        );
        //签名Transaction，这里要对交易做签名
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //发送交易
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        return ethSendTransaction;
    }

    /**
     * 发送ETH
     */
    public String sendETH(String fromAddress, String privateKey, String toAddress, BigDecimal value, BigInteger gasLimit, BigInteger gasPrice) throws Exception {
        BigDecimal ethBalance = getBalance(fromAddress);
        if (ethBalance == null) {
            throw new RuntimeException("获取当前地址ETH余额失败");
        }
        BigInteger bigIntegerValue = convertEthToWei(value);
        if (ethBalance.toBigInteger().compareTo(bigIntegerValue.add(gasLimit.multiply(gasPrice))) < 0) {
            //余额小于转账金额与手续费之和
            throw new RuntimeException("账户金额小于转账金额与手续费之和!");
        }
        BigInteger nonce = getNonce(fromAddress);
        if (nonce == null) {
            throw new RuntimeException("获取当前地址nonce失败");
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            getLog().error(e.getMessage(), e);
        }
        RawTransaction etherTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, bigIntegerValue);
        //交易签名
        Credentials credentials = Credentials.create(privateKey);
        byte[] signedMessage = TransactionEncoder.signMessage(etherTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        //发送广播
        EthSendTransaction send = send(hexValue);
        if (send == null || send.getResult() == null) {
            return null;
        }
        if (send.getResult().equals("nonce too low")) {
            sendETH(fromAddress, privateKey, toAddress, value, gasLimit, gasPrice);
        }
        return send.getTransactionHash();
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

    private <T, R> R timeOutWrapperFunction(String functionName, T arg, ExceptionFunction<T, R> fucntion) throws Exception {
        return this.timeOutWrapperFunctionReal(functionName, fucntion, 0, arg);
    }

    private <T, R> R timeOutWrapperFunctionReal(String functionName, ExceptionFunction<T, R> fucntion, int times, T arg) throws Exception {
        try {
            this.checkIfResetWeb3j(times);
            return fucntion.apply(arg);
        } catch (Exception e) {
            throw e;
        }
    }

    protected void checkIfResetWeb3j(int times) {
        int mod = times % 6;
        if (mod == 5 && web3j != null && rpcAddress != null) {
            restartApi();
            web3j = newInstanceWeb3j(rpcAddress);
        }
    }

    private Web3j newInstanceWeb3j(String rpcAddress) {
        return Web3j.build(new HttpService(rpcAddress));
    }

    @Override
    public Block getBlock(String hash) throws Exception {
        EthBlock.Block block = this.timeOutWrapperFunction("getBlock", hash, args -> {
            return web3j.ethGetBlockByHash(args, true).send().getBlock();
        });
        if (block == null) {
            return null;
        }
        return createBlock(block);
    }

    @Override
    public Block getBlock(long height) throws Exception {
        EthBlock.Block block = getBlockByHeight(height);
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
            getLog().error(e.getMessage());
        }
        return transaction;
    }

    public static BigDecimal convertWeiToEth(BigInteger balance) {
        BigDecimal cardinalNumber = new BigDecimal("1000000000000000000");
        BigDecimal decimalBalance = new BigDecimal(balance);
        BigDecimal value = decimalBalance.divide(cardinalNumber, 18, RoundingMode.DOWN);
        return value;
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
        return block;
    }

    /**
     * ETH余额
     *
     * @param address
     * @return
     * @throws Exception
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
     * 获取BEP-20 token指定地址余额
     *
     * @param address         查询地址
     * @param contractAddress 合约地址
     * @return
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
            boolean isTimeOut = Tools.isTimeOutError(message);
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

    @Override
    public boolean canTransferBatch() {
        return false;
    }

    @Override
    public void confirmUnspent(Block block) {

    }

    @Override
    public EthSendTransaction sendTransaction(String fromAddress, String secretKey, Map<String, BigDecimal> transferRequests) {
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
            result = sendETH(fromAddress, secretKey, toAddress, amount, ETH_GAS_LIMIT_OF_ETH, EthContext.getEthGasPrice());
        } catch (Exception e) {
            getLog().error("send fail", e);
        }
        return result;
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
        EthSendTransaction result = transferERC20Token(
                fromAddress,
                toAddress,
                amount.toBigInteger(),
                secretKey, contractAddress,
                EthContext.getEthGasPrice(),
                ETH_GAS_LIMIT_OF_ERC20);
        return result;
    }


    /**
     * 获取nonce，Pending模式 适用于连续转账
     *
     * @param from
     * @return
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

    public BigInteger convertEthToWei(BigDecimal value) {
        BigDecimal cardinalNumber = new BigDecimal("1000000000000000000");
        value = value.multiply(cardinalNumber);
        return value.toBigInteger();
    }

    @Override
    public String convertToNewAddress(String address) {
        return address;
    }



    /**
     * 充值ETH
     */
    public String rechargeEth(String fromAddress, String prikey, BigInteger value, String toAddress, String multySignContractAddress) throws Exception {
        Function txFunction = getCrossOutFunction(toAddress, value, ZERO_ADDRESS);
        return sendTx(fromAddress, prikey, txFunction, value, multySignContractAddress);
    }

    /**
     * 充值ERC20
     * 1.授权使用ERC20资产
     * 2.充值
     */
    public String rechargeErc20(String fromAddress, String prikey, BigInteger value, String toAddress, String multySignContractAddress, String erc20Address) throws Exception {
        Function crossOutFunction = getCrossOutFunction(toAddress, value, erc20Address);
        String hash = this.sendTx(fromAddress, prikey, crossOutFunction, multySignContractAddress);
        return hash;
    }

    /**
     * ERC20 授权
     */
    public String authorization(String fromAddress, String prikey, String multySignContractAddress, String erc20Address) throws Exception {
        BigInteger approveAmount = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",16);
        Function approveFunction = this.getERC20ApproveFunction(multySignContractAddress, approveAmount);
        String authHash = this.sendTx(fromAddress, prikey, approveFunction, null, erc20Address);
        return authHash;
    }

    /**
     * 是否已授权过
     *
     * @throws Exception
     */
    public boolean isAuthorized(String fromAddress, String multySignContractAddress, String erc20Address) throws Exception {
        Function allowanceFunction = new Function("allowance",
                Arrays.asList(new Address(fromAddress), new Address(multySignContractAddress)),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        BigInteger approveAmount = new BigInteger("FF00000000000000000000000000000000000000000000000000000000000000",16);
        BigInteger allowanceAmount = (BigInteger) callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
        if (allowanceAmount.compareTo(approveAmount) > 0) {
            return true;
        }
        return false;
    }

    private Function getCrossOutFunction(String toAddress, BigInteger value, String erc20) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Utf8String(toAddress));
        inputParameters.add(new Uint256(value));
        inputParameters.add(new Address(erc20));

        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Type>() {
        });

        return new Function(EthConstant.METHOD_CROSS_OUT, inputParameters, outputParameters);
    }

    protected Function getERC20ApproveFunction(String spender, BigInteger value) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(spender));
        inputParameters.add(new Uint256(value));

        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(new TypeReference<Type>() {
        });
        return new Function(
                "approve",
                inputParameters,
                outputParameters
        );
    }

    public String sendTx(String fromAddress, String priKey, Function txFunction, String contract) throws Exception {
        return this.sendTx(fromAddress, priKey, txFunction, null, contract);
    }

    public String sendTx(String fromAddress, String priKey, Function txFunction, BigInteger value, String contract) throws Exception {
        // 验证合约交易合法性
        EthCall ethCall = validateContractCall(fromAddress, contract, txFunction, value);
        if (ethCall.isReverted()) {
            throw new Exception("异构链合约交易验证失败 - " + ethCall.getRevertReason());
        }
        // 估算GasLimit
        BigInteger estimateGas = ethEstimateGas(fromAddress, contract, txFunction, value);
        if (estimateGas.compareTo(BigInteger.ZERO) == 0) {
            throw new Exception("异构链合约交易估算GasLimit失败");
        }
        BigInteger gasLimit = estimateGas;
        EthSendTransactionPo ethSendTransactionPo = callContract(fromAddress, priKey, contract, gasLimit, txFunction, value, null);
        String ethTxHash = ethSendTransactionPo.getTxHash();
        return ethTxHash;
    }

    public TransactionReceipt getTxReceipt(String txHash) throws Exception {
        return this.timeOutWrapperFunction("getTxReceipt", txHash, args -> {
            Optional<TransactionReceipt> result = web3j.ethGetTransactionReceipt(args).send().getTransactionReceipt();
            if (result == null || !result.isPresent()) {
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
        List argsList = new ArrayList();
        argsList.add(contractAddress);
        argsList.add(encode);
        argsList.add(latest);
        List<Type> typeList = this.timeOutWrapperFunction("callViewFunction", argsList, args -> {
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

    private EthSendTransactionPo callContract(String from, String privateKey, String contractAddress, BigInteger gasLimit, Function function) throws Exception {
        return this.callContract(from, privateKey, contractAddress, gasLimit, function, null, null);
    }

    private EthSendTransactionPo callContract(String from, String privateKey, String contractAddress, BigInteger gasLimit, Function function, BigInteger value, BigInteger gasPrice) throws Exception {
        value = value == null ? BigInteger.ZERO : value;
        gasPrice = gasPrice == null || gasPrice.compareTo(BigInteger.ZERO) == 0 ? EthContext.getEthGasPrice() : gasPrice;
        String encodedFunction = FunctionEncoder.encode(function);
        List argsList = new ArrayList();
        argsList.add(from);
        argsList.add(privateKey);
        argsList.add(contractAddress);
        argsList.add(gasLimit);
        argsList.add(encodedFunction);
        argsList.add(value);
        argsList.add(gasPrice);
        EthSendTransactionPo txPo = this.timeOutWrapperFunction("callContract", argsList, args -> {
            int i = 0;
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
            byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signMessage);
            //发送交易
            EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
            if (send == null) {
                throw new RuntimeException("send transaction request error");
            }
            if (send.hasError()) {
                throw new RuntimeException(send.getError().getMessage());
            }
            return new EthSendTransactionPo(send.getTransactionHash(), _from, rawTransaction);
        });
        return txPo;
    }

    private EthCall validateContractCall(String from, String contractAddress, Function function) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        return this.validateContractCall(from, contractAddress, encodedFunction, null);
    }

    private EthCall validateContractCall(String from, String contractAddress, Function function, BigInteger value) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        return this.validateContractCall(from, contractAddress, encodedFunction, value);
    }

    private EthCall validateContractCall(String from, String contractAddress, String encodedFunction) throws Exception {
        return this.validateContractCall(from, contractAddress, encodedFunction, null);
    }

    private EthCall validateContractCall(String from, String contractAddress, String encodedFunction, BigInteger value) throws Exception {
        value = value == null ? BigInteger.ZERO : value;

        List argsList = new ArrayList();
        argsList.add(from);
        argsList.add(contractAddress);
        argsList.add(encodedFunction);
        argsList.add(value);
        EthCall ethCall = this.timeOutWrapperFunction("validateContractCall", argsList, args -> {
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

    private BigInteger ethEstimateGas(String from, String contractAddress, Function function) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        return this.ethEstimateGas(from, contractAddress, encodedFunction, null);
    }

    private BigInteger ethEstimateGas(String from, String contractAddress, Function function, BigInteger value) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        return this.ethEstimateGas(from, contractAddress, encodedFunction, value);
    }

    private BigInteger ethEstimateGas(String from, String contractAddress, String encodedFunction, BigInteger value) throws Exception {
        value = value == null ? BigInteger.ZERO : value;
        List argsList = new ArrayList();
        argsList.add(from);
        argsList.add(contractAddress);
        argsList.add(encodedFunction);
        argsList.add(value);
        BigInteger gas = this.timeOutWrapperFunction("ethEstimateGas", argsList, args -> {
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
            if (StringUtils.isBlank(estimateGas.getResult())) {
                return BigInteger.ZERO;
            }
            return estimateGas.getAmountUsed();
        });
        return gas;
    }

    public int getContractTokenDecimals(String tokenContract) throws Exception {
        Function allowanceFunction = new Function("decimals",
                List.of(),
                Arrays.asList(new TypeReference<Uint8>() {
                }));
        BigInteger value = (BigInteger) callViewFunction(tokenContract, allowanceFunction).get(0).getValue();
        return value.intValue();
    }

    public BigInteger totalSupply(String contractAddress) throws Exception {
        Function allowanceFunction = new Function("totalSupply",
                List.of(),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        BigInteger value = (BigInteger) callViewFunction(contractAddress, allowanceFunction).get(0).getValue();
        return value;
    }

}
