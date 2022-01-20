package network.nerve.swap.storage;

import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.po.stable.StableSwapPairPo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Niels
 */
public interface SwapStablePairStorageService {

    boolean savePair(byte[] address, StableSwapPairPo po) throws Exception;

    StableSwapPairPo getPair(byte[] address);

    StableSwapPairPo getPair(String address);

    default Collection<String> findAllPairs(int chainId) {
        return Collections.EMPTY_LIST;
    };

    boolean delelePair(String address) throws Exception;

    String getPairAddressByTokenLP(int chainId, NerveToken tokenLP);


    default boolean savePairForSwapTrade(String address) throws Exception {
        return false;
    };

    default boolean existPairForSwapTrade(String address) {
        return false;
    };

    default void initialDonePairForSwapTrade(int chainId) throws Exception {
    };

    default boolean hadInitialDonePairForSwapTrade(int chainId) {
        return false;
    };

    default boolean delelePairForSwapTrade(String address) throws Exception {
        return false;
    };

    default List<String> findAllForSwapTrade(int chainId) throws Exception {
        return Collections.EMPTY_LIST;
    };
}
