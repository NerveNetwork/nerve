/*
 * Copyright 2018 the bitcoinj-cash developers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.nerve.converter.heterogeneouschain.bchutxo.utils.addr;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bitcoinj.base.exceptions.AddressFormatException;

/**
 * Created by Hash Engineering on 1/19/2018.
 */
public class CashAddressHelper {

    /**
     * The cashaddr character set for encoding.
     */
    final static String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    /**
     * The cashaddr character set for decoding.
     */
    final static byte[] CHARSET_REV = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 15, -1, 10, 17, 21, 20, 26, 30, 7,
            5, -1, -1, -1, -1, -1, -1, -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22,
            31, 27, 19, -1, 1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1,
            -1, -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1, 1, 0,
            3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1};

    /**
     * Concatenate two byte arrays.
     */
    static byte[] concatenateByteArrays(final byte[] x, final byte[] y) {
        byte[] z = new byte[x.length + y.length];
        System.arraycopy(x, 0, z, 0, x.length);
        System.arraycopy(y, 0, z, x.length, y.length);
        return z;
    }

    /**
     * This function will compute what 8 5-bit values to XOR into the last 8 input
     * values, in order to make the checksum 0. These 8 values are packed together
     * in a single 40-bit integer. The higher bits correspond to earlier values.
     */

    static long computePolyMod(final byte[] v) {
        /**
         * The input is interpreted as a list of coefficients of a polynomial over F
         * = GF(32), with an implicit 1 in front. If the input is [v0,v1,v2,v3,v4],
         * that polynomial is v(x) = 1*x^5 + v0*x^4 + v1*x^3 + v2*x^2 + v3*x + v4.
         * The implicit 1 guarantees that [v0,v1,v2,...] has a distinct checksum
         * from [0,v0,v1,v2,...].
         *
         * The output is a 40-bit integer whose 5-bit groups are the coefficients of
         * the remainder of v(x) mod g(x), where g(x) is the cashaddr generator, x^8
         * + {19}*x^7 + {3}*x^6 + {25}*x^5 + {11}*x^4 + {25}*x^3 + {3}*x^2 + {19}*x
         * + {1}. g(x) is chosen in such a way that the resulting code is a BCH
         * code, guaranteeing detection of up to 4 errors within a window of 1025
         * characters. Among the various possible BCH codes, one was selected to in
         * fact guarantee detection of up to 5 errors within a window of 160
         * characters and 6 erros within a window of 126 characters. In addition,
         * the code guarantee the detection of a burst of up to 8 errors.
         *
         * Note that the coefficients are elements of GF(32), here represented as
         * decimal numbers between {}. In this finite field, addition is just XOR of
         * the corresponding numbers. For example, {27} + {13} = {27 ^ 13} = {22}.
         * Multiplication is more complicated, and requires treating the bits of
         * values themselves as coefficients of a polynomial over a smaller field,
         * GF(2), and multiplying those polynomials mod a^5 + a^3 + 1. For example,
         * {5} * {26} = (a^2 + 1) * (a^4 + a^3 + a) = (a^4 + a^3 + a) * a^2 + (a^4 +
         * a^3 + a) = a^6 + a^5 + a^4 + a = a^3 + 1 (mod a^5 + a^3 + 1) = {9}.
         *
         * During the course of the loop below, `c` contains the bitpacked
         * coefficients of the polynomial constructed from just the values of v that
         * were processed so far, mod g(x). In the above example, `c` initially
         * corresponds to 1 mod (x), and after processing 2 inputs of v, it
         * corresponds to x^2 + v0*x + v1 mod g(x). As 1 mod g(x) = 1, that is the
         * starting value for `c`.
         */
        long c = 1;
        for (byte d : v) {
            /**
             * We want to update `c` to correspond to a polynomial with one extra
             * term. If the initial value of `c` consists of the coefficients of
             * c(x) = f(x) mod g(x), we modify it to correspond to
             * c'(x) = (f(x) * x + d) mod g(x), where d is the next input to
             * process.
             *
             * Simplifying:
             * c'(x) = (f(x) * x + d) mod g(x)
             *         ((f(x) mod g(x)) * x + d) mod g(x)
             *         (c(x) * x + d) mod g(x)
             * If c(x) = c0*x^5 + c1*x^4 + c2*x^3 + c3*x^2 + c4*x + c5, we want to
             * compute
             * c'(x) = (c0*x^5 + c1*x^4 + c2*x^3 + c3*x^2 + c4*x + c5) * x + d
             *                                                             mod g(x)
             *       = c0*x^6 + c1*x^5 + c2*x^4 + c3*x^3 + c4*x^2 + c5*x + d
             *                                                             mod g(x)
             *       = c0*(x^6 mod g(x)) + c1*x^5 + c2*x^4 + c3*x^3 + c4*x^2 +
             *                                                             c5*x + d
             * If we call (x^6 mod g(x)) = k(x), this can be written as
             * c'(x) = (c1*x^5 + c2*x^4 + c3*x^3 + c4*x^2 + c5*x + d) + c0*k(x)
             */

            // First, determine the value of c0:
            byte c0 = (byte) (c >> 35);

            // Then compute c1*x^5 + c2*x^4 + c3*x^3 + c4*x^2 + c5*x + d:
            c = ((c & 0x07ffffffffL) << 5) ^ d;

            // Finally, for each set bit n in c0, conditionally add {2^n}k(x):
            if ((c0 & 0x01) != 0) {
                // k(x) = {19}*x^7 + {3}*x^6 + {25}*x^5 + {11}*x^4 + {25}*x^3 +
                //        {3}*x^2 + {19}*x + {1}
                c ^= 0x98f2bc8e61L;
            }

            if ((c0 & 0x02) != 0) {
                // {2}k(x) = {15}*x^7 + {6}*x^6 + {27}*x^5 + {22}*x^4 + {27}*x^3 +
                //           {6}*x^2 + {15}*x + {2}
                c ^= 0x79b76d99e2L;
            }

            if ((c0 & 0x04) != 0) {
                // {4}k(x) = {30}*x^7 + {12}*x^6 + {31}*x^5 + {5}*x^4 + {31}*x^3 +
                //           {12}*x^2 + {30}*x + {4}
                c ^= 0xf33e5fb3c4L;
            }

            if ((c0 & 0x08) != 0) {
                // {8}k(x) = {21}*x^7 + {24}*x^6 + {23}*x^5 + {10}*x^4 + {23}*x^3 +
                //           {24}*x^2 + {21}*x + {8}
                c ^= 0xae2eabe2a8L;
            }

            if ((c0 & 0x10) != 0) {
                // {16}k(x) = {3}*x^7 + {25}*x^6 + {7}*x^5 + {20}*x^4 + {7}*x^3 +
                //            {25}*x^2 + {3}*x + {16}
                c ^= 0x1e4f43e470L;
            }
        }

        /**
         * computePolyMod computes what value to xor into the final values to make the
         * checksum 0. However, if we required that the checksum was 0, it would be
         * the case that appending a 0 to a valid list of values would result in a
         * new valid list. For that reason, cashaddr requires the resulting checksum
         * to be 1 instead.
         */
        return c ^ 1;
    }

    static char toLowerCase(char c) {
        // ASCII black magic.
        return (char) (c | 0x20);
    }

    /**
     * Expand the address prefix for the checksum computation.
     */
    static byte[] expandPrefix(String prefix) {
        byte[] ret = new byte[prefix.length() + 1];

        byte[] prefixBytes = prefix.getBytes();

        for (int i = 0; i < prefix.length(); ++i) {
            ret[i] = (byte) (prefixBytes[i] & 0x1f);
        }

        ret[prefix.length()] = 0;
        return ret;
    }

    static boolean verifyChecksum(String prefix, byte[] payload) {
        return computePolyMod(concatenateByteArrays(expandPrefix(prefix), payload)) == 0;
    }

    static byte[] createChecksum(String prefix, final byte[] payload) {
        byte[] enc = concatenateByteArrays(expandPrefix(prefix), payload);
        // Append 8 zeroes.
        byte[] enc2 = new byte[enc.length + 8];
        System.arraycopy(enc, 0, enc2, 0, enc.length);
        // Determine what to XOR into those 8 zeroes.
        long mod = computePolyMod(enc2);
        byte[] ret = new byte[8];
        for (int i = 0; i < 8; ++i) {
            // Convert the 5-bit groups in mod to checksum values.
            ret[i] = (byte) ((mod >> (5 * (7 - i))) & 0x1f);
        }

        return ret;
    }

    public static String encodeCashAddress(String prefix, byte[] payload) {
        byte[] checksum = createChecksum(prefix, payload);
        byte[] combined = concatenateByteArrays(payload, checksum);
        StringBuilder ret = new StringBuilder(prefix + ':');

        //ret.setLength(ret.length() + combined.length);
        for (byte c : combined) {
            ret.append(CHARSET.charAt(c));
        }

        return ret.toString();
    }

    public static ImmutablePair<String, byte[]> decodeCashAddress(String str, String defaultPrefix) {
        // Go over the string and do some sanity checks.
        boolean lower = false, upper = false, hasNumber = false;
        int prefixSize = 0;
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                lower = true;
                continue;
            }

            if (c >= 'A' && c <= 'Z') {
                upper = true;
                continue;
            }

            if (c == ':') {
                // The separator cannot be the first character, cannot have number
                // and there must not be 2 separators.
                if (hasNumber || i == 0 || prefixSize != 0) {
                    throw new AddressFormatException("cashaddr:  " + str + ": The separator cannot be the first character, cannot have number and there must not be 2 separators");
                }

                prefixSize = i;
                continue;
            }

            // We have an unexpected character.
            throw new AddressFormatException("cashaddr:  " + str + ": Unexpected character at pos " + i);
        }

        // We can't have both upper case and lowercase.
        if (upper && lower) {
            throw new AddressFormatException("cashaddr:  " + str + ": Cannot contain both upper and lower case letters");
        }

        // Get the prefix.
        StringBuilder prefix;
        if (prefixSize == 0) {
            prefix = new StringBuilder(defaultPrefix);
        } else {
            prefix = new StringBuilder(str.substring(0, prefixSize).toLowerCase());

            // Now add the ':' in the size.
            prefixSize++;
        }

        // Decode values.
        final int valuesSize = str.length() - prefixSize;
        byte[] values = new byte[valuesSize];
        for (int i = 0; i < valuesSize; ++i) {
            char c = str.charAt(i + prefixSize);
            // We have an invalid char in there.
            if (c > 127 || CHARSET_REV[c] == -1) {
                throw new AddressFormatException("cashaddr:  " + str + ": Unexpected character at pos " + i);
            }

            values[i] = CHARSET_REV[c];
        }

        // Verify the checksum.
        if (!verifyChecksum(prefix.toString(), values)) {
            throw new AddressFormatException("cashaddr:  " + str + ": Invalid Checksum ");
        }

        byte[] result = new byte[values.length - 8];
        System.arraycopy(values, 0, result, 0, values.length - 8);
        return new ImmutablePair(prefix.toString(), result);
    }

    static public byte[] packAddressData(byte[] payload, byte type) {
        byte version_byte = (byte) (type << 3);
        int size = payload.length;
        byte encoded_size = 0;
        switch (size * 8) {
            case 160:
                encoded_size = 0;
                break;
            case 192:
                encoded_size = 1;
                break;
            case 224:
                encoded_size = 2;
                break;
            case 256:
                encoded_size = 3;
                break;
            case 320:
                encoded_size = 4;
                break;
            case 384:
                encoded_size = 5;
                break;
            case 448:
                encoded_size = 6;
                break;
            case 512:
                encoded_size = 7;
                break;
            default:
                throw new AddressFormatException("Error packing cashaddr: invalid address length");
        }
        version_byte |= encoded_size;
        byte[] data = new byte[1 + payload.length];
        data[0] = version_byte;
        System.arraycopy(payload, 0, data, 1, payload.length);

        // Reserve the number of bytes required for a 5-bit packed version of a
        // hash, with version byte.  Add half a byte(4) so integer math provides
        // the next multiple-of-5 that would fit all the data.

        byte[] converted = new byte[((size + 1) * 8 + 4) / 5];
        ConvertBits(converted, data, 8, 5, true);

        return converted;
    }

    /**
     * Convert from one power-of-2 number base to another.
     * <p>
     * If padding is enabled, this always return true. If not, then it returns true
     * of all the bits of the input are encoded in the output.
     */
    static boolean ConvertBits(byte[] out, byte[] it, int frombits, int tobits, boolean pad) {
        int acc = 0;
        int bits = 0;
        final int maxv = (1 << tobits) - 1;
        final int max_acc = (1 << (frombits + tobits - 1)) - 1;
        int x = 0;
        for (int i = 0; i < it.length; ++i) {
            acc = ((acc << frombits) | (it[i] & 0xff)) & max_acc;
            bits += frombits;
            while (bits >= tobits) {
                bits -= tobits;
                out[x] = (byte) ((acc >> bits) & maxv);
                ++x;
            }
        }

        // We have remaining bits to encode but do not pad.
        if (!pad && bits != 0) {
            return false;
        }

        // We have remaining bits to encode so we do pad.
        if (pad && bits != 0) {
            out[x] = (byte) ((acc << (tobits - bits)) & maxv);
            ++x;
        }

        return true;
    }

    public static String getPrefix(String address) {
        int colon = address.indexOf(':');
        if (colon != -1)
            return address.substring(0, colon);
        return null;
    }
}
