package network.nerve.swap.storage;

import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.po.SwapPairPO;

/**
 * @author Niels
 */
public interface SwapPairStorageService {

    boolean savePair(byte[] address, SwapPairPO po) throws Exception;

    boolean savePair(String address, SwapPairPO po) throws Exception;

    SwapPairPO getPair(byte[] address);

    SwapPairPO getPair(String address);

    String getPairAddressByTokenLP(int chainId, NerveToken tokenLP);

    boolean delelePair(byte[] address) throws Exception;

    boolean delelePair(String address) throws Exception;

}
