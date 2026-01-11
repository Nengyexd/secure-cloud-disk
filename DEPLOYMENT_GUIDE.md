# 🚀 基于 FRP 内网穿透的云盘系统部署指南

## 📋 部署架构

```
┌─────────────────────────────────────────────────────────┐
│                     公网用户                             │
└─────────────────┬───────────────────────────────────────┘
                  │
                  │ HTTPS (443) / HTTP (80)
                  ▼
┌─────────────────────────────────────────────────────────┐
│         宝塔 Linux 服务器 (8.148.73.169)                 │
│  ┌─────────────┐    ┌──────────────┐                    │
│  │  FRP 服务端  │    │    Nginx     │                    │
│  │  (7000端口)  │◄───┤  (80/443端口) │                    │
│  └─────────────┘    └──────────────┘                    │
│         ▲                                                │
└─────────┼────────────────────────────────────────────────┘
          │ FRP 隧道 (7000端口)
          │
┌─────────┴────────────────────────────────────────────────┐
│              本地 Windows 电脑                            │
│  ┌─────────────┐    ┌──────────────┐   ┌─────────────┐  │
│  │  FRP 客户端  │    │   后端服务    │   │   MinIO     │  │
│  │  (转发80端口)│◄───┤  (8080端口)   │   │  (9000端口)  │  │
│  └─────────────┘    └──────────────┘   └─────────────┘  │
│         ▲                  ▲                  ▲          │
│         │                  │                  │          │
│         └──────────────────┴──────────────────┘          │
│                    本地 Nginx (80端口)                    │
│              (托管前端 + 反向代理后端/MinIO)               │
└───────────────────────────────────────────────────────────┘
```

---

## 🎯 部署方案说明

### 方案优势
- ✅ **单端口转发**：只需转发本地 80 端口，简化配置
- ✅ **统一入口**：前端、后端、MinIO 通过一个域名访问
- ✅ **生产环境**：使用构建后的前端，性能更好
- ✅ **易于维护**：所有服务通过 Nginx 统一管理
- ✅ **安全性高**：可以轻松添加 HTTPS 支持

### 部署流程
1. **本地环境准备**：构建前端、配置本地 Nginx、启动服务
2. **服务器环境准备**：配置 FRP 服务端、配置宝塔 Nginx
3. **FRP 连接配置**：建立内网穿透隧道
4. **域名配置（可选）**：绑定域名、配置 SSL 证书

---

## 📦 第一步：本地环境准备

### 1.1 安装本地 Nginx

**下载 Nginx for Windows**：
- 访问：https://nginx.org/en/download.html
- 下载：nginx-1.24.0 (Windows 版本)
- 解压到：`Z:\nginx\` 目录

### 1.2 构建前端生产版本

打开命令行，执行：

```bash
cd Z:\大学Study\AAA毕业设计\基于AES加密的安全云盘系统\frontend

# 安装依赖（如果还没安装）
npm install

# 构建生产版本
npm run build
```

构建完成后，会在 `frontend/dist` 目录生成生产文件。

### 1.3 配置本地 Nginx

创建配置文件：`Z:\nginx\conf\cloud-disk.conf`

```nginx
# 云盘系统本地 Nginx 配置
server {
    listen 80;
    server_name localhost;

    # 前端静态文件
    location / {
        root Z:/大学Study/AAA毕业设计/基于AES加密的安全云盘系统/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 文件上传大小限制
        client_max_body_size 2048M;
        proxy_connect_timeout 600s;
        proxy_send_timeout 600s;
        proxy_read_timeout 600s;
    }

    # MinIO 对象存储代理（用于文件下载）
    location /minio/ {
        proxy_pass http://localhost:9000/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 支持大文件
        client_max_body_size 2048M;
    }

    # 错误页面
    error_page 500 502 503 504 /50x.html;
    location = /50x.html {
        root html;
    }
}
```

**修改主配置文件**：编辑 `Z:\nginx\conf\nginx.conf`，在 `http` 块中添加：

```nginx
http {
    # ... 其他配置 ...

    # 引入云盘配置
    include cloud-disk.conf;
}
```

### 1.4 启动本地服务

**1. 启动 MySQL 数据库**
- 确保 MySQL 服务已启动
- 数据库名称：`secure_cloud_disk`

**2. 启动 MinIO**
```bash
# 如果是 Windows 环境
minio.exe server Z:\minio-data --console-address :9001
```

**3. 启动后端服务**
```bash
cd Z:\大学Study\AAA毕业设计\基于AES加密的安全云盘系统\backend
mvn spring-boot:run

