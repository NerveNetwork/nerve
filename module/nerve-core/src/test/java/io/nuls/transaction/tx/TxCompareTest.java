/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
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

package io.nuls.transaction.tx;

import io.nuls.common.ConfigBean;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.common.NerveCoreResponseMessageProcessor;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.dto.CoinDTO;
import io.nuls.transaction.model.po.TransactionNetPO;
import io.nuls.transaction.utils.OrphanSort;
import io.nuls.transaction.utils.TxUtil;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

/**
 * Transaction sorting test, mainly used for sorting orphan transactions
 *
 * @author: Charlie
 * @date: 2019/5/6
 */
public class TxCompareTest {

    static int chainId = 2;
    static int assetChainId = 2;
    static int assetId = 1;
    private OrphanSort orphanSort;
    static String password = "nuls123456";//"nuls123456";

    static String address20 = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";
    static String address21 = "tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD";
    static String address22 = "tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24";
    static String address23 = "tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD";
    static String address24 = "tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL";
    static String address25 = "tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL";
    static String address26 = "tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm";
    static String address27 = "tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1";
    static String address28 = "tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2";
    static String address29 = "tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn";

    private Chain chain;

    @Before
    public void before() throws Exception {
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":7771");
        chain = new Chain();
        chain.setConfig(new ConfigBean(chainId, assetId));
        //Initialize Context
//        SpringLiteContext.init(TestConstant.CONTEXT_PATH);
//        orphanSort = SpringLiteContext.getBean(OrphanSort.class);
    }

    public static List<Integer> randomIde(int count) {
        List<Integer> list = new ArrayList<>();
        Set<Integer> set = new HashSet<>();
        while (true) {
            Random rand = new Random();
            //The range of values for the random number norm should be0reachlistBetween maximum indexes
            //According to the formularand.nextInt((list.size() - 1) - 0 + 1) + 0;
            int ran = rand.nextInt(count);
            if (set.add(ran)) {
                list.add(ran);
            }
            if (set.size() == count) {
                break;
            }
        }
        return list;
    }

    //Shuffle the order of transactions and then sort them to verify if the sorting is correct
    @Test
    public void test() throws Exception {
//        importPriKey("9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b", password);//20 tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG TNVTdN9iJVX42PxxzvhnkC7vFmTuoPnRAgtyA
        importPriKey("477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75", password);//21 tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD TNVTdN9iB7VveoFG4GwYMRAFAF2Rsyrj9mjR3
        for (int y = 0; y < 1; y++) {
            System.out.println("------------------");
            System.out.println("------------------");
            System.out.println("------------------");
            System.out.println("------------------");


            List<Transaction> txs = new ArrayList<>();
            List<Transaction> txs1 = createTxs(15);
//            List<Transaction> txs1 = createTxs2();
            txs.addAll(txs1);

//            System.out.println("Correct order");
//            for (Transaction tx : txs) {
//                System.out.println("Correct order: " + tx.getHash().toHex());
//            }

            /* Display complete transaction formatting information
            for(Transaction tx : txs){
                TxUtil.txInformationDebugPrint(tx);
            }*/
            int total = txs.size();
            List<TransactionNetPO> txList = new ArrayList<>();
            List<Integer> ide = randomIde(total);
            for (int i = 0; i < total; i++) {
                txList.add(new TransactionNetPO(txs.get(ide.get(i))));
            }
            System.out.println("Size:" + txList.size());

            System.out.println("Before sorting");
            for (TransactionNetPO tx : txList) {
                System.out.println("Order before sorting: " + tx.getTx().getHash().toHex());
            }
            long start = System.currentTimeMillis();
            //sort
            OrphanSort sort = new OrphanSort();
            sort.rank(txList);
            long end = System.currentTimeMillis() - start;
            System.out.println("execution time：" + end);
            System.out.println(txList.size());
            System.out.println("After sorting");
            for (int i = 0; i < txList.size(); i++) {
                TransactionNetPO tx = txList.get(i);
                System.out.println("Sorted order: " + tx.getTx().getHash().toHex());
                String hs = txs.get(i).getHash().toHex();
                System.out.println("Correct sequence: " + tx.getTx().getHash().toHex().equals(hs));
            }

        }
    }

    //Assemble some time account Consistent,nonceIt's a continuous transaction
    private List<Transaction> createTxs(int count) throws Exception {
        Map map = CreateTx.createTransferTx(address21, address20, new BigInteger("100000"));
        long time = NulsDateUtils.getCurrentTimeSeconds();
        List<Transaction> list = new ArrayList<>();
        NulsHash hash = null;
        System.out.println("Correct order");
        for (int i = 0; i < count; i++) {
            Transaction tx = CreateTx.assemblyTransaction((List<CoinDTO>) map.get("inputs"), (List<CoinDTO>) map.get("outputs"), (String) map.get("remark"), hash, time, null);
            list.add(tx);
            hash = tx.getHash();
            CoinData coinData = TxUtil.getCoinData(tx);
            System.out.println("Correct order: " + tx.getHash().toHex() + ", nonce:" + HexUtil.encode(coinData.getFrom().get(0).getNonce()));
//            System.out.println(HexUtil.encode(tx.serialize()));
        }
        return list;
    }

