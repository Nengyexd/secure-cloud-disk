@echo off
chcp 65001 > nul
echo ========================================
echo    安全云盘系统 - 启动脚本
echo ========================================
echo.

echo [1/2] 正在启动 MinIO 服务...
start "MinIO Server" cmd /k "cd /d Z:\大学Study\AAA毕业设计\基于AES加密的安全云盘系统\minio && minio.exe server Z:\大学Study\AAA毕业设计\基于AES加密的安全云盘系统\minio\data --console-address ":9001"

timeout /t 2 /nobreak > nul

echo [2/2] 正在启动前端开发服务器...
start "Frontend Dev Server" cmd /k "cd /d Z:\大学Study\AAA毕业设计\基于AES加密的安全云盘系统\frontend && npm run dev"

echo.
echo ========================================
echo    启动完成！
echo ========================================
echo.
echo MinIO 服务:
echo   - API 地址: http://localhost:9000
echo   - 控制台地址: http://localhost:9001
echo   - 用户名: minioadmin
echo   - 密码: minioadmin
echo.
echo 前端服务:
echo   - 开发服务器: http://localhost:5173 (或查看前端窗口)
echo.
echo 提示: 两个服务已在新窗口中启动
echo       关闭对应窗口即可停止服务
echo.
pause
