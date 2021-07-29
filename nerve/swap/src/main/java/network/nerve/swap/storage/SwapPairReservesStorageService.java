package network.nerve.swap.storage;

import network.nerve.swap.model.po.SwapPairReservesPO;

/**
 * @author Niels
 */
public interface SwapPairReservesStorageService {

    boolean savePairReserves(String address, SwapPairReservesPO dto) throws Exception;

    SwapPairReservesPO getPairReserves(String address);

    boolean delelePairReserves(String address) throws Exception;

}
