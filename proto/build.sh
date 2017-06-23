#!/bin/bash
cd `dirname $0`

mkdir -p build
../tools/thrift/thrift --gen c_nano -o build src/main/thrift/boombox.thrift
../tools/thrift/thrift --gen java -o build src/main/thrift/boombox.thrift

# Work around to get this to work with the arduino toolchain.  At some point I gotta
# get the cmake arduino toolchain working for the 101...until then
sed -i 's/<thrift-nano\//</g' build/gen-c_nano/*
cp build/gen-c_nano/* ../arduino/boombox