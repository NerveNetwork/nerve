package network.nerve.swap.cache.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.po.SwapPairReservesPO;
import network.nerve.swap.storage.SwapPairReservesStorageService;
import network.nerve.swap.storage.SwapPairStorageService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
@Component
public class SwapPairCacheImpl implements SwapPairCache {

    @Autowired
    private SwapPairStorageService swapPairStorageService;
    @Autowired
    private SwapPairReservesStorageService swapPairReservesStorageService;
    @Autowired
    private ChainManager chainManager;

    //Different chain addresses will not be the same, so chains will no longer be distinguished
    private Map<String, SwapPairDTO> CACHE_MAP = new HashMap<>();
    private Map<String, String> LP_CACHE_MAP = new HashMap<>();

    @Override
    public SwapPairDTO get(String address) {
        SwapPairDTO dto = CACHE_MAP.get(address);
        if (dto == null) {
            SwapPairPO pairPO = swapPairStorageService.getPair(address);
            if (pairPO == null) {
                return null;
            }
            dto = new SwapPairDTO();
            dto.setPo(pairPO);
            SwapPairReservesPO reservesPO = swapPairReservesStorageService.getPairReserves(address);
            if (reservesPO == null) {
                reservesPO = new SwapPairReservesPO(AddressTool.getAddress(address));
            }
            dto.setReserve0(reservesPO.getReserve0());
            dto.setReserve1(reservesPO.getReserve1());
            dto.setTotalLP(reservesPO.getTotalLP());
            dto.setBlockTimeLast(reservesPO.getBlockTimeLast());
            dto.setBlockHeightLast(reservesPO.getBlockHeightLast());
            CACHE_MAP.put(address, dto);
        }
        return dto;
    }

    @Override
    public SwapPairDTO put(String address, SwapPairDTO dto) {
        return CACHE_MAP.put(address, dto);
    }

    @Override
    public SwapPairDTO reload(String address) {
        CACHE_MAP.remove(address);
        SwapPairDTO dto = this.get(address);
        return dto;
    }

    @Override
    public SwapPairDTO remove(String address) {
        SwapPairDTO remove = CACHE_MAP.remove(address);
        if (remove != null) {
            LP_CACHE_MAP.remove(remove.getPo().getTokenLP().str());
        }
        return remove;
    }

    @Override
    public Collection<SwapPairDTO> getList() {
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
            pairAddress = swapPairStorageService.getPairAddressByTokenLP(chainId, tokenLP);
            if (StringUtils.isBlank(pairAddress)) {
                return null;
            }
            LP_CACHE_MAP.put(tokenLPStr, pairAddress);
        }
        return pairAddress;
    }
}
