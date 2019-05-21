pragma solidity >=0.5.0;

contract OcrDocument {
    
    event DocumentAdded(uint documentId, address owner, bytes32 documentHash, bytes32 textHash, bytes32 metadataHash);
    
    struct Document {
        uint Id;
        address Owner;          
        bytes32 DocumentHash;
        bytes32 TextHash;
        bytes32 MetadataHash;
    }

    mapping(uint => Document) public documents;    
    
    uint documentCount;

    function addDocument(bytes32 documentHash, bytes32 textHash, bytes32 metadataHash) public {
        uint id = documentCount++;

        Document memory doc = Document({
            Id : id,
            Owner : msg.sender,                        
            DocumentHash : documentHash,
            TextHash : textHash,
            MetadataHash : metadataHash
        });
        
        documents[id] = doc;        
        
        emit DocumentAdded(id, msg.sender, documentHash, textHash, metadataHash);
    }

    function getDocumentHash(uint documentId) public view returns (bytes32) {
        return documents[documentId].DocumentHash;        
    }

    function getTextHash(uint documentId) public view returns (bytes32) {
        return documents[documentId].TextHash;        
    }

    function getMetadataHash(uint documentId) public view returns (bytes32) {
        return documents[documentId].MetadataHash;        
    }
    
    function getCount() public view returns (uint) {
        return documentCount;
    }
}