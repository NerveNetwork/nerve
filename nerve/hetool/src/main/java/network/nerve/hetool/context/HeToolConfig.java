package network.nerve.hetool.context;

import io.nuls.core.basic.ModuleConfig;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.hetool.model.ConfigBean;

/**
 * Transaction module setting
 *
 * @author: Loki
 * @date: 2019/03/14
 */
@Component
@Configuration(domain = ModuleE.Constant.HETOOL)
public class HeToolConfig extends ConfigBean implements ModuleConfig {

    /**
     * Main chainID
     */
    private int mainChainId;
    /**
     * Main asset of the main chainID
     */
    private int mainAssetId;

    /**
     * Withdrawal of black hole public key(Share the same public key with setting aliases)
     */
    private String blackHolePublicKey;


}
