#!/bin/sh

java -Xmx2g -jar ./target/replay_maven-1.0-jar-with-dependencies.jar -h https://pep.bluestaq.com -d ./data/ -u system.darpa-ack -p "${UDL_PWD}"
