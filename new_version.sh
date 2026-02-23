#!/bin/sh

# Maven 版本设置脚本
# 用法: ./new_version.sh <new_version>

set -e  # 遇到错误立即退出

# 检查是否传入版本号参数
if [ $# -ne 1 ]; then
    echo "错误: 请指定版本号参数"
    echo "用法: $0 <new_version>"
    echo "示例: $0 2.4.9"
    exit 1
fi

VERSION="$1"

# 校验版本号格式
if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9_-]+)?$ ]]; then
    echo "错误: 版本号格式无效 '$VERSION'"
    echo "期望格式: 主版本号.次版本号.修订号[-限定符]"
    echo "有效示例: 2.4.9, 1.0.0-SNAPSHOT, 3.5.2-RELEASE"
    exit 1
fi

# 检查当前目录是否存在 pom.xml
if [ ! -f "pom.xml" ]; then
    echo "错误: 当前目录未找到 pom.xml，请在 Maven 项目根目录执行此脚本"
    exit 1
fi

echo "正在将项目版本设置为: $VERSION"

# 执行版本设置命令
if mvn versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=true; then
    echo "版本设置成功，正在提交更改..."
    
    # 执行版本提交命令
    if mvn versions:commit; then
        echo "✅ 版本已成功更新为: $VERSION"
    else
        echo "❌ 版本提交失败，正在回退..."
        mvn versions:revert
        exit 1
    fi
else
    echo "❌ 版本设置失败"
    exit 1
fi
