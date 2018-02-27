#!/bin/bash

PI_ENV=${PI_ENV:-"test"}
PI_OPTS=${PI_OPTS:-"-Xmx512m"}

exec java -Denvironment=$PI_ENV $PI_OPTS -jar kafka-stream.jar
