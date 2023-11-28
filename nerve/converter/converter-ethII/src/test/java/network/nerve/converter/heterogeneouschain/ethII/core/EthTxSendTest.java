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
package network.nerve.converter.heterogeneouschain.ethII;

import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import org.web3j.abi.FunctionEncoder;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/5/16
 */
public class EthTxSendTest {

    public static void main(String[] _args) throws Exception {
        String privateKey = "";
        String gasPriceStr = "22";
        String nonce = "13";
        transferSelf(privateKey, gasPriceStr, nonce);
    }

    public static void transferSelf(String... _args) throws Exception {
        String privateKey = _args[0];
        String gasPriceStr = _args[1];
        BigInteger nonce = new BigInteger(_args[2]);
        Web3j web3j = Web3j.build(new HttpService("https://geth.nerve.network?d=1111&s=2222&p=asds45fgvbcv"));
        Credentials credentials = Credentials.create(privateKey);
        String from = credentials.getAddress();
        String to = from;
        BigInteger gasLimit = BigInteger.valueOf(21000L);
        BigInteger gasPrice = new BigDecimal(gasPriceStr).movePointRight(9).toBigInteger();
        String data = "";
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice, gasLimit, to, BigInteger.ZERO, data
        );
        //签名Transaction，这里要对交易做签名
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, 1, credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //发送交易
        EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        if (send.hasError()) {
            System.out.println(String.format("errorMsg: %s", send.getError().getMessage()));
            System.out.println(String.format("errorCode: %s", send.getError().getCode()));
            System.out.println(String.format("errorData: %s", send.getError().getData()));
        } else {
            System.out.println(String.format("hash: %s", send.getTransactionHash()));
        }
    }

    public static void testNetTransferSelf(String... _args) throws Exception {
        //System.out.println(String.format("args0: %s", _args[0]));
        //System.out.println(String.format("args1: %s", _args[1]));
        //System.out.println(String.format("args2: %s", _args[2]));
        // 测试网转账给自己
        String privateKey = "d8cdccd432fd1bb7711505d97c441672c540ccfcdbba17397619702eeef1d403";
        String gasPriceStr = "10";
        BigInteger nonce = new BigInteger("31");
        Web3j web3j = Web3j.build(new HttpService("https://ropsten.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5"));
        Credentials credentials = Credentials.create(privateKey);
        String from = credentials.getAddress();
        String to = from;
        BigInteger gasLimit = BigInteger.valueOf(21000L);
        BigInteger gasPrice = new BigInteger(gasPriceStr).multiply(BigInteger.TEN.pow(9));
        String data = "";
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice, gasLimit, to, BigInteger.ZERO, data
        );
        //签名Transaction，这里要对交易做签名
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, 3, credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //发送交易
        EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        if (send.hasError()) {
            System.out.println(String.format("errorMsg: %s", send.getError().getMessage()));
            System.out.println(String.format("errorCode: %s", send.getError().getCode()));
            System.out.println(String.format("errorData: %s", send.getError().getData()));
        } else {
            System.out.println(String.format("hash: %s", send.getTransactionHash()));
        }
    }

    public static void managerChange(String... _args) throws Exception {
        String privateKey = _args[0];
        String gasPriceStr = _args[1];
        String contractAddress = "0x6758d4C4734Ac7811358395A8E0c3832BA6Ac624";
        Web3j web3j = Web3j.build(new HttpService("http://geth.nerve.network?d=1111&s=2222&p=asds45fgvbcv"));
        Credentials credentials = Credentials.create(privateKey);
        String from = credentials.getAddress();
        EthGetTransactionCount transactionCount = web3j.ethGetTransactionCount(from, DefaultBlockParameterName.PENDING).sendAsync().get();
        BigInteger nonce = transactionCount.getTransactionCount();
        BigInteger gasLimit = BigInteger.valueOf(350000L);
        BigInteger gasPrice = new BigInteger(gasPriceStr).multiply(BigInteger.TEN.pow(9));
        String data = "0x0071922600000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000140000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000000040633065336439316666316438303636613832623862303966333862383063623034376132666465383862353966346534326466663564663430326561313938320000000000000000000000000000000000000000000000000000000000000001000000000000000000000000196c4b2b6e947b57b366967a1822f3fb7d9be1a8000000000000000000000000000000000000000000000000000000000000000100000000000000000000000010c17be7b6d3e1f424111c8bddf221c9557728b0000000000000000000000000000000000000000000000000000000000000028a0c074974757d9c57fba3e4a82a4c85493abb5107023fcf61ef585d94d5e7b0b75ed9f76a251ba3db8b44028298b5db478053a518ca51ea533b98633e6e5209831ca3367fd298de4967c1e014a9277010ef571ebc2bbc7c8129c377e89efcb2794a0be9c6d63a83f2ae2d5afd9c311bd23785afb9e8cc9fc0f80e57f625b8ace8241c9a6007c4f5d4b5d89478f6afbca0a15496852cc8764b876c7ebcd15d030794ba7dde3063aeb48eec192d53a1705cd5b794004b57437172c76b468e1c7d61dbf81c85963bb8ece815dc5a36bc1c42d463104ea441bdac3c7aca178fb791f424063c26acd3f35ebc891964fd7fa62ea9ff0ab37e2b4bb7e914a8aa515e06713525f61baa27c67e2841649073e12842627003244bfa7653f391f2be2b70ada85f7dcca8016dbb2b251a49a5d39510b39f2c119bf7b14fc4f1c2de2943213fb8f31c11691b24ca74c00ba90d1824692bae4e1cc4739caaf94bbef96d93e2fe0be3575a171c7e6843f5cb6a88992f37baa1a60567703a82f95aa384f92cc4587584484d6a4a1cab182430ed0ea0ae64728e8b27a8687a5aee43774b2f933117cecadea34b1e605cedd4bcbefcfa4e704fd1e4301dc3b428f1aca5ea6de028fc13c394cb0764ba1be5106d7bc1975c1430eff5ad07745e4cdc02c8ed75b6aa7a77c7fbbba7dcb92169bacfb0705700b1cb5a6a7609af2de7c52ef4c4dca042227672c340430c74161bd75ad46a8ba421d8f24d5020f55649110bcf477a35a6f7cdf7682ec7b9d0b23d5e70666840f847435e2c4592b4522cd036330a1df452fef046fe83d911b06fe31b1eaf12755a62e965e17914e162b172573147fa8eef5fb90ed3a4447420f7f9244a77a32dc97b7beb008e7b6b2e6250175c774be147e157d936699927a14239e91b00000000000000000000000000000000000000000000";
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice, gasLimit, contractAddress, BigInteger.ZERO, data
        );
        //签名Transaction，这里要对交易做签名
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, 1, credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //发送交易
        EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        if (send.hasError()) {
            System.out.println(String.format("errorMsg: %s", send.getError().getMessage()));
            System.out.println(String.format("errorCode: %s", send.getError().getCode()));
            System.out.println(String.format("errorData: %s", send.getError().getData()));
        } else {
            System.out.println(String.format("hash: %s", send.getTransactionHash()));
        }
    }

    public static void testNetManagerChange(String... _args) throws Exception {
        System.out.println(String.format("args0: %s", _args[0]));
        System.out.println(String.format("args1: %s", _args[1]));
        // 测试网跨链转入0.1eth
        String privateKey = "d8cdccd432fd1bb7711505d97c441672c540ccfcdbba17397619702eeef1d403";
        String gasPriceStr = "10";
        String contractAddress = "0x7d759a3330cec9b766aa4c889715535eed3c0484";
        Web3j web3j = Web3j.build(new HttpService("https://ropsten.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5"));
        Credentials credentials = Credentials.create(privateKey);
        String from = credentials.getAddress();
        EthGetTransactionCount transactionCount = web3j.ethGetTransactionCount(from, DefaultBlockParameterName.PENDING).sendAsync().get();
        BigInteger nonce = transactionCount.getTransactionCount();
        BigInteger gasLimit = BigInteger.valueOf(90000L);
        BigInteger gasPrice = new BigInteger(gasPriceStr).multiply(BigInteger.TEN.pow(9));

        String data = "0x0889d1f00000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000016345785d8a000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000025544e565464545350526e586b446961677937656e7469314b4c37354e553541784339735141000000000000000000000000000000000000000000000000000000";
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice, gasLimit, contractAddress, new BigDecimal("0.1").scaleByPowerOfTen(18).toBigInteger(), data
        );
        //签名Transaction，这里要对交易做签名
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, 3, credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //发送交易
        EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        System.out.println(String.format("hash: %s", send.getTransactionHash()));
    }

    public static void withdraw(String... _args) throws Exception {
        String privateKey = _args[0];
        String gasPriceStr = _args[1];
        String contractAddress = "0x6758d4C4734Ac7811358395A8E0c3832BA6Ac624";
        Web3j web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5"));
        //Web3j web3j = Web3j.build(new HttpService("http://geth.nerve.network?d=1111&s=2222&p=asds45fgvbcv"));
        Credentials credentials = Credentials.create(privateKey);
        String from = credentials.getAddress();
        EthGetTransactionCount transactionCount = web3j.ethGetTransactionCount(from, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = transactionCount.getTransactionCount();
        BigInteger gasLimit = BigInteger.valueOf(280000L);
        BigInteger gasPrice = new BigInteger(gasPriceStr).multiply(BigInteger.TEN.pow(9));
        String data = FunctionEncoder.encode(
                HtgUtil.getCreateOrSignWithdrawFunction(
                        "11025c9483d1650fba6a12a8fe9daf76868daaa8d51df2ffd693e19f9f631035",
                        "0x13e1079e238b42be9d38c1d9740d88eebad4f289",
                        new BigInteger("13899750180000000000000"),
                        true,
                        "0x24e3794605c84e580eea4972738d633e8a7127c8",
                        "c20d8a3f062eb93a79fbb03109503b2e9fb59e12dd2ec96b97c63f7dbe7a12245448669e5c68f6ab1f0806c32f8926ce946b52dcdb09ddaf01f1ffd29c7897ce1bfd5785ac209b40c7a66a672e4784d657bd8d43378d3ad4ea8fc9a487c19f2cb6265434ecc86a57c38599ff9e05a550e640299c4d7f680dfbd0ea2aa7cb5513131ccf8b85421d686bce377a5ad34cb8d0b1738de9e492dbb097b0a72dd547a2d9f43e9358dae4b277f5ccce3b08d727cd5b88a00f111476c708db8063fc326a44a11cf4a2b7b23c6bba45016c71477cc40070b4ffee5e4d9998b125f48546b183a3393d987470e8e2817e699601b46e4ca0c59427f601323bd612625cc1e6e3b6dbf71c5d31338cc2576bc4c3da8fb84e3fe812b64c76eea2d1ac08e9b6e3966db5cd9b1d050f400bd1e92295c19e92a3c9f6dd61cc208a1c45bae4a1a18d7218c7ffc41c8dd6304a872f68a152bd2c00b27b5b8aadfc66df5e2e3357945327f353ccd73303dd246e17f4b1ec75a09744d0f93c2a9eb4aa2b709ca30e23613dc758ad1cdb1cb41b0eaa4fbd23c0fa60695303c67fa04ad5a57cc2b01e0cf7e0fc69df6709e23a207510e2442ea41f4379ceecf2f3a6e0e2f2a966ea2e50d5eb390d0445c7591cb03486eac55284606a4634bbb095de8a1deed5ec623a5cfa6247cb1783a9893c1d76e3dec53e0cda32bd63e71f5dc90f2dcd5c712ae14e7cafc405b3a3033e801c29f96b637de1257534070b38113862aca70921a01e2d5f604e38fe4c8c2ed0f33455e51461769777680a0817881952a408be5f83b085361ad5c2816e724a74921c8c7be688b40237a8fb4bbd0a0529a9d776a5219ac3e2839d0b448de99ba596c654507797bc8a2c8cf8dae8fbc60362e8a78c456bbee40f2b2f14fc3310bfebc81b"
                )
        );
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice, gasLimit, contractAddress, BigInteger.ZERO, data
        );
        //签名Transaction，这里要对交易做签名
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, 1, credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //发送交易
        EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        if (send.hasError()) {
            System.out.println(String.format("errorMsg: %s", send.getError().getMessage()));
            System.out.println(String.format("errorCode: %s", send.getError().getCode()));
            System.out.println(String.format("errorData: %s", send.getError().getData()));
        } else {
            System.out.println(String.format("hash: %s", send.getTransactionHash()));
        }
    }
}
