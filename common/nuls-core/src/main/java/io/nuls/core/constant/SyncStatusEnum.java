package io.nuls.core.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点同步状态枚举
 * @author: Charlie
 * @date: 2020-03-12
 */
public enum SyncStatusEnum {

    // 同步状态 0:同步中，1:正常运行中
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