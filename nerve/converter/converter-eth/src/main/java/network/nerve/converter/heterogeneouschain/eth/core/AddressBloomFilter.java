package network.nerve.converter.heterogeneouschain.eth.core;

import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.heterogeneouschain.eth.utils.BloomFilter;
import network.nerve.converter.heterogeneouschain.eth.utils.RandomUtil;

/**
 * Created by ln on 2018-06-17.
 */
@Component
public class AddressBloomFilter implements InitializingBean {

    private BloomFilter bloomFilter;

    @Override
    public void afterPropertiesSet() throws NulsException {
        bloomFilter = new BloomFilter(2000000, 0.0001d, RandomUtil.randomLong());
    }

    public boolean contains(String address) {
        return bloomFilter.contains(address.getBytes());
    }

    public void insert(String address) {
        bloomFilter.insert(address.getBytes());
    }
}
