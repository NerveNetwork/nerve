pragma solidity ^0.5.5;

import "https://github.com/GNSPS/solidity-bytes-utils/blob/v0.0.8/contracts/BytesLib.sol";

contract ArrayTestIII {

    using BytesLib for bytes;

    function test(bytes memory sig) public pure returns (address[] memory) {
        uint times = sig.length / 20;
        address[] memory result = new address[](times);
        uint k = 0;
        uint8 j = 0;
        for (uint i = 0; i < times; i++) {
            address mng = sig.slice(k, 20).toAddress(0);
            bool add = validateByArray(mng, result, j);
            if (add) {
                j++;
            }
            k += 20;
        }
        return result;
    }

    function validateByArray(address mng, address[] memory validateArray, uint8 j) internal pure returns (bool) {
        uint len = validateArray.length;
        if(len > 0) {
            address temp;
            for (uint8 i = 0; i < len; i++) {
                temp = validateArray[i];
                if (temp == address(0)) {
                    break;
                }
                if (temp == mng) {
                    return false;
                }
            }
        }
        validateArray[j] = mng;
        return true;
    }

}