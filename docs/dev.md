<img alt="mikan-pic.png" height="80" src="https://docs.wushuo.top/image/mikan-pic.png"/>

## 参与开发

### 基本流程

1. fork本项目

2. 将项目克隆到本地

3. 创建新的分支，并切换到新分支

4. 在新分支上进行开发，并提交代码

5. 提交Pull Request到原项目的 `test` 分支并解决冲突

6. 若自动测试结果无异常则由维护者审核及合并。

**注意**

*1. 请先将代码fork到自己的仓库，再进行开发*

*2. 请不要将代码直接提交到 `master` 分支*

*3. 请在提交Pull Request之前，先在本地进行测试，确保代码能够正常运行*

### 环境配置

- 安装git

- 安装jdk17并配置[maven](https://maven.apache.org/download.cgi)

- 安装[nodejs](https://nodejs.org/zh-cn)

安装`nodejs`时`npm`会被自动安装

- 安装[pnpm](https://pnpm.io/zh/installation)

如果你已经完成了上一步则可以直接用这个命令在全局安装pnpm

```bash
npm install -g pnpm
```

### 开始开发

#### 0. 关闭本地正在运行的`ani-rss`

*如果没有端口冲突可以跳过这一步*

#### 1. 开启后端服务热部署

很简单，只需要用IDEA打开本项目。找到`/src/main/java/ani/rss/Main.java`，点击右上角的绿色调试按钮就好了。

如果`/src/main/resources/dist`里已经有编译好的静态页面，则会自动启用。

**这里打开的页面是不会热更新的，如果你想要修改前端页面则还要执行下一步**

#### 2. 启动前端开发环境

进入`ui`目录，执行以下命令：

```bash
# 安装依赖
pnpm install
# 启动开发服务器
pnpm dev
```

#### 3. 编写代码

开始你的表演。

### 本地编译

#### 1. 编译前端代码

*除本步骤外，其余步骤的命令均在根目录执行*

```bash
# 切换工作目录
cd ui
pnpm run build
```

#### 2. 将编译好的静态文件拷贝到`/src/main/resources/dist`

#### 3. 将`ani-rss-update.exe`放入`/src/main/resources/`

你可以从[https://github.com/wushuo894/ani-rss-update/](https://github.com/wushuo894/ani-rss-update/)下载它

#### 4. 编译后端代码

```bash
mvn -B package -DskipTests --file pom.xml
```

#### 其他操作

请参考`package.sh`和`package-win.sh`