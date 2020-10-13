pragma solidity ^0.5.5;


contract StringTest {

    function test(string memory temp) public pure returns (uint) {
        return bytes(temp).length;
    }

}
