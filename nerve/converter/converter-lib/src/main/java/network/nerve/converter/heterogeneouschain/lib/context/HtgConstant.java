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

package network.nerve.converter.heterogeneouschain.lib.context;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: PierreLuo
 * @date: 2021-03-22
 */
public interface HtgConstant {

    byte VERSION_MULTY_SIGN_LATEST = 3;
    String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";
    String PUBLIC_KEY_UNCOMPRESSED_PREFIX = "04";
    String HEX_PREFIX = "0x";
    String EMPTY_STRING = "";

    int RESEND_TIME = 50;
    int DEFAULT_INTERVAL_WAITTING = 5;
    int MAX_MANAGERS = 15;

    String METHOD_HASH_CREATEORSIGNWITHDRAW = "0xab6c2b10";
    String METHOD_HASH_CREATEORSIGNMANAGERCHANGE = "0x00719226";
    String METHOD_HASH_CREATEORSIGNUPGRADE = "0x408e8b7a";
    String METHOD_HASH_CROSS_OUT = "0x0889d1f0";
    String METHOD_HASH_CROSS_OUT_II = "0x38615bb0";
    String METHOD_HASH_TRANSFER = "0xa9059cbb";
    String METHOD_HASH_TRANSFER_FROM = "0x23b872dd";
    String METHOD_HASH_ONE_CLICK_CROSS_CHAIN = "0x7d02ce34";
    String METHOD_HASH_ADD_FEE_CROSS_CHAIN = "0x0929f4c6";

    String METHOD_CREATE_OR_SIGN_WITHDRAW = "createOrSignWithdraw";
    String METHOD_CREATE_OR_SIGN_MANAGERCHANGE = "createOrSignManagerChange";
    String METHOD_CREATE_OR_SIGN_UPGRADE = "createOrSignUpgrade";
    String METHOD_CROSS_OUT = "crossOut";
    String METHOD_CROSS_OUT_II = "crossOutII";
    String METHOD_VIEW_IS_MINTER_ERC20 = "isMinterERC20";
    String METHOD_ONE_CLICK_CROSS_CHAIN = "oneClickCrossChain";
    String METHOD_ADD_FEE_CROSS_CHAIN = "addFeeCrossChain";

    String METHOD_VIEW_ALL_MANAGERS_TRANSACTION = "allManagers";
    String METHOD_VIEW_IS_COMPLETED_TRANSACTION = "isCompletedTx";
    String METHOD_VIEW_PENDING_WITHDRAW = "pendingWithdrawTx";
    String METHOD_VIEW_PENDING_MANAGERCHANGE = "pendingManagerChangeTx";
    String METHOD_VIEW_ERC20_NAME = "name";
    String METHOD_VIEW_ERC20_SYMBOL = "symbol";
    String METHOD_VIEW_ERC20_DECIMALS = "decimals";

    String EVENT_HASH_HT_DEPOSIT_FUNDS = "0xd241e73300212f6df233a8e6d3146b88a9d4964e06621d54b5ff6afeba7b1b88";
    String EVENT_HASH_ERC20_TRANSFER = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
    String EVENT_HASH_TRANSFERFUNDS = "0xc95f8b91b103304386b955ef73fadac189f8ad66b33369b6c34a17a60db7bd0a";

