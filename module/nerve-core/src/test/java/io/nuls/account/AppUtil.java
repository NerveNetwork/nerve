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
            throw new RuntimeException("密码错误");
        }
    }

    public static String decode(byte[] priKey, String priPwd, int encodeCount) {
        try {
            for (int i = 0; i < encodeCount; i++) {
                priKey = AESEncrypt.decrypt(priKey, priPwd);
            }
            return hexStr2Str(HexUtil.encode(priKey), "utf8");
        } catch (Exception e) {
            throw new RuntimeException("密码错误");
        }
    }

    /**
     * 字符串转换为16进制字符串
     *
     * @param charsetName 用于编码 String 的 Charset
     */
    public static String Str2hexStr(String str, String charsetName) {
        byte[] bytes = new byte[0];
        String hexString = "0123456789abcdef";
        // 使用给定的 charset 将此 String 编码到 byte 序列
        if (!charsetName.equals("")) {
            try {
                bytes = str.getBytes(charsetName);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            // 根据默认编码获取字节数组
            bytes = str.getBytes();
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        // 将字节数组中每个字节拆解成2位16进制整数
        for (int i = 0; i < bytes.length; i++) {
            sb.append(hexString.charAt((bytes[i] & 0xf0) >> 4));
            sb.append(hexString.charAt((bytes[i] & 0x0f)));
        }
        return sb.toString();
    }

    /**
     * 16进制字符串转换为字符串
     *
     * @param charsetName 用于编码 String 的 Charset
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
        String returnStr = "";// 返回的字符串
        if (charsetName == null) {
            // 编译器默认解码指定的 byte 数组，构造一个新的 String,
            // 比如我的集成开发工具即编码器android studio的默认编码格式为"utf-8"
            returnStr = new String(bytes);
        } else {
            // 指定的 charset 解码指定的 byte 数组，构造一个新的 String
            // utf-8中文字符占三个字节，GB18030兼容GBK兼容GB2312中文字符占两个字节，ISO8859-1是拉丁字符（ASCII字符）占一个字节
            try {
                returnStr = new String(bytes, charsetName);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // charset还有utf-8,gbk随需求改变
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