# 或者使用 IDEA 直接运行
```

**4. 启动本地 Nginx**
```bash
cd Z:\nginx
start nginx.exe

# 重启 Nginx（修改配置后）
nginx.exe -s reload

# 停止 Nginx
nginx.exe -s stop
```

**5. 测试本地访问**
- 打开浏览器访问：http://localhost
- 应该能看到云盘登录界面
- 测试登录、上传、下载等功能

---

## 🌐 第二步：服务器环境准备

### 2.1 安装宝塔面板（如果未安装）

SSH 连接到服务器，执行：

```bash
# CentOS 安装命令
yum install -y wget && wget -O install.sh https://download.bt.cn/install/install_6.0.sh && sh install.sh ed8484bec

# Ubuntu/Debian 安装命令
wget -O install.sh https://download.bt.cn/install/install-ubuntu_6.0.sh && sudo bash install.sh ed8484bec
```

### 2.2 安装 FRP 服务端

**方法一：使用宝塔面板安装（推荐）**

1. 登录宝塔面板
2. 进入「软件商店」
3. 搜索「frp」
4. 安装「FRP 内网穿透」插件

**方法二：手动安装**

```bash
# 下载 FRP 服务端
cd /opt
wget https://github.com/fatedier/frp/releases/download/v0.65.0/frp_0.65.0_linux_amd64.tar.gz
tar -zxvf frp_0.65.0_linux_amd64.tar.gz
mv frp_0.65.0_linux_amd64 frp
cd frp
```

### 2.3 配置 FRP 服务端

创建配置文件：`/opt/frp/frps.toml`

```toml
# FRP 服务端配置
bindPort = 7000
auth.token = "60d8a83c544e6168db"

# 仪表板配置（可选，用于查看连接状态）
webServer.addr = "0.0.0.0"
webServer.port = 7500
webServer.user = "admin"
webServer.password = "admin123"

# 日志配置
log.to = "/opt/frp/frps.log"
log.level = "info"
log.maxDays = 3
```

### 2.4 启动 FRP 服务端

创建 systemd 服务：`/etc/systemd/system/frps.service`

```ini
[Unit]
Description=Frp Server Service
After=network.target

[Service]
Type=simple
User=root
Restart=on-failure
RestartSec=5s
ExecStart=/opt/frp/frps -c /opt/frp/frps.toml
ExecReload=/bin/kill -HUP $MAINPID
LimitNOFILE=1048576

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
# 重载 systemd
systemctl daemon-reload

# 启动 FRP 服务
systemctl start frps

# 设置开机自启
systemctl enable frps

# 查看状态
systemctl status frps

# 查看日志
journalctl -u frps -f
```

### 2.5 配置服务器防火墙

**宝塔面板设置**：
1. 进入「安全」
2. 添加端口规则：
   - 7000 端口（FRP 服务端）
   - 7500 端口（FRP 仪表板，可选）
   - 12345 端口（云盘系统 HTTP 端口）
   - 12346 端口（云盘系统 HTTPS 端口，可选）

**命令行设置（如果使用 firewalld）**：

```bash
# 开放端口
firewall-cmd --zone=public --add-port=7000/tcp --permanent
firewall-cmd --zone=public --add-port=7500/tcp --permanent
firewall-cmd --zone=public --add-port=12345/tcp --permanent
firewall-cmd --zone=public --add-port=12346/tcp --permanent

