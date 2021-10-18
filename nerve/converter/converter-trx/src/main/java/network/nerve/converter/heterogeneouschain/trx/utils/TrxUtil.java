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
package network.nerve.converter.heterogeneouschain.trx.utils;

import com.google.protobuf.InvalidProtocolBufferException;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant;
import network.nerve.converter.heterogeneouschain.trx.model.TRC20TransferEvent;
import network.nerve.converter.heterogeneouschain.trx.model.TrxAccount;
import network.nerve.converter.heterogeneouschain.trx.model.TrxTransaction;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.ethereum.crypto.ECKey;
import org.tron.trident.abi.FunctionReturnDecoder;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.*;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.abi.datatypes.generated.Uint8;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.crypto.Hash;
import org.tron.trident.crypto.SECP256K1;
import org.tron.trident.crypto.tuwenitypes.Bytes;
import org.tron.trident.crypto.tuwenitypes.Bytes32;
import org.tron.trident.crypto.tuwenitypes.MutableBytes;
import org.tron.trident.crypto.tuwenitypes.UInt256;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Base58Check;
import org.tron.trident.utils.Numeric;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.protostuff.ByteString.EMPTY_STRING;

/**
 * @author: Mimi
 * @date: 2020-02-26
 */
public class TrxUtil {

    private static final List<TypeReference<Type>> revertReasonType = Collections.singletonList(TypeReference.create((Class<Type>) AbiTypes.getType("string")));
    private static final String errorMethodId = "0x08c379a0";

    public static HeterogeneousTransactionInfo newTransactionInfo(TrxTransaction trxTxInfo, int nerveChainId) throws Exception {
        HeterogeneousTransactionInfo txInfo = new HeterogeneousTransactionInfo();
        txInfo.setTxHash(trxTxInfo.getHash());
        txInfo.setFrom(trxTxInfo.getFrom());
        txInfo.setTo(trxTxInfo.getTo());
        txInfo.setValue(trxTxInfo.getValue());
        txInfo.setNerveAddress(covertNerveAddressByTx(trxTxInfo.getTx(), nerveChainId, trxAddress2eth(trxTxInfo.getFrom())));
        return txInfo;
    }

    public static String covertNerveAddressByTx(Chain.Transaction tx, int nerveChainId) throws Exception {
        return covertNerveAddressByTx(tx, nerveChainId, null);
    }

    public static String covertNerveAddressByTx(Chain.Transaction tx, int nerveChainId, String from) throws Exception {
        if (StringUtils.isBlank(from)) {
            Chain.Transaction.Contract contract = tx.getRawData().getContract(0);
            Chain.Transaction.Contract.ContractType type = contract.getType();
            // 转账
            if (Chain.Transaction.Contract.ContractType.TransferContract == type) {
                Contract.TransferContract tc = Contract.TransferContract.parseFrom(contract.getParameter().getValue());
                from = TrxUtil.trxAddress2eth(Numeric.toHexString(tc.getOwnerAddress().toByteArray()));
            } else if (Chain.Transaction.Contract.ContractType.TriggerSmartContract == type) {
                // 调用合约
                Contract.TriggerSmartContract tg = Contract.TriggerSmartContract.parseFrom(contract.getParameter().getValue());
                from = TrxUtil.trxAddress2eth(Numeric.toHexString(tg.getOwnerAddress().toByteArray()));
            }
        }
        if (StringUtils.isBlank(from)) {
            return null;
        }
        BigInteger ethPublicKey = extractPublicKey(from, tx);
        return covertNerveAddress(ethPublicKey, nerveChainId);
    }

    private static BigInteger extractPublicKey(String from, Chain.Transaction tx) {
        SECP256K1.Signature sign = SECP256K1.Signature.decode(Bytes.wrap(tx.getSignature(0).toByteArray()));
        ECDSASignature signature = new ECDSASignature(sign.getR(), sign.getS());
        SHA256.Digest digest = new SHA256.Digest();
        digest.update(tx.getRawData().toByteArray());
        byte[] hashBytes = digest.digest();

        BigInteger recoverPubKey = Sign.recoverFromSignature(sign.getRecId(), signature, hashBytes);
        if (recoverPubKey != null) {
            String address = HtgConstant.HEX_PREFIX + Keys.getAddress(recoverPubKey);
            if (trxAddress2eth(from).equals(address.toLowerCase())) {
                return recoverPubKey;
            }
        }
        return null;
    }

    private static String covertNerveAddress(BigInteger ethPublickey, int nerveChainId) {
        String pub = Numeric.toHexStringNoPrefix(ethPublickey);
        pub = leftPadding(pub, "0", 128);
        String pubkeyFromEth = TrxConstant.PUBLIC_KEY_UNCOMPRESSED_PREFIX + pub;
        io.nuls.core.crypto.ECKey ecKey = io.nuls.core.crypto.ECKey.fromPublicOnly(Numeric.hexStringToByteArray(pubkeyFromEth));
        return AddressTool.getAddressString(ecKey.getPubKeyPoint().getEncoded(true), nerveChainId);
    }

