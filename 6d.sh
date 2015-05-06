#!/bin/sh 
set -x 

java -jar $(ls -t ./target/six*with*.jar | head -1) "$1" "$2"
#java -jar $(ls -t ./target/six*.jar | head -1) $1 $2

