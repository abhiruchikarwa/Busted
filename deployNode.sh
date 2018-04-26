#!/bin/bash

set -x

# Kill existing version of deployment
killall ng
killall java
rm -r webapp/

# Deploy Java/WAR  
nohup java -jar app/phaseC/cs5500-spring2018-team212/target/cs5500-spring2018-team212-0.0.1-SNAPSHOT.war > nohup_java.out &

# Deploy UI/NG
unzip app/phaseC/cs5500-spring2018-team212/target/cs5500-spring2018-team212-0.0.1-SNAPSHOT.war  -d webapp/
cd webapp/Busted/
sed -i 's/localhost:8080/ec2-18-188-212-155.us-east-2.compute.amazonaws.com:8080/' src/environments/environment.ts
rm package-lock.json
npm install
nohup ng serve --host 0.0.0.0 --disable-host-check > nohup_angular.out &
