/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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
package nerve.network.converter.heterogeneouschain.eth.constant;

import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * @author: Chino
 * @date: 2020-02-20
 */
public interface EthConstant {

    String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";
    String PUBLIC_KEY_UNCOMPRESSED_PREFIX = "04";
    String HEX_PREFIX = "0x";
    String ETH_SYMBOL = "ETH";
    int ETH_CHAIN_ID = 101;
    int ETH_ASSET_ID = 1;
    int ETH_DECIMALS = 18;
    String ETH_ERC20_STANDARD_FILE = "ethTokens.json";

    String METHOD_HASH_CREATEORSIGNWITHDRAW = "0x46b4c37e";
    String METHOD_HASH_CREATEORSIGNMANAGERCHANGE = "0xf16cb636";

    String METHOD_CREATE_OR_SIGN_WITHDRAW = "createOrSignWithdraw";
    String METHOD_CREATE_OR_SIGN_MANAGERCHANGE = "createOrSignManagerChange";

    String METHOD_VIEW_IS_COMPLETED_TRANSACTION = "isCompletedTransaction";
    String METHOD_VIEW_PENDING_WITHDRAW = "pendingWithdrawTransaction";
    String METHOD_VIEW_PENDING_MANAGERCHANGE = "pendingManagerChangeTransaction";
    String METHOD_VIEW_ERC20_NAME = "name";
    String METHOD_VIEW_ERC20_SYMBOL = "symbol";
    String METHOD_VIEW_ERC20_DECIMALS = "decimals";

    String EVENT_HASH_ERC20_TRANSFER = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
    String EVENT_HASH_TRANSFERFUNDS = "0x20d824a439fe2d644eda3bac33288712bada2c7abe9002a33f59746b42bc9240";
    String EVENT_HASH_TRANSACTION_WITHDRAW_COMPLETED = "0x6b2ea4a10539c788315ac265ab7d0156f3dc89e29a65de7733d81d4b2b6ecf8e";
    String EVENT_HASH_TRANSACTION_MANAGER_CHANGE_COMPLETED = "0x3cb0d515dd9f6ce1eff0ad99825e758c07491cecc09f2bbeb75c35a7f1b6ecab";

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
                    new TypeReference<DynamicArray<Address>>(){}
            )
    );

    Event EVENT_TRANSACTION_WITHDRAW_COMPLETED = new Event("TransactionWithdrawCompleted",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<DynamicArray<Address>>(true) {},
                    new TypeReference<Utf8String>(false) {}
            ));
    Event EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED = new Event("TransactionManagerChangeCompleted",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<DynamicArray<Address>>(true) {},
                    new TypeReference<Uint8>(true) {},
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
                    new TypeReference<Uint>(true) {},
                    new TypeReference<Uint>(false) {}
            ));

    byte[] EMPTY_BYTE = new byte[]{0};
    BigInteger ETH_GAS_LIMIT_OF_ETH = BigInteger.valueOf(21000L);
    BigInteger ETH_GAS_LIMIT_OF_USDT = BigInteger.valueOf(60000L);
    //TODO pierre 600000 for test, 300000 for main
    BigInteger ETH_GAS_LIMIT_OF_MULTY_SIGN = BigInteger.valueOf(600000L);

    Long ROLLBACK_NUMER = 100L;

    long MINUTES_20 = 20 * 60 * 1000L;
}
