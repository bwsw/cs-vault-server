![](https://travis-ci.org/bwsw/cs-vault-server.svg?branch=master) [![Coverage Status](https://coveralls.io/repos/github/bwsw/cs-vault-server/badge.svg?branch=master)](https://coveralls.io/github/bwsw/cs-vault-server?branch=master)

# cs-vault-server
CloudStack Vault Plugin Server Handler

# Technical

[Travis Build History](https://travis-ci.org/bwsw/cs-vault-server/builds)

Quick start
-----------
For start server in docker container you should:

1. Required variables.env file with next variables:
    * `TOKEN_PERIOD` - lifetime of vault token in days
    * `ACCOUNTS_VAULT_BASIC_PATH` - path to cloudstack accouts secrets in vault
    * `KAFKA_SERVER_LIST` - list with kafka urls
    * `KAFKA_TOPIC` - kafka topic with cloudstack events
    * `ZOOKEEPER_URL` - zookeeper url
    * `ZOOKEEPER_RETRY_DELAY` - delay after unsuccessful connection attempt to zookeeper before retrying
    * `VAULT_URL` - vault url
    * `VAULT_ROOT_TOKEN` - root token for get access to vault server
    * `VAULT_RETRY_DELAY` -  delay after unsuccessful connection attempt to vault before retrying
    * `CS_API_URL_LIST` - list with cloudstack urls
    * `CS_API_KEY` - api key for get access to cloudstack server
    * `CS_SECRET_KEY` - secret key for get access to cloudstack server
    * `CS_RETRY_DELAY` - delay after unsuccessful connection attempt to cloudstack before retrying
2. Execute command:
    "docker run --env-file variables.env medvedevbwsw/cs-vault-server:latest"

If you need to create local docker container you should execute the next command:
    "docker build -t REPOSITORY:TAG ."
     where REPOSITORY - container name, TAG - version of container

If you need creating you docker container in DockerHub with help of Travis,
when you push project into GitHub (master branch), you should:

1. Authorize your Travis-CI account for access to your GitHub account;
2. Set the next environment variables in the Travis-CI: "DOCKER_USERNAME", "DOCKER_PASSWORD",
   where DOCKER_USERNAME - your user name in DockerHub, and DOCKER_PASSWORD - your password in DockerHub
