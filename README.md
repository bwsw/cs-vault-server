# cs-vault-server
CloudStack Vault Plugin Server Handler. This Instance for handling ApacheCloudStack event notification:
create and write into entities tags (account, vm) Vault HashiCorp tokens, which provide access to secrets of the Vault.

## Quick start

To start a server in Docker container you should:

1. Provide a file 'variables.env' containing the following required variables:
    * `TAG_NAME_PREFIX` - prefix for token name tag
    * `KAFKA_ENDPOINTS` - list of kafka endpoints, which are separated by comma (for example "localhost:9092")
    * `KAFKA_TOPICS` - kafka topics containing cloudstack events, which are separated by comma
    * `KAFKA_CONSUMER_POLL_TIMEOUT` - time of Kafka consumer poll (10000ms by default)
    * `KAFKA_GROUP_ID` - id of Kafka group
    * `EVENT_COUNT` - max event count for one-off processing (10 by default)
    * `ZOOKEEPER_ENDPOINTS` - list of zookeeper endpoints, which are separated by comma
    * `ZOOKEEPER_RETRY_DELAY` - a delay between unsuccessful connection attempt to zookeeper and repeated attempt
    * `ZOOKEEPER_ROOT_NODE` - a root node for keeping application data in zookeeper ("/cs_vault_server" by default)
    * `ZOOKEEPER_MASTER_LATCH_NODE` - a node for master latch keeping ("/cs_vault_server_latch" by default)
    * `VAULT_ENDPOINTS` - list of vault endpoints with "http://" prefix, which are separated by comma
    * `VAULT_ROOT_TOKEN` - root token providing an access to a vault server
    * `VAULT_RETRY_DELAY` -  a delay between unsuccessful connection attempt to vault and repeated attempt
    * `VAULT_TOKEN_PERIOD` - lifetime of vault token in days (3562 days by default)
    * `VAULT_ACCOUNTS_BASIC_PATH` - path to cloudstack accounts' secrets in vault ("secret/cs/accounts/" by default). Last "/" is required
    * `VAULT_VMS_BASIC_PATH` - path to vms' secrets in vault ("secret/cs/vms/" by default). Last "/" is required
    * `CS_ENDPOINTS` - list of cloudstack endpoints, which are separated by comma
    * `CS_API_KEY` - api key providing an access to a cloudstack server
    * `CS_SECRET_KEY` - secret key providing an access to a cloudstack server
    * `CS_RETRY_DELAY` - a delay between unsuccessful connection attempt to cloudstack and repeated attempt
2. Run the following command:
    "docker run --env-file variables.env bwsw/cs-vault-server:latest"

To create a local docker image you should run the following command:
```bash
    docker build -t REPOSITORY:TAG .
```
where REPOSITORY - container name, TAG - version of container
   
## Integration tests

A machine which is being used to run integration tests have to have docker client.

1. Add local environment variables:
    * `IT_KAFKA_HOST` - host of Kafka, for example - "localhost"
    * `IT_KAFKA_PORT` - port of Kafka, for example - "9092"
    * `IT_KAFKA_ENDPOINTS` - $KAFKA_HOST:$KAFKA_PORT
    * `IT_ZOOKEEPER_PORT` - port of ZooKeeper, for example - "2181"
    * `IT_ZOOKEEPER_ENDPOINTS` - $IT_KAFKA_HOST:$IT_ZOOKEEPER_PORT
    * `IT_VAULT_ENDPOINTS` - endpoints of Vault, for example "http://localhost:8200"
    * `IT_VAULT_ROOT_TOKEN` - string which is used such as root Vault token
    * `FAULT_TEST_VAULT_ROOT_TOKEN` - string which is used such as root token in Vault which will be run (in docker container) in fault tolerance tests
    * `FAULT_TEST_VAULT_PORT` - port of Vault for fault tolerance tests, have to be different from port in IT_VAULT_ENDPOINTS
    * `IT_CS_ENDPOINTS` - endpoints of CloudStack, for example "http://localhost:8888/client/api"
    * `IT_CS_ADMIN_LOGIN` - admin login for authorization on CloudStack server
    * `IT_CS_ADMIN_PASSWORD` - admin password for authorization on CloudStack server
    * `IT_CS_PORT` - port from IT_CS_ENDPOINTS endpoint
    
2. Run Kafka server in docker container:
```bash
    docker run -d --rm --name spotify-kafka --tty=true -p $IT_ZOOKEEPER_PORT:$IT_ZOOKEEPER_PORT \
                                                       -p $IT_KAFKA_PORT:$IT_KAFKA_PORT \
                                                       --env ADVERTISED_HOST=$IT_KAFKA_HOST \
                                                       --env ADVERTISED_PORT=$IT_KAFKA_PORT spotify/kafka
```
3. Run Vault server in docker container:
```bash
    docker run --cap-add=IPC_LOCK -e VAULT_DEV_ROOT_TOKEN_ID="${IT_VAULT_ROOT_TOKEN}" \
                                  -e VAULT_DEV_LISTEN_ADDRESS="0.0.0.0:$IT_VAULT_PORT" \
                                  -p $IT_VAULT_PORT:$IT_VAULT_PORT --rm -d --name vault-dev-server vault:0.8.3
```
4. Run CloudStack server in docker container:
```bash
    docker run --rm -e KAFKA_HOST="${IT_KAFKA_HOST}" \
                    -e KAFKA_PORT="${IT_KAFKA_PORT}" \
                    -e KAFKA_TOPIC="${IT_KAFKA_TOPIC}" \
                    --name cloudstack-kafka-sim -d -p $IT_CS_PORT:$IT_CS_PORT bwsw/cs-simulator-kafka:4.10.3-NP
```

5. After the end of the cloudstack simulator deploying (you can check it like ![this](jenkins/run_cs_kafka_vault.sh)) execute: `sbt it:test`

Note: fault tolerance tests could be failed due to unstable starting of vault container.
   
## Versioning

Server has the same version as Apache CloudStack server, and used Vault version 0.8.3

Also see:
* [Event processing logic](docs/logic.md)
* [UML diagrams](docs/diagrams/)
