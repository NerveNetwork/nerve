package io.nuls.consensus.utils;


import io.nuls.account.util.HttpClientUtil;

public class NaboxAssetSystemUtils {

    public static String queryVersion(String address,String localVersion) throws Exception {
        String url = "https://assets.nabox.io/api/nerve/version/"+address+"/"+localVersion;
        return HttpClientUtil.get(url);
    }

}
