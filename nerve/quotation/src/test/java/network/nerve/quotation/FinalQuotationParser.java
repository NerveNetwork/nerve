package network.nerve.quotation;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.quotation.model.txdata.Prices;
import network.nerve.quotation.model.txdata.Quotation;
import network.nerve.quotation.util.CommonUtil;
import org.junit.Test;

public class FinalQuotationParser {
    @Test
    public void test() throws NulsException {
        byte[] txData = HexUtil.decode("1a084254432d555344545467de7f4f42e7400843524f2d5553445457ab1b94d539e13f09555344432d55534454a921817136feef3f084f4b422d55534454a3416557b50e3f40084f4b542d555344544763a362d2aa47400f4e5654555344544e4c502d555344543d0ad7a3d8c8e24008424e422d5553445488b29ef772748040114e5654424e4243616b654c502d55534454780547b3006b3241084f4e452d55534454736f9760e50ec93f124e565448555344484d44584c502d55534454c23e4779e50bf241084441492d5553445426d4c21b88fbef3f0d4e5654424e424c502d55534454f6285c8fbe3422410846544d2d555344540869d7ce41e1f63f084e56542d555344547b14ae47e17aa43f09415641582d55534454e25817b7d1ae5840085041582d555344542237c30df8fcef3f124e5654455448554e4956324c502d55534454166f1ef9c2304d410e4e554c534e56544c502d555344546666267e0e37b141085452582d55534454192b9c6a4edab53f0d4e56544554484c502d55534454ae47e1ba3ac84141094e554c532d5553445416139b8f6b43ea3f09555344542d55534454000000000000f03f0748542d5553445467084b651f8021400a4d415449432d5553445447f332503a9d0040084554482d5553445423ce37602df2ae40084b43532d555344540d32ead642a73540");
        Prices prices = CommonUtil.getInstance(txData, Prices.class);
        System.out.println(prices);
    }

    @Test
    public void test2() throws NulsException {
        byte[] txData = HexUtil.decode("170500015fe8190d72be92e24810dcd6cd6570482a96ac5501110107352d39322d313848e17a148eb4c840");
        Quotation quotation = CommonUtil.getInstance(txData, Quotation.class);
        System.out.println(quotation);
    }
}