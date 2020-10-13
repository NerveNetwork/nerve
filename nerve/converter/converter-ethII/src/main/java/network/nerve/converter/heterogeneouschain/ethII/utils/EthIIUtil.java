/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
package network.nerve.converter.heterogeneouschain.ethII.utils;

import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.ethII.constant.EthIIConstant;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static network.nerve.converter.heterogeneouschain.eth.utils.EthUtil.leftPadding;

/**
 * @author: Mimi
 * @date: 2020-08-31
 */
public class EthIIUtil {

    public static Function getCreateOrSignWithdrawFunction(String nerveTxHash, String toAddress, BigInteger value, boolean isContractAsset, String contractAddressERC20, String signatureHexData) {
        return new Function(
                EthIIConstant.METHOD_CREATE_OR_SIGN_WITHDRAW,
                List.of(new Utf8String(nerveTxHash),
                        new Address(toAddress),
                        new Uint256(value),
                        new Bool(isContractAsset),
                        new Address(contractAddressERC20),
                        new DynamicBytes(Numeric.hexStringToByteArray(signatureHexData))),
                List.of(new TypeReference<Type>() {
                })
        );
    }

    public static Function getCreateOrSignManagerChangeFunction(String nerveTxHash, List<Address> addList, List<Address> removeList, int orginTxCount, String signatureHexData) {
        return new Function(
                EthIIConstant.METHOD_CREATE_OR_SIGN_MANAGERCHANGE,
                List.of(new Utf8String(nerveTxHash),
                        new DynamicArray(Address.class, addList),
                        new DynamicArray(Address.class, removeList),
                        new Uint8(orginTxCount),
                        new DynamicBytes(Numeric.hexStringToByteArray(signatureHexData))),
                List.of(new TypeReference<Type>() {
                })
        );
    }

    public static Function getCreateOrSignUpgradeFunction(String nerveTxHash, String upgradeContract, String signatureHexData) {
        return new Function(
                EthIIConstant.METHOD_CREATE_OR_SIGN_UPGRADE,
                List.of(new Utf8String(nerveTxHash),
                        new Address(upgradeContract),
                        new DynamicBytes(Numeric.hexStringToByteArray(signatureHexData))),
                List.of(new TypeReference<Type>() {
                })
        );
    }

    public static Function getCrossOutFunction(String to, BigInteger value, String erc20) {
        return new Function(
                EthIIConstant.METHOD_CROSS_OUT,
                List.of(new Utf8String(to),
                        new Uint256(value),
                        new Address(erc20)),
                List.of(new TypeReference<Type>() {
                })
        );
    }

    public static Function getIsMinterERC20Function(String erc20) {
        return new Function(
                EthIIConstant.METHOD_VIEW_IS_MINTER_ERC20,
                List.of(new Address(erc20)),
                List.of(new TypeReference<Bool>() {
                })
        );
    }

    public static String encoderWithdraw(String txKey, String toAddress, BigInteger value, Boolean isContractAsset, String erc20, byte version) {
        StringBuilder sb = new StringBuilder();
        sb.append(Numeric.toHexString(txKey.getBytes(StandardCharsets.UTF_8)));
        sb.append(Numeric.cleanHexPrefix(toAddress));
        sb.append(leftPadding(value.toString(16), "0", 64));
        sb.append(isContractAsset ? "01" : "00");
        sb.append(Numeric.cleanHexPrefix(erc20));
        sb.append(String.format("%02x", version & 255));
        byte[] hash = Hash.sha3(Numeric.hexStringToByteArray(sb.toString()));
        return Numeric.toHexString(hash);
    }

    public static String encoderChange(String txKey, String[] adds, int count, String[] removes, byte version) {
        StringBuilder sb = new StringBuilder();
        sb.append(Numeric.toHexString(txKey.getBytes(StandardCharsets.UTF_8)));
        for (String add : adds) {
            sb.append(leftPadding(Numeric.cleanHexPrefix(add), "0", 64));
        }
        sb.append(leftPadding(Integer.toHexString(count), "0", 2));
        for (String remove : removes) {
            sb.append(leftPadding(Numeric.cleanHexPrefix(remove), "0", 64));
        }
        sb.append(String.format("%02x", version & 255));
        byte[] hash = Hash.sha3(Numeric.hexStringToByteArray(sb.toString()));
        return Numeric.toHexString(hash);
    }

    public static String encoderUpgrade(String txKey, String upgradeContract, byte version) {
        StringBuilder sb = new StringBuilder();
        sb.append(Numeric.toHexString(txKey.getBytes(StandardCharsets.UTF_8)));
        sb.append(Numeric.cleanHexPrefix(upgradeContract));
        sb.append(String.format("%02x", version & 255));
        byte[] hash = Hash.sha3(Numeric.hexStringToByteArray(sb.toString()));
        return Numeric.toHexString(hash);
    }

    public static String dataSign(String hashStr, String prikey) {
        byte[] hash = Numeric.hexStringToByteArray(hashStr);
        Credentials credentials = Credentials.create(prikey);
        Sign.SignatureData signMessage = Sign.signMessage(hash, credentials.getEcKeyPair(), false);
        byte[] signed = new byte[65];
        System.arraycopy(signMessage.getR(), 0, signed, 0, 32);
        System.arraycopy(signMessage.getS(), 0, signed, 32, 32);
        System.arraycopy(signMessage.getV(), 0, signed, 64, 1);
        String signedHex = Numeric.toHexStringNoPrefix(signed);
        return signedHex;
    }

    public static Boolean verifySign(String signAddress, String vHash, String signed) {
        signed = Numeric.cleanHexPrefix(signed);
        if (signed.length() != 130) {
            return false;
        }
        String r = "0x" + signed.substring(0, 64);
        String s = "0x" + signed.substring(64, 128);
        ECDSASignature signature = new ECDSASignature(Numeric.decodeQuantity(r), Numeric.decodeQuantity(s));
        byte[] hashBytes = Numeric.hexStringToByteArray(vHash);
        signAddress = signAddress.toLowerCase();
        for (int i = 0; i < 4; i++) {
            BigInteger recover = Sign.recoverFromSignature(i, signature, hashBytes);
            if (recover != null) {
                String address = "0x" + Keys.getAddress(recover);
                if (signAddress.equals(address.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
}
