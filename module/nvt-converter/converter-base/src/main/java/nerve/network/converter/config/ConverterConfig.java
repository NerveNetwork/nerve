package nerve.network.converter.config;

import nerve.network.converter.model.bo.ConfigBean;
import io.nuls.core.basic.ModuleConfig;
import io.nuls.core.basic.VersionChangeInvoker;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.rpc.model.ModuleE;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

/**
 * Transaction module setting
 *
 * @author: Chino
 * @date: 2019/03/14
 */
@Component
@Configuration(domain = ModuleE.Constant.CONVERTER)
public class ConverterConfig extends ConfigBean implements ModuleConfig {

    /**
     * ROCK DB 数据库文件存储路径
     */
    private String dataPath;
    /**
     * 模块code
     */
    private String moduleCode;
    /**
     * 主链链ID
     */
    private int mainChainId;
    /**
     * 主链主资产ID
     */
    private int mainAssetId;
    /**
     * 触发初始化虚拟银行的区块高度
     */
    public long initVirtualBankHeight;
    /**
     * 手续费汇集分发公钥
     */
    public String feePubkey;
    /**
     * 提现黑洞公钥
     */
    public String withdrawalBlackholePubkey;
    /**
     * 触发执行虚拟银行变更交易的高度周期 配置
     * 按2秒一个块 大约1天的出块数量
     */
    public long executeChangeVirtualBankPeriodicHeight;
    /**
     * 虚拟银行共识节点总数（非种子节点）
     */
    public int virtualBankAgentNumber;
    /**
     * 网络第一次开启节点变更服务的节点总数阈值
     */
    public  int agentCountOfEnableVirtualBankChanges;

    public  BigInteger proposalPrice;

    public String getTxDataRoot() {
        return dataPath + File.separator + ModuleE.CV.name;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
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

    public long getExecuteChangeVirtualBankPeriodicHeight() {
        return executeChangeVirtualBankPeriodicHeight;
    }

    public void setExecuteChangeVirtualBankPeriodicHeight(long executeChangeVirtualBankPeriodicHeight) {
        this.executeChangeVirtualBankPeriodicHeight = executeChangeVirtualBankPeriodicHeight;
    }

    public String getFeePubkey() {
        return feePubkey;
    }

    public void setFeePubkey(String feePubkey) {
        this.feePubkey = feePubkey;
    }

    public String getWithdrawalBlackholePubkey() {
        return withdrawalBlackholePubkey;
    }

    public void setWithdrawalBlackholePubkey(String withdrawalBlackholePubkey) {
        this.withdrawalBlackholePubkey = withdrawalBlackholePubkey;
    }

    public long getInitVirtualBankHeight() {
        return initVirtualBankHeight;
    }

    public void setInitVirtualBankHeight(long initVirtualBankHeight) {
        this.initVirtualBankHeight = initVirtualBankHeight;
    }

    public int getVirtualBankAgentNumber() {
        return virtualBankAgentNumber;
    }

    public void setVirtualBankAgentNumber(int virtualBankAgentNumber) {
        this.virtualBankAgentNumber = virtualBankAgentNumber;
    }

    public int getAgentCountOfEnableVirtualBankChanges() {
        return agentCountOfEnableVirtualBankChanges;
    }

    public void setAgentCountOfEnableVirtualBankChanges(int agentCountOfEnableVirtualBankChanges) {
        this.agentCountOfEnableVirtualBankChanges = agentCountOfEnableVirtualBankChanges;
    }

    public BigInteger getProposalPrice() {
        return proposalPrice;
    }

    public void setProposalPrice(BigInteger proposalPrice) {
        this.proposalPrice = proposalPrice;
    }

    @Override
    public VersionChangeInvoker getVersionChangeInvoker() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        //todo 协议升级触发 待完善
        Class<?> aClass = Class.forName("io.nuls.converter.rpc.upgrade.VersionChangeInvoker");
        return (VersionChangeInvoker) aClass.getDeclaredConstructor().newInstance();
    }
}
