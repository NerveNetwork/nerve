#===== 构建镜像
./build.sh <JRE_PATH>

#===== 启动容器
docker run --name nerve-wallet -d \
  -p 18001:7001 \
  -p 18002:7002 \
  -v `pwd`/data:/nerve/data \
  -v `pwd`/logs:/nerve/Logs \
  <NERVE-WALLET-IMAGE>


#===== 查看模块启动状态
docker exec -it nerve-wallet ./check-status

#===== 进入命令行
docker exec -it nerve-wallet ./cmd

