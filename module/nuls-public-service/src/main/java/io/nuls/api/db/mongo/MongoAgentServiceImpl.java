package io.nuls.api.db.mongo;

import com.mongodb.client.model.*;
import io.nuls.api.ApiContext;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.db.AgentService;
import io.nuls.api.db.AliasService;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.AgentInfo;
import io.nuls.api.model.po.AliasInfo;
import io.nuls.api.model.po.PageInfo;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.nuls.api.constant.DBTableConstant.AGENT_TABLE;

@Component
public class MongoAgentServiceImpl implements AgentService {

    @Autowired
    private MongoDBService mongoDBService;
    @Autowired
    private MongoAliasServiceImpl mongoAliasServiceImpl;

    @Autowired
    AliasService aliasService;

    @Override
    public void initCache() {
        for (ApiCache apiCache : CacheManager.getApiCaches().values()) {
            List<Document> documentList = mongoDBService.query(AGENT_TABLE + apiCache.getChainInfo().getChainId());
            List<AgentInfo> saveAlias = new ArrayList<>();
            for (Document document : documentList) {
                AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
                AliasInfo aliasInfo = aliasService.getAliasByAddress(ApiContext.defaultChainId,agentInfo.getAgentAddress());
                if(aliasInfo != null && StringUtils.isBlank(agentInfo.getAgentAlias())){
                    agentInfo.setAgentAlias(aliasInfo.getAlias());
                    saveAlias.add(agentInfo);
                }
                apiCache.addAgentInfo(agentInfo);
            }
            if(!saveAlias.isEmpty()){
                saveAgentList(ApiContext.defaultChainId,saveAlias);
            }
        }
    }

    @Override
    public AgentInfo getAgentByHash(int chainID, String agentHash) {
        AgentInfo agentInfo = CacheManager.getCache(chainID).getAgentInfo(agentHash);
        if (agentInfo == null) {
            Document document = mongoDBService.findOne(AGENT_TABLE + chainID, Filters.eq("_id", agentHash));
            agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
            CacheManager.getCache(chainID).addAgentInfo(agentInfo);
        }
        return agentInfo.copy();
    }

    @Override
    public PageInfo<AgentInfo> getAgentByHashList(int chainID, int pageNumber, int pageSize, List<String> hashList) {
        PageInfo<AgentInfo> page = new PageInfo<>(pageNumber, pageSize);
        page.setTotalCount(hashList.size());
        int start = (pageNumber - 1) * pageSize;
        if (hashList.size() < start) {
            return page;
        }
        int end = pageNumber * pageSize;
        if (end > hashList.size()) {
            end = hashList.size();
        }
        hashList = hashList.subList(start, end);
        List<AgentInfo> agentInfoList = new ArrayList<>();
        for (String agentHash : hashList) {
            agentInfoList.add(getAgentByHash(chainID, agentHash));
        }
        page.setList(agentInfoList);
        return page;
    }

    @Override
    public AgentInfo getAgentByRewardAddress(int chainID, String rewardAddress) {
        Collection<AgentInfo> agentInfos = CacheManager.getCache(chainID).getAgentMap().values();
        AgentInfo agentInfo = null;
        for (AgentInfo agent : agentInfos) {
            if (!agent.getRewardAddress().equals(rewardAddress)) {
                continue;
            }
            if (null == agentInfo || agent.getCreateTime() > agentInfo.getCreateTime()) {
                agentInfo = agent;
            }
        }
        if (agentInfo == null) {
            return null;
        }
        return agentInfo.copy();
    }

    @Override
    public AgentInfo getAgentByAgentAddress(int chainID, String agentAddress) {
        Collection<AgentInfo> agentInfos = CacheManager.getCache(chainID).getAgentMap().values();
        AgentInfo agentInfo = null;
        for (AgentInfo agent : agentInfos) {
            if (!agentAddress.equals(agent.getAgentAddress())) {
                continue;
            }
            if (null == agentInfo || agent.getCreateTime() > agentInfo.getCreateTime()) {
                agentInfo = agent;
            }
        }
        if (agentInfo == null) {
            return null;
        }
        return agentInfo.copy();
    }

    @Override
    public AgentInfo getAliveAgentByAgentAddress(int chainID, String agentAddress) {
        Collection<AgentInfo> agentInfos = CacheManager.getCache(chainID).getAgentMap().values();
        AgentInfo agentInfo = null;
        for (AgentInfo agent : agentInfos) {
            if (!agentAddress.equals(agent.getAgentAddress())) {
                continue;
            }
            if (agent.getStatus() == 2) {
                continue;
            }
            if (null == agentInfo || agent.getCreateTime() > agentInfo.getCreateTime()) {
                agentInfo = agent;
            }
        }
        if (agentInfo == null) {
            return null;
        }
        return agentInfo.copy();
    }

