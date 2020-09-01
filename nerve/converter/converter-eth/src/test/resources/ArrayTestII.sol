pragma solidity ^0.5.5;

import "https://github.com/GNSPS/solidity-bytes-utils/blob/v0.0.8/contracts/BytesLib.sol";

contract ArrayTestII {

    using BytesLib for bytes;

    function test(bytes memory sig) public pure returns (address[] memory) {
        uint times = sig.length / 20;
        bytes[] memory array = new bytes[](times);
        address[] memory result = new address[](times);
        uint k = 0;
        uint8 j = 0;
        for (uint i = 0; i < times; i++) {
            bytes memory mng = sig.slice(k, 20);
            bool add = validateByArray(mng, array, j);
            if (add) {
                result[j] = mng.toAddress(0);
                j++;
            }
            k += 20;
        }
        uint n = times - j;
        assembly { mstore(result, sub(mload(result), n)) }
        delete array;
        return result;
    }

    function validateByArray(bytes memory mng, bytes[] memory validateArray, uint8 j) internal pure returns (bool) {
        uint len = validateArray.length;
        if(len > 0) {
            bytes memory temp;
            for (uint8 i = 0; i < len; i++) {
                temp = validateArray[i];
                if (temp.length == 0) {
                    break;
                }
                if (temp.equal(mng)) {
                    return false;
                }
            }
        }
        validateArray[j] = mng;
        return true;
    }


}