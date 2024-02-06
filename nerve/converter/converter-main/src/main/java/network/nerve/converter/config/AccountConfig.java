package network.nerve.converter.config;

import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.model.ModuleE;

@Component
@Configuration(domain = ModuleE.Constant.ACCOUNT)
public class AccountConfig implements InitializingBean {
    private int sigMode = 0;
    private String sigMacApiKey;
    private String sigMacAddress;

    public int getSigMode() {
        return sigMode;
    }

    public void setSigMode(int sigMode) {
        this.sigMode = sigMode;
    }

    public String getSigMacApiKey() {
        return sigMacApiKey;
    }

    public void setSigMacApiKey(String sigMacApiKey) {
        this.sigMacApiKey = sigMacApiKey;
    }

    public String getSigMacAddress() {
        return sigMacAddress;
    }

    public void setSigMacAddress(String sigMacAddress) {
        this.sigMacAddress = sigMacAddress;
    }

    @Override
    public void afterPropertiesSet() throws NulsException {
    }

    /**
     * Verify the username and password of the account based on the signature machine, if returnedfalseNeed to intercept
     *
     * @param account
     * @param password
     * @return
     */
    public boolean vaildPassword(String account, String password) {
        if (StringUtils.isBlank(account) || StringUtils.isBlank(password)) {
            return false;
        }
        if (this.sigMode != 1) {
            return true;
        }
        if (!this.sigMacAddress.equals(account)) {
            return true;
        }
        if (this.sigMacApiKey.equals(password.trim())) {
            return true;
        }
        return false;
    }
}
