package io.nuls.base.api.provider.dex;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.dex.facode.CoinTradingReq;
import io.nuls.base.api.provider.dex.facode.DexQueryReq;
import io.nuls.base.api.provider.dex.facode.EditTradingReq;

import java.util.Map;

public interface DexProvider {

    Result<String> createTrading(CoinTradingReq req);

    Result<String> editTrading(EditTradingReq req);

    Result<Map> getTrading(DexQueryReq req);

}
