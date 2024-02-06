package network.nerve.converter.heterogeneouschain.eth.utils;

import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.heterogeneouschain.eth.base.Base;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.junit.Test;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint128;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.ETH_ERC20_STANDARD_FILE;

public class EthUtilTest extends Base {

    @Test
    public void newTransactionInfoByEthUnconfirmedTxPoTest() {
        EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
        po.setNerveTxHash("nerveTxHash_xxxxxxxxx");
        po.setDeletedHeight(9999L);
        po.setAddAddresses(new String[]{"a", "b"});

        po.setTxHash("txHash_asd");
        po.setBlockHeight(1L);
        po.setFrom("from_xxx");
        po.setTo("to_xxx");
        po.setValue(BigInteger.valueOf(666666L));
        po.setTxTime(8989L);
        po.setDecimals(6);
        //po.setIfContractAsset(true);
        //po.setContractAddress("contractAddress_qweqweqwe");
        po.setAssetId(2);
        po.setNerveAddress("nerveAddress_ttttxxxxx");
        HeterogeneousTransactionInfo txInfo = EthUtil.newTransactionInfo(po);
        System.out.println(txInfo.toString());
    }

    @Test
    public void decode() {
        List<TypeReference<Type>> INPUT_UPGRADE = Utils.convert(
                List.of(
                        new TypeReference<DynamicBytes>(){},
                        new TypeReference<Address>(){},
                        new TypeReference<Uint128>(){},
                        new TypeReference<Uint256>(){},
                        new TypeReference<Uint256>(){}
                )
        );
        String input = "0x75ceafe6000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000e4490b083300c79f00fc6f04c3736351340bd43d00000000000000000000000000000000000000000000000000354a6ba7a18000000000000000000000000000000000000000000000000000002e0d22e2926cec00000000000000000000000000000000000000000000000000000000655dd106000000000000000000000000000000000000000000000000000000000000002be5d7c2a44ffddf6b295a15c148167daaaf5cf34f0001f4b5bedd42000b71fdde22d3ee8a79bd49a568fc8f000000000000000000000000000000000000000000";
        List<Object> objects = HtgUtil.parseInput(input, INPUT_UPGRADE);
        System.out.println();
    }

    @Test
    public void parseERC20() throws Exception {
        setMain();
        List<TokenInfo> list = new ArrayList<>();

        BigDecimal value = new BigDecimal("0.05");

        for (int i = 1; i <= 18; i++) {
            String file = String.format("token/token_%s.txt", i);
            System.out.println();
            System.out.println(String.format("open file: %s", file));
            String read = IoUtils.read(file);
            int q = read.indexOf("<tbody>");
            int w = read.indexOf("</tbody>", q);
            if (q == -1 || w == -1) {
                continue;
            }
            String topERC20 = read.substring(q, w);

            int m, n, s, k, j;
            while ((m = topERC20.indexOf("<img class='u-xs-avatar mr-2'")) != -1) {
                n = topERC20.indexOf("'>", m);
                String icon = topERC20.substring(m, n);
                s = icon.indexOf("src=");
                icon = icon.substring(s);
                icon = "https://etherscan.io" + icon.substring(5);

                m = topERC20.indexOf("</div></td><td class='text-nowrap'>");
                n = topERC20.indexOf("<div", m);
                String usdPrice = topERC20.substring(m, n);
                s = usdPrice.indexOf("$");
                usdPrice = usdPrice.substring(s + 1);
                usdPrice = usdPrice.replace(",", "");

                m = topERC20.indexOf("<a class='text-primary'");
                n = topERC20.indexOf("</a>", m);
                String tokenInfo = topERC20.substring(m, n);
                k = tokenInfo.indexOf("(");
                j = tokenInfo.indexOf(")");
                String address, name, symbol;
                address = tokenInfo.substring(37, 37 + 42);
                if (k == -1) {
                    name = tokenInfo.substring(81);
                    symbol = name;
                } else {
                    name = tokenInfo.substring(81, k);
                    symbol = tokenInfo.substring(k + 1, j);
                }
                name = name.trim();
                symbol = symbol.trim();
                int decimals = decimals(address);

                n = topERC20.indexOf("</tr>", m);
                topERC20 = topERC20.substring(n);

                BigDecimal price = new BigDecimal(usdPrice);
                if (price.compareTo(value) > 0) {
                    System.out.println(String.format("address: %s, name: %s, symbol: %s, decimals: %s, price:%s, icon: %s,", address, name, symbol, decimals, usdPrice, icon));
                    list.add(new TokenInfo(address, name, symbol, decimals, usdPrice, icon));
                }
            }
        }
        String json = JSONUtils.obj2PrettyJson(list);
        //System.out.println(json);
        String path = new File(EthUtilTest.class.getClassLoader().getResource("").getPath()).getParentFile().getParent()
                + String.format("%ssrc%stest%sresources%s", File.separator, File.separator, File.separator, File.separator);
        IoUtils.writeString(new File(path + "eth_tokens.json"), json);
    }

