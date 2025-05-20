#!/bin/bash
# ANI-RSS 完全卸载脚本
# 版本: 1.1
# 功能: 移除服务、配置、数据和系统用户

# 定义颜色代码
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # 重置颜色

# 配置常量
SERVICE_NAME="ani-rss.service"
INSTALL_DIR="/opt/ani-rss"
SERVICE_USER="ani-rss"
FIREWALL_PORT=7789  # 根据安装时使用的端口修改

# 检查root权限
check_root() {
    if [ "$EUID" -ne 0 ]; then
        echo -e "${RED}错误：请使用sudo或以root身份运行此脚本${NC}"
        exit 1
    fi
}

# 确认卸载
confirm_uninstall() {
    echo -e "${YELLOW}即将执行以下操作："
    echo "1. 停止并禁用服务: ${SERVICE_NAME}"
    echo "2. 删除安装目录: ${INSTALL_DIR}"
    echo "3. 删除系统用户: ${SERVICE_USER}"
    echo "4. 移除防火墙规则(如果存在)"
    echo -e "\n${RED}该操作无法撤销！所有程序数据将被删除。${NC}"

    read -p "是否继续？(y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${GREEN}已取消卸载${NC}"
        exit 0
    fi
}

# 停止并移除服务
remove_service() {
    echo -e "${YELLOW}正在停止服务...${NC}"
    if systemctl is-active --quiet "$SERVICE_NAME"; then
        systemctl stop "$SERVICE_NAME" || {
            echo -e "${RED}服务停止失败，尝试强制终止...${NC}"
            pkill -f "ani-rss-jar-with-dependencies.jar" && echo -e "${GREEN}进程已终止${NC}"
        }
    fi

    echo -e "${YELLOW}禁用服务...${NC}"
    if systemctl is-enabled --quiet "$SERVICE_NAME"; then
        systemctl disable "$SERVICE_NAME" >/dev/null 2>&1
    fi

    echo -e "${YELLOW}删除服务文件...${NC}"
    rm -f "/etc/systemd/system/${SERVICE_NAME}" \
       "/etc/systemd/system/multi-user.target.wants/${SERVICE_NAME}" \
       "/etc/systemd/system/${SERVICE_NAME}.d/"* >/dev/null 2>&1

    systemctl daemon-reload
    systemctl reset-failed
}

# 删除安装目录
remove_install_dir() {
    echo -e "${YELLOW}清理安装目录...${NC}"
    if [ -d "$INSTALL_DIR" ]; then
        rm -rf "$INSTALL_DIR" && echo -e "${GREEN}目录已删除${NC}" || {
            echo -e "${RED}目录删除失败，请手动检查${NC}"
            exit 1
        }
    else
        echo -e "${YELLOW}安装目录不存在，跳过${NC}"
    fi
    rm /usr/local/bin/ani-rss
}

# 删除系统用户
remove_service_user() {
    echo -e "${YELLOW}移除系统用户...${NC}"
    if id "$SERVICE_USER" &>/dev/null; then
        userdel -r "$SERVICE_USER" >/dev/null 2>&1 && \
        echo -e "${GREEN}用户已删除${NC}" || {
            echo -e "${RED}用户删除失败，请手动检查${NC}"
            exit 1
        }
    else
        echo -e "${YELLOW}用户不存在，跳过${NC}"
    fi
}

# 清理防火墙规则
clean_firewall() {
    echo -e "${YELLOW}清理防火墙规则...${NC}"
    # 处理UFW
    if command -v ufw >/dev/null; then
        if ufw status | grep -q "$FIREWALL_PORT"; then
            ufw delete allow "$FIREWALL_PORT/tcp"
            echo -e "${GREEN}UFW规则已移除${NC}"
        fi
    fi

    # 处理Firewalld
    if command -v firewall-cmd >/dev/null; then
        if firewall-cmd --list-ports | grep -q "$FIREWALL_PORT/tcp"; then
            firewall-cmd --remove-port="$FIREWALL_PORT/tcp" --permanent
            firewall-cmd --reload
            echo -e "${GREEN}Firewalld规则已移除${NC}"
        fi
    fi
}

# 最终验证
verify_uninstall() {
    echo -e "\n${YELLOW}验证卸载结果：${NC}"
    local error=0

    # 检查服务状态
    if systemctl is-active --quiet "$SERVICE_NAME"; then
        echo -e "${RED}错误：服务仍在运行${NC}"
        error=1
    fi

    # 检查安装目录
    if [ -d "$INSTALL_DIR" ]; then
        echo -e "${RED}错误：安装目录仍然存在${NC}"
        error=1
    fi

    # 检查用户
    if id "$SERVICE_USER" &>/dev/null; then
        echo -e "${RED}错误：系统用户仍然存在${NC}"
        error=1
    fi

    # 综合结果
    if [ $error -eq 0 ]; then
        echo -e "${GREEN}验证通过：卸载完成${NC}"
    else
        echo -e "${RED}存在未完全清理的组件，请手动处理${NC}"
        exit 1
    fi
}

remove_jdk() {
    # 最终确认
    read -p "是否卸载JDk?(y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${GREEN}已取消JDK卸载${NC}"
        return
    fi

    # 执行卸载命令
    echo -e "${YELLOW}正在卸载JDK...${NC}"
    if command -v apt &>/dev/null; then
        apt purge -y openjdk-17-jdk >/dev/null 2>&1
    elif command -v yum &>/dev/null; then
        yum remove -y java-17-openjdk-devel >/dev/null 2>&1
    else
      echo -e "${YELLOW}没有安装jdk${NC}"
      return
    fi

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}JDK卸载完成${NC}"
    else
        echo -e "${RED}JDK卸载过程中出现错误${NC}"
        exit 1
    fi
}

# 主流程
main() {
    check_root
    confirm_uninstall
    remove_service
    remove_install_dir
    remove_service_user
    clean_firewall
    verify_uninstall
    remove_jdk
    echo -e "\n${GREEN}===== 卸载完成 =====${NC}"
}

main
