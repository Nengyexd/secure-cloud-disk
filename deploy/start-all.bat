@echo off
chcp 65001 >nul
echo ======================================
echo   吉亦云盘系统 - 一键启动脚本
echo ======================================
echo.

:: 设置颜色
color 0A

:: 1. 检查并启动 MySQL
echo [1/5] 检查 MySQL 数据库...
sc query MySQL80 | find "RUNNING" >nul
if %errorlevel% == 0 (
    echo ✓ MySQL 已运行
) else (
    echo 正在启动 MySQL...
    net start MySQL80
    if %errorlevel% == 0 (
        echo ✓ MySQL 启动成功
    ) else (
        echo × MySQL 启动失败，请手动检查
        pause
        exit /b 1
    )
)
echo.

:: 2. 检查并启动 MinIO
echo [2/5] 检查 MinIO 对象存储...
tasklist | find "minio.exe" >nul
if %errorlevel% == 0 (
    echo ✓ MinIO 已运行
) else (
    echo 正在启动 MinIO...
    start "MinIO Server" /min cmd /c "cd /d Z:\minio && minio.exe server Z:\minio-data --console-address :9001"
    timeout /t 3 /nobreak >nul
    echo ✓ MinIO 启动成功
)
echo.

:: 3. 检查并启动后端服务
echo [3/5] 检查后端服务...
:: 检查是否有 Java 进程运行在 8080 端口
netstat -ano | find ":8080" | find "LISTENING" >nul
if %errorlevel% == 0 (
    echo ✓ 后端服务已运行
) else (
    echo 正在启动后端服务...
    cd /d "Z:\大学Study\AAA毕业设计\基于AES加密的安全云盘系统\backend"
    start "Cloud Disk Backend" /min cmd /c "mvn spring-boot:run"
    echo ✓ 后端服务启动中...（约需30秒）
)
echo.

:: 4. 检查并启动 Nginx
echo [4/5] 检查本地 Nginx...
tasklist | find "nginx.exe" >nul
if %errorlevel% == 0 (
    echo ✓ Nginx 已运行
) else (
    echo 正在启动 Nginx...
    cd /d Z:\nginx
    start nginx.exe
    timeout /t 2 /nobreak >nul
    tasklist | find "nginx.exe" >nul
    if %errorlevel% == 0 (
        echo ✓ Nginx 启动成功
    ) else (
        echo × Nginx 启动失败，请检查配置文件
        pause
        exit /b 1
    )
)
echo.

:: 5. 检查并启动 FRP 客户端
echo [5/5] 检查 FRP 客户端...
tasklist | find "frpc.exe" >nul
if %errorlevel% == 0 (
    echo ✓ FRP 客户端已运行
) else (
    echo 正在启动 FRP 客户端...
    cd /d "Z:\大学Study\AAA毕业设计\基于AES加密的安全云盘系统\frp\frp_0.65.0_windows_amd64"
    start "FRP Client" /min cmd /c "frpc.exe -c frpc.toml"
    timeout /t 2 /nobreak >nul
    echo ✓ FRP 客户端启动成功
)
echo.

:: 等待后端服务完全启动
echo 等待后端服务完全启动（约30秒）...
:wait_loop
netstat -ano | find ":8080" | find "LISTENING" >nul
if %errorlevel% == 0 (
    echo ✓ 后端服务已就绪
    goto :services_ready
)
timeout /t 2 /nobreak >nul
goto :wait_loop

:services_ready
echo.
echo ======================================
echo   所有服务启动完成！
echo ======================================
echo.
echo 本地访问地址：http://localhost
echo 公网访问地址：http://8.148.73.169:12345
echo.
echo 服务状态：
echo   ✓ MySQL 数据库
echo   ✓ MinIO 对象存储
echo   ✓ 后端服务 (8080)
echo   ✓ Nginx (80)
echo   ✓ FRP 客户端
echo.
echo 按任意键在浏览器中打开系统...
pause >nul

:: 打开浏览器
start http://localhost

exit /b 0