# 重载防火墙
firewall-cmd --reload

# 查看开放的端口
firewall-cmd --list-ports
```

**阿里云/腾讯云安全组**：
- 登录云服务器控制台
- 进入「安全组」→「配置规则」
- 添加入站规则，开放上述端口

---

## 🔧 第三步：配置 FRP 客户端

### 3.1 更新 FRP 客户端配置

编辑本地配置文件：`Z:\大学Study\AAA毕业设计\基于AES加密的安全云盘系统\frp\frp_0.65.0_windows_amd64\frpc.toml`

```toml
# FRP 客户端配置
serverAddr = "8.148.73.169"
serverPort = 7000
auth.token = "60d8a83c544e6168db"

# 日志配置
log.to = "./frpc.log"
log.level = "info"
log.maxDays = 3

# 云盘系统 HTTP 代理
[[proxies]]
name = "cloud-disk-http"
type = "tcp"
localIP = "127.0.0.1"
localPort = 80
remotePort = 12345

# 云盘系统 HTTPS 代理（可选，如果配置了本地 HTTPS）
[[proxies]]
name = "cloud-disk-https"
type = "tcp"
localIP = "127.0.0.1"
localPort = 443
remotePort = 12346
```

### 3.2 启动 FRP 客户端

**方法一：使用命令行（开发测试）**

```bash
cd Z:\大学Study\AAA毕业设计\基于AES加密的安全云盘系统\frp\frp_0.65.0_windows_amd64

# 启动 FRP 客户端
frpc.exe -c frpc.toml
```

**方法二：使用 Windows 服务（推荐生产环境）**

我会为你创建一个 Windows 服务安装脚本。

### 3.3 验证 FRP 连接

**查看客户端日志**：
```bash
# 查看 frpc.log
tail -f Z:\大学Study\AAA毕业设计\基于AES加密的安全云盘系统\frp\frp_0.65.0_windows_amd64\frpc.log
```

**查看服务端日志**：
```bash
# SSH 连接到服务器
tail -f /opt/frp/frps.log

# 或使用 systemd 日志
journalctl -u frps -f
```

**查看 FRP 仪表板**：
- 访问：http://8.148.73.169:7500
- 用户名：admin
- 密码：admin123
- 查看客户端连接状态

---

## 🌍 第四步：配置服务器 Nginx 反向代理

### 4.1 在宝塔面板添加站点

1. 登录宝塔面板
2. 进入「网站」
3. 点击「添加站点」
4. 配置：
   - **域名**：`云盘.你的域名.com`（或直接使用 IP）
   - **根目录**：`/www/wwwroot/cloud-disk`（创建一个空目录即可）
   - **PHP 版本**：纯静态
   - 不创建 FTP 和数据库

### 4.2 配置 Nginx 反向代理

点击站点设置 → 配置文件，修改为：

```nginx
server {
    listen 80;
    server_name 云盘.你的域名.com;  # 修改为你的域名或IP

    # 反向代理到本地 FRP 转发的端口
    location / {
        proxy_pass http://127.0.0.1:12345;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 文件上传大小限制
        client_max_body_size 2048M;
        proxy_connect_timeout 600s;
        proxy_send_timeout 600s;
        proxy_read_timeout 600s;
    }

    # 访问日志
    access_log /www/wwwlogs/cloud-disk_access.log;
    error_log /www/wwwlogs/cloud-disk_error.log;
}
```

### 4.3 配置 HTTPS（可选但推荐）

**如果有域名**：

1. 在宝塔面板站点设置中
2. 点击「SSL」
3. 选择「Let's Encrypt」
4. 申请免费证书
5. 开启「强制 HTTPS」

**Nginx 配置会自动更新为**：

```nginx
server {
    listen 443 ssl http2;
    server_name 云盘.你的域名.com;

    # SSL 证书配置（宝塔自动生成）
    ssl_certificate /www/server/panel/vhost/cert/云盘.你的域名.com/fullchain.pem;
    ssl_certificate_key /www/server/panel/vhost/cert/云盘.你的域名.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # 反向代理
    location / {
        proxy_pass http://127.0.0.1:12345;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;

        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 文件上传大小限制
        client_max_body_size 2048M;
        proxy_connect_timeout 600s;
        proxy_send_timeout 600s;
        proxy_read_timeout 600s;
    }
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name 云盘.你的域名.com;
    return 301 https://$server_name$request_uri;
}
```

---

## 🧪 第五步：测试访问

### 5.1 本地测试

1. 确保所有本地服务已启动：
   - ✅ MySQL 数据库
   - ✅ MinIO 对象存储
   - ✅ 后端 Spring Boot 服务
   - ✅ 本地 Nginx
   - ✅ FRP 客户端

2. 本地测试：
   - 访问 http://localhost
   - 测试登录、文件上传、下载等功能

### 5.2 公网访问测试

1. 使用其他网络环境（手机热点、其他电脑）

2. 访问方式：
   - **使用 IP**：`http://8.148.73.169:12345`
   - **使用域名（如果配置）**：`http://云盘.你的域名.com`
   - **使用 HTTPS（如果配置）**：`https://云盘.你的域名.com`

