package io.nuls.account;

/**
 * ClassName:CeShi
 * Description:
 */
public class Bip39Test {

    /**
     * generate a random group of mnemonics
     * 生成一组随机的助记词
     */
    /*public static String generateMnemonics() {
        StringBuilder sb = new StringBuilder();
        byte[] entropy = new byte[Words.TWELVE.byteLength()];
        new SecureRandom().nextBytes(entropy);

        new MnemonicGenerator(English.INSTANCE)
                .createMnemonic(entropy, sb::append);
        return sb.toString();
    }



    public static void main(String[] args) throws NoSuchAlgorithmException, CipherException {
        String s = generateMnemonics();
        Log.info("sssssssssss =============== " + s);
        //根据助记词和密码生成种子seed
        byte[] seed = new SeedCalculator().calculateSeed(generateMnemonics(), "");
        // 根据seed生成公钥、私钥等
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hash = messageDigest.digest(seed);
        ECKeyPair ecKeyPair = ECKeyPair.create(hash);
        Numeric.toHexStringWithPrefix(ecKeyPair.getPrivateKey());
        Numeric.toHexStringWithPrefix(ecKeyPair.getPublicKey());
        //根据公钥或者ECKeyPair获取钱包地址
        String address = Keys.getAddress(ecKeyPair);
        Log.info("address ================= "+ address);
        // 根据公钥 私钥 密码 得到 生成钱包文件keystore
        WalletFile walletFile = Wallet.createLight("", ecKeyPair);
        Log.info("prikey ============= "+ ECKey.fromPrivate(ecKeyPair.getPrivateKey()).getPrivateKeyAsHex());
        Log.info("walletFile =================== " + walletFile);
        Log.info("walletFile =================== " + walletFile.toString());
    }*/
}
