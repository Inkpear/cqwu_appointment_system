docker run -d \
    --user=$UID:$(id -g $USER) \
    --name mysql-cqwu-appointment \
    -p 3306:3306 \
    -e MYSQL_ROOT_PASSWORD=admin123 \
    -v ~/docker-volumes/mysql-cqwu-appointment/data:/var/lib/mysql:Z \
    -v ~/docker-volumes/mysql-cqwu-appointment/conf/my.cnf:/etc/mysql/conf.d/custom.cnf:Z \
    --restart=always \
    mysql:8.0

