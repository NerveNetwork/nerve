#!/bin/bash

for (( i=1; i <= 20; i++ ))
do
 curl https://etherscan.io/tokens?p=$i > token_$i.txt
done

echo "curl done."
exit 0