    @Override
    public void saveAgentList(int chainID, List<AgentInfo> agentInfoList) {
        if (agentInfoList.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (AgentInfo agentInfo : agentInfoList) {
            Document document = DocumentTransferTool.toDocument(agentInfo, "txHash");

            if (agentInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
                agentInfo.setNew(false);
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", agentInfo.getTxHash()), document));
            }
        }
        BulkWriteOptions options = new BulkWriteOptions();
        options.ordered(false);
        mongoDBService.bulkWrite(AGENT_TABLE + chainID, modelList, options);
        ApiCache cache = CacheManager.getCache(chainID);
        for (AgentInfo agentInfo : agentInfoList) {
            cache.addAgentInfo(agentInfo);
        }
    }

    public void rollbackAgentList(int chainId, List<AgentInfo> agentInfoList) {
        initCache();
        if (agentInfoList.isEmpty()) {
            return;
        }
        ApiCache apiCache = CacheManager.getCache(chainId);
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (AgentInfo agentInfo : agentInfoList) {
            if (agentInfo.isNew()) {
                modelList.add(new DeleteOneModel(Filters.eq("_id", agentInfo.getTxHash())));
                apiCache.getAgentMap().remove(agentInfo.getTxHash());
            } else {
                Document document = DocumentTransferTool.toDocument(agentInfo, "txHash");
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", agentInfo.getTxHash()), document));
                apiCache.addAgentInfo(agentInfo);
            }
        }
        BulkWriteOptions options = new BulkWriteOptions();
        options.ordered(false);
        mongoDBService.bulkWrite(AGENT_TABLE + chainId, modelList, options);
    }

    @Override
    public List<AgentInfo> getAgentListByStartHeight(int chainId, long startHeight) {
        ApiCache apiCache = CacheManager.getCache(chainId);
        Collection<AgentInfo> agentInfos = apiCache.getAgentMap().values();
        List<AgentInfo> resultList = new ArrayList<>();
        for (AgentInfo agent : agentInfos) {
            if (agent.getDeleteHash() != null && agent.getDeleteHeight() <= startHeight) {
                continue;
            }
            if (agent.getBlockHeight() > startHeight) {
                continue;
            }
            resultList.add(agent);
        }

//        Bson bson = Filters.and(Filters.lte("blockHeight", startHeight), Filters.or(Filters.eq("deleteHeight", 0), Filters.gt("deleteHeight", startHeight)));
//
//        List<Document> list = this.mongoDBService.query(MongoTableName.AGENT_INFO, bson);

//        for (Document document : list) {
//            AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
//            AliasInfo alias = mongoAliasServiceImpl.getAliasByAddress(agentInfo.getAgentAddress());
//            if (alias != null) {
//                agentInfo.setAgentAlias(alias.getAlias());
//            }
//            resultList.add(agentInfo);
//        }

        return resultList;
    }

    @Override
    public List<AgentInfo> getAllAgentList(int chainId) {
        Bson filter = Filters.eq("deleteHeight", 0);
        int limit = Integer.MAX_VALUE;
        List<AgentInfo> agentInfoList = DocumentTransferTool.toInfoList(
                this.mongoDBService.getCollection(AGENT_TABLE + chainId)
                        .find(filter)
                        .sort(Sorts.descending("deposit")),
                "txHash",
                AgentInfo.class);
        agentInfoList.forEach(agentInfo->{
            AliasInfo alias = mongoAliasServiceImpl.getAliasByAddress(chainId, agentInfo.getAgentAddress());
            if (alias != null) {
                agentInfo.setAgentAlias(alias.getAlias());
            }
        });
        return agentInfoList;
    }

    @Override
    public PageInfo<AgentInfo> getAgentList(int chainId, int pageNumber, int pageSize) {
        long totalCount = this.mongoDBService.getCount(AGENT_TABLE + chainId);
        List<Document> docsList = this.mongoDBService.pageQuery(AGENT_TABLE + chainId, Sorts.descending("createTime"), pageNumber, pageSize);
        List<AgentInfo> agentInfoList = new ArrayList<>();
        for (Document document : docsList) {
            AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
            AliasInfo alias = mongoAliasServiceImpl.getAliasByAddress(chainId, agentInfo.getAgentAddress());
            if (alias != null) {
                agentInfo.setAgentAlias(alias.getAlias());
            }
            agentInfoList.add(agentInfo);
            if (agentInfo.getType() == 0 && null != agentInfo.getAgentAddress()) {
                if (ApiContext.DEVELOPER_NODE_ADDRESS.contains(agentInfo.getAgentAddress())) {
                    agentInfo.setType(2);
                } else if (ApiContext.AMBASSADOR_NODE_ADDRESS.contains(agentInfo.getAgentAddress())) {
                    agentInfo.setType(3);
                } else {
                    agentInfo.setType(1);
                }
            }
        }
        PageInfo<AgentInfo> pageInfo = new PageInfo<>(pageNumber, pageSize, totalCount, agentInfoList);
        return pageInfo;
    }

    @Override
    public long agentsCount(int chainId, long startHeight) {
        ApiCache apiCache = CacheManager.getCache(chainId);
        Collection<AgentInfo> agentInfos = apiCache.getAgentMap().values();
        long count = 0;
        for (AgentInfo agent : agentInfos) {
            if (agent.getDeleteHash() != null && agent.getDeleteHeight() <= startHeight) {
                continue;
            }
            if (agent.getBlockHeight() > startHeight) {
                continue;
            }
            count++;
        }
        return count;
//        Bson bson = Filters.and(Filters.lte("blockHeight", startHeight), Filters.or(Filters.eq("deleteHeight", 0), Filters.gt("deleteHeight", startHeight)));
//        return this.mongoDBService.getCount(MongoTableName.AGENT_INFO, bson);
    }

    @Override
    public BigInteger getConsensusCoinTotal(int chainId) {
        BigInteger total = BigInteger.ZERO;

        ApiCache apiCache = CacheManager.getCache(chainId);
        for (AgentInfo agentInfo : apiCache.getAgentMap().values()) {
            if (agentInfo.getDeleteHash() == null) {
                total = total.add(agentInfo.getDeposit());
            }
        }
        return total;
    }
}
