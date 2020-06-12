package network.nerve.pocbft.service.impl;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.StackingAsset;
import network.nerve.pocbft.model.bo.tx.txdata.CancelDeposit;
import network.nerve.pocbft.model.bo.tx.txdata.Deposit;
import network.nerve.pocbft.model.dto.input.CreateDepositDTO;
import network.nerve.pocbft.model.dto.input.SearchDepositDTO;
import network.nerve.pocbft.model.dto.input.WithdrawDTO;
import network.nerve.pocbft.model.dto.output.DepositDTO;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.service.DepositService;
import network.nerve.pocbft.utils.enumeration.DepositTimeType;
import network.nerve.pocbft.utils.enumeration.DepositType;
import network.nerve.pocbft.utils.manager.ChainManager;
import network.nerve.pocbft.utils.manager.CoinDataManager;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.core.basic.Page;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ObjectUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;

import java.io.IOException;
import java.math.BigInteger;
import static network.nerve.pocbft.constant.ParameterConstant.*;
import java.util.*;


/**
 * 共识模块RPC接口实现类
 * Consensus Module RPC Interface Implementation Class
 *
 * @author tag
 * 2018/11/7
 */
@Component
public class DepositServiceImpl implements DepositService {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private CoinDataManager coinDataManager;

    /**
     * 委托共识
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result depositToAgent(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        CreateDepositDTO dto = JSONUtils.map2pojo(params, CreateDepositDTO.class);
        try {
            ObjectUtils.canNotEmpty(dto);
            ObjectUtils.canNotEmpty(dto.getAddress());
            ObjectUtils.canNotEmpty(dto.getDeposit());
            ObjectUtils.canNotEmpty(dto.getPassword());
            ObjectUtils.canNotEmpty(dto.getAssetChainId());
            ObjectUtils.canNotEmpty(dto.getAssetId());
            ObjectUtils.canNotEmpty(dto.getDepositType());
            ObjectUtils.canNotEmpty(dto.getTimeType());
        }catch (RuntimeException e){
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            if (!AddressTool.validAddress((short) dto.getChainId(), dto.getAddress())) {
                throw new NulsException(ConsensusErrorCode.ADDRESS_ERROR);
            }
            //账户验证
            HashMap callResult = CallMethodUtils.accountValid(dto.getChainId(), dto.getAddress(), dto.getPassword());

            //验证资产是否可以参与stacking
            if(!chainManager.assetStackingVerify(dto.getAssetChainId(), dto.getAssetId())){
                chain.getLogger().error("The current asset does not support stacking");
                return Result.getFailed(ConsensusErrorCode.ASSET_NOT_SUPPORT_STACKING);
            }

            //如果为存定期则验证定期类型是否存在
            if(dto.getDepositType() == DepositType.REGULAR.getCode()){
                DepositTimeType depositTimeType = DepositTimeType.getValue(dto.getTimeType());
                if(depositTimeType == null){
                    chain.getLogger().error("Recurring delegation type does not exist");
                    return Result.getFailed(ConsensusErrorCode.REGULAR_DEPOSIT_TIME_TYPE_NOT_EXIST);
                }
            }

            Transaction tx = new Transaction(TxType.DEPOSIT);
            Deposit deposit = new Deposit(dto);
            tx.setTxData(deposit.serialize());
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            CoinData coinData = coinDataManager.getCoinData(deposit.getAddress(), chain, new BigInteger(dto.getDeposit()), ConsensusConstant.CONSENSUS_LOCK_TIME, tx.size() + P2PHKSignature.SERIALIZE_LENGTH,dto.getAssetChainId(),dto.getAssetId());
            tx.setCoinData(coinData.serialize());
            //交易签名
            String priKey = (String) callResult.get(PARAM_PRI_KEY);
            CallMethodUtils.transactionSignature(dto.getChainId(), dto.getAddress(), dto.getPassword(), priKey, tx);
            String txStr = RPCUtil.encode(tx.serialize());
            CallMethodUtils.sendTx(chain,txStr);
            Map<String, Object> result = new HashMap<>(ConsensusConstant.INIT_CAPACITY_2);
            result.put(PARAM_TX_HASH, tx.getHash().toHex());
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_PARSE_ERROR);
        }
    }

    /**
     * 退出共识
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result withdraw(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        WithdrawDTO dto = JSONUtils.map2pojo(params, WithdrawDTO.class);
        if (!NulsHash.validHash(dto.getTxHash())) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            if (!AddressTool.validAddress((short) dto.getChainId(), dto.getAddress())) {
                return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
            }
            //账户验证
            HashMap callResult = CallMethodUtils.accountValid(dto.getChainId(), dto.getAddress(), dto.getPassword());
            NulsHash hash = NulsHash.fromHex(dto.getTxHash());
            Transaction depositTransaction = CallMethodUtils.getTransaction(chain,dto.getTxHash());
            if (depositTransaction == null) {
                return Result.getFailed(ConsensusErrorCode.TX_NOT_EXIST);
            }
            CoinData depositCoinData = new CoinData();
            depositCoinData.parse(depositTransaction.getCoinData(), 0);
            Deposit deposit = new Deposit();
            deposit.parse(depositTransaction.getTxData(), 0);
            byte[] address = AddressTool.getAddress(dto.getAddress());
            if(!Arrays.equals(deposit.getAddress(), address)){
                chain.getLogger().error("The account is not the creator of the entrusted transaction");
                return Result.getFailed(ConsensusErrorCode.ACCOUNT_IS_NOT_CREATOR);
            }
            boolean flag = false;
            for (CoinTo to : depositCoinData.getTo()) {
                if (to.getLockTime() == -1L && to.getAmount().compareTo(deposit.getDeposit()) == 0) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                return Result.getFailed(ConsensusErrorCode.DATA_ERROR);
            }

            //如果为定期委托则验证委托是否到期
            long time = NulsDateUtils.getCurrentTimeSeconds();
            if(deposit.getDepositType() == DepositType.REGULAR.getCode()){
                DepositTimeType depositTimeType = DepositTimeType.getValue(deposit.getTimeType());
                if(depositTimeType == null){
                    chain.getLogger().error("Recurring delegation type does not exist");
                    return Result.getFailed(ConsensusErrorCode.DATA_ERROR);
                }
                long periodicTime = depositTransaction.getTime() + depositTimeType.getTime();
                if(time < periodicTime){
                    chain.getLogger().error("Term commission not due");
                    return Result.getFailed(ConsensusErrorCode.DEPOSIT_NOT_DUE);
                }
            }

            Transaction cancelDepositTransaction = new Transaction(TxType.CANCEL_DEPOSIT);
            CancelDeposit cancelDeposit = new CancelDeposit();
            cancelDeposit.setAddress(address);
            cancelDeposit.setJoinTxHash(hash);
            cancelDepositTransaction.setTime(time);
            cancelDepositTransaction.setTxData(cancelDeposit.serialize());
            CoinData coinData = coinDataManager.getWithdrawCoinData(cancelDeposit.getAddress(), chain, deposit.getDeposit(), 0, cancelDepositTransaction.size() + P2PHKSignature.SERIALIZE_LENGTH, deposit.getAssetChainId(),deposit.getAssetId());
            coinData.getFrom().get(0).setNonce(CallMethodUtils.getNonce(hash.getBytes()));
            cancelDepositTransaction.setCoinData(coinData.serialize());

            //交易签名
            String priKey = (String) callResult.get(PARAM_PRI_KEY);
            CallMethodUtils.transactionSignature(dto.getChainId(), dto.getAddress(), dto.getPassword(), priKey, cancelDepositTransaction);
            String txStr = RPCUtil.encode(cancelDepositTransaction.serialize());
            CallMethodUtils.sendTx(chain,txStr);
            Map<String, Object> result = new HashMap<>(ConsensusConstant.INIT_CAPACITY_2);
            result.put(PARAM_TX_HASH, cancelDepositTransaction.getHash().toHex());
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_PARSE_ERROR);
        }
    }

    /**
     * 获取委托列表信息
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result getDepositList(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        SearchDepositDTO dto = JSONUtils.map2pojo(params, SearchDepositDTO.class);
        int pageNumber = dto.getPageNumber();
        int pageSize = dto.getPageSize();
        int chainId = dto.getChainId();
        if (pageNumber == MIN_VALUE) {
            pageNumber = PAGE_NUMBER_INIT_VALUE;
        }
        if (pageSize == MIN_VALUE) {
            pageSize = PAGE_SIZE_INIT_VALUE;
        }
        if (pageNumber < MIN_VALUE || pageSize < MIN_VALUE || pageSize > PAGE_SIZE_MAX_VALUE || chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        String address = dto.getAddress();
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        List<Deposit> depositList = chain.getDepositList();
        List<Deposit> handleList = new ArrayList<>();
        long startBlockHeight = chain.getNewestHeader().getHeight();
        byte[] addressBytes = null;
        if (StringUtils.isNotBlank(address)) {
            addressBytes = AddressTool.getAddress(address);
        }
        for (Deposit deposit : depositList) {
            if (deposit.getDelHeight() != -1L && deposit.getDelHeight() <= startBlockHeight) {
                continue;
            }
            if (deposit.getBlockHeight() > startBlockHeight || deposit.getBlockHeight() < 0L) {
                continue;
            }
            if (addressBytes != null && !Arrays.equals(deposit.getAddress(), addressBytes)) {
                continue;
            }
            handleList.add(deposit);
        }
        int start = pageNumber * pageSize - pageSize;
        int handleSize = handleList.size();
        Page<DepositDTO> page = new Page<>(pageNumber, pageSize, handleSize);
        if (start >= handleSize) {
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(page);
        }
        List<DepositDTO> resultList = new ArrayList<>();
        for (int i = start; i < handleSize && i < (start + pageSize); i++) {
            Deposit deposit = handleList.get(i);
            resultList.add(new DepositDTO(deposit));
        }
        page.setList(resultList);
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(page);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getAssetBySymbol(Map<String, Object> params) {
        if (params == null || params.get(PARAM_SYMBOL) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        String symbol = (String)params.get(PARAM_SYMBOL);
        if(symbol == null || symbol.isEmpty()){
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        StackingAsset stackingAsset = chainManager.getAssetBySymbol(symbol);
        if(stackingAsset == null){
            return Result.getFailed(ConsensusErrorCode.ASSET_NOT_SUPPORT_STACKING);
        }
        Map<String, Object> result = new HashMap<>(ConsensusConstant.INIT_CAPACITY_2);
        result.put(PARAM_ASSET_CHAIN_ID, stackingAsset.getChainId());
        result.put(PARAM_ASSET_ID, stackingAsset.getAssetId());
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getCanStackingAssetList(Map<String, Object> params) {
        if (params == null || params.get(PARAM_CHAIN_ID) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        Map<String, Object> resultMap = new HashMap<>(4);
        resultMap.put(PARAM_LIST, chainManager.getStackingAssetList());
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(resultMap);
    }
}
