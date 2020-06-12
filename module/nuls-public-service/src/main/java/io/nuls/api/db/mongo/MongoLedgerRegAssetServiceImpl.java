package io.nuls.api.db.mongo;

import com.mongodb.client.model.Filters;
import io.nuls.api.db.LedgerRegAssetService;
import io.nuls.api.model.po.LedgerRegAssetInfo;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.nuls.api.constant.DBTableConstant.LEDGER_REG_ASSET_TABLE;

/**
 * @Author: zhoulijun
 * @Time: 2020-04-24 14:11
 * @Description: 功能描述
 */
@Component
public class MongoLedgerRegAssetServiceImpl implements LedgerRegAssetService {

    @Autowired
    MongoDBService mongoDBService;

    @Override
    public void save(LedgerRegAssetInfo info) {
        Objects.requireNonNull(info);
        mongoDBService.insertOne(LEDGER_REG_ASSET_TABLE + info.getAssetChainId(), DocumentTransferTool.toDocument(info));
    }

    @Override
    public List<LedgerRegAssetInfo> queryByHeight(int chainId, Long blockHeight) {
        return mongoDBService.query(LEDGER_REG_ASSET_TABLE + chainId, Filters.eq("blockHeight",blockHeight))
                .stream().map(d-> DocumentTransferTool.toInfo(d,LedgerRegAssetInfo.class)).collect(Collectors.toList());
    }
}
