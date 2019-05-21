pragma solidity >=0.5.0;

contract Expenses {
    
    event ExpenseLogAdded(uint expenseLogId, string category, string date, uint amount);
    event ExpenseTotalsUpdated(string category, uint totalAmount);
    
    struct ExpenseLog {
        uint Id;        
        bytes32 CategoryHash;
        string Date;
        uint Amount;        
    }

    mapping(uint => ExpenseLog) public ExpenseLogs;    
    mapping(bytes32 => uint) public Totals; 
    
    uint ExpenseLogCount;

    function addExpenseLog(string memory category, string memory date, uint amount) public {
        uint id = ExpenseLogCount++;
        bytes32 categoryHash = keccak256(bytes(category));

        ExpenseLog memory doc = ExpenseLog({
            Id : id,
            CategoryHash : categoryHash,                        
            Date : date,
            Amount : amount
        });
        
        ExpenseLogs[id] = doc;                
        Totals[categoryHash]+= amount;

        emit ExpenseLogAdded(id, category, date, amount);
        emit ExpenseTotalsUpdated(category, Totals[categoryHash]);

    }

    function getTotalAmount(string memory category) public view returns (uint) {        
        return Totals[keccak256(bytes(category))];        
    }
    
    function getCount() public view returns (uint) {
        return ExpenseLogCount;
    }
}