@echo off
echo Git快速存档脚本
echo ===================

git add .
if errorlevel 1 (
    echo git add 失败
    pause
    exit /b 1
)

git commit -m "update"
if errorlevel 1 (
    echo git commit 失败
    pause
    exit /b 1
)

git push
if errorlevel 1 (
    echo git push 失败
    pause
    exit /b 1
)

echo 存档完成!
pause