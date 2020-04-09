package io.nuls.dex.rpc;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.Parameter;
import io.nuls.core.rpc.model.Parameters;
import io.nuls.core.rpc.model.TypeDescriptor;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.dex.context.DexConfig;
import io.nuls.dex.context.DexConstant;
import io.nuls.dex.context.DexContext;
import io.nuls.dex.context.DexErrorCode;
import io.nuls.dex.manager.DexManager;
import io.nuls.dex.manager.TradingContainer;
import io.nuls.dex.model.po.CoinTradingPo;
import io.nuls.dex.model.po.TradingOrderPo;
import io.nuls.dex.model.txData.CoinTrading;
import io.nuls.dex.model.txData.EditCoinTrading;
import io.nuls.dex.model.txData.TradingOrder;
import io.nuls.dex.model.txData.TradingOrderCancel;
import io.nuls.dex.rpc.call.AccountCall;
import io.nuls.dex.rpc.call.TransactionCall;
import io.nuls.dex.storage.TradingOrderStorageService;
import io.nuls.dex.util.DexUtil;
import io.nuls.dex.util.LoggerUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class DexTxResource extends BaseCmd {

    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private TradingOrderStorageService orderStorageService;
    @Autowired
    private DexManager dexManager;


    @CmdAnnotation(cmd = "dx_createCoinTradingTx", version = 1.0, description = "create CoinTradingTransaction and broadcast")
    @Parameters(value = {
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易创建者地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "账户密码"),
            @Parameter(parameterName = "quoteAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "计价币种chainId"),
            @Parameter(parameterName = "quoteAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "计价币种assetId"),
            @Parameter(parameterName = "scaleQuoteDecimal", requestType = @TypeDescriptor(value = int.class), parameterDes = "计价币种允许最小交易小数位"),
            @Parameter(parameterName = "baseAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "交易币种chainId"),
            @Parameter(parameterName = "baseAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "交易币种assetId"),
            @Parameter(parameterName = "scaleBaseDecimal", requestType = @TypeDescriptor(value = int.class), parameterDes = "交易币种允许最小交易小数位"),
            @Parameter(parameterName = "minTradingAmount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "交易币种交易支持最小额")
    })
    public Response createCoinTradingTx(Map params) {
        //组装交易对txData
        try {
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            //判断地址是否为本地chainId地址
            boolean isAddressValidate = (AddressTool.getChainIdByAddress(address) == dexConfig.getChainId());
            if (!isAddressValidate) {
                return failed(DexErrorCode.ERROR_ADDRESS_ERROR);
            }
            String priKey = AccountCall.getPriKey(dexConfig.getChainId(), address, password);
            Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getAssetId(), address);
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            /* 组装交易发送 (Send transaction) */
            Transaction tx = new Transaction();
            tx.setType(TxType.COIN_TRADING);
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            CoinData coinData = createCoinTradingTxCoinData(address, nonce, available);
            tx.setCoinData(coinData.serialize());
            CoinTrading trading = DexUtil.createCoinTrading(params, address);
            tx.setTxData(trading.serialize());

            AccountCall.txSignature(dexConfig.getChainId(), address, password, priKey, tx);
            String txStr = RPCUtil.encode(tx.serialize());
            TransactionCall.sendTx(dexConfig.getChainId(), txStr);
            Map<String, Object> result = new HashMap<>(2);
            result.put("txHash", tx.getHash().toHex());
            return success(result);
        } catch (NulsException e) {
            LoggerUtil.dexLog.error(e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
            return failed(DexErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = "dx_createTradingOrderTx", version = 1.0, description = "create TradingOrderTransaction and broadcast")
    @Parameters(value = {
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易创建者地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "账户密码"),
            @Parameter(parameterName = "tradingHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易对hash"),
            @Parameter(parameterName = "type", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-2]", parameterDes = "交易类型：1买单,2买单"),
            @Parameter(parameterName = "amount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "币种交易数量"),
            @Parameter(parameterName = "price", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "挂单价格")
    })
    public Response createTradingOrderTx(Map params) {
        try {
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            //判断地址是否为本地chainId地址
            boolean isAddressValidate = (AddressTool.getChainIdByAddress(address) == dexConfig.getChainId());
            if (!isAddressValidate) {
                return failed(DexErrorCode.ERROR_ADDRESS_ERROR);
            }
            String priKey = AccountCall.getPriKey(dexConfig.getChainId(), address, password);

            int type = (int) params.get("type");
            BigInteger amount = new BigInteger(params.get("amount").toString());
            BigInteger price = new BigInteger(params.get("price").toString());
            String tradingHash = (String) params.get("tradingHash");
            NulsHash nulsHash = NulsHash.fromHex(tradingHash);

            /* 组装交易发送 (Send transaction) */
            Transaction tx = new Transaction();
            tx.setType(TxType.TRADING_ORDER);
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            CoinData coinData;
            if (type == DexConstant.ORDER_BUY_OVER) {
                coinData = createBuyOrderTxCoinData(address, tradingHash, amount, price);
            } else {
                coinData = createSellOrderTxCoinData(address, tradingHash, amount);
            }

            for (CoinFrom from : coinData.getFrom()) {
                System.out.println(from.toString());
            }
            tx.setCoinData(coinData.serialize());
            TradingOrder order = DexUtil.createTradingOrder(nulsHash, type, amount, price);
            tx.setTxData(order.serialize());

            AccountCall.txSignature(dexConfig.getChainId(), address, password, priKey, tx);
            String txStr = RPCUtil.encode(tx.serialize());
            TransactionCall.sendTx(dexConfig.getChainId(), txStr);
            Map<String, Object> result = new HashMap<>(2);
            result.put("txHash", tx.getHash().toHex());
            return success(result);
        } catch (NulsException e) {
            LoggerUtil.dexLog.error(e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
            return failed(DexErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = "dx_createTradingCancelOrderTx", version = 1.0, description = "create TradingCancelOrderTransaction and broadcast")
    @Parameters(value = {
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易创建者地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "账户密码"),
            @Parameter(parameterName = "orderHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "挂单hash")
    })
    public Response createTradingCancelOrderTx(Map params) {
        try {
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            String orderHash = (String) params.get("orderHash");
            //判断地址是否为本地chainId地址
            boolean isAddressValidate = (AddressTool.getChainIdByAddress(address) == dexConfig.getChainId());
            if (!isAddressValidate) {
                return failed(DexErrorCode.ERROR_ADDRESS_ERROR);
            }
            String priKey = null;
//            String priKey = AccountCall.getPriKey(dexConfig.getChainId(), address, password);
            //查找order是否存在
            TradingOrderPo orderPo = orderStorageService.query(HexUtil.decode(orderHash));
            if (orderPo == null) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND);
            }
            //查看挂单账户是否等于当前调用接口账户
            if (!Arrays.equals(AddressTool.getAddress(address), orderPo.getAddress())) {
                throw new NulsException(DexErrorCode.ACCOUNT_VALID_ERROR);
            }

            /* 组装交易发送 (Send transaction) */
            Transaction tx = new Transaction();
            tx.setType(TxType.TRADING_ORDER_CANCEL);
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            CoinData coinData = createTradingOrderCancelTxCoinData(address, orderPo);
            tx.setCoinData(coinData.serialize());
            TradingOrderCancel orderCancel = new TradingOrderCancel();
            orderCancel.setOrderHash(orderPo.getOrderHash().getBytes());
            tx.setTxData(orderCancel.serialize());

            AccountCall.txSignature(dexConfig.getChainId(), address, password, priKey, tx);
            String txStr = RPCUtil.encode(tx.serialize());
            TransactionCall.sendTx(dexConfig.getChainId(), txStr);
            Map<String, Object> result = new HashMap<>(2);
            result.put("txHash", tx.getHash().toHex());
            return success(result);

        } catch (NulsException e) {
            LoggerUtil.dexLog.error(e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
            return failed(DexErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = "dx_editCoinTradingTx", version = 1.0, description = "create CoinTradingTransaction and broadcast")
    @Parameters(value = {
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易创建者地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "账户密码"),
            @Parameter(parameterName = "tradingHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易对hash"),
            @Parameter(parameterName = "scaleQuoteDecimal", requestType = @TypeDescriptor(value = int.class), parameterDes = "计价币种允许最小交易小数位"),
            @Parameter(parameterName = "scaleBaseDecimal", requestType = @TypeDescriptor(value = int.class), parameterDes = "交易币种允许最小交易小数位"),
            @Parameter(parameterName = "minTradingAmount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "交易币种交易支持最小额")
    })
    public Response editCoinTradingTx(Map params) {
        try {
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            String tradingHash = (String) params.get("tradingHash");

            String priKey = AccountCall.getPriKey(dexConfig.getChainId(), address, password);
            Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getAssetId(), address);
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            if (available.compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
            }
            Transaction tx = new Transaction();
            tx.setType(232);
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());

            CoinData coinData = new CoinData();
            CoinFrom from = new CoinFrom();
            from.setAssetsChainId(dexConfig.getChainId());
            from.setAssetsId(dexConfig.getAssetId());
            from.setAddress(AddressTool.getAddress(address));
            from.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
            from.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from);

            CoinTo to = new CoinTo();
            to.setAmount(BigInteger.ZERO);
            to.setAssetsChainId(dexConfig.getChainId());
            to.setAssetsId(dexConfig.getAssetId());
            to.setAddress(AddressTool.getAddress(address));
            coinData.addTo(to);

            tx.setCoinData(coinData.serialize());

            EditCoinTrading coinTrading = new EditCoinTrading();
            coinTrading.setTradingHash(NulsHash.fromHex(tradingHash));
            coinTrading.setScaleQuoteDecimal(Byte.parseByte(params.get("scaleQuoteDecimal").toString()));
            coinTrading.setScaleBaseDecimal(Byte.parseByte(params.get("scaleBaseDecimal").toString()));
            coinTrading.setMinTradingAmount(new BigInteger(params.get("minTradingAmount").toString()));
            tx.setTxData(coinTrading.serialize());

            AccountCall.txSignature(dexConfig.getChainId(), address, password, priKey, tx);
            String txStr = RPCUtil.encode(tx.serialize());
            TransactionCall.sendTx(dexConfig.getChainId(), txStr);
            Map<String, Object> result = new HashMap<>(2);
            result.put("txHash", tx.getHash().toHex());
            return success(result);
        } catch (NulsException e) {
            LoggerUtil.dexLog.error(e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
            return failed(DexErrorCode.SYS_UNKOWN_EXCEPTION);
        }

    }

    @CmdAnnotation(cmd = "dx_getTradingOrderNonceInfo", version = 1.0, description = "get the current nonce of the tradingOrder")
    @Parameters(value = {
            @Parameter(parameterName = "orderHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "订单hash")
    })
    public Response getTradingOrderNonceInfo(Map params) {
        try {
            String orderHash = (String) params.get("orderHash");
            //查找order是否存在
            TradingOrderPo orderPo = orderStorageService.query(HexUtil.decode(orderHash));
            if (orderPo == null) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("orderHash", orderHash);
            result.put("nonce", HexUtil.encode(orderPo.getNonce()));
            result.put("type", orderPo.getType());
            result.put("tradingHash", orderPo.getTradingHash().toHex());
            if (orderPo.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
                result.put("leftAmount", orderPo.getLeftQuoteAmount());
            } else {
                result.put("leftAmount", orderPo.getLeftAmount());
            }

            return success(result);
        } catch (NulsException e) {
            LoggerUtil.dexLog.error(e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
            return failed(DexErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    private CoinData createCoinTradingTxCoinData(String address, String nonce, BigInteger available) throws NulsException {
        //创建交易对费用
        BigInteger amount = DexContext.createTradingAmount.add(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
        if (available.compareTo(amount) < 0) {
            throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
        }

        CoinData coinData = new CoinData();
        CoinFrom from = new CoinFrom();
        from.setAssetsChainId(dexConfig.getChainId());
        from.setAssetsId(dexConfig.getAssetId());
        from.setAddress(AddressTool.getAddress(address));
        from.setAmount(amount);
        from.setNonce(HexUtil.decode(nonce));
        coinData.addFrom(from);

        CoinTo to = new CoinTo();
        to.setAssetsChainId(dexConfig.getChainId());
        to.setAssetsId(dexConfig.getAssetId());
        to.setAddress(DexContext.feeAddress);
        to.setAmount(DexContext.createTradingAmount);
        coinData.addTo(to);

        return coinData;
    }


    private CoinData createBuyOrderTxCoinData(String address, String tradingHash, BigInteger baseAmount, BigInteger priceBigInteger) throws NulsException {
        CoinData coinData = new CoinData();

        TradingContainer container = dexManager.getTradingContainer(tradingHash);
        if (container == null) {
            throw new NulsException(CommonCodeConstanst.DATA_NOT_FOUND, "coinTrading not found");
        }
        CoinTradingPo tradingPo = container.getCoinTrading();
        //首先通过交易币数量和单价，计算出需要的计价货币总量

        BigDecimal price = new BigDecimal(priceBigInteger).movePointLeft(tradingPo.getQuoteDecimal());
        BigDecimal amount = new BigDecimal(baseAmount).movePointLeft(tradingPo.getBaseDecimal());
        amount = amount.multiply(price).movePointRight(tradingPo.getQuoteDecimal()).setScale(0, RoundingMode.DOWN);
        BigInteger quoteAmount = amount.toBigInteger();     //最终可以兑换到的计价币种总量

        //如果挂单是本链资产，直接将手续费加在一起
        if (tradingPo.getQuoteAssetChainId() == dexConfig.getChainId() && tradingPo.getQuoteAssetId() == dexConfig.getAssetId()) {
            Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getAssetId(), address);
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            BigInteger fromAmount = quoteAmount.add(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
            if (available.compareTo(fromAmount) < 0) {
                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
            }
            CoinFrom from = new CoinFrom();
            from.setAssetsChainId(dexConfig.getChainId());
            from.setAssetsId(dexConfig.getAssetId());
            from.setAddress(AddressTool.getAddress(address));
            from.setAmount(fromAmount);
            from.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from);
        } else {
            //如果挂单是外链资产，则需要添加一条本链资产的手续费from
            Map balanceMap = AccountCall.getAccountBalance(tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), address);
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            if (available.compareTo(quoteAmount) < 0) {
                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
            }
            CoinFrom from1 = new CoinFrom();
            from1.setAssetsChainId(tradingPo.getQuoteAssetChainId());
            from1.setAssetsId(tradingPo.getQuoteAssetId());
            from1.setAddress(AddressTool.getAddress(address));
            from1.setAmount(quoteAmount);
            from1.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from1);

            balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getAssetId(), address);
            available = new BigInteger(balanceMap.get("available").toString());
            nonce = (String) balanceMap.get("nonce");
            if (available.compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
                throw new NulsException(DexErrorCode.INSUFFICIENT_FEE);
            }
            CoinFrom from2 = new CoinFrom();
            from2.setAssetsChainId(dexConfig.getChainId());
            from2.setAssetsId(dexConfig.getAssetId());
            from2.setAddress(AddressTool.getAddress(address));
            from2.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
            from2.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from2);
        }

        CoinTo to = new CoinTo();
        to.setAssetsChainId(tradingPo.getQuoteAssetChainId());
        to.setAssetsId(tradingPo.getQuoteAssetId());
        to.setAddress(AddressTool.getAddress(address));
        to.setAmount(quoteAmount);
        to.setLockTime(DexConstant.DEX_LOCK_TIME);
        coinData.addTo(to);

        return coinData;
    }

    private CoinData createSellOrderTxCoinData(String address, String tradingHash, BigInteger baseAmount) throws NulsException {
        CoinData coinData = new CoinData();

        TradingContainer container = dexManager.getTradingContainer(tradingHash);
        if (container == null) {
            throw new NulsException(CommonCodeConstanst.DATA_NOT_FOUND, "coinTrading not found");
        }
        CoinTradingPo tradingPo = container.getCoinTrading();

        //如果挂单是本链资产，直接将手续费加在一起
        if (tradingPo.getBaseAssetChainId() == dexConfig.getChainId() && tradingPo.getBaseAssetId() == dexConfig.getAssetId()) {
            Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getAssetId(), address);
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            BigInteger fromAmount = baseAmount.add(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
            if (available.compareTo(fromAmount) < 0) {
                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
            }
            CoinFrom from = new CoinFrom();
            from.setAssetsChainId(dexConfig.getChainId());
            from.setAssetsId(dexConfig.getAssetId());
            from.setAddress(AddressTool.getAddress(address));
            from.setAmount(fromAmount);
            from.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from);
        } else {
            //如果挂单是外链资产，则需要添加一条本链资产的手续费from
            Map balanceMap = AccountCall.getAccountBalance(tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId(), address);
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            if (available.compareTo(baseAmount) < 0) {
                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
            }
            CoinFrom from1 = new CoinFrom();
            from1.setAssetsChainId(tradingPo.getBaseAssetChainId());
            from1.setAssetsId(tradingPo.getBaseAssetId());
            from1.setAddress(AddressTool.getAddress(address));
            from1.setAmount(baseAmount);
            from1.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from1);

            balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getAssetId(), address);
            available = new BigInteger(balanceMap.get("available").toString());
            nonce = (String) balanceMap.get("nonce");
            if (available.compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
                throw new NulsException(DexErrorCode.INSUFFICIENT_FEE);
            }
            CoinFrom from2 = new CoinFrom();
            from2.setAssetsChainId(dexConfig.getChainId());
            from2.setAssetsId(dexConfig.getAssetId());
            from2.setAddress(AddressTool.getAddress(address));
            from2.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
            from2.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from2);
        }

        CoinTo to = new CoinTo();
        to.setAssetsChainId(tradingPo.getBaseAssetChainId());
        to.setAssetsId(tradingPo.getBaseAssetId());
        to.setAddress(AddressTool.getAddress(address));
        to.setAmount(baseAmount);
        to.setLockTime(DexConstant.DEX_LOCK_TIME);
        coinData.addTo(to);

        return coinData;
    }


    private CoinData createTradingOrderCancelTxCoinData(String address, TradingOrderPo orderPo) throws NulsException {
        TradingContainer container = dexManager.getTradingContainer(orderPo.getTradingHash().toHex());
        CoinTradingPo tradingPo = container.getCoinTrading();

        CoinData coinData = new CoinData();

        CoinFrom from = new CoinFrom();
        from.setAddress(orderPo.getAddress());
        from.setNonce(orderPo.getNonce());
        if (orderPo.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
            from.setAmount(orderPo.getLeftQuoteAmount());
        } else {
            from.setAmount(orderPo.getLeftAmount());
        }
        if (orderPo.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
            from.setAssetsChainId(tradingPo.getQuoteAssetChainId());
            from.setAssetsId(tradingPo.getQuoteAssetId());
        } else {
            from.setAssetsChainId(tradingPo.getBaseAssetChainId());
            from.setAssetsId(tradingPo.getBaseAssetId());
        }
        from.setLocked(DexConstant.ASSET_LOCK_TYPE);
        coinData.addFrom(from);
        //如果取消委托的资产不等于本链主资产， 则需要另外查询手续费
        if (from.getAssetsChainId() != dexConfig.getChainId() ||
                from.getAssetsId() != dexConfig.getAssetId() ||
                from.getAmount().compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
            Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getAssetId(), address);
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            if (available.compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
                throw new NulsException(DexErrorCode.INSUFFICIENT_FEE);
            }

            CoinFrom from2 = new CoinFrom();
            from2.setAssetsChainId(dexConfig.getChainId());
            from2.setAssetsId(dexConfig.getAssetId());
            from2.setAddress(AddressTool.getAddress(address));
            from2.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
            from2.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from2);
        }

        CoinTo to = new CoinTo();
        if (from.getAssetsChainId() != dexConfig.getChainId() || from.getAssetsId() != dexConfig.getAssetId()) {
            to.setAssetsChainId(from.getAssetsChainId());
            to.setAssetsId(from.getAssetsId());
            to.setAddress(from.getAddress());
            to.setLockTime(0);
            to.setAmount(from.getAmount());
            coinData.addTo(to);
            return coinData;
        } else {
            to.setAssetsChainId(from.getAssetsChainId());
            to.setAssetsId(from.getAssetsId());
            to.setAddress(from.getAddress());
            to.setLockTime(0);
            to.setAmount(from.getAmount().subtract(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES));
            coinData.addTo(to);
            return coinData;
        }
    }

}
