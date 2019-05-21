package com.mycompany.functions;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AppServiceMSICredentials;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.keyvault.KeyVaultClient;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {

    // Ethereum JSON-RPC Endpoint
    private static final String ETH_JSON_RPC = System.getenv("ETH_JSON_RPC");

    // Resource ID of Azure Key Vault
    private static final String VAULT_ID = System.getenv("VAULT_ID");

    // Resource ID of Ethereum Keystore JSON stored in Azure Key Vault Secrets
    private static final String KEYSTORE_SECRET_ID = System.getenv("KEYSTORE_SECRET_ID");

    // Resource ID of Ethereum Keystore Passphrase stored in Azure Key Vault Secrets
    private static final String PASSPHRASE_SECRET_ID = System.getenv("PASSPHRASE_SECRET_ID");

    // Set gas price to 0 for 0 fee
    private static final BigInteger GAS_PRICE = BigInteger.ZERO;

    // JSON parser
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final boolean IS_LOCAL =
            System.getenv("WEBSITE_INSTANCE_ID") == null || System.getenv("WEBSITE_INSTANCE_ID").isEmpty();

    /**
     * This method sends a zero-value self-transaction using the secrets stored in Azure Key Vault.
     */
    @FunctionName("send-test-transaction")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        try {
            // connect to Ethereum JSON-RPC
            Web3j web3j = Web3j.build(new HttpService(ETH_JSON_RPC));
            if (web3j.ethSyncing().send().isSyncing()) {
                return request.createResponseBuilder(HttpStatus.SERVICE_UNAVAILABLE).body("Node Syncing").build();
            }

            // load credentials
            Credentials credentials = getCredentials();
            context.getLogger().info("Credential loaded for address " + credentials.getAddress());

            // send transaction
            context.getLogger().info("Sending transaction...");
            TransactionReceipt transactionReceipt = (new Transfer(web3j, new RawTransactionManager(web3j, credentials)))
                    .sendFunds(
                            credentials.getAddress(),
                            BigDecimal.valueOf(0),
                            Convert.Unit.ETHER,
                            GAS_PRICE,
                            Transfer.GAS_LIMIT
                    ).send();
            if (!transactionReceipt.isStatusOK()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("Transaction failed: " + transactionReceipt.getStatus()).build();
            }

            // return transaction receipt
            return request.createResponseBuilder(HttpStatus.OK).body(transactionReceipt.toString()).build();
        } catch (Exception e) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + ExceptionUtils.getStackTrace(e)).build();
        }
    }

    private static Credentials getCredentials() throws IOException, CipherException {
        // retrieve Ethereum secrets from Azure Key Vault
        KeyVaultClient keyVaultClient = getKeyVaultClient();
        String keystore = keyVaultClient.getSecret(KEYSTORE_SECRET_ID).value();
        String passphrase = keyVaultClient.getSecret(PASSPHRASE_SECRET_ID).value();

        // parse and decrypt keystore
        WalletFile walletFile = objectMapper.readValue(keystore, WalletFile.class);
        return Credentials.create(Wallet.decrypt(passphrase, walletFile));
    }

    private static KeyVaultClient getKeyVaultClient() throws IOException {
        if (IS_LOCAL) {
            return new KeyVaultClient(AzureCliCredentials.create());
        } else {
            return new KeyVaultClient(new AppServiceMSICredentials(AzureEnvironment.AZURE));
        }
    }
}
