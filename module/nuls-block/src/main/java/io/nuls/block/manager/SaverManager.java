package io.nuls.block.manager;

import io.nuls.block.service.BlockService;
import io.nuls.block.thread.BlockSaver;
import io.nuls.core.thread.ThreadUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
public class SaverManager {

    private static Map<Integer, BlockSaver> saverMap = new HashMap<>();

    public static void startSaver(int chainId, BlockService blockService) {
        BlockSaver saver = new BlockSaver(blockService);
        saverMap.put(chainId, saver);
        ThreadUtils.createAndRunThread("saver" + chainId, saver);
    }

    public static void offer(BlockSaver.Saver saver) {
        if (null == saver || saver.getChainId() <= 0) {
            return;
        }
        BlockSaver blockSaver = saverMap.get(saver.getChainId());
        blockSaver.offer(saver);
    }
}
