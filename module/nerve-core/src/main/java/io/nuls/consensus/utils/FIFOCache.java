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
     * Rewrite elimination mechanism
     * @param eldest
     * @return
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        //If the cache storage reaches its maximum value, delete the last one
        return size() > SIZE;
    }
}
