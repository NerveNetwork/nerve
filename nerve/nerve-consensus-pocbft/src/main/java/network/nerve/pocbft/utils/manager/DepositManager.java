package network.nerve.pocbft.utils.manager;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.StackingAsset;
import network.nerve.pocbft.model.bo.config.ConsensusChainConfig;
import network.nerve.pocbft.model.bo.tx.txdata.Deposit;
import network.nerve.pocbft.model.po.DepositPo;
import network.nerve.pocbft.utils.ConsensusAwardUtil;
import network.nerve.pocbft.utils.compare.DepositComparator;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.storage.DepositStorageService;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.pocbft.utils.enumeration.DepositTimeType;
import network.nerve.pocbft.utils.enumeration.DepositType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 委托信息管理类，负责委托信息相关处理
 * Delegated information management category, responsible for delegated information related processing
 *
 * @author tag
 * 2018/12/5
 */
@Component
public class DepositManager {
    @Autowired
    private DepositStorageService depositStorageService;

    @Autowired
    private ConsensusChainConfig config;

    @Autowired
    private static ChainManager chainManager;

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
     * @param endHeight       高度
     * @param depositMap   委托
     * @param totalAmount  总委托金额
     * @param date         按那一天的喂价计算
     * */
    public BigDecimal getDepositByHeight(Chain chain, long startHeight,long endHeight, Map<String, BigDecimal> depositMap, BigDecimal totalAmount, String date) throws NulsException{
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
            if(deposit.getBlockHeight() > endHeight){
                continue;
            }
            if(deposit.getDelHeight() != -1 && deposit.getDelHeight() <= endHeight){
                continue;
            }
            if (endHeight > chain.getConfig().getDepositAwardChangeHeight() && deposit.getBlockHeight() > startHeight) {
                continue;
            }
            StringBuilder ss = new StringBuilder();
            ss.append(AddressTool.getStringAddressByBytes(deposit.getAddress()));
            ss.append("-");
            ss.append(deposit.getAssetChainId());
            ss.append("-");
            ss.append(deposit.getAssetId());
            ss.append("-");
            ss.append(deposit.getDeposit().toString());
            realAmount = calcDepositBase(chain, deposit, date);
            ss.append("-real:");
            ss.append(realAmount.toString());
            totalAmount = totalAmount.add(realAmount);

            ss.append("-total:");
            ss.append(totalAmount.toString());

            address = AddressTool.getStringAddressByBytes(deposit.getAddress());
            chain.getLogger().info(ss.toString());
            depositMap.merge(address, realAmount, (oldValue, value) -> oldValue.add(value));
        }
        return totalAmount;
    }


    /**
     * 计算委托实际对应的NVT
     * @param chain        链信息
     * @param deposit      委托信息
     * @@param date        结算日期
     * */
    private BigDecimal calcDepositBase(Chain chain, DepositPo deposit, String date) throws NulsException{
        BigDecimal realDeposit = new BigDecimal(deposit.getDeposit());
        double weightSqrt = 1;
        //如果委托资产为本链主资产或为主网主资产则乘以相应的基数
        if(deposit.getAssetChainId() == chain.getChainId() && deposit.getAssetId() == chain.getAssetId()){
            weightSqrt = chain.getConfig().getLocalAssertBase();
        } else if(deposit.getAssetChainId() == config.getMainChainId() && deposit.getAssetId() == config.getMainAssetId()){
            StackingAsset stackingAsset = chainManager.getAssetByAsset(deposit.getAssetChainId(),deposit.getAssetId());
            realDeposit = ConsensusAwardUtil.getRealAmount(chain.getChainId(),realDeposit, stackingAsset, date);
            weightSqrt = chain.getConfig().getMainAssertBase();
        }else{
            StackingAsset stackingAsset = chainManager.getAssetByAsset(deposit.getAssetChainId(),deposit.getAssetId());
            realDeposit = ConsensusAwardUtil.getRealAmount(chain.getChainId(),realDeposit, stackingAsset, date);
        }
        //如果为定期委托，则根据定期时间乘以相应基数,定期委托到期之后按活期计算权重
        if(deposit.getDepositType() == DepositType.REGULAR.getCode()){
            DepositTimeType depositTimeType = DepositTimeType.getValue(deposit.getTimeType());
            if(depositTimeType != null && deposit.getTime() + depositTimeType.getTime() >= NulsDateUtils.getCurrentTimeSeconds()){
                weightSqrt = weightSqrt * depositTimeType.getWeight();
            }
        }
        realDeposit = DoubleUtils.mul(realDeposit, new BigDecimal(Math.sqrt(weightSqrt)).setScale(4, BigDecimal.ROUND_HALF_UP));
        return realDeposit;
    }
}
