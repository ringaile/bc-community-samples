import logging
import os
import traceback

import azure.functions as func
from azure.keyvault import KeyVaultClient
from eth_keys import keys
from msrestazure.azure_active_directory import ServicePrincipalCredentials
from web3 import Web3


def main(req: func.HttpRequest) -> func.HttpResponse:
    try:
        logging.info('Python HTTP trigger function processed a request.')

        key_vault_client = get_key_vault_client()

        # Get application settings
        eth_json_rpc = os.environ.get("ETH_JSON_RPC")
        vault_base_url = os.environ.get("VAULT_BASE_URL")
        keystore_secret_name = os.environ.get("KEYSTORE_SECRET_NAME")
        keystore_secret_version = os.environ.get("KEYSTORE_SECRET_VERSION")
        passphrase_secret_name = os.environ.get("PASSPHRASE_SECRET_NAME")
        passphrase_secret_version = os.environ.get("PASSPHRASE_SECRET_VERSION")

        # Get Ethereum secrets
        keystore_secret = key_vault_client.get_secret(vault_base_url, keystore_secret_name,
                                                      keystore_secret_version).value
        passphrase_secret = key_vault_client.get_secret(vault_base_url, passphrase_secret_name,
                                                        passphrase_secret_version).value

        # Connect to Ethereum JSON-RPC and Decrypt Ethereum keystore
        w3 = Web3(Web3.HTTPProvider(eth_json_rpc))
        private_key = keys.PrivateKey(w3.eth.account.decrypt(keystore_secret, passphrase_secret))
        address = Web3.toChecksumAddress(private_key.public_key.to_address())
        logging.info('Loaded credentials for Ethereum address {}'.format(address))

        # Sign and send a zero-value self-transaction
        transaction = {
            'to': address,
            'value': 0,
            'gas': 21000,
            'gasPrice': 0,
            'nonce': w3.eth.getTransactionCount(address),
        }
        logging.info('Signing transaction {}'.format(transaction))
        signedTransaction = w3.eth.account.signTransaction(transaction, private_key)
        logging.info('Sending signed transaction {}'.format(signedTransaction))
        transactionHash = w3.eth.sendRawTransaction(signedTransaction.rawTransaction)
        transactionReceipt = w3.eth.waitForTransactionReceipt(transactionHash)

        # Return transaction receipt
        return func.HttpResponse("{}".format(transactionReceipt))
    except:
        return func.HttpResponse("{}".format(traceback.format_exc()), status_code=500)


def get_key_vault_client():
    return KeyVaultClient(ServicePrincipalCredentials(
        client_id=os.environ.get("CLIENT_ID"),
        secret=os.environ.get("CLIENT_SECRET"),
        tenant=os.environ.get("TENANT_ID"),
        resource='https://vault.azure.net'
    ))
