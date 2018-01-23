#!/bin/bash -e
#VAULT
docker run --cap-add=IPC_LOCK --name vault-dev-server \
                              -e VAULT_DEV_ROOT_TOKEN_ID="${IT_VAULT_ROOT_TOKEN}" \
                              -e VAULT_DEV_LISTEN_ADDRESS="${IT_VAULT_HOST}" vault
#KAFKA
KAFKA_ACKS=all
KAFKA_WRITE_RETRIES=1

docker run -d --rm --name spotify-kafka --tty=true -p $IT_ZOOKEEPER_PORT:$IT_ZOOKEEPER_PORT -p $IT_KAFKA_PORT:$IT_KAFKA_PORT --env ADVERTISED_HOST=$IT_KAFKA_HOST --env ADVERTISED_PORT=$IT_KAFKA_PORT spotify/kafka

#CLOUDSTACK
docker run --rm -e KAFKA_HOST="${IT_KAFKA_HOST}" \
                -e KAFKA_PORT="${IT_KAFKA_PORT}" \
                -e KAFKA_ACKS="${KAFKA_ACKS}" \
                -e KAFKA_TOPIC="${IT_KAFKA_TOPIC}" \
                -e KAFKA_WRITE_RETRIES="${KAFKA_WRITE_RETRIES}" \
                --name cloudstack-kafka-sim -d -p $CS_PORT:$CS_PORT bwsw/cs-simulator-kafka:4.10.3-NP

ITERATIONS=40
SLEEP=30

echo "wait for CloudStack simulator deploys"

for i in `seq 1 ${ITERATIONS}`
do
    curl -s -I http://localhost:${IT_CS_PORT}/client/ | head -1 | grep "200"

    if [ $? -eq 0 ]
    then
        echo "OK"
        break
    else
        echo "retry number $i"
        sleep ${SLEEP}
    fi

    if [ ${i} -eq ${ITERATIONS} ]
    then
        exit 1
    fi
done