3. 测试所有功能：
   - ✅ 用户注册/登录
   - ✅ 文件上传
   - ✅ 文件下载
   - ✅ 文件预览
   - ✅ 文件分享
   - ✅ 文件收藏
   - ✅ VIP 购买

---

## 📝 第六步：更新前端配置

### 6.1 修改前端 API 基础 URL

如果使用域名访问，需要修改前端的 API 请求地址。

编辑 `frontend/src/utils/request.js`：

```javascript
import axios from 'axios'

const request = axios.create({
  // 开发环境使用代理
  baseURL: import.meta.env.DEV ? '/api' : 'https://云盘.你的域名.com/api',
  timeout: 60000
})

// ... 其他配置
```

### 6.2 修改后端分享链接配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
business:
  share:
    base-url: https://云盘.你的域名.com/share/  # 修改为公网地址
```

### 6.3 重新构建前端

```bash
cd Z:\大学Study\AAA毕业设计\基于AES加密的安全云盘系统\frontend
npm run build
```

### 6.4 重启后端服务

重启 Spring Boot 应用，使配置生效。

---

## 🔒 第七步：安全加固（推荐）

### 7.1 修改默认端口

**FRP 服务端**：
```toml
# 修改 /opt/frp/frps.toml
bindPort = 17000  # 改为非标准端口
```

**FRP 仪表板**：
```toml
webServer.port = 17500
webServer.password = "你的强密码"  # 修改默认密码
```

### 7.2 修改数据库密码

```bash
# SSH 连接到服务器
# 如果数据库在本地，需要修改本地数据库密码

# 修改 application.yml 中的数据库密码
```

### 7.3 配置防火墙白名单

```bash
# 只允许特定 IP 访问某些端口
firewall-cmd --zone=public --add-rich-rule='rule family="ipv4" source address="你的IP" port port="7500" protocol="tcp" accept' --permanent
```

### 7.4 定期备份

**宝塔面板自动备份**：
1. 进入「计划任务」
2. 添加任务类型「备份网站」
3. 执行周期：每天凌晨 2:00
4. 保留备份数量：7 个

---

## 🛠️ 常见问题排查

### 问题 1：FRP 客户端连接失败

**检查步骤**：
```bash
# 1. 检查服务器防火墙
firewall-cmd --list-ports

# 2. 检查云服务器安全组

# 3. 检查 FRP 服务端状态
systemctl status frps
journalctl -u frps -n 50

