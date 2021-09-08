package network.nerve.swap.sender;

import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.txdata.FarmUpdateData;
import network.nerve.swap.utils.AssembleTransaction;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Niels
 */
public class FarmTxSender extends ApiTxSender {

    private static final String apiUrl = "http://127.0.0.1:17004/jsonrpc/";
    private static final String prikeyHex = "dd03f5165bf8b7463a745aa0870e5434f70950c12aaa6afd9293486085192e00";
    private static final String address = "TNVTdTSPSznM2jpDQCEzih2sNBnyhhsJG4xmm";
    private static final int chainId = 5;

    public static void main(String[] args) throws Exception {
        NulsHash farmHash = NulsHash.fromHex("aa74bc44fc55e86e9a905006cbce4caf4f8cd4ef7b8cedb3dea96766b02d5652");
//        new FarmTxSender().sendUpdateTx(farmHash, BigInteger.valueOf(100000000), 0, BigInteger.ZERO, 10000, 5, 1);
        new FarmTxSender().sendUpdateTx(farmHash, BigInteger.valueOf(100000000), 0, BigInteger.valueOf(50000000000L), 10000, 5, 1);

    }

    private void sendUpdateTx(NulsHash farmHash, BigInteger newSyrupPerBlock, int changeType, BigInteger changeTotalSyrupAmount, long withdrawLockTime, int syrupAssetChainId, int syrupAssetId) throws Exception {
        FarmUpdateData txData = new FarmUpdateData();
        txData.setFarmHash(farmHash);
        txData.setNewSyrupPerBlock(newSyrupPerBlock);
        txData.setChangeType((short) changeType);
        txData.setChangeTotalSyrupAmount(changeTotalSyrupAmount);
        txData.setWithdrawLockTime(withdrawLockTime);
        AssembleTransaction aTx = new AssembleTransaction(txData.serialize());

        aTx.setTime(System.currentTimeMillis() / 1000L);
        aTx.setTxType(TxType.FARM_UPDATE);

        BigInteger amount = BigInteger.ZERO;
        if (changeType == 0) {
            amount = changeTotalSyrupAmount;
        }
        //追加糖果资产的总额
        LedgerBalance balance = getLedgerBalance(chainId, address, syrupAssetChainId, syrupAssetId);
        if (null == balance) {
            System.out.println("余额获取失败");
            return;
        }
        aTx.newFrom().setFrom(balance, amount).endFrom();
        aTx.newTo().setToAddress(SwapUtils.getFarmAddress(chainId)).setToLockTime(0).setToAmount(amount).setToAssetsChainId(syrupAssetChainId).setToAssetsId(syrupAssetId).endTo();

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
