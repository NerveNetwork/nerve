pragma solidity ^0.5.5;



contract ArrayTest {

    function test(bytes memory sig) public pure returns (address[] memory) {
        uint times = sig.length / 20;
        address[] memory result = new address[](times);
        uint k = 0;
        uint8 j = 0;
        for (uint i = 0; i < times; i++) {
            address mng = bytesToAddress(slice(sig, k, 20));
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
            for (uint8 i = 0; i < len; i++) {
                if (validateArray[i] == mng) {
                    return false;
                }
            }
        }
        validateArray[j] = mng;
        return true;
    }

    function bytesToAddress(bytes memory bys) private pure returns (address addr) {
        assembly {
            addr := mload(add(bys,20))
        }
    }

    function slice(bytes memory data, uint start, uint8 len) internal pure returns (bytes memory){
        bytes memory b = new bytes(len);

        for(uint8 i = 0; i < len; i++){
            b[i] = data[i + start];
        }
        return b;
    }
}