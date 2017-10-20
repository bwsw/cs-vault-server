FROM openjdk:8-jre-alpine
MAINTAINER BITWORKS medvedev_vv@bw-sw.com


ENV SBT_VERSION  0.13.16
ENV SBT_JAR      https://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/$SBT_VERSION/sbt-launch.jar


ADD  $SBT_JAR  /usr/local/bin/sbt-launch.jar
COPY sbt.sh    /usr/local/bin/sbt

RUN apk update && apk add bash

RUN echo "==> fetch all sbt jars from Maven repo..."       && \
    echo "==> [CAUTION] this may take several minutes!!!"  && \
    sbt


VOLUME [ "/cs-vault-server" ]
WORKDIR /cs-vault-server
ADD . /cs-vault-server


# Define default command.
ENTRYPOINT ["sbt"]
CMD ["run"]
