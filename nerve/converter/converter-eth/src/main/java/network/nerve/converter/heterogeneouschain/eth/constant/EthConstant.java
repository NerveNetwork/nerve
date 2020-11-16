/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
package network.nerve.converter.heterogeneouschain.eth.constant;

import io.nuls.core.constant.ErrorCode;
import io.nuls.core.rpc.model.ModuleE;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
public interface EthConstant {

    byte VERSION = 1;

    String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";
    String PUBLIC_KEY_UNCOMPRESSED_PREFIX = "04";
    String HEX_PREFIX = "0x";
    String ETH_SYMBOL = "ETH";
    int ETH_CHAIN_ID = 101;
    int ETH_ASSET_ID = 1;
    int ETH_DECIMALS = 18;
    int RESEND_TIME = 30;
    int DEFAULT_INTERVAL_WAITTING = 5;
    int MAX_MANAGERS = 15;
    String ETH_RECOVERY_I = "recovery1";
    String ETH_RECOVERY_II = "recovery2";
    String ETH_ERC20_STANDARD_FILE = "ethTokens.json";

    String METHOD_HASH_CREATEORSIGNWITHDRAW = "0x46b4c37e";
    String METHOD_HASH_CREATEORSIGNMANAGERCHANGE = "0xbdeaa8ba";
    String METHOD_HASH_CREATEORSIGNUPGRADE = "0x976cd397";

    String METHOD_CREATE_OR_SIGN_WITHDRAW = "createOrSignWithdraw";
    String METHOD_CREATE_OR_SIGN_MANAGERCHANGE = "createOrSignManagerChange";
    String METHOD_CREATE_OR_SIGN_UPGRADE = "createOrSignUpgrade";

    String METHOD_VIEW_ALL_MANAGERS_TRANSACTION = "allManagers";
    String METHOD_VIEW_IS_COMPLETED_TRANSACTION = "isCompletedTx";
    String METHOD_VIEW_PENDING_WITHDRAW = "pendingWithdrawTx";
    String METHOD_VIEW_PENDING_MANAGERCHANGE = "pendingManagerChangeTx";
    String METHOD_VIEW_ERC20_NAME = "name";
    String METHOD_VIEW_ERC20_SYMBOL = "symbol";
    String METHOD_VIEW_ERC20_DECIMALS = "decimals";

    String EVENT_HASH_ETH_DEPOSIT_FUNDS = "0xd241e73300212f6df233a8e6d3146b88a9d4964e06621d54b5ff6afeba7b1b88";
    String EVENT_HASH_ERC20_TRANSFER = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
    String EVENT_HASH_TRANSFERFUNDS = "0xc95f8b91b103304386b955ef73fadac189f8ad66b33369b6c34a17a60db7bd0a";
    String EVENT_HASH_TRANSACTION_WITHDRAW_COMPLETED = "0xfd23ffb098649ce6213f56e0e6cf43f10192958241ce25558500a2d4ec4a60c0";
    String EVENT_HASH_TRANSACTION_MANAGER_CHANGE_COMPLETED = "0x0b5bbdd1fbc5b7677ad4d5aba8707a4441f3954ddfc6679037ffceef8cb983b8";
    String EVENT_HASH_TRANSACTION_UPGRADE_COMPLETED = "0xd2ad4d272c593b1e89ec9d9152c5593d5dc34d5fc3daf4d603cd93cdcc897141";

    List<TypeReference<Type>> INPUT_WITHDRAW = Utils.convert(
            List.of(
                    new TypeReference<Utf8String>(){},
                    new TypeReference<Address>(){},
                    new TypeReference<Uint256>(){},
                    new TypeReference<Bool>(){},
                    new TypeReference<Address>(){}
            )
    );

    List<TypeReference<Type>> INPUT_CHANGE = Utils.convert(
            List.of(
                    new TypeReference<Utf8String>(){},
                    new TypeReference<DynamicArray<Address>>(){},
                    new TypeReference<DynamicArray<Address>>(){},
                    new TypeReference<Uint8>(){}
            )
    );

