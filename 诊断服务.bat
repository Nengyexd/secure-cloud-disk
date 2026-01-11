@echo off
chcp 65001 > nul
echo ========================================
echo    系统诊断脚本
echo ========================================
echo.

echo [1/5] 检查 MinIO 服务状态...
netstat -ano | findstr ":9000" > nul
if %errorlevel% equ 0 (
    echo [✓] MinIO 服务正在运行 (端口 9000)
) else (
    echo [×] MinIO 服务未运行
)

echo.
echo [2/5] 检查 MinIO 控制台...
netstat -ano | findstr ":9001" > nul
if %errorlevel% equ 0 (
    echo [✓] MinIO 控制台正在运行 (端口 9001)
) else (
    echo [×] MinIO 控制台未运行
)

echo.
echo [3/5] 检查后端服务...
netstat -ano | findstr ":8080" > nul
if %errorlevel% equ 0 (
    echo [✓] 后端服务正在运行 (端口 8080)
) else (
    echo [×] 后端服务未运行
)

echo.
echo [4/5] 检查前端服务...
netstat -ano | findstr ":5173" > nul
if %errorlevel% equ 0 (
    echo [✓] 前端服务正在运行 (端口 5173)
) else (
    netstat -ano | findstr ":3000" > nul
    if %errorlevel% equ 0 (
        echo [✓] 前端服务正在运行 (端口 3000)
    ) else (
        echo [×] 前端服务未运行
    )
)

echo.
echo [5/5] 检查 MySQL 服务...
netstat -ano | findstr ":3306" > nul
if %errorlevel% equ 0 (
    echo [✓] MySQL 服务正在运行 (端口 3306)
) else (
    echo [×] MySQL 服务未运行
)

echo.
echo ========================================
echo    诊断完成
echo ========================================
echo.
echo 提示:
echo 1. 如果后端服务运行中但txt预览失败，请检查后端控制台日志
echo 2. 可以访问 http://localhost:9001 查看MinIO控制台
echo.
pause
