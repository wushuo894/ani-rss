#!/bin/bash
# ANI-RSS 一体化安装脚本 with Systemd 服务
# 版本: 2.0
# 适用系统: Ubuntu/Debian/CentOS/RHEL

# 定义颜色代码
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

# 定义常量
INSTALL_DIR="/opt/ani-rss"
SERVICE_USER="ani-rss"
SERVICE_NAME="ani-rss.service"
PORT=7789

# 检查root权限
check_root() {
    [ "$EUID" -ne 0 ] && echo -e "${RED}错误：请使用sudo或以root运行${NC}" && exit 1
}

# 安装JDK
install_jdk() {
    echo -e "${YELLOW}正在检查Java环境...${NC}"
    if which java ; then
        echo -e "${GREEN}检测到JDK已安装${NC}"
        return
    fi

    echo -e "${YELLOW}正在安装OpenJDK 17...${NC}"
    if grep -Ei 'ubuntu|debian' /etc/*release*; then
        apt update -qq && apt install -y openjdk-17-jdk
    elif grep -Ei 'centos|rhel' /etc/*release*; then
        yum install -y java-17-openjdk-devel
    else
        echo -e "${RED}不支持的Linux发行版${NC}"
        exit 1
    fi

    ! which java && echo -e "${RED}JDK安装失败${NC}" && exit 1
}

# 创建专用用户
create_user() {
    if id "$SERVICE_USER" &>/dev/null; then
        echo -e "${GREEN}用户 $SERVICE_USER 已存在${NC}"
    else
        useradd -r -s /bin/false "$SERVICE_USER" && \
        echo -e "${GREEN}已创建系统用户 $SERVICE_USER${NC}" || {
            echo -e "${RED}用户创建失败${NC}"
            exit 1
        }
    fi
}

# 部署应用文件
deploy_app() {
    echo -e "${YELLOW}正在部署应用程序...${NC}"
    mkdir -p "$INSTALL_DIR" || exit 1

    echo "正在下载 ani-rss-jar-with-dependencies.jar"
    # 下载jar包
    if ! wget -q https://github.com/wushuo894/ani-rss/releases/latest/download/ani-rss-jar-with-dependencies.jar -O "$INSTALL_DIR/ani-rss-jar-with-dependencies.jar"; then
        echo -e "${RED}下载 ani-rss-jar-with-dependencies.jar 失败${NC}"
        exit 1
    fi
    echo "下载完成 ani-rss-jar-with-dependencies.jar"

    echo "正在下载 run.sh"
    # 下载启动脚本
    if ! wget -q https://github.com/wushuo894/ani-rss/raw/master/docker/run.sh -O "$INSTALL_DIR/run.sh"; then
        echo -e "${RED}下载启动脚本失败${NC}"
        exit 1
    fi
    echo "下载完成 run.sh"

    echo "正在下载 ani-rss.sh"
    # 下载启动脚本
    if ! wget -q https://github.com/wushuo894/ani-rss/raw/master/linux/ani-rss.sh -O "/usr/local/bin/ani-rss"; then
        echo -e "${RED}下载启动脚本失败${NC}"
        exit 1
    fi
    echo "下载完成 ani-rss.sh"

    sudo chmod +x /usr/local/bin/ani-rss

    # 设置权限
    chown -R "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR"
    chmod 750 "$INSTALL_DIR"
    chmod 770 "$INSTALL_DIR/run.sh"
    echo -e "${GREEN}程序部署完成${NC}"
}

# 配置系统服务
setup_service() {
    echo -e "${YELLOW}正在配置系统服务...${NC}"
    tee /etc/systemd/system/"$SERVICE_NAME" > /dev/null <<EOF
[Unit]
Description=ANI-RSS Anime RSS Service
After=network.target

[Service]
Type=simple
User=$SERVICE_USER
Group=$SERVICE_USER
WorkingDirectory=$INSTALL_DIR
ExecStart=/bin/bash $INSTALL_DIR/run.sh
Restart=on-failure
RestartSec=30
LimitNOFILE=65535
Environment="PORT=$PORT"

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    systemctl enable "$SERVICE_NAME" > /dev/null 2>&1

    if ! systemctl start "$SERVICE_NAME"; then
        echo -e "${RED}服务启动失败，请检查日志：journalctl -u $SERVICE_NAME${NC}"
        exit 1
    fi
    echo -e "${GREEN}系统服务配置完成${NC}"
}

# 验证安装
verify_install() {
    echo -e "\n${YELLOW}验证安装...${NC}"
    if ! systemctl is-active "$SERVICE_NAME" | grep -q "active"; then
        echo -e "${RED}服务未正常运行${NC}"
        exit 1
    fi

    sleep 3
    if ! ss -tulnp | grep -q ":$PORT"; then
        echo -e "${RED}端口 $PORT 未监听${NC}"
        exit 1
    fi

    echo -e "${GREEN}验证通过，服务运行正常${NC}"
}

# 显示访问信息
show_info() {
    IP=$(hostname -I | awk '{print $1}')
    echo -e "\n${GREEN}安装完成！访问信息："
    echo -e "URL: http://$IP:$PORT"
    echo -e "用户名: admin"
    echo -e "初始密码: admin${NC}"
    ani-rss
}

# 主流程
main() {
    check_root
    install_jdk
    create_user
    deploy_app
    setup_service
    verify_install
    show_info
}

main