    String EVENT_HASH_TRANSACTION_WITHDRAW_COMPLETED = "0x8ed8b1f0dd3babfdf1477ba2b27a5b0d2f1c9148448fd22cf2c75e658293c7b1";
    String EVENT_HASH_TRANSACTION_MANAGER_CHANGE_COMPLETED = "0xac9b82db4e104d515319a481096bfd91a4f40ee10837d5a2c8d51b9a03dc48ae";
    String EVENT_HASH_TRANSACTION_UPGRADE_COMPLETED = "0x5e06c4b22547d430736ce834764dbfee08f1c4cf7ae3d53178aa56effa593ed0";
    String EVENT_HASH_CROSS_OUT_FUNDS = "0x5ddf9724d8fe5d9e12499be2867f93d41a582733dcd65f74a486ad7e30667146";
    String EVENT_HASH_CROSS_OUT_II_FUNDS = "0x692e6a6e27573f2a2a757e34cb16ae101c5fca8834f9b8a6cdbcf64b8450d870";
    String EVENT_HASH_UNKNOWN_ON_POLYGON = "0x4dfe1bbbcf077ddc3e01291eea2d5c70c2b422b415d95645b9adcfd678cb1d63";
    String EVENT_HASH_UNKNOWN_ON_REI = "0x873c82cd37aaacdcf736cbb6beefc8da36d474b65ad23aaa1b1c6fbd875f7076";
    Set<String> TRANSACTION_COMPLETED_TOPICS = Set.of(
            EVENT_HASH_TRANSACTION_WITHDRAW_COMPLETED,
            EVENT_HASH_TRANSACTION_MANAGER_CHANGE_COMPLETED,
            EVENT_HASH_TRANSACTION_UPGRADE_COMPLETED
    );

    List<TypeReference<Type>> INPUT_WITHDRAW = Utils.convert(
            List.of(
                    new TypeReference<Utf8String>(){},
                    new TypeReference<Address>(){},
                    new TypeReference<Uint256>(){},
                    new TypeReference<Bool>(){},
                    new TypeReference<Address>(){},
                    new TypeReference<DynamicBytes>(){}
            )
    );

    List<TypeReference<Type>> INPUT_CHANGE = Utils.convert(
            List.of(
                    new TypeReference<Utf8String>(){},
                    new TypeReference<DynamicArray<Address>>(){},
                    new TypeReference<DynamicArray<Address>>(){},
                    new TypeReference<Uint8>(){},
                    new TypeReference<DynamicBytes>(){}
            )
    );

    List<TypeReference<Type>> INPUT_UPGRADE = Utils.convert(
            List.of(
                    new TypeReference<Utf8String>(){},
                    new TypeReference<Address>(){},
                    new TypeReference<DynamicBytes>(){}
            )
    );

    List<TypeReference<Type>> INPUT_CROSS_OUT = Utils.convert(
            List.of(
                    new TypeReference<Utf8String>(){},
                    new TypeReference<Uint256>(){},
                    new TypeReference<Address>(){}
            )
    );

    List<TypeReference<Type>> INPUT_CROSS_OUT_II = Utils.convert(
            List.of(
                    new TypeReference<Utf8String>(){},
                    new TypeReference<Uint256>(){},
                    new TypeReference<Address>(){},
                    new TypeReference<DynamicBytes>(){}
            )
    );

    List<TypeReference<Type>> INPUT_ERC20_TRANSFER = Utils.convert(
            List.of(
                    new TypeReference<Address>(){},
                    new TypeReference<Uint256>(){}
            )
    );
    List<TypeReference<Type>> INPUT_ERC20_TRANSFER_FROM = Utils.convert(
            List.of(
                    new TypeReference<Address>(){},
                    new TypeReference<Address>(){},
                    new TypeReference<Uint256>(){}
            )
    );

    List<TypeReference<Type>> INPUT_ONE_CLICK_CROSS_CHAIN = Utils.convert(
            List.of(
                    new TypeReference<Uint256>(){},
                    new TypeReference<Uint256>(){},
                    new TypeReference<Utf8String>(){},
                    new TypeReference<Uint256>(){},
                    new TypeReference<Utf8String>(){},
                    new TypeReference<DynamicBytes>(){}
            )
    );

    List<TypeReference<Type>> INPUT_ADD_FEE_CROSS_CHAIN = Utils.convert(
            List.of(
                    new TypeReference<Utf8String>(){},
                    new TypeReference<DynamicBytes>(){}
            )
    );

