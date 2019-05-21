# Azure Key Vault And Azure Function (Python)

## Table of Contents

* [Table of Contents](#table-of-contents)
* [Overview](#overview)
* [Prerequisites](#prerequisites)
* [Step 1 - Login to Azure](#step-1---login-to-azure)
* [Step 2 - Generate Ethereum Keypair](#step-2---generate-ethereum-keypair)
* [Step 3 - Deploy Ethereum PoA Network on Azure Cloud](#step-3---deploy-ethereum-poa-network-on-azure-cloud)
* [Step 4 - Create an Azure Key Vault](#step-4---create-an-azure-key-vault)
* [Step 5 - Store Ethereum Secrets on Azure Key Vault](#step-5---store-ethereum-secrets-on-azure-key-vault)
* [Step 6 - List Stored Secrets on Azure Key Vault](#step-6---list-stored-secrets-on-azure-key-vault)
* [Step 7 - Clone the Repo](#step-7---clone-the-repo)
* [Step 8 - Create and Activate a Python Virtual Environment](#step-8---create-and-activate-a-python-virtual-environment)
* [Step 9 - Install Dependencies](#step-9---install-dependencies)
* [Step 10 - Configure Application Settings](#step-10---configure-application-settings)
* [Step 11 - Create an Azure Active Directory Service Principal](#step-11---create-an-azure-active-directory-service-principal)
* [Step 12 - Test the Azure Function Locally](#step-12---test-the-azure-function-locally)
* [Step 13 - Deploy the Azure Function to Azure Cloud](#step-13---deploy-the-azure-function-to-azure-cloud)
* [Troubleshoot](#troubleshoot)

## Overview

By following this solution, you will learn how to:

 - Generate an Ethereum keypair locally using Go Ethereum.
 - Deploy an Ethereum PoA network on Azure Cloud.
 - Store and retrieve Ethereum secrets using Azure Key Vault Secrets API.
 - Sign and send Ethereum transactions using Azure Function in Python.

## Prerequisites

* POSIX compliant command line
* [Go Ethereum](https://geth.ethereum.org/)
* [Python 3.6](https://www.python.org/downloads/release/python-368/)
* [Python venv](https://docs.python.org/3.6/library/venv.html)
* [Git](https://www.git-scm.com/)
* [Docker](https://docs.docker.com/install/)
* [Curl](https://curl.haxx.se/download.html)
* [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli?view=azure-cli-latest) 2.0.4 or later
* [Azure Function Core Tools](https://github.com/Azure/azure-functions-core-tools)
* An Azure subscription. If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?WT.mc_id=A261C142F) before you begin.

## Step 1 - Login to Azure

To log in to Azure using the CLI, run:

```bash
az login
```

## Step 2 - Generate Ethereum Keypair

To generate an Ethereum keypair for development purpose, run:

```bash
geth account new
```

Please make sure to remember the passphrase you entered for the new keypair.  

## Step 3 - Deploy Ethereum PoA Network on Azure Cloud

Follow [this guide](./EthereumPoA.md) to deploy an Ethereum PoA Network on Azure Cloud.

When asked for entering an `Admin Ethereum Address` during the deployment, fill in the Ethereum address created in the previous step. 

## Step 4 - Create an Azure Key Vault

Next you create a Key Vault using the resource group created in the previous step. Please note you have to use a globally unique name. Provide the following information:

* `<YourKeyVaultName>` - **Select an Unique Key Vault Name here**.
* `<YourResourceGroupName>` - **Use the same resource group with the deployed Ethereum PoA Network in the previous step**.

```bash
export YourKeyVaultName=<YourKeyVaultName>
export YourResourceGroupName=<YourResourceGroupName>
az keyvault create --name "$YourKeyVaultName" --resource-group "$YourResourceGroupName" --location "westus"
```

At this point, your Azure account is the only one authorized to perform any operations on this new vault.

## Step 5 - Store Ethereum Secrets on Azure Key Vault

First, list local key stores and locate the keystore file for development purpose:
```bash
geth account list
```

Second, store the keystore JSON file:
```bash
az keyvault secret set --vault-name "$YourKeyVaultName" --name "EthKeystore" --file <PathToYourKeyStoreFile>
```

Third, store the passphrase of the keystore:
```bash
az keyvault secret set --vault-name "$YourKeyVaultName" --name "EthKeystorePassphrase" --value "<YourPassphrase>"
```

## Step 6 - List Stored Secrets on Azure Key Vault

To see the stored Ethereum secrets on Azure Key Vault, run:

```bash
az keyvault secret list --vault-name "$YourKeyVaultName"
``` 

Please make sure the output lists 2 secrets including `EthKeystore` and `EthKeystorePassphrase` created in the previous step. 

## Step 7 - Clone the Repo

Clone the repo in order to make a local copy for you to edit the source by running the following command:

```
git clone https://github.com/Azure-Samples/bc-community-samples.git
cd akv-functions-python 
```

## Step 8 - Create and Activate a Python Virtual Environment

It is required that you work in a Python 3.6 virtual environment. Run the following commands to create and activate a virtual environment named .env
```bash
python3.6 -m venv .env
source .env/bin/activate
```

## Step 9 - Install Dependencies

To install dependencies of this project, run:
```bash
pip install -r requirements.txt
```

## Step 10 - Configure Application Settings 

First, copy [local.settings.sample.json](./local.settings.sample.json) to [local.settings.json](local.settings.json) by running:

```bash
cp local.settings.sample.json local.settings.json
```

Second, open [local.settings.json](./local.settings.json) and change the following values with the results from the Azure CLI commands:

|local.settings.json field|Azure CLI Command|
|-------------------------|-----------------|
|ETH_JSON_RPC|<code>echo "http://$(az network public-ip show --ids $(az network lb frontend-ip show --resource-group "$YourResourceGroupName" --lb-name $(az network lb list --query [0].name &#124; tr -d '"') --name LBFrontEnd --query publicIpAddress.id &#124; tr -d '"') --query ipAddress &#124; tr -d '"'):8540"</code>|
|VAULT_BASE_URL|<code>az keyvault show --name "$YourKeyVaultName" --query properties.vaultUri</code>|
|KEYSTORE_SECRET_VERSION|<code>az keyvault secret list-versions --vault-name "$YourKeyVaultName" --name EthKeystore --query [-1].id &#124; tr -d '"' &#124; awk -F / '{print $NF}'</code>|
|PASSPHRASE_SECRET_VERSION|<code>az keyvault secret list-versions --vault-name "$YourKeyVaultName" --name EthKeystorePassphrase --query [-1].id &#124; tr -d '"' &#124; awk -F / '{print $NF}'</code>|

## Step 11 - Create an Azure Active Directory Service Principal

To authorize the function for reading the created key vault, a service principal needs to be created on Azure Cloud Active Directory.

Run the following command to create a service principal scoped by the created key vault in Step 4:
```bash
export YourKeyVaultId=$(az keyvault show --resource-group $YourResourceGroupName --name $YourKeyVaultName --query id | tr -d '"')
az ad sp create-for-rbac --name "eth-vault-reader" --role reader --scopes "$YourKeyVaultId"
```

After the command is successfully executed, you will receive a JSON response like this:

```
{
  "appId": "470008ce-ce24-4390-9087-cf5ed6d7f426",
  "displayName": "eth-vault-reader",
  "name": "http://eth-vault-reader",
  "password": "cded66ce-a1b9-48bf-b9d8-da44758d30bd",
  "tenant": "834c5c99-707d-4d0d-a288-e1f0444ec17c"
}
```

Open your [local.settings.json](./local.settings.json), then fill in the following fields with the values of the JSON response from service principal creation correspondingly: 

|local.settings.json field|Service Principal JSON response field|
|-------------------------|-------------------------------------|
|CLIENT_ID|appId|
|TENANT_ID|tenant|
|CLIENT_SECRET|password|

Next, create a readonly access policy to key vault for the service principal: 

```
export YourServicePrincipalObjectId=$(az ad sp show --id http://eth-vault-reader --query objectId | tr -d '"')
az keyvault set-policy --name $YourKeyVaultName --object-id $YourServicePrincipalObjectId --secret-permissions get
```

## Step 12 - Test the Azure Function Locally

First, compile and run the Azure Function locally:

```bash
func host start
```

Second, call the Azure Function to sign and send an Ethereum transaction to the deployed PoA network:

```bash
curl http://localhost:7071/api/send-test-transaction
```

An Ethereum transaction receipt will be returned after a few seconds if the transaction is successfully sent to the PoA network. 

## Step 13 - Deploy the Azure Function to Azure Cloud

To deploy the Azure Function to Azure Cloud, run the following commands:

Create a storage account for storing the artifacts of Azure Functions:
```
export YourFunctionName=akv-functions-python
export YourStorageAccountName=akvfunctionspython
az storage account create --name $YourStorageAccountName --location westus \
    --resource-group $YourResourceGroupName --sku Standard_LRS
```

Create a function app with `python` runtime under Linux OS:
```
az functionapp create --resource-group $YourResourceGroupName --consumption-plan-location westus \
    --name $YourFunctionName --storage-account $YourStorageAccountName --os-type linux --runtime python 
```

Last, bundle and publish the function with local settings:
```
func azure functionapp publish $YourFunctionName --publish-local-settings --overwrite-settings --build-native-deps
```

To test the deployed Azure Function, run:
```bash
curl https://$YourFunctionName.azurewebsites.net/api/create-test-transaction
```

## Troubleshoot

### Got permission denied while trying to connect to the Docker daemon socket at unix:///var/run/docker.sock
By default Docker only allows root user to connect to its daemon on Linux, follow [this guide](https://docs.docker.com/install/linux/linux-postinstall/#manage-docker-as-a-non-root-user) to manage Docker as a non-root user.

### There was an error restoring dependencies.ERROR: cannot install <package name - version> dependency: binary dependencies without wheels are not supported.
Check [this link](https://docs.microsoft.com/en-us/azure/azure-functions/functions-reference-python#publishing-to-azure).

### Preparing archive... Value cannot be null.
This is a [known issue](https://github.com/Azure/azure-functions-python-worker/issues/387) that Azure Functions in Python cannot be re-published. Please delete the created function app before you re-publish your function. 