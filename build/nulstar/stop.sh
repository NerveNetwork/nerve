#!/bin/bash
cd `dirname $0`

echo "do stop" >> ./Logs/stop
KILL_WAIT_COUNT=120
stop(){
    pid=$1;
    kill $pid > /dev/null 2>&1
    COUNT=0
    while [ $COUNT -lt ${KILL_WAIT_COUNT} ]; do
        echo -e ".\c"
        sleep 1
        let COUNT=$COUNT+1
        PID_EXIST=`ps -f -p $pid | grep -w $2`
        if [ -z "$PID_EXIST" ]; then
#            echo -e "\n"
#            echo "stop ${pid} success."
            return 0;
        fi
    done

    echo "stop ${pid} failure,dump and kill -9 it."
    kill -9 $pid > /dev/null 2>&1
}
BIN_PATH=`pwd`
APP_PID=`ps -ef|grep -w "${BIN_PATH}/Modules/Nulstar/Nulstar/0.1.0/Nulstar"|grep -v grep|awk '{print $2}'`
if [ -z "${APP_PID}" ]; then
 echo "Nuls wallet not running"
        exit 0
fi
echo "stoping"
total=0
NERVE_STOP_FILE=`pwd`/.nerve-stop
echo 1 > $NERVE_STOP_FILE
echo "call stop" >> ./Logs/stop
while [ 1 == 1 ]
do
    if((total==5)); then
        break;
    fi
    cmd=`cat $NERVE_STOP_FILE`;
    if [ "$cmd" == "2" ]; then
        echo "block notify stop ready" >> ./Logs/stop
        break;
    fi
    echo "sleep $total block pid: `ps -ef|grep -w "Dapp.name=block"|grep -v grep|awk '{print $2}'`" >> ./Logs/stop
    ((total+=1))
    sleep 1
done
for pid in $APP_PID
do
   stop $pid "`pwd`/Modules/Nulstar/Nulstar/0.1.0/Nulstar"
done
echo ""
echo "shutdown success"
if [ -f "./.DONE" ]; then
    rm ./.DONE
fi
