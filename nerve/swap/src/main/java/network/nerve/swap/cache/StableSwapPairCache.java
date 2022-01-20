package network.nerve.swap.cache;

import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;

import java.util.Collection;

/**
 * @author Niels
 */
public interface StableSwapPairCache {

    StableSwapPairDTO get(String address);

    StableSwapPairDTO put(String address, StableSwapPairDTO dto);

    StableSwapPairDTO reload(String address);

    StableSwapPairDTO remove(String address);

    Collection<StableSwapPairDTO> getList();

    boolean isExist(String pairAddress);

    String getPairAddressByTokenLP(int chainId, NerveToken tokenLP);
}
