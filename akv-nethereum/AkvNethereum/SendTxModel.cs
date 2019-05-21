using Newtonsoft.Json;

namespace AkvNethereum
{
    public class SendTxModel
    {
        [JsonProperty("identifier")]
        public string KeyIdentifier { get; set; }

        [JsonProperty("tx")]
        public TransactionModel Transaction { get; set; }
    }
}
