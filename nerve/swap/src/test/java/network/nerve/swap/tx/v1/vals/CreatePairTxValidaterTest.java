package network.nerve.swap.tx.v1.vals;

import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.cache.impl.SwapPairCacheImpl;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.utils.NerveCallback;
import network.nerve.swap.utils.SwapUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Niels
 */
public class CreatePairTxValidaterTest {

    @Test
    public void isPairExist() {
        SwapPairDTO dto = new SwapPairDTO();
        String address = SwapUtils.getStringPairAddress(9,new NerveToken(1,1),new NerveToken(9,1));
        String address1 = SwapUtils.getStringPairAddress(2,new NerveToken(1,1),new NerveToken(9,1));
        CreatePairTxValidater validater = new CreatePairTxValidater();
        SwapPairCache cache = new SwapPairCacheImpl();
        cache.put(address,dto);
        validater.setCacher(cache);

        List<JunitCase> items = new ArrayList<>();
        items.add(new JunitCase("case0",validater, new Object[]{address}, ValidaterResult.getFailed(SwapErrorCode.PAIR_ALREADY_EXISTS), false, null, NerveCallback.NULL_CALLBACK));
        items.add(new JunitCase("case1",validater, new Object[]{address1}, ValidaterResult.getSuccess(), false, null, NerveCallback.NULL_CALLBACK));
        JunitExecuter<CreatePairTxValidater> executer = new JunitExecuter<>() {
            @Override
            public Object execute(JunitCase<CreatePairTxValidater> junitCase) {
                return junitCase.getObj().isPairNotExist((String) junitCase.getParams()[0]);
            }
        };
        JunitUtils.execute(items, executer);
    }
}