    public static List<Object> parseEvent(String eventData, Event event) {
        List<Type> typeList = FunctionReturnDecoder.decode(eventData, event.getParameters());
        return typeList.stream().map(type -> type.getValue()).collect(Collectors.toList());
    }

    public static TRC20TransferEvent parseTRC20Event(Response.TransactionInfo.Log log) {
        String from = new Address(new BigInteger(log.getTopics(1).toByteArray())).getValue();
        String to = new Address(new BigInteger(log.getTopics(2).toByteArray())).getValue();
        BigInteger value = new BigInteger(log.getData().toByteArray());
        return new TRC20TransferEvent(from, to, value);
    }

    public static List<Object> parseInput(String inputData, List<TypeReference<Type>> parameters) {
        if(StringUtils.isBlank(inputData)) {
            return null;
        }
        inputData = Numeric.cleanHexPrefix(inputData);
        if(inputData.length() < 8) {
            return null;
        }
        inputData = inputData.substring(8);
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
                TrxConstant.METHOD_VIEW_ERC20_NAME,
                List.of(),
                List.of(new TypeReference<Utf8String>() {}));
    }
    public static Function getSymbolERC20Function() {
        return new Function(
                TrxConstant.METHOD_VIEW_ERC20_SYMBOL,
                List.of(),
                List.of(new TypeReference<Utf8String>() {}));
    }
    public static Function getDecimalsERC20Function() {
        return new Function(
                TrxConstant.METHOD_VIEW_ERC20_DECIMALS,
                List.of(),
                List.of(new TypeReference<Uint8>() {}));
    }


    public static Function getIsCompletedFunction(String nerveTxHash) {
        return new Function(
                TrxConstant.METHOD_VIEW_IS_COMPLETED_TRANSACTION,
                List.of(new Utf8String(nerveTxHash)),
                List.of(new TypeReference<Bool>() {
                })
        );
    }

    public static Function getAllManagersFunction() {
        return new Function(
                TrxConstant.METHOD_VIEW_ALL_MANAGERS_TRANSACTION,
                List.of(),
                List.of(new TypeReference<DynamicArray<Address>>() {
                })
        );
    }

    public static String leftPadding(String orgin, String padding, int total) {
        return padding.repeat(total - orgin.length()) + orgin;
    }

    public static Function getCreateOrSignWithdrawFunction(String nerveTxHash, String toAddress, BigInteger value, boolean isContractAsset, String contractAddressERC20, String signatureHexData) {
        return new Function(
                TrxConstant.METHOD_CREATE_OR_SIGN_WITHDRAW,
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
                TrxConstant.METHOD_CREATE_OR_SIGN_MANAGERCHANGE,
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
                TrxConstant.METHOD_CREATE_OR_SIGN_UPGRADE,
                List.of(new Utf8String(nerveTxHash),
                        new Address(upgradeContract),
                        new DynamicBytes(Numeric.hexStringToByteArray(signatureHexData))),
                List.of(new TypeReference<Type>() {
                })
        );
    }

    public static Function getCrossOutFunction(String to, BigInteger value, String erc20) {
        return new Function(
                TrxConstant.METHOD_CROSS_OUT,
                List.of(new Utf8String(to),
                        new Uint256(value),
                        new Address(erc20)),
                List.of(new TypeReference<Type>() {
                })
        );
    }

    public static Function getIsMinterERC20Function(String erc20) {
        return new Function(
                TrxConstant.METHOD_VIEW_IS_MINTER_ERC20,
                List.of(new Address(erc20)),
                List.of(new TypeReference<Bool>() {
                })
        );
    }

    public static String encoderWithdraw(String txKey, String toAddress, BigInteger value, Boolean isContractAsset, String erc20, byte version) {
        StringBuilder sb = new StringBuilder();
        sb.append(Numeric.toHexString(txKey.getBytes(StandardCharsets.UTF_8)));
        sb.append(Numeric.cleanHexPrefix(TrxUtil.trxAddress2eth(toAddress)));
        sb.append(leftPadding(value.toString(16), "0", 64));
        sb.append(isContractAsset ? "01" : "00");
        sb.append(Numeric.cleanHexPrefix(TrxUtil.trxAddress2eth(erc20)));
        sb.append(String.format("%02x", version & 255));
        byte[] hash = Hash.sha3(Numeric.hexStringToByteArray(sb.toString()));
        return Numeric.toHexString(hash);
    }

    public static String encoderChange(String txKey, String[] adds, int count, String[] removes, byte version) {
        StringBuilder sb = new StringBuilder();
        sb.append(Numeric.toHexString(txKey.getBytes(StandardCharsets.UTF_8)));
        for (String add : adds) {
            sb.append(leftPadding(Numeric.cleanHexPrefix(TrxUtil.trxAddress2eth(add)), "0", 64));
        }
        sb.append(leftPadding(Integer.toHexString(count), "0", 2));
        for (String remove : removes) {
            sb.append(leftPadding(Numeric.cleanHexPrefix(TrxUtil.trxAddress2eth(remove)), "0", 64));
        }
        sb.append(String.format("%02x", version & 255));
        byte[] hash = Hash.sha3(Numeric.hexStringToByteArray(sb.toString()));
        return Numeric.toHexString(hash);
    }

    public static String encoderUpgrade(String txKey, String upgradeContract, byte version) {
        StringBuilder sb = new StringBuilder();
        sb.append(Numeric.toHexString(txKey.getBytes(StandardCharsets.UTF_8)));
        sb.append(Numeric.cleanHexPrefix(TrxUtil.trxAddress2eth(upgradeContract)));
        sb.append(String.format("%02x", version & 255));
        byte[] hash = Hash.sha3(Numeric.hexStringToByteArray(sb.toString()));
        return Numeric.toHexString(hash);
    }

    public static String dataSign(String hashStr, String prikey) {
        byte[] hash = Numeric.hexStringToByteArray(hashStr);
        KeyPair pair = new KeyPair(prikey);
        SECP256K1.Signature sig = SECP256K1.sign(Bytes32.wrap(hash), pair.getRawPair());
        MutableBytes bytes = MutableBytes.create(65);
        UInt256.valueOf(sig.getR()).toBytes().copyTo(bytes, 0);
        UInt256.valueOf(sig.getS()).toBytes().copyTo(bytes, 32);
        byte recId = sig.getRecId();
        if (recId <= 1) {
            recId += 27;
        }
        bytes.set(64, recId);
        byte[] signed = bytes.toArray();
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
        int v = Integer.parseInt(signed.substring(128), 16);
        if (v >= 27) {
            v -= 27;
        }
        ECDSASignature signature = new ECDSASignature(Numeric.decodeQuantity(r), Numeric.decodeQuantity(s));
        byte[] hashBytes = Numeric.hexStringToByteArray(vHash);
        BigInteger recover = Sign.recoverFromSignature(v, signature, hashBytes);
        if (recover != null) {
            String address = "0x" + Keys.getAddress(recover);
            if (trxAddress2eth(signAddress).equals(address.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static BigDecimal calcOtherMainAssetOfWithdraw(AssetName otherMainAssetName, BigDecimal otherMainAssetUSD, BigDecimal htgUSD) {
        BigDecimal feeLimit = new BigDecimal(TrxConstant.FEE_LIMIT_OF_WITHDRAW);
        BigDecimal otherMainAssetAmount = feeLimit.multiply(htgUSD).movePointRight(otherMainAssetName.decimals()).movePointLeft(6).divide(otherMainAssetUSD, 0, RoundingMode.DOWN);
        // 当NVT作为手续费时，向上取整
        if (otherMainAssetName == AssetName.NVT) {
            otherMainAssetAmount = otherMainAssetAmount.divide(BigDecimal.TEN.pow(8), 0, RoundingMode.UP).movePointRight(8);
        }
        return otherMainAssetAmount;
    }

    public static BigDecimal calcTrxOfWithdrawProtocol15() {
        return new BigDecimal(TrxConstant.FEE_LIMIT_OF_WITHDRAW);
    }

    public static String trxAddress2eth(String address) {
        if (StringUtils.isBlank(address)) {
            return null;
        }
        byte[] bytes = ApiWrapper.parseAddress(address).toByteArray();
        int length = bytes.length;
        if (length == 20) {
            return Numeric.toHexString(bytes);
        } else if (length == 21) {
            byte[] result = new byte[20];
            System.arraycopy(bytes, 1, result, 0, 20);
            return Numeric.toHexString(result);
        } else {
            return null;
        }
    }

    public static String ethAddress2trx(String address) {
        if (StringUtils.isBlank(address)) {
            return null;
        }
        if (address.startsWith("T")) {
            // need check? Base58Check.base58ToBytes(address);
            return address;
        }
        return ethAddress2trx(Numeric.hexStringToByteArray(address));
    }

    public static String ethAddress2trx(byte[] address) {
        if (address == null) {
            return null;
        }
        int length = address.length;
        if (length == 21) {
            return Base58Check.bytesToBase58(address);
        } else if (length == 20) {
            byte[] result = new byte[21];
            result[0] = 65;
            System.arraycopy(address, 0, result, 1, 20);
            return Base58Check.bytesToBase58(result);
        } else {
            return null;
        }
    }

    public static String genTrxAddressByCompressedPublickey(String compressedPublicKey) {
        return ethAddress2trx(HtgUtil.genEthAddressByCompressedPublickey(compressedPublicKey));
    }

    public static TrxAccount createAccount(String priKey) {
        KeyPair keyPair = new KeyPair(priKey);
        byte[] pubKey = keyPair.getRawPair().getPublicKey().getEncoded();
        TrxAccount account = new TrxAccount();
        account.setAddress(keyPair.toBase58CheckAddress());
        account.setPubKey(pubKey);
        account.setPriKey(keyPair.getRawPair().getPrivateKey().getEncoded());
        account.setEncryptedPriKey(new byte[0]);
        ECKey ecKey = ECKey.fromPrivate(Numeric.hexStringToByteArray(priKey));
        account.setCompressedPublicKey(Numeric.toHexStringNoPrefix(ecKey.getPubKeyPoint().getEncoded(true)));
        return account;
    }

    public static String calcTxHash(Chain.Transaction tx) {
        SHA256.Digest digest = new SHA256.Digest();
        digest.update(tx.getRawData().toByteArray());
        byte[] txid = digest.digest();
        String txHash = Numeric.toHexString(txid);
        return txHash;
    }

    public static TrxTransaction generateTxInfo(Chain.Transaction tx) throws InvalidProtocolBufferException {
        if (tx == null) {
            return null;
        }
        Chain.Transaction.Contract contract = tx.getRawData().getContract(0);
        Chain.Transaction.Contract.ContractType type = contract.getType();
        // 过滤 非TRX转账和调用合约交易
        if (Chain.Transaction.Contract.ContractType.TransferContract != type &&
                Chain.Transaction.Contract.ContractType.TriggerSmartContract != type) {
            return null;
        }
        String from = EMPTY_STRING, to = EMPTY_STRING, input = EMPTY_STRING;
        BigInteger value = BigInteger.ZERO;
        // 转账
        if (Chain.Transaction.Contract.ContractType.TransferContract == type) {
            Contract.TransferContract tc = Contract.TransferContract.parseFrom(contract.getParameter().getValue());
            from = TrxUtil.ethAddress2trx(tc.getOwnerAddress().toByteArray());
            to = TrxUtil.ethAddress2trx(tc.getToAddress().toByteArray());
            value = BigInteger.valueOf(tc.getAmount());
        } else if (Chain.Transaction.Contract.ContractType.TriggerSmartContract == type) {
            // 调用合约
            Contract.TriggerSmartContract tg = Contract.TriggerSmartContract.parseFrom(contract.getParameter().getValue());
            from = TrxUtil.ethAddress2trx(tg.getOwnerAddress().toByteArray());
            to = TrxUtil.ethAddress2trx(tg.getContractAddress().toByteArray());
            value = BigInteger.valueOf(tg.getCallValue());
            input = Numeric.toHexString(tg.getData().toByteArray());
        }
        // 计算txHash
        String trxTxHash = TrxUtil.calcTxHash(tx);
        TrxTransaction trxTxInfo = new TrxTransaction(tx, trxTxHash, from, to, value, input, type);
        return trxTxInfo;
    }

    public static BigInteger convertTrxToSun(BigDecimal value) {
        value = value.movePointRight(6);
        return value.toBigInteger();
    }

    public static BigDecimal convertSunToTrx(BigInteger balance) {
        BigDecimal cardinalNumber = new BigDecimal("1000000");
        BigDecimal decimalBalance = new BigDecimal(balance);
        BigDecimal value = decimalBalance.divide(cardinalNumber, 6, RoundingMode.DOWN);
        return value;
    }

    public static boolean isErrorInResult(String result) {
        return result != null && result.startsWith(errorMethodId);
    }

    public static String getRevertReason(String result) {
        if (isErrorInResult(result)) {
            String hexRevertReason = result.substring(errorMethodId.length());
            List<Type> decoded = FunctionReturnDecoder.decode(hexRevertReason, revertReasonType);
            Utf8String decodedRevertReason = (Utf8String) decoded.get(0);
            return decodedRevertReason.getValue();
        }
        return null;
    }

    public static boolean checkTransactionSuccess(Response.TransactionInfo receipt) {
        if (receipt == null) {
            return false;
        }
        long energyUsage = receipt.getReceiptOrBuilder().getEnergyUsage();
        if (energyUsage == 0) {
            // 没有能量消耗，视为普通转账交易
            return true;
        }
        if (receipt.getReceipt().getResult() != Chain.Transaction.Result.contractResult.SUCCESS) {
            return false;
        }
        return true;
    }
}
