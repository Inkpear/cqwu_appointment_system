#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  CQWU 预约系统 Docker 部署脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查 Docker 和 Docker Compose
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: 未安装 Docker${NC}"
    exit 1
fi

if ! command -v docker compose &> /dev/null && ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}错误: 未安装 Docker Compose${NC}"
    exit 1
fi

# 检查 .env 文件
if [ ! -f .env ]; then
    echo -e "${YELLOW}未找到 .env 文件，从 .env.example 创建...${NC}"
    cp .env.example .env
    echo -e "${YELLOW}请编辑 .env 文件设置密码和配置！${NC}"
    echo -e "${YELLOW}按 Enter 继续使用默认配置，或 Ctrl+C 退出...${NC}"
    read
fi

# 构建并启动服务
echo -e "${GREEN}正在构建 Docker 镜像...${NC}"
docker compose build

echo ""
echo -e "${GREEN}正在启动服务...${NC}"
docker compose up -d

echo ""
echo -e "${GREEN}等待服务启动...${NC}"
sleep 10

# 检查服务状态
echo ""
echo -e "${GREEN}服务状态:${NC}"
docker compose ps

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "应用地址: ${GREEN}http://localhost:8080${NC}"
echo -e "MinIO 控制台: ${GREEN}http://localhost:9001${NC}"
echo ""
echo -e "${YELLOW}查看日志:${NC} docker compose logs -f app"
echo -e "${YELLOW}停止服务:${NC} docker compose down"
echo -e "${YELLOW}重启服务:${NC} docker compose restart app"
echo ""
