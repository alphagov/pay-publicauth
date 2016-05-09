#!/usr/bin/env bash
if [ "${ENABLE_NEWRELIC}" == "yes" ]; then
  NEWRELIC_JVM_FLAG="-javaagent:/app/newrelic/newrelic.jar"
fi

java -jar *-allinone.jar waitOnDependencies *.yaml && \
java -jar *-allinone.jar db migrate *.yaml && \
java ${NEWRELIC_JVM_FLAG} -jar *-allinone.jar server *.yaml