    Event EVENT_TRANSACTION_WITHDRAW_COMPLETED = new Event("TxWithdrawCompleted",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Utf8String>(false) {}
            ));
    Event EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED = new Event("TxManagerChangeCompleted",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Utf8String>(false) {}
            ));
    Event EVENT_TRANSACTION_UPGRADE_COMPLETED = new Event("TxUpgradeCompleted",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Utf8String>(false) {}
            ));
    Event EVENT_CROSS_OUT_FUNDS = new Event("CrossOutFunds",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(false) {},
                    new TypeReference<Utf8String>(false) {},
                    new TypeReference<Uint>(false) {},
                    new TypeReference<Address>(false) {}
            ));
    Event EVENT_CROSS_OUT_II_FUNDS = new Event("CrossOutIIFunds",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(false) {},
                    new TypeReference<Utf8String>(false) {},
                    new TypeReference<Uint>(false) {},
                    new TypeReference<Address>(false) {},
                    new TypeReference<Uint>(false) {},
                    new TypeReference<DynamicBytes>(false) {}
            ));

    Event EVENT_DEPOSIT_FUNDS = new Event("DepositFunds",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(false) {},
                    new TypeReference<Uint>(false) {}
            ));
    Event EVENT_TRANSFER_FUNDS = new Event("TransferFunds",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(false) {},
                    new TypeReference<Uint>(false) {}
                    //new TypeReference<Uint>(false) {}
            ));

    byte[] EMPTY_BYTE = new byte[]{1};


    long HOURS_3 = 3L * 60L * 60L * 1000L;
    long MINUTES_20 = 20 * 60 * 1000L;
    long MINUTES_5 = 5 * 60 * 1000L;
    long MINUTES_3 = 3 * 60 * 1000L;
    long MINUTES_2 = 2 * 60 * 1000L;
    long MINUTES_1 = 1 * 60 * 1000L;
    long SECOND_30 = 30 * 1000L;
    long SECOND_20 = 20 * 1000L;
    long SECOND_10 = 10 * 1000L;
    long WAITING_MINUTES = MINUTES_2;

    BigDecimal NUMBER_1_DOT_1 = new BigDecimal("1.1");
    BigDecimal NUMBER_0_DOT_1 = new BigDecimal("0.1");
    BigDecimal BD_20K = BigDecimal.valueOf(20000L);

    Long ROLLBACK_NUMER = 100L;

    BigInteger GWEI_DOT_01 = BigInteger.valueOf(1L).multiply(BigInteger.TEN.pow(7));
    BigInteger GWEI_DOT_1 = BigInteger.valueOf(1L).multiply(BigInteger.TEN.pow(8));
    BigInteger GWEI_DOT_3 = BigInteger.valueOf(1L).multiply(BigInteger.TEN.pow(8));
    BigInteger GWEI_1 = BigInteger.valueOf(1L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_2 = BigInteger.valueOf(2L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_3 = BigInteger.valueOf(3L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_5 = BigInteger.valueOf(5L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_10 = BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_11 = BigInteger.valueOf(11L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_20 = BigInteger.valueOf(20L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_30 = BigInteger.valueOf(20L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_100 = BigInteger.valueOf(100L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_200 = BigInteger.valueOf(200L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_300 = BigInteger.valueOf(300L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_750 = BigInteger.valueOf(750L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_1000 = BigInteger.valueOf(1000L).multiply(BigInteger.TEN.pow(9));
    BigInteger GWEI_5000 = BigInteger.valueOf(5000L).multiply(BigInteger.TEN.pow(9));
    BigInteger HIGH_GAS_PRICE = GWEI_200;
    BigInteger MAX_HTG_GAS_PRICE = GWEI_300;

    ErrorCode TX_ALREADY_EXISTS_0 = ErrorCode.init(ModuleE.TX.getPrefix() + "_0013");
    ErrorCode TX_ALREADY_EXISTS_1 = ErrorCode.init(ModuleE.CV.getPrefix() + "_0040");
    ErrorCode TX_ALREADY_EXISTS_2 = ErrorCode.init(ModuleE.CV.getPrefix() + "_0048");
    byte[] ZERO_BYTES = new byte[]{0};
}
