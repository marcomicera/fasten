FROM alpine:latest

# Installing JDK, Maven, and kubectl
RUN apk update && \
    apk add --no-cache openjdk11 maven curl && \
    curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl && \
    chmod +x ./kubectl && \
    mv ./kubectl /usr/local/bin

WORKDIR /home/demo

# Launching the demo
CMD mvn install -Dmaven.test.skip=true && \
  mvn install exec:java \
  -f analyzer/compliance-analyzer/pom.xml \
  -Dexec.args="--repository analyzer/compliance-analyzer/dummyKafkaTopic.json --cluster-credentials credentials.json"
