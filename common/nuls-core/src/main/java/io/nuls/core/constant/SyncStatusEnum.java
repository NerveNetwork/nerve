package io.nuls.core.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * Node synchronization status enumeration
 * @author: Charlie
 * @date: 2020-03-12
 */
public enum SyncStatusEnum {

    // Synchronization status 0:In synchronization,1:Under normal operation
    SYNC(0),

    RUNNING(1);

    private int value;
    private static Map<Integer, SyncStatusEnum> map;

    private SyncStatusEnum(int value) {
        this.value = value;
        putValue(value, this);
    }

    public int value() {
        return value;
    }

    private static SyncStatusEnum putValue(int value, SyncStatusEnum valueEnum) {
        if (map == null) {
            map = new HashMap<>(8);
        }
        return map.put(value, valueEnum);
    }

    public static SyncStatusEnum getEnum(int value) {
        return map.get(value);
    }
}
