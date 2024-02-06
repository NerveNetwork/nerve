package network.nerve.dex.rpc;

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
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.Parameter;
import io.nuls.core.rpc.model.Parameters;
import io.nuls.core.rpc.model.TypeDescriptor;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.context.DexContext;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.manager.TradingContainer;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.model.po.TradingOrderPo;
import network.nerve.dex.model.txData.CoinTrading;
import network.nerve.dex.model.txData.EditCoinTrading;
import network.nerve.dex.model.txData.TradingOrder;
import network.nerve.dex.model.txData.TradingOrderCancel;
import network.nerve.dex.rpc.call.AccountCall;
import network.nerve.dex.rpc.call.TransactionCall;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.util.DexUtil;
import network.nerve.dex.util.LoggerUtil;

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
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction creator address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account password"),
            @Parameter(parameterName = "quoteAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Pricing currencychainId"),
            @Parameter(parameterName = "quoteAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Pricing currencyassetId"),
            @Parameter(parameterName = "scaleQuoteDecimal", requestType = @TypeDescriptor(value = int.class), parameterDes = "Pricing currency allows for minimum transaction decimal places"),
            @Parameter(parameterName = "baseAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Transaction CurrencychainId"),
            @Parameter(parameterName = "baseAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Transaction CurrencyassetId"),
            @Parameter(parameterName = "scaleBaseDecimal", requestType = @TypeDescriptor(value = int.class), parameterDes = "Transaction currency allows for minimum transaction decimal places"),
            @Parameter(parameterName = "minBaseAmount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Transaction currency supports minimum transaction amount"),
            @Parameter(parameterName = "minQuoteAmount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Pricing currency transactions support the minimum amount")
    })
    public Response createCoinTradingTx(Map params) {
        //Assembly transaction pairstxData
        try {
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            //Determine if the address is localchainIdaddress
            boolean isAddressValidate = (AddressTool.getChainIdByAddress(address) == dexConfig.getChainId());
            if (!isAddressValidate) {
                return failed(DexErrorCode.ERROR_ADDRESS_ERROR);
            }
            String priKey = AccountCall.getPriKey(dexConfig.getChainId(), address, password);
            Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getChainId(), dexConfig.getAssetId(), address);
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            /* Assembly transaction sending (Send transaction) */
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
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction creator address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account password"),
            @Parameter(parameterName = "tradingHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction pairshash"),
            @Parameter(parameterName = "type", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-2]", parameterDes = "Transaction type：1Pay the bill,2Pay the bill"),
            @Parameter(parameterName = "amount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Currency transaction quantity"),
            @Parameter(parameterName = "price", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Listing price")
    })
    public Response createTradingOrderTx(Map params) {
        try {
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            //Determine if the address is localchainIdaddress
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

            /* Assembly transaction sending (Send transaction) */
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
            TradingOrder order = DexUtil.createTradingOrder(nulsHash, type, amount, price, address);
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
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction creator address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account password"),
            @Parameter(parameterName = "orderHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Order placementhash")
    })
    public Response createTradingCancelOrderTx(Map params) {
        try {
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            String orderHash = (String) params.get("orderHash");
            //Determine if the address is localchainIdaddress
            boolean isAddressValidate = (AddressTool.getChainIdByAddress(address) == dexConfig.getChainId());
            if (!isAddressValidate) {
                return failed(DexErrorCode.ERROR_ADDRESS_ERROR);
            }
            String priKey = null;
//            String priKey = AccountCall.getPriKey(dexConfig.getChainId(), address, password);
            //lookuporderDoes it exist
            TradingOrderPo orderPo = orderStorageService.query(HexUtil.decode(orderHash));
            if (orderPo == null) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND);
            }
            //Check whether the pending account is equal to the current calling interface account
            if (!Arrays.equals(AddressTool.getAddress(address), orderPo.getAddress())) {
                throw new NulsException(DexErrorCode.ACCOUNT_VALID_ERROR);
            }

            /* Assembly transaction sending (Send transaction) */
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
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction creator address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account password"),
            @Parameter(parameterName = "tradingHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction pairshash"),
            @Parameter(parameterName = "scaleQuoteDecimal", requestType = @TypeDescriptor(value = int.class), parameterDes = "Pricing currency allows for minimum transaction decimal places"),
            @Parameter(parameterName = "scaleBaseDecimal", requestType = @TypeDescriptor(value = int.class), parameterDes = "Transaction currency allows for minimum transaction decimal places"),
            @Parameter(parameterName = "minBaseAmount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Transaction currency supports minimum transaction amount"),
            @Parameter(parameterName = "minQuoteAmount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Pricing currency transactions support the minimum amount")
    })
    public Response editCoinTradingTx(Map params) {
        try {
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            String tradingHash = (String) params.get("tradingHash");

            String priKey = AccountCall.getPriKey(dexConfig.getChainId(), address, password);
            Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getChainId(), dexConfig.getAssetId(), address);
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            if (available.compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
            }
            Transaction tx = new Transaction();
            tx.setType(TxType.EDIT_COIN_TRADING);
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
            coinTrading.setMinBaseAmount(new BigInteger(params.get("minBaseAmount").toString()));
            coinTrading.setMinQuoteAmount(new BigInteger(params.get("minQuoteAmount").toString()));
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
            @Parameter(parameterName = "orderHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "order formhash")
    })
    public Response getTradingOrderNonceInfo(Map params) {
        try {
            String orderHash = (String) params.get("orderHash");
            //lookuporderDoes it exist
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
        //Create transaction pair fees
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
        to.setAddress(DexContext.sysFeeAddress);
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
        //Firstly, calculate the total amount of pricing currency required based on the quantity and unit price of transaction coins

        BigDecimal price = new BigDecimal(priceBigInteger).movePointLeft(tradingPo.getQuoteDecimal());
        BigDecimal amount = new BigDecimal(baseAmount).movePointLeft(tradingPo.getBaseDecimal());
        amount = amount.multiply(price).movePointRight(tradingPo.getQuoteDecimal()).setScale(0, RoundingMode.DOWN);
        BigInteger quoteAmount = amount.toBigInteger();     //The total amount of pricing currencies that can ultimately be exchanged

        Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), address);
        BigInteger available = new BigInteger(balanceMap.get("available").toString());
        String nonce = (String) balanceMap.get("nonce");
        if (available.compareTo(quoteAmount) < 0) {
            throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
        }
        CoinFrom from = new CoinFrom();
        from.setAssetsChainId(tradingPo.getQuoteAssetChainId());
        from.setAssetsId(tradingPo.getQuoteAssetId());
        from.setAddress(AddressTool.getAddress(address));
        from.setAmount(quoteAmount);
        from.setNonce(HexUtil.decode(nonce));
        coinData.addFrom(from);

        CoinTo to = new CoinTo();
        to.setAssetsChainId(tradingPo.getQuoteAssetChainId());
        to.setAssetsId(tradingPo.getQuoteAssetId());
        to.setAddress(AddressTool.getAddress(address));
        to.setAmount(quoteAmount);
        to.setLockTime(DexConstant.DEX_LOCK_TIME);
        coinData.addTo(to);

        return coinData;

