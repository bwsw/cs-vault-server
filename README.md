![](https://travis-ci.org/bwsw/cs-vault-server.svg?branch=master) [![Coverage Status](https://coveralls.io/repos/github/bwsw/cs-vault-server/badge.svg?branch=master)](https://coveralls.io/github/bwsw/cs-vault-server?branch=master)

Also see:
* [Event processing logic](docs/logic.md)

# cs-vault-server
CloudStack Vault Plugin Server Handler

# Technical

[Travis Build History](https://travis-ci.org/bwsw/cs-vault-server/builds)

Quick start
-----------
To start a server in Docker container you should:

1. Provide a file 'variables.env' containing the following required variables:
    * `TAG_NAME_PREFIX` - prefix for token name tag
    * `KAFKA_ENDPOINTS` - list of kafka endpoints
    * `KAFKA_TOPIC` - kafka topic containing cloudstack events
    * `ZOOKEEPER_ENDPOINTS` - list of zookeeper endpoints, which is separated by comma
    * `ZOOKEEPER_RETRY_DELAY` - a delay between unsuccessful connection attempt to zookeeper and repeated attempt
    * `ZOOKEEPER_ROOT_NODE` - a root node for keeping application data in zookeeper ("/cs_vault_server" by default)
    * `ZOOKEEPER_MASTER_LATCH_NODE` - a node for master latch keeping ("/cs_vault_server_latch" by default)
    * `VAULT_ENDPOINT` - vault endpoint
    * `VAULT_ROOT_TOKEN` - root token providing an access to a vault server
    * `VAULT_RETRY_DELAY` -  a delay between unsuccessful connection attempt to vault and repeated attempt
    * `VAULT_TOKEN_PERIOD` - lifetime of vault token in days (3562 days by default)
    * `VAULT_ACCOUNTS_BASIC_PATH` - path to cloudstack accounts' secrets in vault ("secret/cs/accounts/" by default). Last "/" is required
    * `VAULT_VMS_BASIC_PATH` - path to vms' secrets in vault ("secret/cs/vms/" by default). Last "/" is required
    * `CS_ENDPOINTS` - list of cloudstack endpoints, which is separated by comma
    * `CS_API_KEY` - api key providing an access to a cloudstack server
    * `CS_SECRET_KEY` - secret key providing an access to a cloudstack server
    * `CS_RETRY_DELAY` - a delay between unsuccessful connection attempt to cloudstack and repeated attempt
2. Run the following command:
    "docker run --env-file variables.env medvedevbwsw/cs-vault-server:latest"

To create a local docker image you should run the following command:
    "docker build -t REPOSITORY:TAG ."
     where REPOSITORY - container name, TAG - version of container

If you need to create your own docker container with Travis help in DockerHub after project push into GitHub (master branch) you should:

1. authorize your Travis-CI account to get an access to your GitHub account
2. set the following environment variables in the Travis-CI: 'DOCKER_USERNAME', 'DOCKER_PASSWORD',
   'DOCKER_USERNAME' it is Docker Hub user name; 'DOCKER_PASSWORD' it is Docker Hub password
   
