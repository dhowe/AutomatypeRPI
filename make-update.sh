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
jar cvf /tmp/$ZIP  *.* lib src bin
jar tf /tmp/$ZIP
echo Copying $ZIP to rednoise-ftp...
scp /tmp/$ZIP $RED:~/www/ftp
