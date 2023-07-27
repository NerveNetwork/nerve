package io.nuls.consensus.service;

import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.StackingAsset;

import java.math.BigInteger;

/**
 * @author Niels
 */
public interface StakingLimitService {

    boolean validate(Chain chain, StackingAsset stackingAsset, BigInteger deposit);

    boolean add(Chain chain, StackingAsset stackingAsset, BigInteger deposit);

    boolean sub(Chain chain, StackingAsset stackingAsset, BigInteger deposit);
}
