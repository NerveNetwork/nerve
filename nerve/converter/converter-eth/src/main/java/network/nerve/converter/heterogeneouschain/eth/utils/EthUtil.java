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
package network.nerve.converter.heterogeneouschain.eth.utils;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.model.EthAccount;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.ethereum.crypto.ECKey;
import org.springframework.beans.BeanUtils;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.*;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.utils.Numeric;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Mimi
 * @date: 2020-02-26
 */
public class EthUtil {

    private static ETHWalletApi ethWalletApi;

    private static ETHWalletApi getETHWalletApi() {
        if(ethWalletApi == null) {
            ethWalletApi = SpringLiteContext.getBean(ETHWalletApi.class);
        }
        return ethWalletApi;
    }

    public static HeterogeneousTransactionInfo newTransactionInfo(Transaction tx) {
        HeterogeneousTransactionInfo txInfo = new HeterogeneousTransactionInfo();
        txInfo.setTxHash(tx.getHash());
        txInfo.setBlockHeight(tx.getBlockNumber().longValue());
        txInfo.setFrom(tx.getFrom());
        txInfo.setTo(tx.getTo());
        txInfo.setValue(tx.getValue());
        txInfo.setNerveAddress(covertNerveAddressByEthTx(tx));
        return txInfo;
    }

    public static HeterogeneousTransactionInfo newTransactionInfo(EthUnconfirmedTxPo txPo) {
        HeterogeneousTransactionInfo txInfo = new HeterogeneousTransactionInfo();
        BeanUtils.copyProperties(txPo, txInfo);
        return txInfo;
    }

    public static EthAccount createAccount(String prikey) {
        Credentials credentials = Credentials.create(prikey);
        ECKeyPair ecKeyPair = credentials.getEcKeyPair();
        byte[] pubKey = ecKeyPair.getPublicKey().toByteArray();
        EthAccount account = new EthAccount();
        account.setAddress(credentials.getAddress());
        account.setPubKey(pubKey);
        account.setPriKey(ecKeyPair.getPrivateKey().toByteArray());
        account.setEncryptedPriKey(new byte[0]);
        ECKey ecKey = ECKey.fromPrivate(Numeric.hexStringToByteArray(prikey));
        account.setCompressedPublicKey(Numeric.toHexStringNoPrefix(ecKey.getPubKeyPoint().getEncoded(true)));
        return account;
    }

    public static boolean isEmptyList(List list) {
        if (list != null && list.size() > 0) {
            return false;
        }
        return true;
    }

    public static String covertNerveAddressByEthTx(Transaction tx) {
        BigInteger ethPublicKey = extractEthPublicKey(tx);
        return covertNerveAddress(ethPublicKey);
    }

    public static String genEthAddressByCompressedPublickey(String compressedPublickey) {
        ECKey ecKey = ECKey.fromPublicOnly(Numeric.hexStringToByteArray(compressedPublickey));
        String orginPubkeyStr = EthConstant.HEX_PREFIX + Numeric.toHexStringNoPrefix(ecKey.getPubKey()).substring(2);
        return EthConstant.HEX_PREFIX + Keys.getAddress(orginPubkeyStr);
    }

    public static List<Object> parseEvent(String eventData, Event event) {
        List<Type> typeList = FunctionReturnDecoder.decode(eventData, event.getParameters());
        return typeList.stream().map(type -> type.getValue()).collect(Collectors.toList());
    }

    public static List<Object> parseInput(String inputData, List<TypeReference<Type>> parameters) {
        if(StringUtils.isBlank(inputData)) {
            return null;
        }
        if(inputData.length() < 10) {
            return null;
        }
        inputData = EthConstant.HEX_PREFIX + inputData.substring(10);
        List<Type> typeList = FunctionReturnDecoder.decode(inputData, parameters);
        return typeList.stream().map(type -> type.getValue()).collect(Collectors.toList());
    }

    public static <T>  T[] list2array(List<T> list) {
        if(list == null || list.isEmpty()) {
            return null;
        }
        T[] array = (T[]) Array.newInstance(list.get(0).getClass(), list.size());
        return list.toArray(array);
    }

    public static Function getNameERC20Function() {
        return new Function(
                EthConstant.METHOD_VIEW_ERC20_NAME,
                List.of(),
                List.of(new TypeReference<Utf8String>() {}));
    }
    public static Function getSymbolERC20Function() {
        return new Function(
                EthConstant.METHOD_VIEW_ERC20_SYMBOL,
                List.of(),
                List.of(new TypeReference<Utf8String>() {}));
    }
    public static Function getDecimalsERC20Function() {
        return new Function(
                EthConstant.METHOD_VIEW_ERC20_DECIMALS,
                List.of(),
                List.of(new TypeReference<Uint8>() {}));
    }

