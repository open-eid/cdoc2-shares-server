FROM openjdk:17-slim
ARG NAME
WORKDIR /opt/cdoc2

COPY erver/docker/docker-entrypoint.sh /
COPY server/docker/application.properties /opt/cdoc2/
COPY server/target/cdoc2-server*.jar /opt/cdoc2/${NAME}.jar
COPY keys/servertruststore.jks keys/cdoc2server.p12 /opt/cdoc2/keys/

ENV NAME ${NAME}

ENTRYPOINT ["/docker-entrypoint.sh"]
