package nerve.network.pocbft.utils.manager;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.DoubleUtils;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.model.bo.config.ConsensusChainConfig;
import nerve.network.pocbft.model.bo.tx.txdata.Deposit;
import nerve.network.pocbft.model.po.DepositPo;
import nerve.network.pocbft.storage.DepositStorageService;
import nerve.network.pocbft.utils.ConsensusAwardUtil;
import nerve.network.pocbft.utils.compare.DepositComparator;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import nerve.network.pocbft.utils.enumeration.DepositTimeType;
import nerve.network.pocbft.utils.enumeration.DepositType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 委托信息管理类，负责委托信息相关处理
 * Delegated information management category, responsible for delegated information related processing
 *
 * @author: Jason
 * 2018/12/5
 */
@Component
public class DepositManager {
    @Autowired
    private DepositStorageService depositStorageService;

    @Autowired
    private ConsensusChainConfig config;

    /**
     * 初始化委托信息
     * Initialize delegation information
     *
     * @param chain 链信息/chain info
     */
    public void loadDeposits(Chain chain) throws Exception {
        List<Deposit> allDepositList = new ArrayList<>();
        List<DepositPo> poList = depositStorageService.getList(chain.getConfig().getChainId());
        for (DepositPo po : poList) {
            Deposit deposit = new Deposit(po);
            allDepositList.add(deposit);
        }
        allDepositList.sort(new DepositComparator());
        chain.setDepositList(allDepositList);
    }

    /**
     * 添加委托缓存
     * Add delegation cache
     *
     * @param chain   chain info
     * @param deposit deposit info
     */
    public boolean addDeposit(Chain chain, Deposit deposit) {
        if (!depositStorageService.save(new DepositPo(deposit), chain.getConfig().getChainId())) {
            chain.getLogger().error("Data save error!");
            return false;
        }
        chain.getDepositList().add(deposit);
        return true;
    }

    /**
     * 修改委托缓存
     * modify delegation cache
     *
     * @param chain   chain
     * @param deposit deposit info
     */
    public boolean updateDeposit(Chain chain, Deposit deposit) {
        if (!depositStorageService.save(new DepositPo(deposit), chain.getChainId())) {
            chain.getLogger().error("Data save error!");
            return false;
        }
        for (Deposit oldDeposit:chain.getDepositList()) {
            if(oldDeposit.getTxHash().equals(deposit.getTxHash())){
                oldDeposit.setDelHeight(deposit.getDelHeight());
                break;
            }
        }
        return true;
    }

    /**
     * 删除指定链的委托信息
     * Delete delegate information for a specified chain
     *
     * @param chain  chain nfo
     * @param txHash 创建该委托交易的Hash/Hash to create the delegated transaction
     */
    public boolean removeDeposit(Chain chain, NulsHash txHash){
        if (!depositStorageService.delete(txHash, chain.getConfig().getChainId())) {
            chain.getLogger().error("Data save error!");
            return false;
        }
        chain.getDepositList().removeIf(s -> s.getTxHash().equals(txHash));
        return true;
    }

    /**
     * 获取指定委托信息
     * Get the specified delegation information
     *
     * @param chain  chain nfo
     * @param txHash 创建该委托交易的Hash/Hash to create the delegated transaction
     */
    public Deposit getDeposit(Chain chain, NulsHash txHash){
        for (Deposit deposit:chain.getDepositList()) {
            if(deposit.getTxHash().equals(txHash)){
                return deposit;
            }
        }
        return null;
    }

    /**
     * 计算委托各账户委托金额并返回总的委托金
     * @param chain        链信息
     * @param height       高度
     * @param depositMap   委托
     * @param totalAmount  总委托金额
     * @param date         按那一天的喂价计算
     * */
    public BigDecimal getDepositByHeight(Chain chain, long height, Map<String, BigDecimal> depositMap, BigDecimal totalAmount, String date) {
        BigDecimal realAmount;
        String address;
        List<DepositPo> depositList;
        try {
            //这儿不能是有缓存中的数据，因为有可能中途有新数据插入
            depositList = depositStorageService.getList(chain.getConfig().getChainId());
        }catch (NulsException e){
            chain.getLogger().error(e);
            return totalAmount;
        }
        if(depositList == null || depositList.isEmpty()){
            return totalAmount;
        }
        for (DepositPo deposit:depositList) {
            //有效委托，委托高度要小指定高度且退出委托高度大于指定高度
            if(deposit.getBlockHeight() <= height && (deposit.getDelHeight() == -1 || deposit.getDelHeight() > height)){
                realAmount = calcDepositBase(chain, deposit, date);
                totalAmount = totalAmount.add(realAmount);
                address = AddressTool.getStringAddressByBytes(deposit.getAddress());
                depositMap.merge(address, totalAmount, (oldValue, value) -> oldValue.add(value));
            }
        }
        return totalAmount;
    }


    /**
     * 计算委托实际对应的NVT
     * @param chain        链信息
     * @param deposit      委托信息
     * @@param date        结算日期
     * */
    private BigDecimal calcDepositBase(Chain chain, DepositPo deposit, String date){
        BigDecimal realDeposit;
        //如果委托资产为本链主资产或为主网主资产则乘以相应的基数
        if(deposit.getAssetChainId() == chain.getChainId() && deposit.getAssetId() == chain.getAssetId()){
            realDeposit = DoubleUtils.mul(new BigDecimal(deposit.getDeposit()), chain.getConfig().getLocalAssertBase());
        } else if(deposit.getAssetChainId() == config.getMainChainId() && deposit.getAssetId() == config.getMainAssetId()){
            realDeposit = ConsensusAwardUtil.getRealAmount(new BigDecimal(deposit.getDeposit()), deposit.getAssetChainId(), deposit.getAssetId(), date);
            realDeposit = DoubleUtils.mul(realDeposit, chain.getConfig().getMainAssertBase());
        }else{
            realDeposit = ConsensusAwardUtil.getRealAmount(new BigDecimal(deposit.getDeposit()), deposit.getAssetChainId(), deposit.getAssetId(), date);
        }
        //如果为定期委托，则根据定期时间乘以相应基数
        if(deposit.getDepositType() == DepositType.REGULAR.getCode()){
            DepositTimeType depositTimeType = DepositTimeType.getValue(deposit.getTimeType());
            if(depositTimeType != null){
                realDeposit = DoubleUtils.mul(realDeposit, depositTimeType.getWeight());
            }
        }
        return realDeposit;
    }
}
