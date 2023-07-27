package io.nuls.consensus.v1.cache;

import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.core.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eva
 */
public class RoundCache {

    private MeetingRound currentRound;

    private List<Long> keyList = new ArrayList<>();
    private Map<Long, MeetingRound> roundMap = new HashMap<>();

    public MeetingRound get(Long key) {
        return roundMap.get(key);
    }

    public void put(Long key, MeetingRound round) {
        this.roundMap.put(key, round);
        keyList.add(key);
        if (keyList.size() > 50) {
            Long oldKey = keyList.remove(0);
            roundMap.remove(oldKey);
        }
    }

    public MeetingRound getCurrentRound() {
        return currentRound;
    }

    public void switchRound(MeetingRound round) {
        this.currentRound = round;
        this.put(round.getIndex(),round);
    }

    public void clear() {
        this.roundMap.clear();
    }

    public MeetingRound getRoundByIndex(long roundIndex) {
        for (Map.Entry<Long, MeetingRound> entry : roundMap.entrySet()) {
            if (entry.getValue().getIndex() == roundIndex) {
                return entry.getValue();
            }
        }
//        找不到的情况，看看里面都存了啥
        for (Map.Entry<Long, MeetingRound> entry : roundMap.entrySet()) {
            Log.info(entry.getKey()+"");
        }
        return null;
    }
}
