@echo off

:: 切换到当前脚本所在目录
cd /D %~dp0

:: 获取当前批处理文件所在目录
set "currentDir=%CD%"

:: 设置快捷方式的名称
set "shortcutName=ani-rss.lnk"

:: 设置目标程序路径（假设目标程序在当前目录）
set "targetPath=%currentDir%\ani-rss-launcher.exe"

:: 设置启动文件夹路径
set "startupFolder=C:\ProgramData\Microsoft\Windows\Start Menu\Programs\StartUp"

:: 设置快捷方式完整路径
set "shortcutPath=%startupFolder%\%shortcutName%"

:: 创建 VBS 脚本生成快捷方式
echo Set objShell = CreateObject("WScript.Shell") > create_shortcut.vbs
echo Set objShortcut = objShell.CreateShortcut("%shortcutPath%") >> create_shortcut.vbs
echo objShortcut.TargetPath = "%targetPath%" >> create_shortcut.vbs
echo objShortcut.WorkingDirectory = "%currentDir%" >> create_shortcut.vbs
echo objShortcut.WindowStyle = 1 >> create_shortcut.vbs
echo objShortcut.Description = "ani-rss Auto Start" >> create_shortcut.vbs
echo objShortcut.Save >> create_shortcut.vbs

:: 运行 VBS 脚本
cscript //nologo create_shortcut.vbs

:: 删除临时 VBS 脚本
del create_shortcut.vbs

echo Successful!
pause
