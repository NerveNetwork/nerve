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


import org.bitcoinj.base.exceptions.AddressFormatException;

public class CashAddressValidator {

    public static CashAddressValidator create() {
        return new CashAddressValidator();
    }

    public void checkValidPrefix(NetworkParameters params, String prefix) throws AddressFormatException {
        if (!prefix.equals(params.getCashAddrPrefix())) {
            throw new AddressFormatException("Invalid prefix for network: " +
                    prefix + " != " + params.getCashAddrPrefix() + " (expected)");
        }
    }

    public void checkNonEmptyPayload(byte[] payload) throws AddressFormatException {
        if (payload.length == 0) {
            throw new AddressFormatException("No payload");
        }
    }

    public void checkAllowedPadding(byte extraBits) throws AddressFormatException {
        if (extraBits >= 5) {
            throw new AddressFormatException("More than allowed padding");
        }
    }

    public void checkNonZeroPadding(byte last, byte mask) {
        if ((last & mask) != 0) {
            throw new AddressFormatException("Nonzero bytes ");
        }
    }

    public void checkFirstBitIsZero(byte versionByte) {
        if ((versionByte & 0x80) != 0) {
            throw new AddressFormatException("First bit is reserved");
        }
    }

    public void checkDataLength(byte[] data, int hashSize) {
        if (data.length != hashSize + 1) {
            throw new AddressFormatException("Data length " + data.length + " != hash size " + hashSize);
        }
    }

}
