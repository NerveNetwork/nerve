#!/bin/bash
cd `dirname $0`
if [ -z "$1" ]; then
    cat <<- EOF
        Desc: need set jre path, jre version must equals 11
        Usage: ./build.sh ./jre
EOF
    exit 0
fi
WORK_PATH=`pwd`
cd ..
PROJECT_PATH=`pwd`
PACKAGE_NAME=NERVE_Wallet.tar.gz
./release.sh linux $PACKAGE_NAME
mv $PACKAGE_NAME docker
cd $WORK_PATH
docker build -t nerve-wallet .

