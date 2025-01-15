/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.btc.txdata.WithdrawalFeeLog;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgBlockHandler;
import network.nerve.converter.heterogeneouschain.lib.management.BeanMap;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.heterogeneouschain.trx.docking.TrxDocking;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2022/11/29
 */
public class TempTest {
    int rpcVersion = -1;
    String apiUrl = null;

    @Test
    public void testBeanMap() throws Exception {
        BeanMap map = new BeanMap();
        map.add(BitCoinLibWalletApi.class);
        map.add(HtgBlockHandler.class);
        map.add(TrxDocking.class);
        map.beanMap.forEach((k,v) -> System.out.println(k + "===" + v));
    }

    @Test
    public void txTest() throws Exception {
        // fde22f9ce7a7bfb27f2b70d2cc017b49c1fdbb322a3d1fdffe19a96c2ec779d2
        Transaction tx = new Transaction();
        tx.setType(TxType.WITHDRAWAL_UTXO_FEE_PAYMENT);
        tx.setTime(1722851383);
        tx.setRemark("Record withdraw fee [RECHARGE], chainId: 202, amount: 93000000, hash: 314363ec7b9fdfab95209fc43633cd33f0048bde4095c412a465d180f8ca0f67, blockHeight: 2363795, blockHash: 000000000000015f3e8f67a1f74459cb3fff0d9f2d633f92e593e2b1f4fd7112".getBytes(StandardCharsets.UTF_8));
        WithdrawalFeeLog feeLog = new WithdrawalFeeLog();
        //{"blockHeight":2363795,
        // "blockHash":"",
        // "htgTxHash":"314363ec7b9fdfab95209fc43633cd33f0048bde4095c412a465d180f8ca0f67",
        // "htgChainId":202,
        // "fee":93000000,
        // "recharge":true,
        // "nerveInner":false}
        feeLog.setBlockHeight(2363795);
        feeLog.setRecharge(true);
        feeLog.setFee(93000000);
        feeLog.setHtgChainId(202);
        feeLog.setHtgTxHash("314363ec7b9fdfab95209fc43633cd33f0048bde4095c412a465d180f8ca0f67");
        feeLog.setBlockHash("000000000000015f3e8f67a1f74459cb3fff0d9f2d633f92e593e2b1f4fd7112");
        feeLog.setNerveInner(false);
        tx.setTxData(feeLog.serialize());
        CoinData coinData = new CoinData();
        tx.setCoinData(coinData.serialize());
        System.out.println(tx.getHash().toHex());
        System.out.println(tx.format(WithdrawalFeeLog.class));
    }

