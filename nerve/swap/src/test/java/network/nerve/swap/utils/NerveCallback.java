package network.nerve.swap.utils;

import io.nuls.core.exception.NulsException;
import network.nerve.swap.JunitCase;

/**
 * @author Niels
 */
public interface NerveCallback<T> {
    NerveCallback NULL_CALLBACK = new NerveCallback() {
        @Override
        public void callback(JunitCase junitCase, Object result) {
            System.out.println("blank");
        }
    };

    public void callback(JunitCase junitCase, T result) throws Exception;
}