    public static Function getPendingWithdrawTransactionFunction(String nerveTxHash) {
        return new Function(
                EthConstant.METHOD_VIEW_PENDING_WITHDRAW,
                List.of(new Utf8String(nerveTxHash)),
                List.of(
                        new TypeReference<Address>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Bool>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Uint8>() {}
                )
        );
    }

    public static Function getPendingManagerChangeTransactionFunction(String nerveTxHash) {
        return new Function(
                EthConstant.METHOD_VIEW_PENDING_MANAGERCHANGE,
                List.of(new Utf8String(nerveTxHash)),
                List.of(
                        new TypeReference<Uint8>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<DynamicArray<Address>>() {},
                        new TypeReference<DynamicArray<Address>>() {},
                        new TypeReference<Uint8>() {}
                )
        );
    }

    public static Function getIsCompletedFunction(String nerveTxHash) {
        return new Function(
                EthConstant.METHOD_VIEW_IS_COMPLETED_TRANSACTION,
                List.of(new Utf8String(nerveTxHash)),
                List.of(new TypeReference<Bool>() {
                })
        );
    }

    public static Function getAllManagersFunction() {
        return new Function(
                EthConstant.METHOD_VIEW_ALL_MANAGERS_TRANSACTION,
                List.of(),
                List.of(new TypeReference<DynamicArray<Address>>() {
                })
        );
    }

    public static Function getCreateOrSignWithdrawFunction(String nerveTxHash, String toAddress, BigInteger value, boolean isContractAsset, String contractAddressERC20) {
        return new Function(
                EthConstant.METHOD_CREATE_OR_SIGN_WITHDRAW,
                List.of(new Utf8String(nerveTxHash), new Address(toAddress), new Uint256(value), new Bool(isContractAsset), new Address(contractAddressERC20)),
                List.of(new TypeReference<Type>() {
                })
        );
    }

    public static Function getCreateOrSignManagerChangeFunction(String nerveTxHash, List<Address> addList, List<Address> removeList, int orginTxCount) {
        return new Function(
                EthConstant.METHOD_CREATE_OR_SIGN_MANAGERCHANGE,
                List.of(new Utf8String(nerveTxHash),
                        new DynamicArray(Address.class, addList),
                        new DynamicArray(Address.class, removeList),
                        new Uint8(orginTxCount)),
                List.of(new TypeReference<Type>() {
                })
        );
    }

    public static Function getCreateOrSignUpgradeFunction(String nerveTxHash) {
        return new Function(
                EthConstant.METHOD_CREATE_OR_SIGN_UPGRADE,
                List.of(new Utf8String(nerveTxHash)),
                List.of(new TypeReference<Type>() {
                })
        );
    }

    private static BigInteger extractEthPublicKey(Transaction tx) {
        ECDSASignature signature = new ECDSASignature(Numeric.decodeQuantity(tx.getR()), Numeric.decodeQuantity(tx.getS()));
        byte[] hashBytes = getRawTxHashBytes(tx);

        for (int i = 0; i < 4; i++) {
            BigInteger recoverPubKey = Sign.recoverFromSignature(i, signature, hashBytes);
            if (recoverPubKey != null) {
                String address = EthConstant.HEX_PREFIX + Keys.getAddress(recoverPubKey);
                if (tx.getFrom().toLowerCase().equals(address.toLowerCase())) {
                    return recoverPubKey;
                }
            }
        }
        return null;
    }

    private static byte[] getRawTxHashBytes(Transaction tx) {
        String data = "";
        if (StringUtils.isNotBlank(tx.getInput()) && !EthConstant.HEX_PREFIX.equals(tx.getInput().toLowerCase())) {
            data = tx.getInput();
        }
        RawTransaction rawTx = RawTransaction.createTransaction(
                tx.getNonce(),
                tx.getGasPrice(),
                tx.getGas(),
                tx.getTo(),
                tx.getValue(),
                data);
        byte[] rawTxEncode;
        if (tx.getChainId() != null) {
            rawTxEncode = TransactionEncoder.encode(rawTx, tx.getChainId());
        } else {
            rawTxEncode = TransactionEncoder.encode(rawTx);
        }
        byte[] hashBytes = Hash.sha3(rawTxEncode);
        return hashBytes;
    }

    private static String covertNerveAddress(BigInteger ethPublickey) {
        String pub = Numeric.toHexStringNoPrefix(ethPublickey);
        pub = leftPadding(pub, "0", 128);
        String pubkeyFromEth = EthConstant.PUBLIC_KEY_UNCOMPRESSED_PREFIX + pub;
        io.nuls.core.crypto.ECKey ecKey = io.nuls.core.crypto.ECKey.fromPublicOnly(HexUtil.decode(pubkeyFromEth));
        return AddressTool.getAddressString(ecKey.getPubKeyPoint().getEncoded(true), EthContext.NERVE_CHAINID);
    }

    public static String leftPadding(String orgin, String padding, int total) {
        return padding.repeat(total - orgin.length()) + orgin;
    }

}
