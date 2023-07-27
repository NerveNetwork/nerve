package io.nuls.consensus.model.bo.config;

import java.util.List;

/**
 * @author Niels
 */
public class AssetsStakingLimitCfg {
    private String key;
    private long totalCount;
    private List<AssetsType> assetsTypeList;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public List<AssetsType> getAssetsTypeList() {
        return assetsTypeList;
    }

    public void setAssetsTypeList(List<AssetsType> assetsTypeList) {
        this.assetsTypeList = assetsTypeList;
    }
}
