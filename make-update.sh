#!/bin/bash


set -e # die on errors


if [ $# -lt "1"  ]
then
    echo
    echo "  error:   version required"
    echo "  usage:   make-update.sh [64]"
    exit
fi

VERSION=$1

ZIP=automatype_v$VERSION.zip
if [ -d "/tmp/dist" ]; then
    rm -rf "/tmp/dist"
fi
mkdir /tmp/dist
cp -r *.* lib src bin /tmp/dist
pushd /tmp
jar cvf $ZIP dist 
jar tf $ZIP
echo Copying $ZIP to rednoise-ftp...
scp $ZIP $RED:~/www/ftp
popd
