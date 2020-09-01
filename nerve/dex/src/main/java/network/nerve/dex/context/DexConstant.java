package network.nerve.dex.context;

import java.math.BigDecimal;

public interface DexConstant {

    //买单委托类型
    byte TRADING_ORDER_BUY_TYPE = 1;
    //卖单委托类型
    byte TRADING_ORDER_SELL_TYPE = 2;
    //有效资产
    int ASSET_ENABLE = 1;
    //无效资产
    int ASSET_DISABLE = 0;
    //from锁定类型
    byte ASSET_LOCK_TYPE = -1;
    //委托挂单锁定类型
    byte DEX_LOCK_TIME = -2;

    //买单完全成交
    byte ORDER_BUY_OVER = 1;
    //卖单完全成交
    byte ORDER_SELL_OVER = 2;
    //买单卖单都完全成交
    byte ORDER_ALL_OVER = 3;
    //买单主动吃单
    byte BUY_TAKER = 1;
    //卖单主动吃单
    byte SELL_TAKER = 2;
    //取消订单成功
    byte CANCEL_ORDER_SUCC = 1;
    //取消订单失败
    byte CANCEL_ORDER_FAIL = 0;

    int NONCE_LENGTH = 8;

    BigDecimal PROP = new BigDecimal(10000);

    String SYS_FILE_ENCODING = "file.encoding";

    String DEX_CONFIG_FILE = "dex-config-";
}
