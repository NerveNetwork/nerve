package network.nerve.swap.cache;

import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;

/**
 * @author Niels
 */
public interface StableSwapPairCache {

    StableSwapPairDTO get(String address);

    StableSwapPairDTO put(String address, StableSwapPairDTO dto);

    StableSwapPairDTO remove(String address);

    boolean isExist(String pairAddress);

    String getPairAddressByTokenLP(int chainId, NerveToken tokenLP);
}