//        //If the order is an asset in this chain, add the handling fee directly together
//        if (tradingPo.getQuoteAssetChainId() == dexConfig.getChainId() && tradingPo.getQuoteAssetId() == dexConfig.getAssetId()) {
//            Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getChainId(), dexConfig.getAssetId(), address);
//            BigInteger available = new BigInteger(balanceMap.get("available").toString());
//            String nonce = (String) balanceMap.get("nonce");
//            BigInteger fromAmount = quoteAmount.add(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
//            if (available.compareTo(fromAmount) < 0) {
//                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
//            }
//            CoinFrom from = new CoinFrom();
//            from.setAssetsChainId(dexConfig.getChainId());
//            from.setAssetsId(dexConfig.getAssetId());
//            from.setAddress(AddressTool.getAddress(address));
//            from.setAmount(fromAmount);
//            from.setNonce(HexUtil.decode(nonce));
//            coinData.addFrom(from);
//        } else {
//            //If the hanging order is an external chain asset, a handling fee for adding an internal chain asset is requiredfrom
//            Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), address);
//            BigInteger available = new BigInteger(balanceMap.get("available").toString());
//            String nonce = (String) balanceMap.get("nonce");
//            if (available.compareTo(quoteAmount) < 0) {
//                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
//            }
//            CoinFrom from1 = new CoinFrom();
//            from1.setAssetsChainId(tradingPo.getQuoteAssetChainId());
//            from1.setAssetsId(tradingPo.getQuoteAssetId());
//            from1.setAddress(AddressTool.getAddress(address));
//            from1.setAmount(quoteAmount);
//            from1.setNonce(HexUtil.decode(nonce));
//            coinData.addFrom(from1);
//
//            balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getChainId(), dexConfig.getAssetId(), address);
//            available = new BigInteger(balanceMap.get("available").toString());
//            nonce = (String) balanceMap.get("nonce");
//            if (available.compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
//                throw new NulsException(DexErrorCode.INSUFFICIENT_FEE);
//            }
//            CoinFrom from2 = new CoinFrom();
//            from2.setAssetsChainId(dexConfig.getChainId());
//            from2.setAssetsId(dexConfig.getAssetId());
//            from2.setAddress(AddressTool.getAddress(address));
//            from2.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
//            from2.setNonce(HexUtil.decode(nonce));
//            coinData.addFrom(from2);
//        }
//
//        CoinTo to = new CoinTo();
//        to.setAssetsChainId(tradingPo.getQuoteAssetChainId());
//        to.setAssetsId(tradingPo.getQuoteAssetId());
//        to.setAddress(AddressTool.getAddress(address));
//        to.setAmount(quoteAmount);
//        to.setLockTime(DexConstant.DEX_LOCK_TIME);
//        coinData.addTo(to);
//
//        return coinData;
    }

    private CoinData createSellOrderTxCoinData(String address, String tradingHash, BigInteger baseAmount) throws NulsException {
        CoinData coinData = new CoinData();

        TradingContainer container = dexManager.getTradingContainer(tradingHash);
        if (container == null) {
            throw new NulsException(CommonCodeConstanst.DATA_NOT_FOUND, "coinTrading not found");
        }
        CoinTradingPo tradingPo = container.getCoinTrading();

        Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId(), address);
        BigInteger available = new BigInteger(balanceMap.get("available").toString());
        String nonce = (String) balanceMap.get("nonce");
        if (available.compareTo(baseAmount) < 0) {
            throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
        }
        CoinFrom from = new CoinFrom();
        from.setAssetsChainId(tradingPo.getBaseAssetChainId());
        from.setAssetsId(tradingPo.getBaseAssetId());
        from.setAddress(AddressTool.getAddress(address));
        from.setAmount(baseAmount);
        from.setNonce(HexUtil.decode(nonce));
        coinData.addFrom(from);

        CoinTo to = new CoinTo();
        to.setAssetsChainId(tradingPo.getBaseAssetChainId());
        to.setAssetsId(tradingPo.getBaseAssetId());
        to.setAddress(AddressTool.getAddress(address));
        to.setAmount(baseAmount);
        to.setLockTime(DexConstant.DEX_LOCK_TIME);
        coinData.addTo(to);

        return coinData;

