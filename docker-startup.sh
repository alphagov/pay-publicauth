#!/usr/bin/env bash
echo "determine migration..."
set -eu
RUN_MIGRATION=${RUN_MIGRATION:-false}
RUN_APP=${RUN_APP:-true}

java -jar *-allinone.jar waitOnDependencies *.yaml

if [ "$RUN_MIGRATION" == "true" ]; then
    echo "running migration..."
  java -jar *-allinone.jar db migrate *.yaml
fi

if [ "$RUN_APP" == "true" ]; then
    echo "running app..."
  java $JAVA_OPTS -jar *-allinone.jar server *.yaml
fi

exit 0
