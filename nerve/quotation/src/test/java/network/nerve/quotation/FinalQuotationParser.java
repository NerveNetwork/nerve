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
        byte[] txData = HexUtil.decode("14084e56542d5553445470ce88d2dee0bb3f084254432d55534454ed6e33af96e6eb4009555344432d55534454cfbd874b8efbef3f084f4b422d555344540000000000303940084f4b542d555344543333333333a34840085041582d5553445479cc4065fcfbef3f124e5654455448554e4956324c502d55534454c40a77ad0f8152410f4e5654555344544e4c502d5553445448e17a148eb4c840085452582d55534454b68a387c56c2b83f08424e422d555344540000000000918340114e5654424e4243616b654c502d555344547ee541960c583641094e554c532d55534454d89b18929389df3f09555344542d55534454000000000000f03f0748542d555344545f419ab1684a2040084f4e452d5553445410afeb17ec06cf3f0a4d415449432d55534454d92b4555c0ebff3f124e565448555344484d44584c502d55534454ec2d04baf62ef441084441492d55534454ee7c3f355efaef3f084554482d5553445474909dd8b9e1b140084b43532d55534454560e2db29d773b40");
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
