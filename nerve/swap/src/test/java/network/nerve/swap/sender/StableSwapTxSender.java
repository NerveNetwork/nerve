package network.nerve.swap.sender;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.txdata.FarmUpdateData;
import network.nerve.swap.model.txdata.stable.CreateStablePairData;
import network.nerve.swap.utils.AssembleTransaction;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Niels
 */
public class StableSwapTxSender extends ApiTxSender {

    private static final String apiUrl = "https://api.nerve.network/jsonrpc/";
    private static final String prikeyHex = "75d9ef37704b7026e004af26c56a20ecf08f63f5ded37afdfa4adfa1a3f61cab";
    private static final String address = "NERVEepb6DYfNDy22sR4NjQ8rpGRrT11Yb7tiM";
    private static final int chainId = 9;

    public static void main(String[] args) throws Exception {
        String coins = "1-81,9-420";
        String lpName = "VIBKN";
//        new FarmTxSender().sendUpdateTx(farmHash, BigInteger.valueOf(100000000), 0, BigInteger.ZERO, 10000, 5, 1);
        new StableSwapTxSender().createStableSwap(coins, lpName);

    }

    private void createStableSwap(String coins, String lpName) throws Exception {
        CreateStablePairData txData = new CreateStablePairData();
        String[] coinsKeyArray = coins.split(",");
        NerveToken[] array = new NerveToken[coinsKeyArray.length];
        int index = 0;
        for (String key : coinsKeyArray) {
            String str[] = key.split("-");
            array[index++] = new NerveToken(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
        }
        txData.setCoins(array);
        txData.setSymbol(lpName);

        AssembleTransaction aTx = new AssembleTransaction(txData.serialize());

        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.CREATE_SWAP_PAIR_STABLE_COIN);

        BigInteger amount = BigInteger.ZERO;

        //追加糖果资产的总额
        LedgerBalance balance = getLedgerBalance(chainId, address, chainId, 1);
        aTx.newFrom().setFrom(balance, amount).endFrom();

        aTx.newTo().setToAddress(AddressTool.getAddress(address)).setToAmount(amount).setToAssetsChainId(chainId).setToAssetsId(1).setToLockTime(0).endTo();

        Transaction tx = aTx.build();
        P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByEckey(tx.getHash(), ECKey.fromPrivate(HexUtil.decode(prikeyHex)));
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> list = new ArrayList<>();
        list.add(p2PHKSignature);
        transactionSignature.setP2PHKSignatures(list);
        tx.setTransactionSignature(transactionSignature.serialize());
        System.out.println("txHash: " + tx.getHash().toHex());
        System.out.println("txHex: " + HexUtil.encode(tx.serialize()));
        broadcastTx(chainId, tx);
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }
}
