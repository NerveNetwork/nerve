#!/bin/bash

for (( i=1; i <= 2; i++ ))
do
 curl https://bscscan.com/tokens?p=$i > token_$i.txt
done

echo "curl done."
exit 0
