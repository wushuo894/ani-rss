## 增加Swagger接口文档

设置环境变量 `SWAGGER_ENABLED=true`

通过链接访问 http://127.0.0.1:7789/swagger-ui/index.html

## 破坏性改动

### 环境变量与参数

| 旧        | 新                  |
|----------|--------------------|
| `--port` | `--server.port`    |
| `--host` | `--server.address` |
| `PORT`   | `SERVER_PORT`      |
| `HOST`   | `SERVER_ADDRESS`   |

### emby点格子

旧: `http://[IP]:7789/api/web_hook?s=[ApiKey]`

新: `http://[IP]:7789/api/embyWebhook?s=[ApiKey]`

## Windows端

不再提供内置jdk的压缩包, 需自行安装

## 此次更新方式

Docker需要重新部署

Windows需要手动重新下载

[请不要将本项目在国内宣传](https://github.com/wushuo894/ani-rss/discussions/504)

[从1.0升级至2.0的配置继承](https://github.com/wushuo894/ani-rss/discussions/427)
