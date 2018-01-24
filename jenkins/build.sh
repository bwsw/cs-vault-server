#!/bin/bash -e

echo "---------------------------------------------"
echo "----------------- Unit tests ----------------"
echo "---------------------------------------------"

sbt clean coverage test coverageReport

echo "---------------------------------------------"
echo "-------------- Integration tests ------------"
echo "---------------------------------------------"

sh ./jenkins/run_cs_kafka_vault.sh

#sbt it:test

echo "---------------------------------------------"
echo "-------------- Scalastyle check ------------"
echo "---------------------------------------------"

sbt scalastyle

sbt test:scalastyle

#sbt it:scalastyle

echo "git branch: $GIT_BRANCH"
if [ -n "$GIT_BRANCH" ]; then
    if [ "$GIT_BRANCH" = "origin/master" ]; then
        echo "---------------------------------------------"
        echo "------- Publish to Docker repository ---------"
        echo "---------------------------------------------"
        TAG=`cat version`;
        docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD";
        CONTAINER_NAME=$DOCKER_USERNAME/cs-vault-server;
        docker build -t $CONTAINER_NAME:latest .;
        docker images;
        docker push $CONTAINER_NAME:latest;
        docker tag $CONTAINER_NAME $CONTAINER_NAME:$TAG;
        docker push $CONTAINER_NAME:$TAG;
    fi
fi
