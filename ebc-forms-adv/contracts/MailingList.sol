pragma solidity =0.5.0;
pragma experimental ABIEncoderV2;

contract MailingList {
    address public owner = msg.sender;

    mapping(string => bool) MailAddresses;

    string[] MailAddressesArray; // for quick enumeration of added mail addresses

    mapping(string => string) HashToAddress;

    event MailAddressAdded(string mailAddress);

    function addMailAddress(string memory mailAddress, string memory hash) public {
        require(MailAddresses[mailAddress] == false); // check duplication
        MailAddresses[mailAddress] = true;
        MailAddressesArray.push(mailAddress);
        HashToAddress[hash] = mailAddress;
        emit MailAddressAdded(mailAddress);
    }

    function getMailAddresses() public view returns(string[] memory) {
        return MailAddressesArray;
    }

    function getMailAddressByHash(string memory hash) public view returns(string memory) {
        return HashToAddress[hash];
    }
}