    @Test
    public void listFilterTest() throws Exception {
        List<HeterogeneousChainInfo> list = new ArrayList<>();
        list.add(new HeterogeneousChainInfo(111, "aaa", "0x2804a4296211ab079aed4e12120808f1703841b3"));
        list.add(new HeterogeneousChainInfo(222, "bbb", "0x1EA3FfD41c3ed3e3f788830aAef553F8F691aD8C"));
        list.add(new HeterogeneousChainInfo(333, "ccc", "0x5e7E2AbAa58e108f5B9D5D30A76253Fa8Cb81f9d"));
        list.add(new HeterogeneousChainInfo(444, "ddd", "0x3c2ff003fF996836d39601cA22394A58ca9c473b"));
        list.add(new HeterogeneousChainInfo(201, "btc", "39xsUsh4h1FBPiUTYqaGBi9nJKP4PgFrjV"));
        list.add(new HeterogeneousChainInfo(201, "btc", "2NDu3vcpjyiMgvRjDpQfbyh9uF2McfDJ3NF"));
        list.add(new HeterogeneousChainInfo(201, "btc", "tb1qtskq8773jlhjqm7ad6a8kxhxleznp0nech0wpk0nxt45khuy0vmqwzeumf"));
        list.add(new HeterogeneousChainInfo(555, "eee", "0x6c2039B5fDaE068baD4931E8Cc0b8E3a542937ac"));
        list.stream().filter(
                // Bitcoin chain’s multi-signature address hard upgrade
                info -> info.getChainId() == 111 || info.getChainId() == 222
        ).forEach(info -> {
            try {
                System.out.println(JSONUtils.obj2json(info));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        list.stream().filter(
                // Bitcoin chain’s multi-signature address hard upgrade
                info -> !(info.getChainId() == 201
                        && (info.getMultySignAddress().equals("39xsUsh4h1FBPiUTYqaGBi9nJKP4PgFrjV")
                        || info.getMultySignAddress().equals("2NDu3vcpjyiMgvRjDpQfbyh9uF2McfDJ3NF")))
        ).forEach(info -> {
            try {
                System.out.println(JSONUtils.obj2json(info));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void hashTest() throws IOException {
        String json = "[{\"tx\":{\"type\":43,\"coinData\":\"AhcFAAHm2UCcW99illmYpN3FGguO5RB4vgIAAQAA4fUFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAhOoUo0a44UOwAXBQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4FAAEAoNTNtQIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAITqFKNGuOFDsAAhcFAAEpz8Y3YlWnhFHutLEp7Y6s/6L+7wIAAQAA4fUFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFwUAAY7Ezz7hYLBU4Ku29cgXe57lb6UeBQABAABOzLUCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\",\"txData\":\"KjB4Nzc1MzJmMDI2ZmFhYTk3MDRlM2Q4NmZmOTExMTY2ZTIyNzcwODhhZGYA\",\"time\":1724660513,\"transactionSignature\":\"IQIzYAeLRPeP/hBsDWPhLUug6ogvAwL3UQshjf4Ue7zgLkYwRAIgDDn8vgFBSf3tRUTqYiy8Qm2KP/+DsQPyaJCnTeuBC9ACIDyaKN2nvgdVvBvhf1XgAax8lEI5mA6Ebky/X5TsQMo5\",\"remark\":\"ZDRkZmZiMzQtMTYyYi00NzhlLTg5NzItZDk3OWE4ZTEyY2Qy\",\"hash\":{\"bytes\":\"K9yDA3H9qaqny5qYHZrO0924Ay53aZwaWo76sbw1Fus=\",\"blank\":false},\"blockHeight\":-1,\"status\":\"UNCONFIRM\",\"size\":476,\"coinDataInstance\":{\"from\":[{\"address\":\"BQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4=\",\"assetsChainId\":2,\"assetsId\":1,\"amount\":100000000,\"nonce\":\"TqFKNGuOFDs=\",\"locked\":0},{\"address\":\"BQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":11640100000,\"nonce\":\"TqFKNGuOFDs=\",\"locked\":0}],\"to\":[{\"address\":\"BQABKc/GN2JVp4RR7rSxKe2OrP+i/u8=\",\"assetsChainId\":2,\"assetsId\":1,\"amount\":100000000,\"lockTime\":0},{\"address\":\"BQABjsTPPuFgsFTgq7b1yBd7nuVvpR4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":11640000000,\"lockTime\":0}],\"fromAddressList\":[\"TNVTdTSPUZwRbKqS5EKrGDgtvaESiE4cKTCsP\"],\"fromAddressCount\":1},\"inBlockIndex\":0,\"fee\":100000,\"multiSignTx\":false},\"listInDirector\":[],\"listOutDirector\":[],\"isConfirmedVerifyCount\":0,\"blockHeader\":{\"hash\":{\"bytes\":\"6HawyN12nfIBOHyp871OqcyXBQg+7rILTJIpSCt2z60=\",\"blank\":false},\"preHash\":{\"bytes\":\"frIamCHjLssXNGwQQf1+opaJ9vU1qfXtZ6obtNsf1Us=\",\"blank\":false},\"merkleHash\":{\"bytes\":\"hKpdBiOs8oTGBxuYpfsq5vaiTs/qjQKfnmUspoxYbmQ=\",\"blank\":false},\"time\":1724660513,\"height\":49233659,\"txCount\":2,\"blockSignature\":{\"signData\":{\"signBytes\":\"MEQCIAE32jGHthozAR6JQd+XyL1OJC1aprPAFrj25HFgnnaQAiAO8cVqODedFCDo5meRcJoZnHWqoM4XrB9xHnJ2g9FrEw==\"},\"publicKey\":\"AyAL2onkEWOSqluTnXOebJNYYAwPjBeQ3U8oRZGyhd5w\"},\"extend\":\"c8kRAQQAHzvMZgIAAQABADxkACAp2TDpuO3m03+6kCuxrmHrlZej/o5SeZjBqn/My7NK8Q==\",\"extendsData\":{\"roundIndex\":17942899,\"consensusMemberCount\":4,\"roundStartTime\":1724660511,\"packingIndexOfRound\":2,\"mainVersion\":1,\"blockVersion\":1,\"effectiveRatio\":60,\"continuousIntervalCount\":100,\"stateRoot\":\"Kdkw6bjt5tN/upArsa5h65WXo/6OUnmYwap/zMuzSvE=\",\"seed\":null,\"nextSeedHash\":null},\"stateRoot\":null},\"syncStatusEnum\":\"RUNNING\",\"currentDirector\":true,\"currentJoin\":false,\"currentQuit\":false,\"currentQuitDirector\":null,\"currenVirtualBankTotal\":3,\"retry\":false,\"retryVirtualBankInit\":false,\"prepare\":0,\"timeForMakeUTXO\":0},{\"tx\":{\"type\":43,\"coinData\":\"AhcFAAHm2UCcW99illmYpN3FGguO5RB4vgIAAQAA4fUFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAhajvqxvDUW6wAXBQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4FAAEAoGf3BQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIHUg5+fb3FyUAAhcFAAEpz8Y3YlWnhFHutLEp7Y6s/6L+7wIAAQAA4fUFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFwUAAY7Ezz7hYLBU4Ku29cgXe57lb6UeBQABAADh9QUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\",\"txData\":\"KjB4ZDIwOTU0YzdmNjg0ODgzYjllMTY2MGIxMGE0ZWNjN2M2NWNjMGY3YWoA\",\"time\":1724744133,\"transactionSignature\":\"IQIzYAeLRPeP/hBsDWPhLUug6ogvAwL3UQshjf4Ue7zgLkcwRQIhAPwYc7MC35kyl5vTOeoqEAIfEpQN4brQSj4N2a1a+Ok9AiAeGHg0PDWRzVprp+oCeUr3OtnIRPysXf3baAZkY2j/9w==\",\"remark\":\"OTI4YTNlMTgtMDVkMi00NWVmLWE5YjAtNWY0NDNmNjEyMTk5\",\"hash\":{\"bytes\":\"J0lSqeyd7TeLDyo8g1Z7PXF7YZ84JsvUCvfS41C4kE8=\",\"blank\":false},\"blockHeight\":-1,\"status\":\"UNCONFIRM\",\"size\":477,\"coinDataInstance\":{\"from\":[{\"address\":\"BQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4=\",\"assetsChainId\":2,\"assetsId\":1,\"amount\":100000000,\"nonce\":\"Wo76sbw1Fus=\",\"locked\":0},{\"address\":\"BQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":100100000,\"nonce\":\"HUg5+fb3FyU=\",\"locked\":0}],\"to\":[{\"address\":\"BQABKc/GN2JVp4RR7rSxKe2OrP+i/u8=\",\"assetsChainId\":2,\"assetsId\":1,\"amount\":100000000,\"lockTime\":0},{\"address\":\"BQABjsTPPuFgsFTgq7b1yBd7nuVvpR4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":100000000,\"lockTime\":0}],\"fromAddressList\":[\"TNVTdTSPUZwRbKqS5EKrGDgtvaESiE4cKTCsP\"],\"fromAddressCount\":1},\"inBlockIndex\":0,\"fee\":100000,\"multiSignTx\":false},\"listInDirector\":[],\"listOutDirector\":[],\"isConfirmedVerifyCount\":0,\"blockHeader\":{\"hash\":{\"bytes\":\"OcGTbwRUhSwTlz6QkGn35fh1dp9WkbHyN9dFf8x2HSk=\",\"blank\":false},\"preHash\":{\"bytes\":\"FVePdEQiaDKY60yYT4Au+2lvROnG1uKWN6G+sPKcV2c=\",\"blank\":false},\"merkleHash\":{\"bytes\":\"5tVRdWlpjn/qjiwkvIESKR9c+mcZqcd1tYA1cPAEaHQ=\",\"blank\":false},\"time\":1724744135,\"height\":49275425,\"txCount\":2,\"blockSignature\":{\"signData\":{\"signBytes\":\"MEQCIASUM2cI8HpCUJX6V5PFgR6YoiiGWvzS6+dXat2fhKxCAiBDwaK3PBw742GZkgu2qICb5f1xJuMl1Eb75Pk5TuYqOw==\"},\"publicKey\":\"ArQqACOqOOCI/8CITXjqY4uUODYvFcYQhl377ZcINHdQ\"},\"extend\":\"QfIRAQQAxYHNZgIAAQABADxkACC2U4B65xqJfSu/PwIX1nbyUuXa2MsDaZRkfTOeJtVVKg==\",\"extendsData\":{\"roundIndex\":17953345,\"consensusMemberCount\":4,\"roundStartTime\":1724744133,\"packingIndexOfRound\":2,\"mainVersion\":1,\"blockVersion\":1,\"effectiveRatio\":60,\"continuousIntervalCount\":100,\"stateRoot\":\"tlOAeucaiX0rvz8CF9Z28lLl2tjLA2mUZH0znibVVSo=\",\"seed\":null,\"nextSeedHash\":null},\"stateRoot\":null},\"syncStatusEnum\":\"RUNNING\",\"currentDirector\":true,\"currentJoin\":false,\"currentQuit\":false,\"currentQuitDirector\":null,\"currenVirtualBankTotal\":3,\"retry\":false,\"retryVirtualBankInit\":false,\"prepare\":0,\"timeForMakeUTXO\":0},{\"tx\":{\"type\":43,\"coinData\":\"AhcFAAHm2UCcW99illmYpN3FGguO5RB4vgIAAQAA4fUFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAiRYKNOmb09LwAXBQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4FAAEAoGf3BQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIjT2CUnmcDxgAAhcFAAEpz8Y3YlWnhFHutLEp7Y6s/6L+7wIAAQAA4fUFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFwUAAY7Ezz7hYLBU4Ku29cgXe57lb6UeBQABAADh9QUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\",\"txData\":\"KjB4ZDIwOTU0YzdmNjg0ODgzYjllMTY2MGIxMGE0ZWNjN2M2NWNjMGY3YWYA\",\"time\":1724812866,\"transactionSignature\":\"IQIzYAeLRPeP/hBsDWPhLUug6ogvAwL3UQshjf4Ue7zgLkcwRQIhAJEmUATDciKcfeWpkyzis1cK1jD2w2LFTkXg/V0ifl7lAiBXFeFzIjIdBK337Hs3FCQd2CDFSLUJ4VXST5a05BuR3Q==\",\"remark\":\"OTZlOGVlOWQtYTgxNC00OGU1LTk2NjYtZmM1ZDI2YzAwM2I3\",\"hash\":{\"bytes\":\"mzn2WoTi06txJRjp/duwMCNasxD7tg9hvg07CjXw3mQ=\",\"blank\":false},\"blockHeight\":-1,\"status\":\"UNCONFIRM\",\"size\":477,\"coinDataInstance\":{\"from\":[{\"address\":\"BQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4=\",\"assetsChainId\":2,\"assetsId\":1,\"amount\":100000000,\"nonce\":\"kWCjTpm9PS8=\",\"locked\":0},{\"address\":\"BQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":100100000,\"nonce\":\"jT2CUnmcDxg=\",\"locked\":0}],\"to\":[{\"address\":\"BQABKc/GN2JVp4RR7rSxKe2OrP+i/u8=\",\"assetsChainId\":2,\"assetsId\":1,\"amount\":100000000,\"lockTime\":0},{\"address\":\"BQABjsTPPuFgsFTgq7b1yBd7nuVvpR4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":100000000,\"lockTime\":0}],\"fromAddressList\":[\"TNVTdTSPUZwRbKqS5EKrGDgtvaESiE4cKTCsP\"],\"fromAddressCount\":1},\"inBlockIndex\":0,\"fee\":100000,\"multiSignTx\":false},\"listInDirector\":[],\"listOutDirector\":[],\"isConfirmedVerifyCount\":0,\"blockHeader\":{\"hash\":{\"bytes\":\"yleVj9UBNFU5mqRxx53ag86RjLm/BhRjbmSZwdxSwg4=\",\"blank\":false},\"preHash\":{\"bytes\":\"a8QVptcF+bLDeUbZ+R/cIkqVP/9/4KtyKKAslj3hEio=\",\"blank\":false},\"merkleHash\":{\"bytes\":\"hOxUQwEUxHUEaqkZshSlvyRKXQRs0v/4pDnZVBgpQMw=\",\"blank\":false},\"time\":1724812866,\"height\":49309748,\"txCount\":2,\"blockSignature\":{\"signData\":{\"signBytes\":\"MEUCIQD8V9XAwfd4Xtxs66cXD3f1osPuTBueg94WIIiWJbM3DgIgUakX9vJrmG15V1qePAgN5j5Z/uFhEuKCloX34tTXKGU=\"},\"publicKey\":\"A+ICnd+MAVDYpolGUiPNypSgyEzbWB45rBPKQdJ5wk/1\"},\"extend\":\"yhMSAQQAQI7OZgIAAQABADxkACDtH55bC3GL2gPOLu21aFyGGyvUHPBSX2KekpvbS89LJw==\",\"extendsData\":{\"roundIndex\":17961930,\"consensusMemberCount\":4,\"roundStartTime\":1724812864,\"packingIndexOfRound\":2,\"mainVersion\":1,\"blockVersion\":1,\"effectiveRatio\":60,\"continuousIntervalCount\":100,\"stateRoot\":\"7R+eWwtxi9oDzi7ttWhchhsr1BzwUl9inpKb20vPSyc=\",\"seed\":null,\"nextSeedHash\":null},\"stateRoot\":null},\"syncStatusEnum\":\"RUNNING\",\"currentDirector\":true,\"currentJoin\":false,\"currentQuit\":false,\"currentQuitDirector\":null,\"currenVirtualBankTotal\":3,\"retry\":false,\"retryVirtualBankInit\":false,\"prepare\":0,\"timeForMakeUTXO\":0},{\"tx\":{\"type\":43,\"coinData\":\"AhcFAAHm2UCcW99illmYpN3FGguO5RB4vgUAqwAQJwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAhV6ggGQ7fqUAAXBQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4FAAEAoGf3BQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIUhCgTNwAvc4AAhcFAAEpz8Y3YlWnhFHutLEp7Y6s/6L+7wUAqwAQJwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFwUAAY7Ezz7hYLBU4Ku29cgXe57lb6UeBQABAADh9QUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\",\"txData\":\"PnRiMXA4a2F0a2h3djhjNXJndHBoOTB4eDB5ODdjOXJ0ejYzdXBlY3Jja2hkNTJrNTRuc3YzamNzYXY4dGZjyQA=\",\"time\":1724830902,\"transactionSignature\":\"IQIzYAeLRPeP/hBsDWPhLUug6ogvAwL3UQshjf4Ue7zgLkYwRAIgYvinx/bbumbI9ZZZhSkssjNZUTmnUfwFu40L7QIq8RMCIA/7vGWIBDbD1QrUn+RAxaQnNfyIyeApaXdrtnPkXJ8O\",\"remark\":\"MTlmODQyMGUtN2Y1Yy00ZThjLWE1NmUtMDVhMzM2MjgzMjRk\",\"hash\":{\"bytes\":\"k6W7o++WdRrU8qyTwSuHHoDROzrI1Lo6QaqN4C7xW9o=\",\"blank\":false},\"blockHeight\":-1,\"status\":\"UNCONFIRM\",\"size\":496,\"coinDataInstance\":{\"from\":[{\"address\":\"BQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4=\",\"assetsChainId\":5,\"assetsId\":171,\"amount\":10000,\"nonce\":\"VeoIBkO36lA=\",\"locked\":0},{\"address\":\"BQAB5tlAnFvfYpZZmKTdxRoLjuUQeL4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":100100000,\"nonce\":\"UhCgTNwAvc4=\",\"locked\":0}],\"to\":[{\"address\":\"BQABKc/GN2JVp4RR7rSxKe2OrP+i/u8=\",\"assetsChainId\":5,\"assetsId\":171,\"amount\":10000,\"lockTime\":0},{\"address\":\"BQABjsTPPuFgsFTgq7b1yBd7nuVvpR4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":100000000,\"lockTime\":0}],\"fromAddressList\":[\"TNVTdTSPUZwRbKqS5EKrGDgtvaESiE4cKTCsP\"],\"fromAddressCount\":1},\"inBlockIndex\":0,\"fee\":100000,\"multiSignTx\":false},\"listInDirector\":[],\"listOutDirector\":[],\"isConfirmedVerifyCount\":0,\"blockHeader\":{\"hash\":{\"bytes\":\"9A/YU2zCep/v87bnzpxtNNevP038Udbne3u0r/ru3/Y=\",\"blank\":false},\"preHash\":{\"bytes\":\"mY81GB0oUBNrjZ9GAKgvlJxDh0FWzrcTbVsG+EtlAHw=\",\"blank\":false},\"merkleHash\":{\"bytes\":\"CKtN6uMmqPMHzSbt1cehg+pcJ2PXkyLgiBm0x6xR+6k=\",\"blank\":false},\"time\":1724830903,\"height\":49318737,\"txCount\":2,\"blockSignature\":{\"signData\":{\"signBytes\":\"MEQCIAfGsf5R4mKaXSTkBihUusk6TpPoDcnFWRlnK6n7Ud8NAiBbaZ+3lX2XCi5EaD5jnPXdzmxv84eoe9QLOjnczq4CSg==\"},\"publicKey\":\"Awh4Tj1K/2iiSWSWiHeznSJElZbBx4kTak4l4tt4GYJg\"},\"extend\":\"lhwSAQQAs9TOZgMAAQABADxkACByBvLCst9mMDLAbB2xt9JLlq/qzAxrCgyPHdcITJZffw==\",\"extendsData\":{\"roundIndex\":17964182,\"consensusMemberCount\":4,\"roundStartTime\":1724830899,\"packingIndexOfRound\":3,\"mainVersion\":1,\"blockVersion\":1,\"effectiveRatio\":60,\"continuousIntervalCount\":100,\"stateRoot\":\"cgbywrLfZjAywGwdsbfSS5av6swMawoMjx3XCEyWX38=\",\"seed\":null,\"nextSeedHash\":null},\"stateRoot\":null},\"syncStatusEnum\":\"RUNNING\",\"currentDirector\":true,\"currentJoin\":false,\"currentQuit\":false,\"currentQuitDirector\":null,\"currenVirtualBankTotal\":3,\"retry\":false,\"retryVirtualBankInit\":false,\"prepare\":0,\"timeForMakeUTXO\":0},{\"tx\":{\"type\":43,\"coinData\":\"ARcFAAEQmOsvE5N5DU3tzGUxK7DPsIya/wUAAQAAh+1lAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAi0izq9xgAa/QACFwUAASnPxjdiVaeEUe60sSntjqz/ov7vBQABAGBa9AUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAXBQABjsTPPuFgsFTgq7b1yBd7nuVvpR4FAAEAAKb3XwIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==\",\"txData\":\"KjB4MjgwNGE0Mjk2MjExYWIwNzlhZWQ0ZTEyMTIwODA4ZjE3MDM4NDFiM2YA\",\"time\":1725611271,\"transactionSignature\":\"IQKDBg4eK4wYvIYT92w2G1Dbc1Agij2oJSOi33fV94Zs9kcwRQIhAI5G3W+Vy2EDzsMgHMocyC8M42wUj4S5gPmOOlHnSNV9AiAeqjQNN0uZywm51pvVbw+1ClCqqSfIzv+Tb/ly9CV9Qw==\",\"remark\":\"b3JkZXJJZDogMWFhMDk0NzgtMTY1OC00MjcxLThlMDQtMzk5Zjc4N2Y1OTNiLCBmcm9tIE5FUlZFOlROVlRkVFNQTGJoUUV3NGhoTGMyRW5yNVl0VGhlQWpnOHlEc1YsIHRvIEJTQzoweDI4MDRBNDI5NjIxMUFiMDc5QUVENGUxMjEyMDgwOEYxNzAzODQxYjM=\",\"hash\":{\"bytes\":\"suIOL2l1L1IVVMzccpJsAA2GK44J+ow1D6G1Iplqhog=\",\"blank\":false},\"blockHeight\":-1,\"status\":\"UNCONFIRM\",\"size\":515,\"coinDataInstance\":{\"from\":[{\"address\":\"BQABEJjrLxOTeQ1N7cxlMSuwz7CMmv8=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":10300000000,\"nonce\":\"tIs6vcYAGv0=\",\"locked\":0}],\"to\":[{\"address\":\"BQABKc/GN2JVp4RR7rSxKe2OrP+i/u8=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":99900000,\"lockTime\":0},{\"address\":\"BQABjsTPPuFgsFTgq7b1yBd7nuVvpR4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":10200000000,\"lockTime\":0}],\"fromAddressList\":[\"TNVTdTSPFPov2xBAMRSnnfEwXjEDTVAASFEh6\"],\"fromAddressCount\":1},\"inBlockIndex\":0,\"fee\":100000,\"multiSignTx\":false},\"listInDirector\":[],\"listOutDirector\":[],\"isConfirmedVerifyCount\":0,\"blockHeader\":{\"hash\":{\"bytes\":\"ISiOk3qvgH/bIjz7KuDuFpxHwgtu1T558vN1POL/XIg=\",\"blank\":false},\"preHash\":{\"bytes\":\"+MDdPZUh+yuOQCAPjd1gLgUDx0bpbGwr2r390k/TPZo=\",\"blank\":false},\"merkleHash\":{\"bytes\":\"cV1Dh+s9YhEzRXYvtzr8yixJiZbUCU9DIGNDCbMqMfo=\",\"blank\":false},\"time\":1725611272,\"height\":49708528,\"txCount\":4,\"blockSignature\":{\"signData\":{\"signBytes\":\"MEQCIFU13StL8vJ9m4taU8hh+Ymgdr3qqdy/6A0PYyET/P1tAiBQ/un0K98QDqhgvRCBlGl7Qn8/1VbpkgK7sri+LWLP4Q==\"},\"publicKey\":\"Awh4Tj1K/2iiSWSWiHeznSJElZbBx4kTak4l4tt4GYJg\"},\"extend\":\"apkTAQQACL3aZgEAAQABADxkACBlVijlOsBfQXN67OXsM+jVfE5T030z0k+oHtY6C2H5pg==\",\"extendsData\":{\"roundIndex\":18061674,\"consensusMemberCount\":4,\"roundStartTime\":1725611272,\"packingIndexOfRound\":1,\"mainVersion\":1,\"blockVersion\":1,\"effectiveRatio\":60,\"continuousIntervalCount\":100,\"stateRoot\":\"ZVYo5TrAX0Fzeuzl7DPo1XxOU9N9M9JPqB7WOgth+aY=\",\"seed\":null,\"nextSeedHash\":null},\"stateRoot\":null},\"syncStatusEnum\":\"RUNNING\",\"currentDirector\":true,\"currentJoin\":false,\"currentQuit\":false,\"currentQuitDirector\":null,\"currenVirtualBankTotal\":3,\"retry\":false,\"retryVirtualBankInit\":false,\"prepare\":0,\"timeForMakeUTXO\":0},{\"tx\":{\"type\":43,\"coinData\":\"ARcFAAEQmOsvE5N5DU3tzGUxK7DPsIya/wUAAQAAh+1lAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgPobUimWqGiAACFwUAASnPxjdiVaeEUe60sSntjqz/ov7vBQABAGBa9AUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAXBQABjsTPPuFgsFTgq7b1yBd7nuVvpR4FAAEAAKb3XwIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==\",\"txData\":\"KjB4MjgwNGE0Mjk2MjExYWIwNzlhZWQ0ZTEyMTIwODA4ZjE3MDM4NDFiM2YA\",\"time\":1725618939,\"transactionSignature\":\"IQKDBg4eK4wYvIYT92w2G1Dbc1Agij2oJSOi33fV94Zs9kcwRQIhAJ8EbAZXjoROMAU7nQJR7X2mgKGOk2v2yw0zq15HSofcAiAX3V9mv1oTrbcpGRGjGtEVd/navFn3gP77/k2CXPOzvw==\",\"remark\":\"b3JkZXJJZDogMWFhMDk0NzgtMTY1OC00MjcxLThlMDQtMzk5Zjc4N2Y1OTNiLCBmcm9tIE5FUlZFOlROVlRkVFNQTGJoUUV3NGhoTGMyRW5yNVl0VGhlQWpnOHlEc1YsIHRvIEJTQzoweDI4MDRBNDI5NjIxMUFiMDc5QUVENGUxMjEyMDgwOEYxNzAzODQxYjM=\",\"hash\":{\"bytes\":\"dZcdAqk047KyJgCYM6pWHilCRLp1fnUago8t1jk1Rkk=\",\"blank\":false},\"blockHeight\":-1,\"status\":\"UNCONFIRM\",\"size\":515,\"coinDataInstance\":{\"from\":[{\"address\":\"BQABEJjrLxOTeQ1N7cxlMSuwz7CMmv8=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":10300000000,\"nonce\":\"D6G1Iplqhog=\",\"locked\":0}],\"to\":[{\"address\":\"BQABKc/GN2JVp4RR7rSxKe2OrP+i/u8=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":99900000,\"lockTime\":0},{\"address\":\"BQABjsTPPuFgsFTgq7b1yBd7nuVvpR4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":10200000000,\"lockTime\":0}],\"fromAddressList\":[\"TNVTdTSPFPov2xBAMRSnnfEwXjEDTVAASFEh6\"],\"fromAddressCount\":1},\"inBlockIndex\":0,\"fee\":100000,\"multiSignTx\":false},\"listInDirector\":[],\"listOutDirector\":[],\"isConfirmedVerifyCount\":0,\"blockHeader\":{\"hash\":{\"bytes\":\"+ksXfY8CKGMSRK/KoCIURPG1na2YdHs6nG7/anJU23o=\",\"blank\":false},\"preHash\":{\"bytes\":\"45oNwFECZ83RIqdwJPFEBhTpOrPEnYQGa/NbksHFkbw=\",\"blank\":false},\"merkleHash\":{\"bytes\":\"pBMz51PSoLHJtVqbrqc8NzPYxiNfTbjXSJcGawJ0Gls=\",\"blank\":false},\"time\":1725618942,\"height\":49712343,\"txCount\":4,\"blockSignature\":{\"signData\":{\"signBytes\":\"MEUCIQCQEtiOAtuV4NNfcSI30dyDlt27S+ju/QhFAxv9o8IVGAIgaUP4a0SBXByEIZR8BFcKBEODtNV5TJpl8//OIBEEu/Y=\"},\"publicKey\":\"A+ICnd+MAVDYpolGUiPNypSgyEzbWB45rBPKQdJ5wk/1\"},\"extend\":\"JZ0TAQQA9draZgQAAQABADxkACAMYVv4oIaiaJSfcgPLV1a3Bcc5yv+4FTdkNz80PclNiw==\",\"extendsData\":{\"roundIndex\":18062629,\"consensusMemberCount\":4,\"roundStartTime\":1725618933,\"packingIndexOfRound\":4,\"mainVersion\":1,\"blockVersion\":1,\"effectiveRatio\":60,\"continuousIntervalCount\":100,\"stateRoot\":\"DGFb+KCGomiUn3IDy1dWtwXHOcr/uBU3ZDc/ND3JTYs=\",\"seed\":null,\"nextSeedHash\":null},\"stateRoot\":null},\"syncStatusEnum\":\"RUNNING\",\"currentDirector\":true,\"currentJoin\":false,\"currentQuit\":false,\"currentQuitDirector\":null,\"currenVirtualBankTotal\":3,\"retry\":false,\"retryVirtualBankInit\":false,\"prepare\":0,\"timeForMakeUTXO\":0},{\"tx\":{\"type\":43,\"coinData\":\"ARcFAAEQmOsvE5N5DU3tzGUxK7DPsIya/wUAAQAAXXhQAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAiCjy3WOTVGSQACFwUAASnPxjdiVaeEUe60sSntjqz/ov7vBQABAGBa9AUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAXBQABjsTPPuFgsFTgq7b1yBd7nuVvpR4FAAEAAHyCSgIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==\",\"txData\":\"KjB4MjgwNGE0Mjk2MjExYWIwNzlhZWQ0ZTEyMTIwODA4ZjE3MDM4NDFiM2YA\",\"time\":1726123437,\"transactionSignature\":\"IQKDBg4eK4wYvIYT92w2G1Dbc1Agij2oJSOi33fV94Zs9kcwRQIhANYYa+DrLRCTl6Nf9XpAWXhwRaxbnugAas2nXWlwMCdaAiBsj4BMAvH5fg5Q8AQVmYeWpLQiK9bhLzNid68rvHDmrw==\",\"remark\":\"b3JkZXJJZDogMWFhMDk0NzgtMTY1OC00MjcxLThlMDQtMzk5Zjc4N2Y1OTNiLCBmcm9tIE5FUlZFOlROVlRkVFNQTGJoUUV3NGhoTGMyRW5yNVl0VGhlQWpnOHlEc1YsIHRvIEJTQzoweDI4MDRBNDI5NjIxMUFiMDc5QUVENGUxMjEyMDgwOEYxNzAzODQxYjM=\",\"hash\":{\"bytes\":\"zbpXF/irDZ6ibTYkA0Uj+DscM6icjjdLlbbob4HgAOI=\",\"blank\":false},\"blockHeight\":-1,\"status\":\"UNCONFIRM\",\"size\":515,\"coinDataInstance\":{\"from\":[{\"address\":\"BQABEJjrLxOTeQ1N7cxlMSuwz7CMmv8=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":9940000000,\"nonce\":\"go8t1jk1Rkk=\",\"locked\":0}],\"to\":[{\"address\":\"BQABKc/GN2JVp4RR7rSxKe2OrP+i/u8=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":99900000,\"lockTime\":0},{\"address\":\"BQABjsTPPuFgsFTgq7b1yBd7nuVvpR4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":9840000000,\"lockTime\":0}],\"fromAddressList\":[\"TNVTdTSPFPov2xBAMRSnnfEwXjEDTVAASFEh6\"],\"fromAddressCount\":1},\"inBlockIndex\":0,\"fee\":100000,\"multiSignTx\":false},\"listInDirector\":[],\"listOutDirector\":[],\"isConfirmedVerifyCount\":0,\"blockHeader\":{\"hash\":{\"bytes\":\"JTz6zgEpRYUo3QjJSKjyiUXYp8iUf8Ndnn4MysmSpKw=\",\"blank\":false},\"preHash\":{\"bytes\":\"mNSPD/BC3cAQHcFQj1t2qcx2lR96dq8AYKG3kEHHOuc=\",\"blank\":false},\"merkleHash\":{\"bytes\":\"sGn+3xHIdlsHzUWUz4cR++MC4B+vzmgxEnM1Vxrv8fk=\",\"blank\":false},\"time\":1726123437,\"height\":49964187,\"txCount\":2,\"blockSignature\":{\"signData\":{\"signBytes\":\"MEUCIQCWtO0YTGU4im2C54I8b3rNUVwGm7uRnvIpURjxUe2IqQIgJUIZ4HoHiR+LhEPho1wAbXhCtM+Cos13a+VAM1VRmKg=\"},\"publicKey\":\"ArQqACOqOOCI/8CITXjqY4uUODYvFcYQhl377ZcINHdQ\"},\"extend\":\"XZMUAQQAp43iZgQAAQABADxkACAMYVv4oIaiaJSfcgPLV1a3Bcc5yv+4FTdkNz80PclNiw==\",\"extendsData\":{\"roundIndex\":18125661,\"consensusMemberCount\":4,\"roundStartTime\":1726123431,\"packingIndexOfRound\":4,\"mainVersion\":1,\"blockVersion\":1,\"effectiveRatio\":60,\"continuousIntervalCount\":100,\"stateRoot\":\"DGFb+KCGomiUn3IDy1dWtwXHOcr/uBU3ZDc/ND3JTYs=\",\"seed\":null,\"nextSeedHash\":null},\"stateRoot\":null},\"syncStatusEnum\":\"RUNNING\",\"currentDirector\":true,\"currentJoin\":false,\"currentQuit\":false,\"currentQuitDirector\":null,\"currenVirtualBankTotal\":3,\"retry\":false,\"retryVirtualBankInit\":false,\"prepare\":0,\"timeForMakeUTXO\":0}]";
        List<Map> list = JSONUtils.json2list(json, Map.class);
        list.forEach(m -> {
            Map map = (Map) m.get("tx");
            int type = (int) map.get("type");
            Map hashMap = (Map) map.get("hash");
            String hashBS64 = hashMap.get("bytes").toString();
            System.out.println(type + ": " + HexUtil.encode(Base64.getDecoder().decode(hashBS64)));
        });
    }
    @Test
    public void pubTest() {
        String pub = "222222222222222222222222222222222222222222222222222222222222222222";
        System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pub, 5)));
    }

    @Test
    public void rpcFromAssetSystemTest() {
        do {
            try {
                this._rpcFromAssetSystem();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while (true);
    }

    @Test
    public void strTest() {
        Integer a = 1;
        Integer b = 8;
        System.out.println(a + 1);
        System.out.println(b + 1);
    }

    private void _rpcFromAssetSystem() throws Exception {
        do {
            String result = HttpClientUtil.get(String.format("https://assets.nabox.io/api/chainapi"));
            if (StringUtils.isNotBlank(result)) {
                List<Map> list = JSONUtils.json2list(result, Map.class);
                Map<Long, Map> map = list.stream().collect(Collectors.toMap(m -> Long.valueOf(m.get("nativeId").toString()), Function.identity()));
                ConverterContext.HTG_RPC_CHECK_MAP = map;
            }

            // Force updates from third-party systemsrpc
            Map<Long, Map> rpcCheckMap = ConverterContext.HTG_RPC_CHECK_MAP;
            Map<String, Object> resultMap = rpcCheckMap.get(10000);
            if (resultMap == null) {
                break;
            }
            Integer _version = (Integer) resultMap.get("rpcVersion");
            if (_version == null) {
                break;
            }
            if (rpcVersion == -1) {
                rpcVersion = _version.intValue();
                System.out.println("flag1");
                break;
            }
            if (rpcVersion == _version.intValue()){
                System.out.println("flag2");
                break;
            }
            if (_version.intValue() > rpcVersion){
                // findversionChange, switchrpc
                Integer _index = (Integer) resultMap.get("index");
                if (_index == null) {
                    break;
                }
                apiUrl = (String) resultMap.get("extend" + (_index + 1));
                if (StringUtils.isBlank(apiUrl)) {
                    break;
                }
                rpcVersion = _version.intValue();
                System.out.println("flag3");
                break;
            }
        } while (false);
        System.out.println(rpcVersion);
        System.out.println(apiUrl);
        System.out.println("------------------");
    }
}
