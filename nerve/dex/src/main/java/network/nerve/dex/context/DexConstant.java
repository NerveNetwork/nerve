package network.nerve.dex.context;

import java.math.BigDecimal;

public interface DexConstant {

    //Purchase order commission type
    byte TRADING_ORDER_BUY_TYPE = 1;
    //Sales order commission type
    byte TRADING_ORDER_SELL_TYPE = 2;
    //Effective assets
    int ASSET_ENABLE = 1;
    //Invalid assets
    int ASSET_DISABLE = 0;
    //fromLock type
    byte ASSET_LOCK_TYPE = -1;
    //Entrusted order locking type
    byte DEX_LOCK_TIME = -2;

    //Purchase order fully completed
    byte ORDER_BUY_OVER = 1;
    //Complete transaction of sales order
    byte ORDER_SELL_OVER = 2;
    //Both buy and sell orders are fully executed
    byte ORDER_ALL_OVER = 3;
    //Take the initiative to pay the bill
    byte BUY_TAKER = 1;
    //Selling orders and actively accepting orders
    byte SELL_TAKER = 2;
    //Cancel order successful
    byte CANCEL_ORDER_SUCC = 1;
    //Cancel order failed
    byte CANCEL_ORDER_FAIL = 0;

    int NONCE_LENGTH = 8;

    BigDecimal PROP = new BigDecimal(10000);

    String SYS_FILE_ENCODING = "file.encoding";

    String DEX_CONFIG_FILE = "dex-config-";
}
