package network.nerve.pocbft.service.impl;

import io.nuls.core.model.DoubleUtils;
import io.nuls.economic.nuls.constant.NulsEconomicConstant;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Niels
 */
public class ChainServiceImplTest extends TestCase {

    public void test() {
        long height = 4320010;
        long initEndHeight = 4320001;

        long differentCount = (height - initEndHeight) / 4320000;
        if ((height - initEndHeight) % 4320000 != 0) {
            differentCount++;
        }
        double ratio = 1 - 0.0082;
        BigInteger inflationAmount = DoubleUtils.mul(new BigDecimal(864000000000000L), BigDecimal.valueOf(Math.pow(ratio, differentCount))).toBigInteger();
        BigInteger award = DoubleUtils.div(new BigDecimal(inflationAmount), 4320000).toBigInteger();
        System.out.println(award.toString());
    }
}