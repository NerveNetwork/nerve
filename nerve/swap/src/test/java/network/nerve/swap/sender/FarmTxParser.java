package network.nerve.swap.sender;

import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.model.txdata.FarmUpdateData;

/**
 * @author Niels
 */
public class FarmTxParser {
    public static void main(String[] args) throws NulsException {
        String txHex = "4b008d121661156661726d207570646174652072656d61726b2e2e2e6980f375ffb94ac8058647e7d6eda6ef1463433d525de03f4e644efea334cd0bc700e1f5050000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000040420f00000000008c0117050001b9978dbea4abebb613b6be2d0d66b4179d2511cb05000f00000000000000000000000000000000000000000000000000000000000000000008644efea334cd0bc7000117050005b8bcb07f6344b42ab04250c86a6e8b75d3fdbbc605000f00000000000000000000000000000000000000000000000000000000000000000000000000000000006a210369b20002bc58c74cb6fd5ef564f603834393f53bed20c3314b4b7aba8286a7e04730450221008b84ed0aa94d30e04ffd09f98d2b3dc59ba64e7d34e2fb6e1109b695aaa44ff402202df6445ab2030af36cf7dfcc1c1425ee6df77fcb43f14b970a78baffd70c8633";
        Transaction tx = new Transaction();
        tx.parse(HexUtil.decode(txHex),0);
        CoinData coinData  = tx.getCoinDataInstance();
        if(tx.getType() == TxType.FARM_UPDATE){
            FarmUpdateData txData = new FarmUpdateData();
            txData.parse(tx.getTxData(),0);
            System.out.println(txData);
        }
        System.out.println(coinData.getTo().size());
    }
}
