package io.nuls.account;

/**
 * ClassName:CeShi
 * Description:
 */
public class Bip39Test {

    /**
     * generate a random group of mnemonics
     * Generate a random set of mnemonic words
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
        //Generate seeds based on mnemonics and passwordsseed
        byte[] seed = new SeedCalculator().calculateSeed(generateMnemonics(), "");
        // according toseedGenerate public key、Private keys, etc
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hash = messageDigest.digest(seed);
        ECKeyPair ecKeyPair = ECKeyPair.create(hash);
        Numeric.toHexStringWithPrefix(ecKeyPair.getPrivateKey());
        Numeric.toHexStringWithPrefix(ecKeyPair.getPublicKey());
        //Based on public key orECKeyPairGet wallet address
        String address = Keys.getAddress(ecKeyPair);
        Log.info("address ================= "+ address);
        // According to the public key Private key password obtain Generate wallet filekeystore
        WalletFile walletFile = Wallet.createLight("", ecKeyPair);
        Log.info("prikey ============= "+ ECKey.fromPrivate(ecKeyPair.getPrivateKey()).getPrivateKeyAsHex());
        Log.info("walletFile =================== " + walletFile);
        Log.info("walletFile =================== " + walletFile.toString());
    }*/
}
