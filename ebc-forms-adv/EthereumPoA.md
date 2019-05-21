# Deploy Ethereum PoA Network on Azure Cloud

## Overview

By following this guide, you will learn how to deploy a simple 2-node Ethereum PoA network on Azure Cloud.

## Prerequisites

* An Azure subscription. If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?WT.mc_id=A261C142F) before you begin.
* [Go Ethereum](https://geth.ethereum.org/)

## Step 0 - Get "Ethereum on Azure" Template

Visit https://aka.ms/blockchain-ethereum-on-azure then click the highlighted button of the following screenshot:

![Step 0](https://i.imgur.com/fyFHvXx.png)
![Step 0.1](https://i.imgur.com/oO6aUYT.png)

## Step 1 - Configure Basic Settings 

![Step 1](https://i.imgur.com/RV5LDIF.png)

## Step 2 - Configure Deployment Regions

![Step 2](https://i.imgur.com/oYpbCsn.png)

## Step 3 - Configure Network Size and Performance

![Step 3](https://i.imgur.com/7dKoFXd.png)

## Step 4 - Configure Ethereum Nodes

When asked for entering an `Admin Ethereum Address` during the deployment, generate a development keypair using [Go Ethereum](https://geth.ethereum.org/) or [Metamask Ethereum Wallet](https://metamask.io/). Next, change the `Block Reseal Period (sec)
` to `5` for faster confirmation. 

![Step 4](https://i.imgur.com/mICl3wf.png)

## Step 5 - Configure Monitoring

![Step 5](https://i.imgur.com/5LM3WRB.png)

## Step 6 - Review Summary

![Step 6](https://i.imgur.com/OT3NWWU.png)

## Step 7 - Review ToS and Buy the Azure Cloud Resources

![Step 7](https://i.imgur.com/6YcpUtz.png)

## Step 8 - Wait for the Deployment to be Completed

![Step 8](https://i.imgur.com/Np9wlEZ.png)
![Step 8.1](https://i.imgur.com/Kp9UmNg.png)

## Step 9 - Verify the Deployment by Connecting to Its JSON-RPC

To retrieve the public IP to the deployment, navigate to resources section of Azure Portal then look for the Load Balance resource deployed for the PoA Network:

![Step 9](https://i.imgur.com/AtxK8uG.png)

To verify the deployment, use Go Ethereum to connect to the deployment's JSON-RPC by running the following command:

```bash
geth attach http://<PublicIpToTheDeployment>:8540
```

In case of a successful deployment, you will see the Geth JavaScript console like the following screenshot:

![Step 9](https://i.imgur.com/7eG0PI2.png)  