package network.nerve.swap.storage;

import network.nerve.swap.model.po.stable.StableSwapPairBalancesPo;

/**
 * @author Niels
 */
public interface SwapStablePairBalancesStorageService {

    boolean savePairBalances(String address, StableSwapPairBalancesPo dto) throws Exception;

    StableSwapPairBalancesPo getPairBalances(String address);

    boolean delelePairBalances(String address) throws Exception;

}
