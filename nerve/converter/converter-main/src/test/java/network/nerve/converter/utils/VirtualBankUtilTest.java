package network.nerve.converter.utils;

import network.nerve.converter.model.bo.VirtualBankDirector;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VirtualBankUtilTest {

    @Test
    public void test() {

        List<VirtualBankDirector> list = new ArrayList<>();
        list.add(newInstance(5, "eee"));
        list.add(newInstance(1, "aaa"));
        list.add(newInstance(3, "ccc"));
        list.add(newInstance(2, "bbb"));
        list.add(newInstance(4, "ddd"));
        Collections.sort(list, VirtualBankDirectorSort.getInstance());
        list.stream().forEach(v -> {
            System.out.println(String.format("order: %s, hash: %s", v.getOrder(), v.getAgentHash()));
        });
    }

    private VirtualBankDirector newInstance(int order, String hash) {
        VirtualBankDirector director = new VirtualBankDirector();
        director.setOrder(order);
        director.setAgentHash(hash);
        return director;
    }

    @Test
    public void sleepTest() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
    }
}