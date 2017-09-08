#!/usr/bin/env bash
#Needs set all necessery environment variables in variables.env before run script

set -e

sbt docker:publishLocal

docker run --env-file variables.env -d cs-vault-server:1.0
