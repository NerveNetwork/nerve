package network.nerve.swap.db;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.rockdb.manager.RocksDBManager;
import io.nuls.core.rockdb.model.Entry;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.po.FarmUserInfoPO;
import network.nerve.swap.utils.SwapDBUtil;

import java.util.List;

/**
 * @author Niels
 */
public class FarmUserStorageReader {

    public static void main(String[] args) throws Exception {
        String path = "/Users/niels/workspace/nerve-network/data/swap";
        RocksDBManager.init(path);
        List<Entry<byte[], byte[]>> list = RocksDBManager.entryList("sw_table_farm_user_5");
        for (Entry<byte[], byte[]> entry : list) {
            System.out.println(HexUtil.encode(entry.getKey()));
            FarmUserInfoPO po = SwapDBUtil.getModel(entry.getValue(), FarmUserInfoPO.class);
            System.out.println(po);
        }
    }
}
