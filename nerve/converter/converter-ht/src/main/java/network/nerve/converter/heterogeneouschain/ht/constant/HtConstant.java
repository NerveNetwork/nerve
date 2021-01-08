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
package network.nerve.converter.heterogeneouschain.ht.constant;

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
public interface HtConstant {

    byte VERSION = 2;

    String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";
    String PUBLIC_KEY_UNCOMPRESSED_PREFIX = "04";
    String HEX_PREFIX = "0x";
    String HT_SYMBOL = "HT";
    int HT_CHAIN_ID = 103;
    int HT_ASSET_ID = 1;
    int HT_DECIMALS = 18;
    int RESEND_TIME = 50;
    int DEFAULT_INTERVAL_WAITTING = 5;
    int MAX_MANAGERS = 15;

    String METHOD_HASH_CREATEORSIGNWITHDRAW = "0xab6c2b10";
    String METHOD_HASH_CREATEORSIGNMANAGERCHANGE = "0x00719226";
    String METHOD_HASH_CREATEORSIGNUPGRADE = "0x408e8b7a";
    String METHOD_HASH_CROSS_OUT = "0x0889d1f0";

    String METHOD_CREATE_OR_SIGN_WITHDRAW = "createOrSignWithdraw";
    String METHOD_CREATE_OR_SIGN_MANAGERCHANGE = "createOrSignManagerChange";
    String METHOD_CREATE_OR_SIGN_UPGRADE = "createOrSignUpgrade";
    String METHOD_CROSS_OUT = "crossOut";
    String METHOD_VIEW_IS_MINTER_ERC20 = "isMinterERC20";

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
                    new TypeReference<Address>(true) {},
                    new TypeReference<Utf8String>(true) {},
                    new TypeReference<Uint>(true) {},
                    new TypeReference<Address>(false) {}
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
    BigInteger HT_GAS_LIMIT_OF_HT = BigInteger.valueOf(21000L);
    BigInteger HT_GAS_LIMIT_OF_USDT = BigInteger.valueOf(60000L);
    BigInteger HT_ESTIMATE_GAS = BigInteger.valueOf(1000000L);
    BigDecimal NUMBER_1_DOT_1 = new BigDecimal("1.1");
    BigInteger BASE_GAS_LIMIT = BigInteger.valueOf(50000L);

    Long ROLLBACK_NUMER = 100L;

    long HOURS_3 = 3L * 60L * 60L * 1000L;
    long MINUTES_20 = 20 * 60 * 1000L;
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
