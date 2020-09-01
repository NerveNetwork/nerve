package io.nuls.core.parse;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * @author Niels
 */
public class SerializeUtilsTest   {

    @Test
    public void testBigDecimal2Bytes() throws UnsupportedEncodingException {
        BigDecimal decimal = new BigDecimal("1234567890123456789012345678901234567890.1234567809");
        byte[] bytes = SerializeUtils.bigDecimal2Bytes(decimal);
        BigDecimal decimal1 = SerializeUtils.bytes2BigDecimal(bytes);
        assertEquals(decimal,decimal1);
    }
}