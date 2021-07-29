package network.nerve.swap.tx.v1.helpers.converter;

import io.nuls.core.exception.NulsException;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.NonceBalance;
import network.nerve.swap.model.dto.LedgerAssetDTO;

/**
 * @author Niels
 */
public interface LedgerService {

    boolean existNerveAsset(int chainId, int assetChainId, int assetId) throws NulsException;

    LedgerAssetDTO getNerveAsset(int chainId, int assetChainId, int assetId);

    NonceBalance getBalanceNonce(int chainId, int assetChainId, int assetId, String address) throws NulsException;

    LedgerBalance getLedgerBalance(int chainId, int assetChainId, int assetId, String address) throws NulsException;
}
