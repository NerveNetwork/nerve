package network.nerve.pocbft.service;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.StackingAsset;
import network.nerve.pocbft.model.bo.tx.txdata.Deposit;

import java.math.BigInteger;

/**
 * @author Niels
 */
public interface StakingLimitService {

    boolean validate(Chain chain, StackingAsset stackingAsset, BigInteger deposit);

    boolean add(Chain chain, StackingAsset stackingAsset, BigInteger deposit);

    boolean sub(Chain chain, StackingAsset stackingAsset, BigInteger deposit);
}
