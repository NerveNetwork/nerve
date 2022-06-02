pragma solidity ^0.5.5;


contract SignTest {
    function ecrecovery(bytes32 hash, bytes memory sig) public pure returns (address) {
        bytes32 r;
        bytes32 s;
        uint8 v;
        if (sig.length != 65) {
            return address(0);
        }
        assembly {
            r := mload(add(sig, 32))
            s := mload(add(sig, 64))
            v := byte(0, mload(add(sig, 96)))
        }
        if(uint256(s) > 0x7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0) {
            return address(0);
        }
        // https://github.com/ethereum/go-ethereum/issues/2053
        if (v < 27) {
            v += 27;
        }
        if (v != 27 && v != 28) {
            return address(0);
        }
        return ecrecover(hash, v, r, s);
    }

    function ecrecoveryII(string memory key, bytes memory sig) public pure returns (address) {
        bytes32 r;
        bytes32 s;
        uint8 v;
        if (sig.length != 65) {
            return address(0);
        }
        assembly {
            r := mload(add(sig, 32))
            s := mload(add(sig, 64))
            v := byte(0, mload(add(sig, 96)))
        }
        if(uint256(s) > 0x7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0) {
            return address(0);
        }
        // https://github.com/ethereum/go-ethereum/issues/2053
        if (v < 27) {
            v += 27;
        }
        if (v != 27 && v != 28) {
            return address(0);
        }
        return ecrecover(keccak256(bytes(key)), v, r, s);
    }

    function testWithdrawHash(string memory txKey, address payable to, uint256 amount, bool isERC20, address ERC20, uint256 hashSalt) public pure returns (bytes32) {
        return keccak256(abi.encodePacked(txKey, to, amount, isERC20, ERC20, hashSalt));
    }

    function testWithdrawEncode(string memory txKey, address payable to, uint256 amount, bool isERC20, address ERC20, uint256 hashSalt) public pure returns (bytes memory) {
        return abi.encodePacked(txKey, to, amount, isERC20, ERC20, hashSalt);
    }

    function testChangeHash(string memory txKey, address[] memory adds, address[] memory removes, uint8 count, uint256 hashSalt) public pure returns (bytes32) {
        return keccak256(abi.encodePacked(txKey, adds, count, removes, hashSalt));
    }

    function testChangeEncode(string memory txKey, address[] memory adds, address[] memory removes, uint8 count) public pure returns (bytes memory) {
        return abi.encodePacked(txKey, adds, count, removes);
    }

    function splitTest(bytes32 hash, bytes memory signatures) public pure returns (address[] memory){
        bytes memory signedString = signatures;

        uint256 times = signedString.length / 65;
        address[] memory result = new address[](times);
        uint k = 0;
        for (uint i = 0; i < times; i++) {
            result[i] = ecrecovery(hash, slice(signedString, k, 65));
            k += 65;
        }
        return result;
    }

    function slice(bytes memory data, uint start, uint len) internal pure returns (bytes memory){
        bytes memory b = new bytes(len);

        for(uint i = 0; i < len; i++){
            b[i] = data[i + start];
        }
        return b;
    }

}
