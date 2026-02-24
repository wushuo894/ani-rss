#!/bin/bash
# ANI-RSS 一体化安装脚本 with Systemd 服务
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
SERVER_PORT="7789"

# 检查root权限
check_root() {
    [ "$EUID" -ne 0 ] && echo -e "${RED}错误：请使用sudo或以root运行${NC}" && exit 1
}

# 安装JDK
install_jdk() {
    echo -e "${YELLOW}正在检查Java环境...${NC}"
    if command -v java >/dev/null 2>&1; then
        echo -e "${GREEN}检测到JDK已安装${NC}"
        return
    fi

    echo -e "${YELLOW}正在安装OpenJDK 17...${NC}"
    if command -v apt >/dev/null 2>&1; then
        apt update -qq && apt install -y openjdk-17-jdk
    elif command -v yum >/dev/null 2>&1; then
        yum install -y java-17-openjdk-devel
    else
        echo -e "${RED}不支持的Linux发行版${NC}"
        exit 1
    fi

    ! command -v java >/dev/null 2>&1 && echo -e "${RED}JDK安装失败${NC}" && exit 1
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

    echo "正在下载 ani-rss.jar"
    # 下载jar包
    if ! wget -q https://github.com/wushuo894/ani-rss/releases/latest/download/ani-rss.jar -O "$INSTALL_DIR/ani-rss.jar"; then
        echo -e "${RED}下载 ani-rss.jar 失败${NC}"
        exit 1
    fi
    echo "下载完成 ani-rss.jar"

    echo "正在下载 run.sh"
    # 下载启动脚本
    if ! wget -q https://github.com/wushuo894/ani-rss/raw/master/docker/run.sh -O "$INSTALL_DIR/run.sh"; then
        echo -e "${RED}下载启动脚本失败${NC}"
        exit 1
    fi
    echo "下载完成 run.sh"

    echo "正在下载 ani-rss.sh"
    # 下载管理脚本
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

configure_port() {
    echo -e "${YELLOW}正在配置端口...${NC}"
    echo -e "当前默认端口: $SERVER_PORT"
    read -p "是否使用默认端口 $SERVER_PORT? [Y/n]: " choice
    case "$choice" in
        [Nn]*)
            while true; do
                read -p "请输入端口号(1-65535): " input_port
                if [[ "$input_port" =~ ^[0-9]+$ ]] && [ "$input_port" -ge 1 ] && [ "$input_port" -le 65535 ]; then
                    SERVER_PORT="$input_port"
                    break
                else
                    echo -e "${RED}端口无效${NC}"
                fi
            done
        ;;
    esac
    echo -e "${GREEN}已选择端口: $SERVER_PORT${NC}"
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
Environment="SERVER_PORT=$SERVER_PORT"

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

    echo -e "${GREEN}验证通过，服务运行正常${NC}"
}

# 显示访问信息
show_info() {
    IP=$(hostname -I | awk '{print $1}')
    echo -e "\n${GREEN}安装完成！访问信息："
    echo -e "URL: http://$IP:$SERVER_PORT"
    echo -e "用户名: admin"
    echo -e "初始密码: admin${NC}"
    echo -e "${RED}请务必及时修改默认用户名与密码${NC}"
    ani-rss help
}

# 主流程
main() {
    check_root
    install_jdk
    create_user
    deploy_app
    configure_port
    setup_service
    verify_install
    show_info
}

main
