#!/usr/bin/env bash
./gradlew clean jar; scp ./build/libs/deployer-0.0.1.jar $MACHINE_CI_USER@$MACHINE_CI:/downloads