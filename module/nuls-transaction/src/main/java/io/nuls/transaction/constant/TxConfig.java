package io.nuls.transaction.constant;

import io.nuls.core.basic.ModuleConfig;
import io.nuls.core.basic.VersionChangeInvoker;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.transaction.model.bo.config.ConfigBean;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * Transaction module setting
 * @author: Charlie
 * @date: 2019/03/14
 */
@Component
@Configuration(domain = ModuleE.Constant.TRANSACTION)
public class TxConfig extends ConfigBean implements ModuleConfig {

    /** ROCK DB 数据库文件存储路径*/
    private String dataPath;
    /** 模块code*/
    private String moduleCode;
    /** 主链链ID*/
    private int mainChainId;
    /** 主链主资产ID*/
    private int mainAssetId;
    /** 编码*/
    private String encoding;
    /** 黑洞公钥*/
    private String blackHolePublicKey;
    /** 交易时间所在区块时间的默认范围值(在区块时间±本值范围内)*/
    private long blockTxTimeRangeSec;
    /** 孤儿交易生命时间,超过会被清理**/
    private int orphanLifeTimeSec;
    /** 未确认交易过期时间秒 */
    private long unconfirmedTxExpireSec;
    /** 单个交易数据最大值(B)*/
    private long txMaxSize;
    /** coinTo 不支持金额等于0 的协议生效高度*/
    private long coinToPtlHeightFirst;
    /** coinTo 支持金额等于0, 只禁止金额为0的锁定 的协议生效高度*/
    private long coinToPtlHeightSecond;

    private String blackListPath;

    public String getBlackListPath() {
        return blackListPath;
    }

    public void setBlackListPath(String blackListPath) {
        this.blackListPath = blackListPath;
    }

    public String getBlackHolePublicKey() {
        return blackHolePublicKey;
    }

    public void setBlackHolePublicKey(String blackHolePublicKey) {
        this.blackHolePublicKey = blackHolePublicKey;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getTxDataRoot() {
        return dataPath + File.separator + ModuleE.TX.name;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public int getMainChainId() {
        return mainChainId;
    }

    public void setMainChainId(int mainChainId) {
        this.mainChainId = mainChainId;
    }

    public int getMainAssetId() {
        return mainAssetId;
    }

    public void setMainAssetId(int mainAssetId) {
        this.mainAssetId = mainAssetId;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public long getUnconfirmedTxExpireSec() {
        return unconfirmedTxExpireSec;
    }

    public void setUnconfirmedTxExpireSec(long unconfirmedTxExpireSec) {
        this.unconfirmedTxExpireSec = unconfirmedTxExpireSec;
    }

    public long getBlockTxTimeRangeSec() {
        return blockTxTimeRangeSec;
    }

    public void setBlockTxTimeRangeSec(long blockTxTimeRangeSec) {
        this.blockTxTimeRangeSec = blockTxTimeRangeSec;
    }

    public int getOrphanLifeTimeSec() {
        return orphanLifeTimeSec;
    }

    public void setOrphanLifeTimeSec(int orphanLifeTimeSec) {
        this.orphanLifeTimeSec = orphanLifeTimeSec;
    }

    public long getTxMaxSize() {
        return txMaxSize;
    }

    public void setTxMaxSize(long txMaxSize) {
        this.txMaxSize = txMaxSize;
    }

    public long getCoinToPtlHeightFirst() {
        return coinToPtlHeightFirst;
    }

    public void setCoinToPtlHeightFirst(long coinToPtlHeightFirst) {
        this.coinToPtlHeightFirst = coinToPtlHeightFirst;
    }

    public long getCoinToPtlHeightSecond() {
        return coinToPtlHeightSecond;
    }

    public void setCoinToPtlHeightSecond(long coinToPtlHeightSecond) {
        this.coinToPtlHeightSecond = coinToPtlHeightSecond;
    }

    @Override
    public VersionChangeInvoker getVersionChangeInvoker() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> aClass = Class.forName("io.nuls.transaction.rpc.upgrade.TxVersionChangeInvoker");
        return (VersionChangeInvoker) aClass.getDeclaredConstructor().newInstance();
    }
}