    List<TypeReference<Type>> INPUT_UPGRADE = Utils.convert(
            List.of(
                    new TypeReference<Utf8String>(){}
            )
    );

    Event EVENT_TRANSACTION_WITHDRAW_COMPLETED = new Event("TxWithdrawCompleted",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<DynamicArray<Address>>(true) {},
                    new TypeReference<Utf8String>(false) {}
            ));
    Event EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED = new Event("TxManagerChangeCompleted",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<DynamicArray<Address>>(true) {},
                    new TypeReference<Utf8String>(false) {}
            ));
    Event EVENT_TRANSACTION_UPGRADE_COMPLETED = new Event("TxUpgradeCompleted",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<DynamicArray<Address>>(true) {},
                    new TypeReference<Utf8String>(false) {}
            ));
    Event EVENT_DEPOSIT_FUNDS = new Event("DepositFunds",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},
                    new TypeReference<Uint>(false) {}
            ));
    Event EVENT_TRANSFER_FUNDS = new Event("TransferFunds",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},
                    new TypeReference<Uint>(false) {}
                    //new TypeReference<Uint>(false) {}
            ));

    byte[] EMPTY_BYTE = new byte[]{1};
    BigInteger ETH_GAS_LIMIT_OF_ETH = BigInteger.valueOf(21000L);
    BigInteger ETH_GAS_LIMIT_OF_USDT = BigInteger.valueOf(60000L);
    BigInteger ETH_GAS_LIMIT_OF_MULTY_SIGN = BigInteger.valueOf(400000L);
    BigInteger ETH_GAS_LIMIT_OF_MULTY_SIGN_CHANGE = BigInteger.valueOf(600000L);
    BigInteger ETH_GAS_LIMIT_OF_MULTY_SIGN_CHANGE_MAX = BigInteger.valueOf(700000L);
    BigInteger ETH_GAS_LIMIT_OF_MULTY_SIGN_RECOVERY = BigInteger.valueOf(800000L);
    BigInteger ETH_GAS_LIMIT_OF_MULTY_SIGN_CHANGE_THRESHOLD = BigInteger.valueOf(610000L);
    BigInteger ETH_ESTIMATE_GAS = BigInteger.valueOf(1000000L);
    BigDecimal NUMBER_1_DOT_1 = new BigDecimal("1.1");
    BigDecimal NUMBER_1_DOT_2 = new BigDecimal("1.2");
    BigInteger BASE_GAS_LIMIT = BigInteger.valueOf(50000L);

    Long ROLLBACK_NUMER = 100L;

    long HOURS_3 = 3L * 60L * 60L * 1000L;
    long MINUTES_20 = 20 * 60 * 1000L;
    long MINUTES_10 = 10 * 60 * 1000L;
    long MINUTES_5 = 5 * 60 * 1000L;
    long MINUTES_3 = 3 * 60 * 1000L;
    long MINUTES_2 = 2 * 60 * 1000L;
    long MINUTES_1 = 1 * 60 * 1000L;
    long SECOND_30 = 30 * 1000L;
    long SECOND_20 = 20 * 1000L;
    long SECOND_10 = 10 * 1000L;
    BigInteger GWEI_2 = BigInteger.valueOf(2L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_3 = BigInteger.valueOf(3L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_100 = BigInteger.valueOf(100L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_200 = BigInteger.valueOf(200L).multiply(BigInteger.TEN.pow(9));
    BigInteger HIGH_GAS_PRICE = GWEI_200;

    ErrorCode TX_ALREADY_EXISTS_0 = ErrorCode.init(ModuleE.TX.getPrefix() + "_0013");
    ErrorCode TX_ALREADY_EXISTS_1 = ErrorCode.init(ModuleE.CV.getPrefix() + "_0040");
    ErrorCode TX_ALREADY_EXISTS_2 = ErrorCode.init(ModuleE.CV.getPrefix() + "_0048");
}
