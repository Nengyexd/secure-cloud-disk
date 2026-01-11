@echo off
chcp 65001 >nul
echo ======================================
echo   吉亦云盘系统 - 一键停止脚本
echo ======================================
echo.

:: 设置颜色
color 0C

:: 1. 停止 FRP 客户端
echo [1/5] 停止 FRP 客户端...
taskkill /F /IM frpc.exe >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ FRP 客户端已停止
) else (
    echo - FRP 客户端未运行
)
echo.

:: 2. 停止 Nginx
echo [2/5] 停止 Nginx...
cd /d Z:\nginx
nginx.exe -s quit >nul 2>&1
timeout /t 2 /nobreak >nul
taskkill /F /IM nginx.exe >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ Nginx 已停止
) else (
    echo - Nginx 未运行
)
echo.

:: 3. 停止后端服务
echo [3/5] 停止后端服务...
:: 查找占用 8080 端口的进程并终止
for /f "tokens=5" %%a in ('netstat -ano ^| find ":8080" ^| find "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
    if %errorlevel% == 0 (
        echo ✓ 后端服务已停止 (PID: %%a)
    )
)
echo.

:: 4. 停止 MinIO
echo [4/5] 停止 MinIO...
taskkill /F /IM minio.exe >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ MinIO 已停止
) else (
    echo - MinIO 未运行
)
echo.

:: 5. 停止 MySQL（可选，通常不停止数据库）
echo [5/5] MySQL 数据库...
echo ! MySQL 保持运行状态（如需停止请手动执行：net stop MySQL80）
echo.

echo ======================================
echo   所有服务已停止！
echo ======================================
echo.
pause
