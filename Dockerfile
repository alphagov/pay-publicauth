FROM java:8-jre
ENV JAVA_HOME /usr/lib/jvm/java-8-*/
ENV PORT 8080
ENV ADMIN_PORT 8081

EXPOSE 8080 8081
WORKDIR /app

ADD target/*.yaml /app/
ADD target/pay-*-allinone.jar /app/

CMD sleep 2 && java -jar *-allinone.jar server *.yaml
