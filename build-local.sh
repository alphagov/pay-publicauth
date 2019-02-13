#!/bin/bash

set -e

cd "$(dirname "$0")"

mvn -DskipITs -DskipTests clean package
docker build -t govukpay/publicauth:local .
