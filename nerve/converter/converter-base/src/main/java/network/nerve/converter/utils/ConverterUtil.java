/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.converter.utils;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.basic.Result;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterErrorCode;

import java.util.Comparator;
import java.util.Map;

import static io.nuls.core.constant.CommonCodeConstanst.FAILED;
import static network.nerve.converter.utils.LoggerUtil.LOG;

/**
 * @author: Mimi
 * @date: 2020-02-18
 */
public class ConverterUtil {

    private static final String HEX_REGEX="^[A-Fa-f0-9]+$";

    public static boolean isHexStr(String str) {
        if(StringUtils.isBlank(str)) {
            return false;
        }
        return str.matches(HEX_REGEX);
    }

    public static String addressToLowerCase(String address) {
        if (StringUtils.isBlank(address)) {
            return address;
        }
        String validAddress = cleanHexPrefix(address);
        if (isHexStr(validAddress)) {
            address = address.toLowerCase();
        }
        return address;
    }

    public static String cleanHexPrefix(String input) {
        return containsHexPrefix(input) ? input.substring(2) : input;
    }

    public static boolean containsHexPrefix(String input) {
        return !StringUtils.isBlank(input) && input.length() > 1 && input.charAt(0) == '0' && input.charAt(1) == 'x';
    }

    public static Result getSuccess() {
        return Result.getSuccess(ConverterErrorCode.SUCCESS);
    }

    public static Result getFailed() {
        return Result.getFailed(FAILED);
    }

    public static boolean isTimeOutError(String error) {
        if (StringUtils.isBlank(error)) {
            return false;
        }
        return error.contains("timeout") || error.contains("timed out");
    }

    public static boolean isDuplicateSignature(String error) {
        if (StringUtils.isBlank(error)) {
            return false;
        }
        return error.contains("Duplicate signature");
    }

    public static boolean isCompletedTransaction(String error) {
        if (StringUtils.isBlank(error)) {
            return false;
        }
        return error.contains("Transaction has been completed");
    }

    public static boolean isRequestExceeded(String error) {
        if (StringUtils.isBlank(error)) {
            return false;
        }
        return error.contains("exceeded");
    }

    public static boolean isRequestDenied(String error) {
        if (StringUtils.isBlank(error)) {
            return false;
        }
        return error.contains("Invalid response received: 601") || error.contains("nerve denied");
    }

    public static boolean isRequestExpired(String error) {
        if (StringUtils.isBlank(error)) {
            return false;
        }
        return error.contains("Invalid response received: 602") || error.contains("nerve expired");
    }

    public static boolean isInsufficientSignature(NulsException e) {
        if (e == null || StringUtils.isBlank(e.getCustomMessage())) {
            return false;
        }
        return e.getCustomMessage().contains("Valid signatures fail");
    }

    public static <T> T getInstance(byte[] bytes, Class<? extends BaseNulsData> clazz) throws NulsException {
        if (null == bytes || bytes.length == 0) {
            throw new NulsException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        try {
            BaseNulsData baseNulsData = clazz.getDeclaredConstructor().newInstance();
            baseNulsData.parse(new NulsByteBuffer(bytes));
            return (T) baseNulsData;
        } catch (NulsException e) {
            LOG.error(e);
            throw new NulsException(ConverterErrorCode.DESERIALIZE_ERROR);
        } catch (Exception e) {
            LOG.error(e);
            throw new NulsException(ConverterErrorCode.DESERIALIZE_ERROR);
        }
    }

    /**
     * RPCUtil 反序列化
     *
     * @param data
     * @param clazz
     * @param <T>
     * @return
     * @throws NulsException
     */
    public static <T> T getInstanceRpcStr(String data, Class<? extends BaseNulsData> clazz) throws NulsException {
        return getInstance(RPCUtil.decode(data), clazz);
    }

    /**
     * HEX反序列化
     *
     * @param hex
     * @param clazz
     * @param <T>
     * @return
     * @throws NulsException
     */
    public static <T> T getInstance(String hex, Class<? extends BaseNulsData> clazz) throws NulsException {
        return getInstance(HexUtil.decode(hex), clazz);
    }

    public static Comparator CHANGE_SORT = new Comparator<Map.Entry<String, Integer>>() {
        @Override
        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            if(o1.getValue() > o2.getValue()) {
                return 1;
            } else if(o1.getValue() < o2.getValue()) {
                return -1;
            }
            return 0;
        }
    };
}
