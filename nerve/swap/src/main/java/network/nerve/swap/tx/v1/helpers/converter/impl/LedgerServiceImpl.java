package network.nerve.swap.tx.v1.helpers.converter.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.NonceBalance;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.rpc.call.LedgerCall;
import network.nerve.swap.tx.v1.helpers.converter.LedgerService;

/**
 * @author Niels
 */
@Component
public class LedgerServiceImpl implements LedgerService {
    @Override
    public boolean existNerveAsset(int chainId, int assetChainId, int assetId) throws NulsException {
        return LedgerCall.existNerveAsset(chainId, assetChainId, assetId);
    }

    @Override
    public LedgerAssetDTO getNerveAsset(int chainId, int assetChainId, int assetId) {
        return LedgerCall.getNerveAsset(chainId, assetChainId, assetId);
    }

    @Override
    public NonceBalance getBalanceNonce(int chainId, int assetChainId, int assetId, String address) throws NulsException {
        return LedgerCall.getBalanceNonce(chainId, assetChainId, assetId, address);
    }

    @Override
    public LedgerBalance getLedgerBalance(int chainId, int assetChainId, int assetId, String address) throws NulsException {
        NonceBalance balanceNonce = LedgerCall.getBalanceNonce(chainId, assetChainId, assetId, address);
        LedgerBalance balance = LedgerBalance.newInstance();
        balance.setBalance(balanceNonce.getAvailable());
        balance.setNonce(balanceNonce.getNonce());
        balance.setFreeze(balanceNonce.getFreeze());
        balance.setAddress(AddressTool.getAddress(address));
        balance.setAssetsChainId(assetChainId);
        balance.setAssetsId(assetId);
        return balance;
    }
}
