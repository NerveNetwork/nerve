package io.nuls.consensus.utils.thread;

import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.ConsensusIdentityData;
import io.nuls.consensus.utils.ConsensusNetUtil;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.consensus.network.message.v1.ConsensusIdentityProcessor;

public class CacheConsensusIdentityProcessor implements Runnable {
    private Chain chain;
    public  CacheConsensusIdentityProcessor(Chain chain){
        this.chain = chain;
    }

    @Override
    public void run() {
//        chain.getLogger().info("Initial completion of consensus network and processing of connection information received before" );
        ConsensusIdentityProcessor processor = SpringLiteContext.getBean(ConsensusIdentityProcessor.class);
        for (ConsensusIdentityData consensusIdentityData : ConsensusNetUtil.UNTREATED_MESSAGE_SET){
            processor.process(chain.getChainId(), consensusIdentityData.getNodeId(), consensusIdentityData.getMessage());
        }
        ConsensusNetUtil.UNTREATED_MESSAGE_SET.clear();
    }
}
