using Nethereum.Hex.HexTypes;
using Newtonsoft.Json;

namespace AkvNethereum
{
    public class TransactionModel
    {
        private HexBigInteger value;
        public HexBigInteger Value
        {
            get
            {
                return value == null ? new HexBigInteger(0) : value;
            }
            set
            {
                this.value = value;
            }
        }

        private HexBigInteger gas;
        public HexBigInteger Gas
        {
            get
            {
                return gas == null ? new HexBigInteger(0) : gas;
            }
            set
            {
                gas = value;
            }
        }

        private HexBigInteger gasPrice;
        public HexBigInteger GasPrice
        {
            get
            {
                return gasPrice == null ? new HexBigInteger(0) : gasPrice;
            }
            set
            {
                gasPrice = value;
            }
        }

        [JsonProperty("to")]
        public string ToAddress { get; set; }

        public string Data { get; set; }
    }
}
