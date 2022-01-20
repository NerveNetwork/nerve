package network.nerve.swap.cache.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.model.po.stable.StableSwapPairBalancesPo;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.storage.SwapStablePairBalancesStorageService;
import network.nerve.swap.storage.SwapStablePairStorageService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
@Component
public class StableSwapPairCacheImpl implements StableSwapPairCache {

    @Autowired
    private SwapStablePairStorageService swapStablePairStorageService;
    @Autowired
    private SwapStablePairBalancesStorageService swapStablePairBalancesStorageService;

    //不同的链地址不会相同，所以不再区分链
    private Map<String, StableSwapPairDTO> CACHE_MAP = new HashMap<>();
    private Map<String, String> LP_CACHE_MAP = new HashMap<>();

    @Override
    public StableSwapPairDTO get(String address) {
        StableSwapPairDTO dto = CACHE_MAP.get(address);
        if (dto == null) {
            StableSwapPairPo pairPo = swapStablePairStorageService.getPair(address);
            if (pairPo == null) {
                return null;
            }
            dto = new StableSwapPairDTO();
            dto.setPo(pairPo);
            StableSwapPairBalancesPo pairBalances = swapStablePairBalancesStorageService.getPairBalances(address);
            if (pairBalances == null) {
                pairBalances = new StableSwapPairBalancesPo(AddressTool.getAddress(address), pairPo.getCoins().length);
            }
            dto.setBalances(pairBalances.getBalances());
            dto.setTotalLP(pairBalances.getTotalLP());
            dto.setBlockTimeLast(pairBalances.getBlockTimeLast());
            dto.setBlockHeightLast(pairBalances.getBlockHeightLast());
            CACHE_MAP.put(address, dto);
        }
        return dto;
    }

    @Override
    public StableSwapPairDTO put(String address, StableSwapPairDTO dto) {
        return CACHE_MAP.put(address, dto);
    }

    @Override
    public StableSwapPairDTO reload(String address) {
        CACHE_MAP.remove(address);
        StableSwapPairDTO dto = this.get(address);
        return dto;
    }

    @Override
    public StableSwapPairDTO remove(String address) {
        StableSwapPairDTO remove = CACHE_MAP.remove(address);
        if (remove != null) {
            LP_CACHE_MAP.remove(remove.getPo().getTokenLP().str());
        }
        return remove;
    }

    @Override
    public Collection<StableSwapPairDTO> getList() {
        return CACHE_MAP.values();
    }

    @Override
    public boolean isExist(String pairAddress) {
        return this.get(pairAddress) != null;
    }

    @Override
    public String getPairAddressByTokenLP(int chainId, NerveToken tokenLP) {
        String tokenLPStr = tokenLP.str();
        String pairAddress = LP_CACHE_MAP.get(tokenLPStr);
        if (StringUtils.isBlank(pairAddress)) {
            pairAddress = swapStablePairStorageService.getPairAddressByTokenLP(chainId, tokenLP);
            if (StringUtils.isBlank(pairAddress)) {
                return null;
            }
            LP_CACHE_MAP.put(tokenLPStr, pairAddress);
        }
        return pairAddress;
    }
}
