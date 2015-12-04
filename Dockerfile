FROM java:8-jre

ENV JAVA_HOME /usr/lib/jvm/java-8-*/
ENV PORT 8080
ENV ADMIN_PORT 8081
ENV ENABLE_NEWRELIC no

EXPOSE 8080
EXPOSE 8081

WORKDIR /app

ADD target/*.yaml /app/
ADD target/pay-*-allinone.jar /app/
ADD docker-startup.sh /app/docker-startup.sh
ADD newrelic/* /app/newrelic/

CMD java -jar *-allinone.jar dbwait *.yaml 30 && \
    java -jar *-allinone.jar db migrate *.yaml && \
    bash ./docker-startup.sh
