# Azure Key Vault And Nethereum

The project contains Http Triggered function `SendTx`
It does:
* Retrieves private key from KeyVault based on key identifier
* Creates Ethereum account based on the private key
* Sign transaction by account's private key
* Send the transaction to a node via RPC connection
* Returns response of the execution

## Prerequirements
0. Azure subscription. If you don't already have an Azure account, you can [create a free](https://azure.microsoft.com/en-us/free/) one here.
1. [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest)
2. [Azure Functions Core Tools](https://github.com/Azure/azure-functions-core-tools)
3. [.NET Core SDK 2.2.1x](https://dotnet.microsoft.com/download/thank-you/dotnet-sdk-2.2.106-windows-x64-installer)

## Getting started

In order to run commands you need to use PowerShell

0. Login to Azure
    ```
    az login
    ```
1. Create resource group
    ```
    az group create -l westeurope -n akv-neth
    ```
2. Create storage account
    ```
    az storage account create --name akvnethstorage --location westeurope --resource-group akv-neth --sku Standard_LRS
    ```
3. Create Azure FunctionApp
    ```
    az functionapp create --resource-group akv-neth --consumption-plan-location westeurope --name akv-func --storage-account akvnethstorage --runtime dotnet
    ```
4. Configure identity of Azure FunctionApp
    ```
    az functionapp identity assign --name akv-func --resource-group akv-neth
    ```
5. Create KeyVault
    ```
    az keyvault create --name akv-kv --resource-group akv-neth --location westeurope
    ```
6. Set policy for the FunctionApp in KeyVault
    ```
    $id = az functionapp identity show --resource-group akv-neth --name akv-func --query principalId
    az keyvault set-policy --name akv-kv --object-id $id --secret-permissions get
    ```
5. Deploy FunctionApp (befor the stap you need to add `local.settings.json` to AkvNethereum directory, see example below)
    ```
    func azure functionapp publish akv-func --publish-local-settings --overwrite-settings
    ```
6. Configure FunctionApp

## Configuration

In order to run the function, you need to provide configuration via `local.settings.json` or you add configuration to Azure FunctionApp.

local.settings.json:
```
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    "AzureWebJobsDashboard": "UseDevelopmentStorage=true",
    "Ethereum:RPC": "http://127.0.0.1:8545",
    "Ethereum:GasLimit": "6721975"
  }
}
```

## AuthorizationLevel

The function has `Function` protection level.

In order to run the function on FunctionApp, you need to add key to url, like `https://func-neth-app.azurewebsites.net/api/sendTx?code=HqssNO/Do4OGncDxa22i....==`

In order to run it locally, you dont need anything extra. `http://localhost:7071/api/sendTx`

## Key Vault

Before execution of the function you ned to create `Secret` in the KeyVault.
The key should contain private key of Ethereum account. Then you need to retrieve the KeyIdentifier.
1. Create Ethereum account. You can choose many options to do this, I would suggest the simplest option. The private key is random 256 bits number or 64 (hex) characters. So you can generate the key by yourself, like:
    ```
    3a1076bf45ab87712ad64ccb3b10217737f7faacbf2872e88fdd9a537d8fe266
    ```
2. Create secret in KeyVault
    ```
    az keyvault secret set --vault-name akv-kv --name akv-secret1 --value 3a1076bf45ab87712ad64ccb3b10217737f7faacbf2872e88fdd9a537d8fe266
    ```
3. Save identifier of the secret, you can find it in response (`id` field) after secret creation
    ```
    {
      "attributes": {
        "created": "2019-04-20T11:23:56+00:00",
        "enabled": true,
        "expires": null,
        "notBefore": null,
        "recoveryLevel": "Purgeable",
        "updated": "2019-04-20T11:23:56+00:00"
      },
      "contentType": null,
      "id": "https://akv-kv.vault.azure.net/secrets/akv-secret1/6515a350d8194a928473ba5a799c8ded",
      "kid": null,
      "managed": null,
      "tags": {
        "file-encoding": "utf-8"
      },
      "value": "3a1076bf45ab87712ad64ccb3b10217737f7faacbf2872e88fdd9a537d8fe266"
    }
    ```

## Execution

In order to execute the function you can use Postman.
You need to create POST request with json body:

```
{
	"identifier": "https://akv-kv.vault.azure.net/secrets/akv-secret1/6515a350d8194a928473ba5a799c8ded",
	"tx": {
		"value": "0",
		"to": "0xF4485b57bE9ACad0253815b7000A7C1fE1D7EEFC",
		"gas": "0x5208",
		"gasPrice": "0x4A817C800",
		"data": "0x"
	}
}
```

The data contains two main parts:
1. identifier - KeyVault identifier, which used for retrieving private key
2. tx - transaction data. All properties of the field should hex format
