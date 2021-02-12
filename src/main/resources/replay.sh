#!/bin/sh

host="${1}"

java -Xmx2g -jar ./replay.jar -h "${host}" -d "./data/" -s 200000