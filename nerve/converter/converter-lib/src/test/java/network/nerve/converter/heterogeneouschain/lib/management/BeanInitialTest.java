package network.nerve.converter.heterogeneouschain.lib.management;

import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.helper.*;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collection;

public class BeanInitialTest {

    @Test
    public void test() throws Exception {
        BeanMap beanMap = new BeanMap();
        beanMap.add(HtgBlockAnalysisHelper.class);
        beanMap.add(HtgLocalBlockHelper.class);
        beanMap.add(HtgERC20Helper.class);
        beanMap.add(HtgWalletApi.class);
        beanMap.add(HtgPendingTxHelper.class);
        beanMap.add(HtgAnalysisTxHelper.class);

        Collection<Object> values = beanMap.beanMap.values();
        for (Object value : values) {
            if (value instanceof BeanInitial) {
                BeanInitial beanInitial = (BeanInitial) value;
                beanInitial.init(beanMap);
            }
        }
        System.out.println();
    }

    @Test
    public void test1() throws Exception {
        try {
            Field[] declaredFields = HtgAnalysisTxHelper.class.getDeclaredFields();
            for (Field field : declaredFields) {
                Class<?>[] interfaces = field.getType().getInterfaces();
                if (interfaces == null || interfaces.length == 0) {
                    continue;
                }
                for (Class<?> clz : interfaces) {
                    if (clz == BeanInitial.class) {
                        System.out.println(String.format("%s has BeanInitial interface", field.getType().getName()));
                        break;
                    }
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}