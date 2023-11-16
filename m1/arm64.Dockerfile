FROM eclipse-temurin:11-jre@sha256:4f82e758d7dfc272cfe04008d722dcf0b020044584ceb9f4326e54cc67844291

ARG DNS_TTL=15

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

RUN echo networkaddress.cache.ttl=$DNS_TTL >> "$JAVA_HOME/conf/security/java.security"

RUN apt-get update && apt-get install -y tini wget

# Add RDS CA certificates to the default truststore
RUN wget -qO - https://s3.amazonaws.com/rds-downloads/rds-ca-2019-root.pem       | keytool -import -cacerts -storepass changeit -noprompt -alias rds-ca-2019-root \
 && wget -qO - https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem | keytool -import -cacerts -storepass changeit -noprompt -alias rds-combined-ca-bundle

ENV PORT 8080
ENV ADMIN_PORT 8081

EXPOSE 8080
EXPOSE 8081

WORKDIR /app

COPY docker-startup.sh /app/docker-startup.sh
COPY target/*.yaml /app/
COPY target/pay-*-allinone.jar /app/

ENTRYPOINT ["tini", "-e", "143", "--"]

CMD ["bash", "./docker-startup.sh"]