    @Test
    public void parseBEP20() throws Exception {
        setMain();
        List<TokenInfo> list = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String file = String.format("bsc_token/token_%s.txt", i);
            System.out.println();
            System.out.println(String.format("open file: %s", file));
            String read = IoUtils.read(file);
            int q = read.indexOf("<tbody>");
            int w = read.indexOf("</tbody>", q);
            if (q == -1 || w == -1) {
                continue;
            }
            String topERC20 = read.substring(q, w);

            int m, n, s, k, j;
            while ((m = topERC20.indexOf("<img class='u-xs-avatar mr-2'")) != -1) {
                n = topERC20.indexOf("'>", m);
                String icon = topERC20.substring(m, n);
                s = icon.indexOf("src=");
                icon = icon.substring(s);
                icon = "https://bscscan.com" + icon.substring(5);

                m = topERC20.indexOf("</div></td><td class='text-nowrap'>");
                n = topERC20.indexOf("<div", m);
                String usdPrice = topERC20.substring(m, n);
                s = usdPrice.indexOf("$");
                usdPrice = usdPrice.substring(s + 1);
                usdPrice = usdPrice.replace(",", "");

                m = topERC20.indexOf("<a class='text-primary'");
                n = topERC20.indexOf("</a>", m);
                String tokenInfo = topERC20.substring(m, n);
                k = tokenInfo.indexOf("(");
                j = tokenInfo.indexOf(")");
                String address, name, symbol;
                address = tokenInfo.substring(37, 37 + 42);
                if (k == -1) {
                    name = tokenInfo.substring(81);
                    symbol = name;
                } else {
                    name = tokenInfo.substring(81, k);
                    symbol = tokenInfo.substring(k + 1, j);
                }
                name = name.trim();
                symbol = symbol.trim();
                int decimals = decimals(address);
                System.out.println(String.format("address: %s, name: %s, symbol: %s, decimals: %s, price:%s, icon: %s,", address, name, symbol, decimals, usdPrice, icon));
                n = topERC20.indexOf("</tr>", m);
                topERC20 = topERC20.substring(n);

                list.add(new TokenInfo(address, name, symbol, decimals, usdPrice, icon));
            }

        }
        String json = JSONUtils.obj2PrettyJson(list);
        //System.out.println(json);
        String path = new File(EthUtilTest.class.getClassLoader().getResource("").getPath()).getParentFile().getParent()
                + String.format("%ssrc%stest%sresources%s", File.separator, File.separator, File.separator, File.separator);
        IoUtils.writeString(new File(path + "bsc_tokens.json"), json);
    }



    @Test
    public void parseHT20() throws Exception {
        setMain();
        List<TokenInfo> list = new ArrayList<>();
        for (int i = 1; i <= 1; i++) {
            String file = String.format("ht_token/token_%s.txt", i);
            System.out.println();
            System.out.println(String.format("open file: %s", file));
            String read = IoUtils.read(file);
            int q = read.indexOf("<tbody>");
            int w = read.indexOf("</tbody>", q);
            if (q == -1 || w == -1) {
                continue;
            }
            String topERC20 = read.substring(q, w);

            int m, n, s, k, j;
            while ((m = topERC20.indexOf("<img class='u-xs-avatar mr-2'")) != -1) {
                n = topERC20.indexOf("'>", m);
                String icon = topERC20.substring(m, n);
                s = icon.indexOf("src=");
                icon = icon.substring(s);
                icon = "https://hecoinfo.com/" + icon.substring(5);

                m = topERC20.indexOf("</div></td><td class='text-nowrap'>");
                n = topERC20.indexOf("<div", m);
                String usdPrice = topERC20.substring(m, n);
                s = usdPrice.indexOf("$");
                usdPrice = usdPrice.substring(s + 1);
                usdPrice = usdPrice.replace(",", "");

                m = topERC20.indexOf("<a class='text-primary'");
                n = topERC20.indexOf("</a>", m);
                String tokenInfo = topERC20.substring(m, n);
                k = tokenInfo.indexOf("(");
                j = tokenInfo.indexOf(")");
                String address, name, symbol;
                address = tokenInfo.substring(37, 37 + 42);
                if (k == -1) {
                    name = tokenInfo.substring(81);
                    symbol = name;
                } else {
                    name = tokenInfo.substring(81, k);
                    symbol = tokenInfo.substring(k + 1, j);
                }
                name = name.trim();
                symbol = symbol.trim();
                int decimals = decimals(address);
                System.out.println(String.format("address: %s, name: %s, symbol: %s, decimals: %s, price:%s, icon: %s,", address, name, symbol, decimals, usdPrice, icon));
                n = topERC20.indexOf("</tr>", m);
                topERC20 = topERC20.substring(n);

                list.add(new TokenInfo(address, name, symbol, decimals, usdPrice, icon));
            }

        }
        String json = JSONUtils.obj2PrettyJson(list);
        //System.out.println(json);
        String path = new File(EthUtilTest.class.getClassLoader().getResource("").getPath()).getParentFile().getParent()
                + String.format("%ssrc%stest%sresources%s", File.separator, File.separator, File.separator, File.separator);
        IoUtils.writeString(new File(path + "ht_tokens.json"), json);
    }

    @Test
    public void viewContractTest() throws Exception {
        setMain();
        String contractAddress = "0XB8C77482E45F1F44DE1745F52C74426C631BDD52".toLowerCase();
        System.out.println(decimals(contractAddress));
    }

    @Test
    public void tokenAddressToLowerCaseTest() throws IOException {
        String json = null;
        try {
            json = IoUtils.read(ETH_ERC20_STANDARD_FILE);
        } catch (Exception e) {
            // skip it
            Log.error("init ERC20Standard error.", e);
        }
        if (json == null) {
            return;
        }
        List<TokenInfo> list = JSONUtils.json2list(json, TokenInfo.class);
        list.stream().forEach(info -> info.setAddress(info.getAddress().toLowerCase()));

        String jsonToLowerCase = JSONUtils.obj2PrettyJson(list);
        //System.out.println(json);
        String path = new File(EthUtilTest.class.getClassLoader().getResource("").getPath()).getParentFile().getParent()
                + String.format("%ssrc%stest%sresources%s", File.separator, File.separator, File.separator, File.separator);
        IoUtils.writeString(new File(path + "lowercasetokens.json"), jsonToLowerCase);
    }

    private int decimals(String contract) {
        try {
            Function decimalsFunction = new Function(
                    "decimals",
                    List.of(),
                    List.of(new TypeReference<Uint8>() {
                    })
            );
            Function function = decimalsFunction;
            List<Type> list = ethWalletApi.callViewFunction(contract, function);
            if (list == null || list.isEmpty()) {
                return 18;
            }
            return Integer.parseInt(list.get(0).getValue().toString());
        } catch (Exception e) {
            Log.error("contract[{}] error[{}]", contract, e.getMessage());
            return 0;
        }
    }

    @Test
    public void getFunctionInfo() throws Exception {
        String nerveTxHash = "";
        Function pendingManagerChangeTransactionFunction = EthUtil.getPendingManagerChangeTransactionFunction(nerveTxHash);
        List<Type> valueTypes = ethWalletApi.callViewFunction("0x480C0B2D4DCb8E9e66498F70dcf7E5560d09De2A", pendingManagerChangeTransactionFunction);
        List<Object> list = valueTypes.stream().map(type -> type.getValue()).collect(Collectors.toList());
        System.out.println(list);
    }

    @Test
    public void orderCalTest() {
        String nerveTxHash = "fe1ee1df4126798a47795c0ee763c89542a777a35dd4143be4dc111f23985cc0";
        int seed = new BigInteger(nerveTxHash.substring(0, 1), 16).intValue() + 1;
        int bankSize = 2;
        if (bankSize > 16) {
            seed += new BigInteger(nerveTxHash.substring(1, 2), 16).intValue() + 1;
        }
        int mod = seed % bankSize + 1;
        // Wait for a fixed time in sequence before sending outETHtransaction
        int bankOrder = 1;
        if (bankOrder < mod) {
            bankOrder += bankSize - (mod - 1);
        } else {
            bankOrder -= mod - 1;
        }
        int waitting = (bankOrder - 1) * EthConstant.DEFAULT_INTERVAL_WAITTING;
        System.out.println(String.format("seed: %s, mod: %s, bankOrder: %s, waitting: %s", seed, mod, bankOrder, waitting));
    }

    private static class TokenInfo {
        private String address;
        private String name;
        private String symbol;
        private int decimals;
        private String usdPrice;
        private String icon;

        public TokenInfo() {
        }

        public TokenInfo(String address, String name, String symbol, int decimals, String usdPrice, String icon) {
            this.address = address;
            this.name = name;
            this.symbol = symbol;
            this.decimals = decimals;
            this.usdPrice = usdPrice;
            this.icon = icon;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public int getDecimals() {
            return decimals;
        }

        public void setDecimals(int decimals) {
            this.decimals = decimals;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getUsdPrice() {
            return usdPrice;
        }

        public void setUsdPrice(String usdPrice) {
            this.usdPrice = usdPrice;
        }
    }
}
