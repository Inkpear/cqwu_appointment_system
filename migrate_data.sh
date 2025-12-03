#!/bin/bash

###############################################################################
# 数据迁移脚本：从本地 Docker 迁移到远程服务器 192.168.31.199
# 用途：使用 mysqldump 逻辑备份方式安全迁移 MySQL 数据
###############################################################################

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置信息
LOCAL_CONTAINER="mysql-cqwu-appointment"
LOCAL_DB="cqwu"
LOCAL_USER="root"
LOCAL_PASSWORD="admin123"

REMOTE_HOST="192.168.31.199"
REMOTE_PORT="3306"
REMOTE_DB="cqwu"
REMOTE_USER="root"
REMOTE_PASSWORD="admin123"

BACKUP_DIR="./backup"
BACKUP_FILE="${BACKUP_DIR}/cqwu_backup_$(date +%Y%m%d_%H%M%S).sql"

###############################################################################
# 函数定义
###############################################################################

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_docker_container() {
    if ! docker ps | grep -q "$LOCAL_CONTAINER"; then
        log_error "本地 MySQL 容器 '$LOCAL_CONTAINER' 未运行"
        log_info "请先启动容器: docker-compose up -d mysql"
        exit 1
    fi
    log_info "本地 MySQL 容器检查通过"
}

check_remote_connection() {
    log_info "检查远程数据库连接..."
    if mysql -h"$REMOTE_HOST" -P"$REMOTE_PORT" -u"$REMOTE_USER" -p"$REMOTE_PASSWORD" -e "SELECT 1" &>/dev/null; then
        log_info "远程数据库连接成功"
    else
        log_error "无法连接到远程数据库 $REMOTE_HOST:$REMOTE_PORT"
        log_info "请检查:"
        log_info "  1. 远程数据库是否运行"
        log_info "  2. 网络连接是否正常"
        log_info "  3. 用户名密码是否正确"
        log_info "  4. 防火墙是否开放 3306 端口"
        exit 1
    fi
}

create_backup_dir() {
    if [ ! -d "$BACKUP_DIR" ]; then
        mkdir -p "$BACKUP_DIR"
        log_info "创建备份目录: $BACKUP_DIR"
    fi
}

backup_local_database() {
    log_info "开始备份本地数据库..."
    log_info "备份文件: $BACKUP_FILE"
    
    # 使用 docker exec 在容器内执行 mysqldump
    docker exec "$LOCAL_CONTAINER" mysqldump \
        -u"$LOCAL_USER" \
        -p"$LOCAL_PASSWORD" \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --hex-blob \
        --databases "$LOCAL_DB" \
        > "$BACKUP_FILE"
    
    if [ $? -eq 0 ]; then
        local file_size=$(du -h "$BACKUP_FILE" | cut -f1)
        log_info "备份完成! 文件大小: $file_size"
    else
        log_error "备份失败"
        exit 1
    fi
}

check_remote_database() {
    log_info "检查远程数据库是否存在..."
    
    local db_exists=$(mysql -h"$REMOTE_HOST" -P"$REMOTE_PORT" -u"$REMOTE_USER" -p"$REMOTE_PASSWORD" \
        -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='$REMOTE_DB';" \
        --skip-column-names 2>/dev/null)
    
    if [ -n "$db_exists" ]; then
        log_warn "远程数据库 '$REMOTE_DB' 已存在"
        read -p "是否要覆盖现有数据? (yes/no): " confirm
        if [ "$confirm" != "yes" ]; then
            log_info "用户取消操作"
            exit 0
        fi
        log_warn "将删除并重建数据库 '$REMOTE_DB'"
    else
        log_info "远程数据库 '$REMOTE_DB' 不存在，将创建"
    fi
}

restore_to_remote() {
    log_info "开始恢复数据到远程数据库..."
    
    # 如果需要，先删除并重建数据库
    mysql -h"$REMOTE_HOST" -P"$REMOTE_PORT" -u"$REMOTE_USER" -p"$REMOTE_PASSWORD" <<EOF
DROP DATABASE IF EXISTS $REMOTE_DB;
CREATE DATABASE $REMOTE_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EOF
    
    log_info "正在导入数据..."
    
    # 修改备份文件中的数据库名称并导入
    sed "s/\`$LOCAL_DB\`/\`$REMOTE_DB\`/g" "$BACKUP_FILE" | \
        mysql -h"$REMOTE_HOST" -P"$REMOTE_PORT" -u"$REMOTE_USER" -p"$REMOTE_PASSWORD"
    
    if [ $? -eq 0 ]; then
        log_info "数据恢复完成!"
    else
        log_error "数据恢复失败"
        exit 1
    fi
}

verify_migration() {
    log_info "验证数据迁移..."
    
    # 统计本地表数量
    local local_tables=$(docker exec "$LOCAL_CONTAINER" mysql -u"$LOCAL_USER" -p"$LOCAL_PASSWORD" \
        -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$LOCAL_DB';" \
        --skip-column-names 2>/dev/null)
    
    # 统计远程表数量
    local remote_tables=$(mysql -h"$REMOTE_HOST" -P"$REMOTE_PORT" -u"$REMOTE_USER" -p"$REMOTE_PASSWORD" \
        -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$REMOTE_DB';" \
        --skip-column-names 2>/dev/null)
    
    log_info "本地表数量: $local_tables"
    log_info "远程表数量: $remote_tables"
    
    if [ "$local_tables" -eq "$remote_tables" ]; then
        log_info "✓ 表数量一致"
    else
        log_warn "⚠ 表数量不一致，请检查"
    fi
    
    # 显示远程数据库中的表
    log_info "远程数据库表列表:"
    mysql -h"$REMOTE_HOST" -P"$REMOTE_PORT" -u"$REMOTE_USER" -p"$REMOTE_PASSWORD" \
        -e "USE $REMOTE_DB; SHOW TABLES;"
}

show_summary() {
    echo ""
    echo "========================================"
    log_info "数据迁移完成!"
    echo "========================================"
    log_info "备份文件保存在: $BACKUP_FILE"
    log_info "远程数据库: $REMOTE_HOST:$REMOTE_PORT/$REMOTE_DB"
    echo ""
    log_info "后续步骤:"
    log_info "  1. 修改 application.properties 中的数据库连接"
    log_info "     spring.datasource.url=jdbc:mysql://$REMOTE_HOST:$REMOTE_PORT/$REMOTE_DB..."
    log_info "  2. 重启应用程序"
    log_info "  3. 验证功能是否正常"
    echo ""
    log_warn "备份文件请妥善保管，以便回滚"
    echo "========================================"
}

###############################################################################
# 主流程
###############################################################################

main() {
    log_info "开始数据迁移流程"
    echo ""
    
    # 1. 检查本地容器
    check_docker_container
    
    # 2. 检查远程连接
    check_remote_connection
    
    # 3. 创建备份目录
    create_backup_dir
    
    # 4. 备份本地数据库
    backup_local_database
    
    # 5. 检查远程数据库
    check_remote_database
    
    # 6. 恢复到远程
    restore_to_remote
    
    # 7. 验证迁移
    verify_migration
    
    # 8. 显示摘要
    show_summary
}

# 执行主流程
main
