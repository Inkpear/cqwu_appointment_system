docker run -d \
    --name mysql-cqwu-appointment \
    -p 3306:3306 \
    -e MYSQL_ROOT_PASSWORD=admin123 \
    -v ~/docker-volumes/mysql-cqwu-appointment/data:/var/lib/mysql \
    -v ~/docker-volumes/mysql-cqwu-appointment/conf/my.cnf:/etc/mysql/conf.d/custom.cnf \
    --restart=always \
    mysql:5.7

