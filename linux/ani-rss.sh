#!/bin/bash
# ANI-RSS 服务控制脚本
# 版本: 1.2
# 用法: ani-rss [start|stop|restart|status|help]

# 定义颜色代码
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

SERVICE="ani-rss.service"
SERVICE_FILE="/etc/systemd/system/${SERVICE}"

# 检查 root 权限
check_root() {
    [ "$EUID" -ne 0 ] && echo -e "${RED}错误：请使用 sudo 或 root 用户执行${NC}" && exit 1
}

# 检查服务是否存在
check_service() {
    [ ! -f "$SERVICE_FILE" ] && echo -e "${RED}错误：服务未安装${NC}" && exit 1
}

# 显示帮助信息
show_help() {
    echo -e "${YELLOW}使用方法:"
    echo "  ani-rss start    启动服务"
    echo "  ani-rss stop     停止服务"
    echo "  ani-rss restart 重启服务"
    echo "  ani-rss status  查看服务状态"
    echo "  ani-rss log  查看服务日志"
    echo "  ani-rss uninstall  卸载"
    echo "  ani-rss help    显示帮助信息${NC}"
}

# 操作执行函数
service_action() {
    case $1 in
        start)
            echo -e "${YELLOW}正在启动服务...${NC}"
            systemctl start "$SERVICE" && echo -e "${GREEN}服务启动成功${NC}" || {
                echo -e "${RED}启动失败，当前状态：${NC}"
                systemctl status "$SERVICE" --no-pager
                exit 1
            }
            ;;
        stop)
            echo -e "${YELLOW}正在停止服务...${NC}"
            systemctl stop "$SERVICE" && echo -e "${GREEN}服务停止成功${NC}" || {
                echo -e "${RED}停止失败，当前状态：${NC}"
                systemctl status "$SERVICE" --no-pager
                exit 1
            }
            ;;
        restart)
            echo -e "${YELLOW}正在重启服务...${NC}"
            systemctl restart "$SERVICE" && echo -e "${GREEN}服务重启成功${NC}" || {
                echo -e "${RED}重启失败，当前状态：${NC}"
                systemctl status "$SERVICE" --no-pager
                exit 1
            }
            ;;
        log)
            journalctl -u "$SERVICE" -f
            ;;
        status)
            systemctl status "$SERVICE" --no-pager
            ;;
        uninstall)
            /bin/bash -c "$(curl -fsSL https://github.com/wushuo894/ani-rss/raw/master/linux/uninstall-ani-rss.sh)"
            ;;
        *)
            show_help
            exit 1
            ;;
    esac
}

# 主程序
main() {
    check_root
    check_service

    if [ $# -eq 0 ]; then
        show_help
        exit 1
    fi

    case $1 in
        help) show_help ;;
        *) service_action "$1" ;;
    esac
}

main "$@"