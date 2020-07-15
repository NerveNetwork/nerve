package network.nerve.pocbft.utils.thread;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.ConsensusIdentityData;
import network.nerve.pocbft.utils.ConsensusNetUtil;
import io.nuls.core.core.ioc.SpringLiteContext;
import network.nerve.pocbft.network.message.v1.ConsensusIdentityProcessor;

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
