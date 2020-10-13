pragma solidity ^0.5.5;

contract ArrayTestIII {

    address[] array;

    constructor(address[] memory _array) public{
        array = _array;
    }

    function setArray(address[] memory _array) public{
        array = _array;
    }
    function allArrays() public view returns (address[] memory) {
        return array;
    }

    function arrayTest() public returns (address[] memory) {
        delete array[1];
        delete array[3];
        uint tempIndex = 0x10;
        for (uint i = 0; i<array.length; i++) {
            address temp = array[i];
            if (temp == address(0)) {
                if (tempIndex == 0x10) tempIndex = i;
                continue;
            } else if (tempIndex != 0x10) {
                array[tempIndex] = temp;
                tempIndex++;
            }
        }
        array.length -= 2;
        return array;
    }
}