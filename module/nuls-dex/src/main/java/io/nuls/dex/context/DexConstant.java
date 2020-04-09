package io.nuls.dex.context;

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

    byte ORDER_BUY_OVER = 1;

    byte ORDER_SELL_OVER = 2;

    byte ORDER_ALL_OVER = 3;

    String SYS_FILE_ENCODING = "file.encoding";
}
