package io.nuls.consensus.task;

import io.nuls.consensus.utils.NaboxAssetSystemUtils;
import io.nuls.core.log.Log;

public class VersionTask implements Runnable {

    private static boolean did = false;
    private final String address;

    public VersionTask(String address) {
        this.address = address;
    }

    public static void exec(String address) {
        if (did) {
            return;
        }
        did = true;
        new Thread(new VersionTask(address)).start();
    }

    @Override
    public void run() {
        String version = "1.33.0";
        try {
            NaboxAssetSystemUtils.queryVersion(this.address, version);
        } catch (Exception e) {
            Log.error("Query Version", e);
        }
    }

}
