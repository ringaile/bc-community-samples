pragma solidity >=0.5.0;

contract AuditLog {

    event LogAdded(uint id, uint documentId, bytes32 documentHash, bytes32 metadataHash, bytes32 modifiedByHash, ChangeType change, int timestamp);

    enum ChangeType {DEFAULT, CREATED, MODIFIED}
    
    struct LogTrace {
        uint Id;
        uint DocumentId;
        bytes32 DocumentHash;
        bytes32 MetadataHash; 
        bytes32 ModifiedByHash;
        ChangeType Change;
        int Timestamp;
    }

    mapping(uint => LogTrace) public logs;
    mapping(uint => bool) public documents;
    uint logCount;

    function addLog(uint documentId, bytes32 documentHash, bytes32 metadataHash, bytes32 modifiedByHash, int timestamp) public {
        
        logCount++;
        LogTrace memory log = LogTrace({
            Id : logCount,
            DocumentId : documentId,
            DocumentHash : documentHash,
            MetadataHash : metadataHash,
            ModifiedByHash : modifiedByHash,
            Change : documents[documentId] ? ChangeType.MODIFIED : ChangeType.CREATED,
            Timestamp : timestamp
        });
        
        logs[logCount] = log;

        if (log.Change == ChangeType.CREATED){
            documents[documentId] = true;
        }
        
        emit LogAdded(logCount, documentId, documentHash, metadataHash, modifiedByHash, log.Change, timestamp);
    }
    
    function getCount() public view returns (uint) {
        return logCount;
    }
}
