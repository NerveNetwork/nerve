package io.nuls.core.crypto;

import io.nuls.core.exception.CryptoException;
import io.nuls.core.parse.SerializeUtils;
import junit.framework.TestCase;
import org.bouncycastle.asn1.ocsp.Signature;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.SignatureException;

/**
 * @author Niels
 */
public class ECKeyTest extends TestCase {

    public static void main(String[] args) {

        ECKey ecKey = ECKey.fromPrivate(Hex.decode("???"));
        String msgSignValue = "MEYCIQDWxEdu5ES08I9QtfUv2Bj3HmihnJffRNnjkrvkZtvoEgIhAIHeuY9O4m9COI3YGS+hpAHOdvD5toAhGHL5+MVptAmx";
        byte[] signValue = Base64.decode(msgSignValue);
        System.out.println("all-hex: "+HexUtil.encode(signValue));
        try {
            ECKey.ECDSASignature signature = ECKey.ECDSASignature.decodeFromDER(signValue);

            System.out.println("s-hex: "+HexUtil.encode(signature.encodeToDER()));
            byte[] messageBytes = ECKey.formatMessageForSigning("asdfasdfasdf");
            // Note that the C++ code doesn't actually seem to specify any character encoding. Presumably it's whatever
            // JSON-SPIRIT hands back. Assume UTF-8 for now.
            Sha256Hash messageHash = Sha256Hash.twiceOf(messageBytes);


            ECKey newEckey = ECKey.recoverFromSignature(0, signature, messageHash, true);

            System.out.println(ecKey.getPublicKeyAsHex().equals(newEckey.getPublicKeyAsHex()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testSignMessage() {
        ECKey ecKey = ECKey.fromPrivate(Hex.decode("???"));
        String msgSignValue = ecKey.signMessage("asdfasdfasdf");
        System.out.println(msgSignValue);
        System.out.println(HexUtil.encode(Base64.decode(msgSignValue)).length());
        System.out.println(HexUtil.encode(Base64.decode(msgSignValue)));
        //BIjrcSZj7nlQI8dZYnjj3ilKuQ6qIrv0VpZXA+ycZHJx4i57jDLIBWE7SYklTcpGDWAARcbzVsg2257QXT2oFJIAAAAAAAAAAAAAAAAAAAAAmWMjjztEDCePsHWVV3/3ne+uAU5anSWtAaSc/kR9UpExcc02pKH90vzGJ3C26soA
        try {
            ECKey key = ECKey.signedMessageToKey("asdfasdfasdf", msgSignValue);
            assertEquals(key.getPublicKeyAsHex(), ecKey.getPublicKeyAsHex());
        } catch (SignatureException e) {
            e.printStackTrace();
        }
    }

    public void testEncryptMessage(){
        ECKey ecKey = ECKey.fromPrivate(Hex.decode("???"));
        String message = "asdfasdfasdf";
        byte[] encryptedValue = ECIESUtil.encrypt(ecKey.getPubKey(),message.getBytes(StandardCharsets.UTF_8));
        String encryptedValueBase64 = new String(Base64.encode(encryptedValue), StandardCharsets.UTF_8);
        System.out.println(encryptedValueBase64);
        //IM8uIyMGXjn5nfvxF1zgcxJjj1gMJ6I/70MQb4kRIF//Wv8b0L9QyTLJYiHfSyPGD3Ke+g066bYpoNtK4xyfJGw=
        try {
            byte[] decryptBytes = ECIESUtil.decrypt(ecKey.getPrivKeyBytes(),Base64.decode(encryptedValueBase64));
            assertEquals(message,new String(decryptBytes,StandardCharsets.UTF_8));
        } catch (CryptoException e) {
            e.printStackTrace();
        }
    }

    public void testHexStringEncryptMessage(){
        ECKey ecKey = ECKey.fromPrivate(Hex.decode("???"));
        String message = "4b5b35cb1fdb";
        byte[] encryptedValue = ECIESUtil.encrypt(ecKey.getPubKey(), HexUtil.decode(message));
        System.out.println(HexUtil.encode(encryptedValue));
        try {
            byte[] decryptBytes = ECIESUtil.decrypt(ecKey.getPrivKeyBytes(), encryptedValue);
            assertEquals(message, HexUtil.encode(decryptBytes));
        } catch (CryptoException e) {
            e.printStackTrace();
        }
    }

    public void test1() throws CryptoException {
        String priKey = "???";
        String data = "04c590421386c62a6875b5e7fa6f640a8660b4d77fe6d587c2a6f1cef150df258afaa1b49fc25bccf77c0218731efc6d7d7468943305143b14a4f526dce5adb0fc00000000000000000000000000000000e317eb39f70a3d691d6b1c6379f9b66ebe6affa614a99960914e94c35f9d9831ac6aeef637ba7a59c964cd5fa7d3228c";
        byte[] bytes = ECIESUtil.decrypt(HexUtil.decode(priKey), data);
        System.out.println(HexUtil.encode(bytes));
        System.out.println(new String(bytes, StandardCharsets.UTF_8));
    }
}