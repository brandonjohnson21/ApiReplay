#!/bin/sh
java -Xmx2g -jar ./target/replay_maven-1.0-jar-with-dependencies.jar -h https://test.unifieddatalibrary.com -d ./data/ -u system.darpa-ack -p "${UDL_PWD}" -s 200000 -t 5
