package io.nuls.consensus.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class FIFOCache <K,V> extends LinkedHashMap<K, V> {
    private final int SIZE;
    public FIFOCache(int size) {
        super();
        SIZE = size;
    }
    /**
     * 重写淘汰机制
     * @param eldest
     * @return
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        //如果缓存存储达到最大值删除最后一个
        return size() > SIZE;
    }
}
