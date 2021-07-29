package network.nerve.swap.manager;

import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.ArraysTool;
import network.nerve.swap.model.po.FarmUserInfoPO;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
public class FarmUserInfoTempManager {
    private final Map<String, FarmUserInfoPO> userInfoMap = new HashMap<>();

    public void putUserInfo(FarmUserInfoPO userInfoPO) {
        userInfoMap.put(getKey(userInfoPO.getFarmHash().getBytes(), userInfoPO.getUserAddress()), userInfoPO);
    }

    public FarmUserInfoPO getUserInfo(NulsHash farmHash, byte[] address) {
        return userInfoMap.get(getKey(farmHash.getBytes(),address));
    }

    public String getKey(byte[] farmHash, byte[] address) {
        return HexUtil.encode(ArraysTool.concatenate(farmHash, address));
    }
}
