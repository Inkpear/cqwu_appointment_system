docker run -d \
    --name redis-cqwu-appointment \
    -p 6379:6379 \
    -v ~/docker-volumes/redis-cqwu-appointment/data:/data \
    --restart=always \
    redis:latest \
    redis-server --requirepass admin123