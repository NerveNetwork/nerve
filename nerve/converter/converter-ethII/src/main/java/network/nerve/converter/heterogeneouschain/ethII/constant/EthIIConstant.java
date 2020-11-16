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
package network.nerve.converter.heterogeneouschain.ethII.constant;

import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;

import java.util.Arrays;
import java.util.List;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
public interface EthIIConstant {

    byte VERSION = 2;
    int RESEND_TIME = 50;

    String METHOD_HASH_CREATEORSIGNWITHDRAW = "0xab6c2b10";
    String METHOD_HASH_CREATEORSIGNMANAGERCHANGE = "0x00719226";
    String METHOD_HASH_CREATEORSIGNUPGRADE = "0x408e8b7a";
    String METHOD_HASH_CROSS_OUT = "0x0889d1f0";

    String METHOD_CREATE_OR_SIGN_WITHDRAW = "createOrSignWithdraw";
    String METHOD_CREATE_OR_SIGN_MANAGERCHANGE = "createOrSignManagerChange";
    String METHOD_CREATE_OR_SIGN_UPGRADE = "createOrSignUpgrade";
    String METHOD_CROSS_OUT = "crossOut";
    String METHOD_VIEW_IS_MINTER_ERC20 = "isMinterERC20";

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
}
