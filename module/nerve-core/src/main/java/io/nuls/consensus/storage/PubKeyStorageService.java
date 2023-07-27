package io.nuls.consensus.storage;

import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.po.PubKeyPo;

public interface PubKeyStorageService {
    /**
     * 保存
     * @param po     公钥数据
     * @param chain  链信息
     * @return  保存是否成功
     * */
    boolean save(PubKeyPo po, Chain chain);

    /**
     * 获取链公钥信息
     * @param chain 链信息
     * @return      本链公钥列表
     * */
    PubKeyPo get(Chain chain);
}
