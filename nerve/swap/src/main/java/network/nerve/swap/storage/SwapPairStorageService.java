package network.nerve.swap.storage;

import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.po.SwapPairPO;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Niels
 */
public interface SwapPairStorageService {

    boolean savePair(byte[] address, SwapPairPO po) throws Exception;

    boolean savePair(String address, SwapPairPO po) throws Exception;

    SwapPairPO getPair(byte[] address);

    SwapPairPO getPair(String address);

    default Collection<String> findAllPairs(int chainId) {
        return Collections.EMPTY_LIST;
    };

    String getPairAddressByTokenLP(int chainId, NerveToken tokenLP);

    boolean delelePair(byte[] address) throws Exception;

    boolean delelePair(String address) throws Exception;

}
