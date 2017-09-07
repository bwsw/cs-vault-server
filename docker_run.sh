#!/usr/bin/env bash
#Needs set all necessery environment variables before run script

set -e

sbt docker:publishLocal

docker run -d \
  -e TOKEN_PERIOD="${TOKEN_PERIOD}" \
  -e ACCOUNTS_VAULT_BASIC_PATH="${ACCOUNTS_VAULT_BASIC_PATH}" \
  -e KAFKA_SERVER_LIST="${KAFKA_SERVER_LIST}" \
  -e KAFKA_TOPIC="${KAFKA_TOPIC}" \
  -e VAULT_URL="${VAULT_URL}" \
  -e VAULT_ROOT_TOKEN="${VAULT_ROOT_TOKEN}" \
  -e VAULT_RETRY_DELAY="${VAULT_RETRY_DELAY}" \
  -e CS_API_URL_LIST="${CS_API_URL_LIST}" \
  -e CS_API_KEY="${CS_API_KEY}" \
  -e CS_SECRET_KEY="${CS_SECRET_KEY}" \
  -e CS_RETRY_DELAY="${CS_RETRY_DELAY}" \
  -e ZOOKEEPER_URL="${ZOOKEEPER_URL}" \
  -e ZOOKEEPER_RETRY_DELAY="${ZOOKEEPER_RETRY_DELAY}" \
  cs-vault-server:1.0