//    private List<Transaction> createTxs2() throws Exception {
//        String s1 = "e5003a0bda5e007a5e350860a922803ea827f6f285d9375a21248088dc2625541dd9c9696f4998250400015884fa407da3005067ce4bd6d29a8e4a2af7846102605bef00000000000000000000000000000000000000000000000000000000000c32ce4a020000000000000000000000000000000000000000000000000000000000d202170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000300605bef000000000000000000000000000000000000000000000000000000000008f1a6292a88c0798300170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a086010000000000000000000000000000000000000000000000000000000000082c2d09ee348fad0d0001170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000300605bef0000000000000000000000000000000000000000000000000000000000feffffffffffffff692102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad846304402205fee2462aa08f9af41acc565f32e39169a6399c1cf397ed24cfb30f5a94408a6022006a675e9c758bc2cbb84c02838c5781e1a6bfeb53c256d5b50f5c0bc969d67ab";
//        String s2 = "e5003a0bda5e007a5e350860a922803ea827f6f285d9375a21248088dc2625541dd9c9696f4998250400015884fa407da3005067ce4bd6d29a8e4a2af78461024878e2050000000000000000000000000000000000000000000000000000000061a86e4b020000000000000000000000000000000000000000000000000000000000d202170400015884fa407da3005067ce4bd6d29a8e4a2af78461040003004878e20500000000000000000000000000000000000000000000000000000000088a4f23768f930d8e00170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a086010000000000000000000000000000000000000000000000000000000000088a4f23768f930d8e0001170400015884fa407da3005067ce4bd6d29a8e4a2af78461040003004878e20500000000000000000000000000000000000000000000000000000000feffffffffffffff6a2102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad84730450221008e5f61bcd7a7cfc26ed4deecc7ec4ebebb1dc9a23abb6ed170b88527a9dcfdf102202b3fc7d3b640cc024a0878d17ec3f785490426cde146f2b7f9da1b78cbe565b8";
//        String s3 = "e5003a0bda5e007a5e350860a922803ea827f6f285d9375a21248088dc2625541dd9c9696f4998250400015884fa407da3005067ce4bd6d29a8e4a2af7846102adbb4402000000000000000000000000000000000000000000000000000000008c90e24a020000000000000000000000000000000000000000000000000000000000d202170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000300adbb44020000000000000000000000000000000000000000000000000000000008039c174f07645dae00170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a08601000000000000000000000000000000000000000000000000000000000008039c174f07645dae0001170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000300adbb440200000000000000000000000000000000000000000000000000000000feffffffffffffff692102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad8463044022057a263ad9ff837ec48b3f7e3bc1ca6782a03e58ef6a88fc1888079d84dbc8db702202815853bc21e689ef169c5b55148d0ef0fa28490732a32553ed61b226b523cdd";
//        String s4 = "e5003a0bda5e007a5e350860a922803ea827f6f285d9375a21248088dc2625541dd9c9696f4998250400015884fa407da3005067ce4bd6d29a8e4a2af7846101a5bfd504000000000000000000000000000000000000000000000000000000004ced9b48020000000000000000000000000000000000000000000000000000000000d202170400015884fa407da3005067ce4bd6d29a8e4a2af78461040002006bc837da0100000000000000000000000000000000000000000000000000000008aeec084ad7cf9baf00170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a08601000000000000000000000000000000000000000000000000000000000008cd3d8febe863b4a00001170400015884fa407da3005067ce4bd6d29a8e4a2af78461040002006bc837da01000000000000000000000000000000000000000000000000000000feffffffffffffff6a2102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad8473045022100c3bc906861e09f995c390a0a4f50c074174c1cd69a47f8dafdde1b874d3637a10220641c51e1c12d06c4cbe9ca39153ad5575d90c5e1f059e2e30349a3245653b087";
//        String s5 = "e5003a0bda5e007a5e350860a922803ea827f6f285d9375a21248088dc2625541dd9c9696f4998250400015884fa407da3005067ce4bd6d29a8e4a2af78461014421f902000000000000000000000000000000000000000000000000000000003d949a48020000000000000000000000000000000000000000000000000000000000d202170400015884fa407da3005067ce4bd6d29a8e4a2af78461040002005ad09b230100000000000000000000000000000000000000000000000000000008974914a32660666700170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a08601000000000000000000000000000000000000000000000000000000000008974914a3266066670001170400015884fa407da3005067ce4bd6d29a8e4a2af78461040002005ad09b2301000000000000000000000000000000000000000000000000000000feffffffffffffff6a2102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad847304502210087d7ea06099a9af9a84314adb65f127a3c74e34ddfc248dc227b90b43698bbaf022026ef433d7ef58db8221edef5c7ab28d2684de02a24ebbdbcea14ab425d5a6de3";
//        String s6 = "e5003a0bda5e007a5e350860a922803ea827f6f285d9375a21248088dc2625541dd9c9696f4998250400015884fa407da3005067ce4bd6d29a8e4a2af784610118357c0200000000000000000000000000000000000000000000000000000000b67d6b47020000000000000000000000000000000000000000000000000000000000d202170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000200e20141f30000000000000000000000000000000000000000000000000000000008b8eed337e30758a500170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a08601000000000000000000000000000000000000000000000000000000000008b8eed337e30758a50001170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000200e20141f300000000000000000000000000000000000000000000000000000000feffffffffffffff6a2102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad8473045022100bbc69287f6098f10d1101933af5c8b780422f647b22ae0fc9b333d072e46ace902205c4aab1ce65896e67b15e9000d675905df1aea09b571da8cca3801132784f9b4";
//        String s7 = "e5003a0bda5e007a35036ba33ea420bdd45d9737ea946d326a40b12d88ee1c5305fedc91b3a6c4460400015884fa407da3005067ce4bd6d29a8e4a2af7846101f618c70400000000000000000000000000000000000000000000000000000000ba020500000000000000000000000000000000000000000000000000000000000000d202170400015884fa407da3005067ce4bd6d29a8e4a2af78461040002003204040000000000000000000000000000000000000000000000000000000000084bb6260ec2926e5e00170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a08601000000000000000000000000000000000000000000000000000000000008727dd8ef5e8d4bb80001170400015884fa407da3005067ce4bd6d29a8e4a2af78461040002003204040000000000000000000000000000000000000000000000000000000000feffffffffffffff6a2102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad8473045022100c47c20f13f09da178d47fb9916960956a4bad86574a3c3fd084dd33e89baab4a02200a127200ca99513f6db8907bf20a742a20fe740c7c28cbd872e76aa4c815f3a8";
//        String s8 = "e5003a0bda5e007a35036ba33ea420bdd45d9737ea946d326a40b12d88ee1c5305fedc91b3a6c4460400015884fa407da3005067ce4bd6d29a8e4a2af78461019e2e6e0000000000000000000000000000000000000000000000000000000000f7020500000000000000000000000000000000000000000000000000000000000000d202170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000200a55c000000000000000000000000000000000000000000000000000000000000087fc97024f0f4bd8000170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a086010000000000000000000000000000000000000000000000000000000000087fc97024f0f4bd800001170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000200a55c000000000000000000000000000000000000000000000000000000000000feffffffffffffff6a2102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad8473045022100b5fb6547754ffde0ab950026e5f3e1bf6169c183ba8effe23ef026d10301e607022038653392075a711a83ca275d368dc8754a48f69efcd9542e6bcfb7cbfdd57acc";
//        String s9 = "e6003a0bda5e002062fbc9feece12155f9149918df782ddafccf72ed129f63e59982e331a50feaab8c01170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a086010000000000000000000000000000000000000000000000000000000000084bb6260ec2926e5e0001170400015884fa407da3005067ce4bd6d29a8e4a2af784610400010000000000000000000000000000000000000000000000000000000000000000000000000000000000692102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad8463044022013f9667c05445d9a6339fd28859b2982246a1c47a2925f8b3ce6322ca301626302200150f6c2f16ed658d2562bf74d85e3b949944713a43f0038f6aa1baf89e80e0a";
//        String s10 = "e6003a0bda5e0020b6304973e60199ccd13a574814c42c567b1674d1f623663f7690138a87199fb48c01170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a08601000000000000000000000000000000000000000000000000000000000008e1f16dd5931cb00c0001170400015884fa407da3005067ce4bd6d29a8e4a2af784610400010000000000000000000000000000000000000000000000000000000000000000000000000000000000692102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad84630440220785903f6b5b3be54cded776bd6037771041a8dd3586b07b38c7c6fd6fbb28cbe022008387aeae9ab9161fae0bfff8724edb6bfaadda7671bf990863776402e2316d0";
//        String s11 = "e5003a0bda5e007a35036ba33ea420bdd45d9737ea946d326a40b12d88ee1c5305fedc91b3a6c4460400015884fa407da3005067ce4bd6d29a8e4a2af7846102aded6e01000000000000000000000000000000000000000000000000000000008b060500000000000000000000000000000000000000000000000000000000000000d202170400015884fa407da3005067ce4bd6d29a8e4a2af7846102000100aded6e010000000000000000000000000000000000000000000000000000000008f4dd41e4ce147c4d00170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a0860100000000000000000000000000000000000000000000000000000000000883ab7af34a9ea2030001170400015884fa407da3005067ce4bd6d29a8e4a2af7846102000100aded6e0100000000000000000000000000000000000000000000000000000000feffffffffffffff692102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad846304402206f10db6433318beebba4a878005326a4bbdbf3901d574bc6aebac93cb18516f8022036fdc8dcfc55fbb5343d0b9964e1240d8286be28cef54430bde825a3b4027651";
//        String s12 = "e5003a0bda5e007a35036ba33ea420bdd45d9737ea946d326a40b12d88ee1c5305fedc91b3a6c4460400015884fa407da3005067ce4bd6d29a8e4a2af784610255b19405000000000000000000000000000000000000000000000000000000002c080500000000000000000000000000000000000000000000000000000000000000d202170400015884fa407da3005067ce4bd6d29a8e4a2af784610200010055b19405000000000000000000000000000000000000000000000000000000000886f29cf08f9be19100170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a0860100000000000000000000000000000000000000000000000000000000000886f29cf08f9be1910001170400015884fa407da3005067ce4bd6d29a8e4a2af784610200010055b1940500000000000000000000000000000000000000000000000000000000feffffffffffffff692102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad846304402205f8fff1d863e241ac3ac5269cd135c1897fef09d0747a547f4b46f8b7fe64b36022047883add6e704b44de84c142c9b9907f48344ee2cb057ea98559d9acdea5a939";
//        String s13 = "e5003a0bda5e007a35036ba33ea420bdd45d9737ea946d326a40b12d88ee1c5305fedc91b3a6c4460400015884fa407da3005067ce4bd6d29a8e4a2af7846102342a7e0100000000000000000000000000000000000000000000000000000000de080500000000000000000000000000000000000000000000000000000000000000d202170400015884fa407da3005067ce4bd6d29a8e4a2af7846102000100342a7e0100000000000000000000000000000000000000000000000000000000089c7598800faf73a200170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a086010000000000000000000000000000000000000000000000000000000000089c7598800faf73a20001170400015884fa407da3005067ce4bd6d29a8e4a2af7846102000100342a7e0100000000000000000000000000000000000000000000000000000000feffffffffffffff6a2102446e0d4d132610b9ac3546fbae0d43cd7be1c4c62c3cb18ff8dc51691d141ad8473045022100989bad2d93a6b3f9538ed0656e70437330dbdac0a0c14b9425a806c9c3a9d16b0220052e7fa2508ccab60b79ce2158641ff295c7b88d74e4c893621a7eee4dc5f9d0";
//        List<Transaction> list = new ArrayList<>();
//        list.add(TxUtil.getInstance(s1, Transaction.class));
//        list.add(TxUtil.getInstance(s2, Transaction.class));
//        list.add(TxUtil.getInstance(s3, Transaction.class));
//        list.add(TxUtil.getInstance(s4, Transaction.class));
//        list.add(TxUtil.getInstance(s5, Transaction.class));
//        list.add(TxUtil.getInstance(s6, Transaction.class));
//        list.add(TxUtil.getInstance(s7, Transaction.class));
//        list.add(TxUtil.getInstance(s8, Transaction.class));
//        list.add(TxUtil.getInstance(s9, Transaction.class));
//        list.add(TxUtil.getInstance(s10, Transaction.class));
//        list.add(TxUtil.getInstance(s11, Transaction.class));
//        list.add(TxUtil.getInstance(s12, Transaction.class));
//        list.add(TxUtil.getInstance(s13, Transaction.class));
//        return list;
//    }

    public void importPriKey(String priKey, String pwd) {
        try {
            //Overwrite if account already exists If the account exists, it covers.
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("priKey", priKey);
            params.put("password", pwd);
            params.put("overwrite", true);
            Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_importAccountByPriKey", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_importAccountByPriKey");
            String address = (String) result.get("address");
            System.out.println(address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
