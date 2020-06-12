#!/bin/bash
cd `dirname $0`
OS=$1
JRE=$2
if [ -z "$1" ]; then
    cat <<- EOF
        Desc: need set target os;
        Usage: ./release.sh window
               ./release.sh linux
               ./release.sh macos
EOF
    exit 0
fi
./package -a cross-chain
./package -a protocol-update
./package -a nuls-api
./package -a converter
./package -a dex
./package -a quotation
if [ -n "$JRE" ]; then
    JRE=" -J $JRE "
fi
./package -O ${OS} -o NERVE_Wallet $JRE
PACKAGE_NAME="NERVE_Wallet_${OS}_v`cat ./.package-version`.tar"
if [ -n "$3" ]; then
    PACKAGE_NAME=$2
fi
cp config/nuls.ncf NERVE_Wallet/nuls.ncf
cp config/genesis-block.json NERVE_Wallet/genesis-block.json
tar -czf $PACKAGE_NAME NERVE_Wallet
rm -rf NERVE_Wallet
