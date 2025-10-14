docker run -d \
    --name redis-cqwu-appointment \
    -v ~/docker-volumes/redis-cqwu-appointment/data:/data \
    --restart=always \
    redis:latest \
    redis-server --requirepass admin123