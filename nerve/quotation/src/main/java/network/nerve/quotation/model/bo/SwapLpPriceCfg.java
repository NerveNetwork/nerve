package network.nerve.quotation.model.bo;

public class SwapLpPriceCfg {

    private int lpAssetChainId;
    private int lpAssetId;
    private int lpAssetDecimals;
    private int aAssetChainId;
    private int aAssetId;
    private int aAssetDecimals;
    private int bAssetChainId;
    private int bAssetId;
    private int bAssetDecimals;
    private int baseAssetDecimals;
    private int baseAssetChainId;
    private int baseAssetId;
    private String[] tokenPath;

    public SwapLpPriceCfg(QuotationContractCfg cfg) {
        String[] lpInfo = cfg.getTokenInfo().split("-");
        lpAssetChainId = Integer.parseInt(lpInfo[0]);
        lpAssetId = Integer.parseInt(lpInfo[1]);
        lpAssetDecimals = Integer.parseInt(lpInfo[2]);
        String[] aInfo = cfg.getSwapTokenContractAddress().split("-");
        aAssetChainId = Integer.parseInt(aInfo[0]);
        aAssetId = Integer.parseInt(aInfo[1]);
        aAssetDecimals = Integer.parseInt(aInfo[2]);
        String[] bInfo = cfg.getBaseTokenContractAddress().split("-");
        bAssetChainId = Integer.parseInt(bInfo[0]);
        bAssetId = Integer.parseInt(bInfo[1]);
        bAssetDecimals = Integer.parseInt(bInfo[2]);
        String[] baseInfo = cfg.getBaseTokenInfo().split("-");
        baseAssetChainId = Integer.parseInt(baseInfo[0]);
        baseAssetId = Integer.parseInt(baseInfo[1]);
        baseAssetDecimals = Integer.parseInt(baseInfo[2]);
        tokenPath = cfg.getRpcAddress().split(",");

    }


    public int getLpAssetChainId() {
        return lpAssetChainId;
    }

    public int getLpAssetId() {
        return lpAssetId;
    }

    public int getLpAssetDecimals() {
        return lpAssetDecimals;
    }

    public int getaAssetChainId() {
        return aAssetChainId;
    }

    public int getaAssetId() {
        return aAssetId;
    }

    public int getaAssetDecimals() {
        return aAssetDecimals;
    }

    public int getbAssetChainId() {
        return bAssetChainId;
    }

    public int getbAssetId() {
        return bAssetId;
    }

    public int getbAssetDecimals() {
        return bAssetDecimals;
    }

    public String[] getTokenPath() {
        return tokenPath;
    }

    public int getBaseAssetChainId() {
        return baseAssetChainId;
    }

    public int getBaseAssetId() {
        return baseAssetId;
    }

    public int getBaseAssetDecimals() {
        return baseAssetDecimals;
    }
}