//        //If the order is an asset in this chain, add the handling fee directly together
//        if (tradingPo.getBaseAssetChainId() == dexConfig.getChainId() && tradingPo.getBaseAssetId() == dexConfig.getAssetId()) {
//            Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getChainId(), dexConfig.getAssetId(), address);
//            BigInteger available = new BigInteger(balanceMap.get("available").toString());
//            String nonce = (String) balanceMap.get("nonce");
//            BigInteger fromAmount = baseAmount.add(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
//            if (available.compareTo(fromAmount) < 0) {
//                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
//            }
//            CoinFrom from = new CoinFrom();
//            from.setAssetsChainId(dexConfig.getChainId());
//            from.setAssetsId(dexConfig.getAssetId());
//            from.setAddress(AddressTool.getAddress(address));
//            from.setAmount(fromAmount);
//            from.setNonce(HexUtil.decode(nonce));
//            coinData.addFrom(from);
//        } else {
//            //If the hanging order is an external chain asset, a handling fee for adding an internal chain asset is requiredfrom
//            Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId(), address);
//            BigInteger available = new BigInteger(balanceMap.get("available").toString());
//            String nonce = (String) balanceMap.get("nonce");
//            if (available.compareTo(baseAmount) < 0) {
//                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
//            }
//            CoinFrom from1 = new CoinFrom();
//            from1.setAssetsChainId(tradingPo.getBaseAssetChainId());
//            from1.setAssetsId(tradingPo.getBaseAssetId());
//            from1.setAddress(AddressTool.getAddress(address));
//            from1.setAmount(baseAmount);
//            from1.setNonce(HexUtil.decode(nonce));
//            coinData.addFrom(from1);
//
//            balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getChainId(), dexConfig.getAssetId(), address);
//            available = new BigInteger(balanceMap.get("available").toString());
//            nonce = (String) balanceMap.get("nonce");
//            if (available.compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
//                throw new NulsException(DexErrorCode.INSUFFICIENT_FEE);
//            }
//            CoinFrom from2 = new CoinFrom();
//            from2.setAssetsChainId(dexConfig.getChainId());
//            from2.setAssetsId(dexConfig.getAssetId());
//            from2.setAddress(AddressTool.getAddress(address));
//            from2.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
//            from2.setNonce(HexUtil.decode(nonce));
//            coinData.addFrom(from2);
//        }
//
//        CoinTo to = new CoinTo();
//        to.setAssetsChainId(tradingPo.getBaseAssetChainId());
//        to.setAssetsId(tradingPo.getBaseAssetId());
//        to.setAddress(AddressTool.getAddress(address));
//        to.setAmount(baseAmount);
//        to.setLockTime(DexConstant.DEX_LOCK_TIME);
//        coinData.addTo(to);
//
//        return coinData;
    }


    private CoinData createTradingOrderCancelTxCoinData(String address, TradingOrderPo orderPo) throws NulsException {
        TradingContainer container = dexManager.getTradingContainer(orderPo.getTradingHash().toHex());

        Map balanceMap = AccountCall.getAccountBalance(dexConfig.getChainId(), dexConfig.getChainId(), dexConfig.getAssetId(), address);
        BigInteger available = new BigInteger(balanceMap.get("available").toString());
        String nonce = (String) balanceMap.get("nonce");
        if (available.compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
            throw new NulsException(DexErrorCode.INSUFFICIENT_FEE);
        }

        CoinData coinData = new CoinData();
        CoinFrom from = new CoinFrom();
        from.setAssetsChainId(dexConfig.getChainId());
        from.setAssetsId(dexConfig.getAssetId());
        from.setAddress(orderPo.getAddress());
        from.setNonce(HexUtil.decode(nonce));
        from.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
        coinData.addFrom(from);

        CoinTo to = new CoinTo();
        to.setAssetsChainId(dexConfig.getChainId());
        to.setAssetsId(dexConfig.getAssetId());
        to.setAddress(from.getAddress());
        to.setLockTime(0);
        to.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
        coinData.addTo(to);

        return coinData;
    }

}