# 4. 测试端口连通性
telnet 8.148.73.169 7000
```

**解决方法**：
- 确保服务器防火墙和安全组开放了 7000 端口
- 检查 token 是否一致
- 检查服务器 IP 是否正确

---

### 问题 2：访问 502 Bad Gateway

**可能原因**：
1. 本地服务未启动
2. FRP 客户端未运行
3. 端口配置错误

**检查步骤**：
```bash
# 1. 检查本地 Nginx 是否运行
tasklist | findstr nginx

# 2. 检查后端服务
tasklist | findstr java

# 3. 检查 FRP 客户端
tasklist | findstr frpc

# 4. 测试本地访问
curl http://localhost
curl http://localhost/api/test
```

---

### 问题 3：文件上传失败

**可能原因**：
1. Nginx 文件大小限制
2. 超时时间配置不足

**解决方法**：

**本地 Nginx**：
```nginx
client_max_body_size 2048M;
proxy_connect_timeout 600s;
proxy_send_timeout 600s;
proxy_read_timeout 600s;
```

**服务器 Nginx**：
```nginx
client_max_body_size 2048M;
proxy_connect_timeout 600s;
proxy_send_timeout 600s;
proxy_read_timeout 600s;
```

---

### 问题 4：HTTPS 访问报错

**检查 SSL 证书**：
```bash
# 查看证书有效期
openssl x509 -in /www/server/panel/vhost/cert/域名/fullchain.pem -noout -dates

# 重新申请证书
certbot renew --force-renewal
```

---

## 📊 性能优化建议

### 1. 启用 Gzip 压缩

**本地 Nginx**：
```nginx
gzip on;
gzip_vary on;
gzip_proxied any;
gzip_comp_level 6;
gzip_types text/plain text/css text/xml text/javascript application/json application/javascript application/xml+rss application/rss+xml font/truetype font/opentype application/vnd.ms-fontobject image/svg+xml;
```

### 2. 配置浏览器缓存

```nginx
location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2|ttf|svg)$ {
    expires 7d;
    add_header Cache-Control "public, immutable";
}
```

### 3. 启用 HTTP/2

```nginx
listen 443 ssl http2;
```

---

## 📋 快速启动清单

### 本地启动（按顺序）

1. [ ] 启动 MySQL 数据库
2. [ ] 启动 MinIO 对象存储
   ```bash
   minio.exe server Z:\minio-data --console-address :9001
   ```
3. [ ] 启动后端服务
   ```bash
   cd backend
   mvn spring-boot:run
   ```
4. [ ] 启动本地 Nginx
   ```bash
   cd Z:\nginx
   start nginx.exe
   ```
5. [ ] 启动 FRP 客户端
   ```bash
   cd frp\frp_0.65.0_windows_amd64
   frpc.exe -c frpc.toml
   ```
6. [ ] 测试本地访问：http://localhost
7. [ ] 测试公网访问：http://8.148.73.169:12345

### 服务器检查

1. [ ] FRP 服务端运行：`systemctl status frps`
2. [ ] Nginx 运行：`systemctl status nginx`
3. [ ] 防火墙端口开放：`firewall-cmd --list-ports`
4. [ ] 查看 FRP 连接状态：http://8.148.73.169:7500

---

## 🎉 部署完成

恭喜！如果一切顺利，你的云盘系统现在可以通过公网访问了。

**访问地址**：
- IP 访问：`http://8.148.73.169:12345`
- 域名访问（如果配置）：`http://云盘.你的域名.com`
- HTTPS 访问（如果配置）：`https://云盘.你的域名.com`

**下一步建议**：
1. 配置域名和 HTTPS 证书
2. 设置定期备份任务
3. 监控服务器资源使用情况
4. 收集用户反馈，持续优化

---

**文档版本**：v1.0
**最后更新**：2026-01-11
**维护者**：开发团队
