@echo off
chcp 65001 > nul
echo ========================================
echo    安全云盘系统 - 停止脚本
echo ========================================
echo.

echo 正在关闭 MinIO 服务...
taskkill /fi "WINDOWTITLE eq MinIO Server*" /f 2>nul
if %errorlevel% equ 0 (
    echo [✓] MinIO 服务已关闭
) else (
    echo [!] MinIO 服务未运行或已关闭
)

timeout /t 1 /nobreak > nul

echo 正在关闭前端开发服务器...
taskkill /fi "WINDOWTITLE eq Frontend Dev Server*" /f 2>nul
if %errorlevel% equ 0 (
    echo [✓] 前端服务已关闭
) else (
    echo [!] 前端服务未运行或已关闭
)

echo.
echo ========================================
echo    所有服务已停止
echo ========================================
echo.
pause
