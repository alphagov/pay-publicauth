FROM                java:8-jre
ENV JAVA_HOME       /usr/lib/jvm/java-8-*/
WORKDIR             /app
ADD                 target/*.yaml /app/
ADD                 target/pay-*-allinone.jar /app/
ADD                 migrate.sh /app/
ENV                 PATH $PATH:/app/
CMD                 sleep 10 && java -jar *-allinone.jar server *.yaml
