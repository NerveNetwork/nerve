package network.nerve.swap.model;

import io.nuls.core.log.logback.NulsLogger;
import network.nerve.swap.config.ConfigBean;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.enums.BlockType;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LatestBasicBlock;
import network.nerve.swap.model.dto.PairsP17Info;

/**
 * 链的基础数据和运行状态数据
 * Chain information class
 *
 * @author: Loki
 * @date: 2019/04/16
 */
public class Chain {

    /**
     * 打包区块 - 0, 验证区块 - 1
     */
    private static ThreadLocal<Integer> currentThreadBlockType = new ThreadLocal<>();

    private ConfigBean config;

    /**
     * 最新区块高度等简略信息
     */
    private LatestBasicBlock latestBasicBlock = new LatestBasicBlock();

    /**
     * 日志
     */
    private NulsLogger logger;

    /**
     * 打包区块时批量执行信息
     */
    private BatchInfo batchInfo;

    /**
     * 验证区块时批量执行信息
     */
    private BatchInfo verifyBatchInfo;
    private PairsP17Info pairsP17Info;

    public PairsP17Info getPairsP17Info() {
        return pairsP17Info;
    }

    public void setPairsP17Info(PairsP17Info pairsP17Info) {
        this.pairsP17Info = pairsP17Info;
    }

    public BatchInfo getBatchInfo() {
        Integer blockType = currentThreadBlockType.get();
        if(blockType == null) {
            return null;
        }
        if(blockType == BlockType.PACKAGE_BLOCK.type()) {
            return batchInfo;
        }
        if(blockType == BlockType.VERIFY_BLOCK.type()) {
            return verifyBatchInfo;
        }
        SwapContext.logger.error("Unkown blockType! - [{}]", blockType);
        return null;
    }

    public void setBatchInfo(BatchInfo batchInfo) {
        Integer blockType = currentThreadBlockType.get();
        if(blockType == null) {
            return;
        }
        if(blockType == BlockType.PACKAGE_BLOCK.type()) {
            this.batchInfo = batchInfo;
            return;
        }
        if(blockType == BlockType.VERIFY_BLOCK.type()) {
            this.verifyBatchInfo = batchInfo;
            return;
        }
        SwapContext.logger.error("Setting value error. Unkown blockType! - [{}]", blockType);
    }

    public LatestBasicBlock getLatestBasicBlock() {
        return latestBasicBlock;
    }

    public void setLatestBasicBlock(LatestBasicBlock latestBasicBlock) {
        this.latestBasicBlock = latestBasicBlock;
    }

    public static void putCurrentThreadBlockType(Integer blockType) {
        currentThreadBlockType.set(blockType);
    }

    public static Integer currentThreadBlockType() {
        return currentThreadBlockType.get();
    }

    public ConfigBean getConfig() {
        return config;
    }

    public void setConfig(ConfigBean config) {
        this.config = config;
    }

    public NulsLogger getLogger() {
        return logger;
    }

    public void setLogger(NulsLogger logger) {
        this.logger = logger;
    }

    public int getChainId() {
        return config.getChainId();
    }

    public Long getBestHeight() {
        return latestBasicBlock.getHeight();
    }

}
