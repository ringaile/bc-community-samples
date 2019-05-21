using System.Threading.Tasks;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Configuration;
using System.Net.Http;
using System.Net;
using Nethereum.Web3;
using Nethereum.Hex.HexTypes;
using Nethereum.Web3.Accounts;
using System;
using Microsoft.Azure.Services.AppAuthentication;
using Microsoft.Azure.KeyVault;
using Microsoft.Azure.KeyVault.Models;

namespace AkvNethereum
{
    public static class SendTx
    {
        [FunctionName("SendTx")]
        public static async Task<HttpResponseMessage> Run(
            [HttpTrigger(AuthorizationLevel.Function, "post", Route = null)] HttpRequestMessage req,
            ILogger log,
            ExecutionContext context)
        {
            IConfigurationRoot config = new ConfigurationBuilder()
                .SetBasePath(context.FunctionAppDirectory)
                .AddJsonFile("local.settings.json", optional: true, reloadOnChange: true)
                .AddEnvironmentVariables()
                .Build();

            try
            {
                var model = await BindModel(req.Content);
                var account = await GetAccount(model.KeyIdentifier);

                var res = await SendRawTx(config, account, model.Transaction);
                return req.CreateResponse(HttpStatusCode.OK, res);
            }
            catch (Exception ex)
            {
                return req.CreateResponse(HttpStatusCode.BadRequest, ex.Message);
            }
        }

        private static async Task<SendTxModel> BindModel(HttpContent content)
        {
            var model = await content.ReadAsAsync<SendTxModel>();
            if (string.IsNullOrEmpty(model.KeyIdentifier))
            {
                throw new ArgumentException("Identifier is null or empty");
            }

            if (model.Transaction == null)
            {
                throw new ArgumentException("Tx is null");
            }

            return model;
        }

        private static async Task<Account> GetAccount(string identifier)
        {
            var azureServiceTokenProvider = new AzureServiceTokenProvider();
            var keyVaultClient = new KeyVaultClient(new KeyVaultClient.AuthenticationCallback(azureServiceTokenProvider.KeyVaultTokenCallback));

            SecretBundle secretBundle;
            try
            {
                secretBundle = await keyVaultClient.GetSecretAsync(identifier);
            }
            catch (KeyVaultErrorException kex)
            {
                throw new Exception(kex.Message);
            }

            return new Account(secretBundle.Value);
        }

        private static async Task<string> SendRawTx(IConfigurationRoot config, Account acc, TransactionModel tx)
        {
            var web3 = new Web3(config.GetValue<string>("Ethereum:RPC"));
            var gasLimitValue = config.GetValue<int>("Ethereum:GasLimit");
            HexBigInteger gasLimit = new HexBigInteger(gasLimitValue);

            HexBigInteger nonce = await web3.Eth.Transactions.GetTransactionCount.SendRequestAsync(acc.Address);
            string transaction = Web3.OfflineTransactionSigner.SignTransaction(acc.PrivateKey, tx.ToAddress, tx.Value, nonce, tx.GasPrice, gasLimit, tx.Data);
            return await web3.Eth.Transactions.SendRawTransaction.SendRequestAsync("0x" + transaction);
        }
    }
}
