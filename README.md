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
    * `TOKEN_PERIOD` - lifetime of vault token in days
    * `ACCOUNTS_VAULT_BASIC_PATH` - path to cloudstack accounts' secrets in vault
    * `VM_VAULT_BASIC_PATH` - path to vms' secrets in vault
    * `KAFKA_SERVER_LIST` - list of kafka urls
    * `KAFKA_TOPIC` - kafka topic containing cloudstack events
    * `ZOOKEEPER_URL` - zookeeper url
    * `ZOOKEEPER_RETRY_DELAY` - a delay between unsuccessful connection attempt to zookeeper and repeated attempt
    * `VAULT_URL` - vault url
    * `VAULT_ROOT_TOKEN` - root token providing an access to a vault server
    * `VAULT_RETRY_DELAY` -  a delay between unsuccessful connection attempt to vault and repeated attempt
    * `CS_API_URL_LIST` - list of cloudstack urls
    * `CS_API_KEY` - api key providing an access to a cloudstack server
    * `CS_SECRET_KEY` - secret key providing an access to a cloudstack server
    * `CS_RETRY_DELAY` - a delay between unsuccessful connection attempt to cloudstack and repeated attempt
2. Execute the following command:
    "docker run --env-file variables.env medvedevbwsw/cs-vault-server:latest"

To create a local docker image you should execute the following command:
    "docker build -t REPOSITORY:TAG ."
     where REPOSITORY - container name, TAG - version of container

If you need to create your own docker container with Travis help in DockerHub after project push into GitHub (master branch) you should:

1. authorize your Travis-CI account to get an access to your GitHub account
2. set the following environment variables in the Travis-CI: 'DOCKER_USERNAME', 'DOCKER_PASSWORD',
   'DOCKER_USERNAME' it is Docker Hub user name; 'DOCKER_PASSWORD' it is Docker Hub password
   
