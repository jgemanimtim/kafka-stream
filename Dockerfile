FROM maven:3-jdk-8

#RUN apt-get update && \ 
#    apt-get install -y thrift-compiler make && \
#    rm -rf /var/lib/apt/lists/*

#COPY core-api-key /root/.ssh/id_rsa
#COPY core-api-key.pub /root/.ssh/id_rsa.pub

#ENV CORE_API_VERSION=develop

COPY  . /usr/src/
COPY pi-server.sh /

RUN ls -1l /usr/src/Kafka-Streams-Country-Counter/


#RUN chmod 755 /root/.ssh && \
#    chmod 600 /root/.ssh/id_rsa && \
#    ssh-keyscan -H github.com >> /root/.ssh/known_hosts && \
RUN cd /usr/src/Kafka-Streams-Country-Counter/ && \
    mvn package -Dmaven.test.skip=true && \
    cp /usr/src/Kafka-Streams-Country-Counter/target/kafka-stream.jar / && \
    find /usr/src/Kafka-Streams-Country-Counter -name "target" | xargs rm -rf && \
    rm -rf /root/.m2/* || true

#EXPOSE 9001

CMD ["/pi-server.sh"]
