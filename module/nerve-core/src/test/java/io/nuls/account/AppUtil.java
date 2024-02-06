/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
package io.nuls.account;

import io.nuls.core.crypto.AESEncrypt;
import io.nuls.core.crypto.HexUtil;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * @author: PierreLuo
 * @date: 2021/8/9
 */
public class AppUtil {

    public static String encode(String priKey, String priPwd, int encodeCount) {
        String privateKey = Str2hexStr(priKey, "utf8");
        try {
            for (int i = 0; i < encodeCount; i++) {
                privateKey = HexUtil.encode(AESEncrypt.encrypt(HexUtil.decode(privateKey), priPwd));
            }
            return privateKey;
        } catch (Exception e) {
            throw new RuntimeException("Password error");
        }
    }

    public static String decode(byte[] priKey, String priPwd, int encodeCount) {
        try {
            for (int i = 0; i < encodeCount; i++) {
                priKey = AESEncrypt.decrypt(priKey, priPwd);
            }
            return hexStr2Str(HexUtil.encode(priKey), "utf8");
        } catch (Exception e) {
            throw new RuntimeException("Password error");
        }
    }

    /**
     * Convert string to16Hexadecimal Strings
     *
     * @param charsetName Used for encoding String of Charset
     */
    public static String Str2hexStr(String str, String charsetName) {
        byte[] bytes = new byte[0];
        String hexString = "0123456789abcdef";
        // Using the given charset Add this String Encode to byte sequence
        if (!charsetName.equals("")) {
            try {
                bytes = str.getBytes(charsetName);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            // Obtain byte array based on default encoding
            bytes = str.getBytes();
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        // Decompose each byte in the byte array into2position16Base integer
        for (int i = 0; i < bytes.length; i++) {
            sb.append(hexString.charAt((bytes[i] & 0xf0) >> 4));
            sb.append(hexString.charAt((bytes[i] & 0x0f)));
        }
        return sb.toString();
    }

    /**
     * 16Convert base string to string
     *
     * @param charsetName Used for encoding String of Charset
     */
    public static String hexStr2Str(String hexStr, String charsetName) {
        hexStr = hexStr.toUpperCase(Locale.US);
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;

        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xFF);
        }
        String returnStr = "";// The returned string
        if (charsetName == null) {
            // The compiler defaults to decoding the specified byte Array, construct a new one String,
            // For example, my integrated development tool, encoderandroid studioThe default encoding format for is"utf-8"
            returnStr = new String(bytes);
        } else {
            // Designated charset Decode specified byte Array, construct a new one String
            // utf-8Chinese characters occupy three bytes,GB18030compatibleGBKcompatibleGB2312Chinese characters occupy two bytes,ISO8859-1It's a Latin character（ASCIIcharacter）Occupy one byte
            try {
                returnStr = new String(bytes, charsetName);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // charsetalsoutf-8,gbkAs demand changes
        return returnStr;
    }


    public static void main(String[] args) {
        String prikey = "aaa";
        System.out.println(prikey);
        String enkey = encode(prikey, "aaa", 3);
        System.out.println(enkey);
        prikey = decode(HexUtil.decode(enkey), "aaa", 3);
        System.out.println(prikey);
    }
}
