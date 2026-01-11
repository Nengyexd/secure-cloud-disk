# 数据库脚本说明

本目录包含基于AES加密的安全云盘系统的所有数据库脚本。

## 文件说明

### init.sql
**完整数据库初始化脚本**

用途：从零开始创建完整的数据库结构

包含以下表：
1. users - 用户表
2. email_verification_codes - 邮箱验证码表
3. login_logs - 登录日志表
4. files - 文件表
5. shares - 文件分享表
6. operation_log - 操作日志表
7. favorite - 文件收藏表
8. vip_orders - VIP订单表
9. vip_reminders - VIP提醒记录表

使用场景：
- 首次部署项目
- 重建数据库

执行方式：
```bash
mysql -u root -p < init.sql
```

### update.sql
**数据库迁移/更新脚本**

用途：在已有数据库基础上进行结构升级

包含以下更新：
- 版本 1.1：文件列表和管理模块优化
- 版本 1.2：用户头像功能
- 版本 1.3：图片缩略图支持

使用场景：
- 数据库版本升级
- 增量更新数据库结构

执行方式：
```bash
mysql -u root -p secure_cloud_disk < update.sql
```

## 使用指南

### 首次部署

1. 执行初始化脚本：
```bash
mysql -u root -p < init.sql
```

2. 验证表结构：
```sql
USE secure_cloud_disk;
SHOW TABLES;
DESC users;
DESC files;
```

### 版本升级

如果已有旧版本数据库，执行更新脚本：
```bash
mysql -u root -p secure_cloud_disk < update.sql
```

## 注意事项

1. **执行前备份**：在执行任何SQL脚本前，请先备份数据库
   ```bash
   mysqldump -u root -p secure_cloud_disk > backup_$(date +%Y%m%d).sql
   ```

2. **检查权限**：确保MySQL用户有足够的权限创建数据库和表

3. **字符集**：数据库使用 utf8mb4 字符集，支持完整的Unicode字符

4. **幂等性**：update.sql 中的部分操作可能不具备幂等性，重复执行可能会报错

## 数据库配置

修改 `backend/src/main/resources/application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/secure_cloud_disk?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

## 问题排查

### 表已存在错误
如果提示表已存在，可以：
1. 删除现有表后重新创建（会丢失数据）
2. 使用 update.sql 进行增量更新

### 字段已存在错误
update.sql 中的 ALTER TABLE 语句如果字段已存在会报错，这是正常的，可以忽略。

### 字符集问题
确保MySQL配置支持 utf8mb4：
```sql
SHOW VARIABLES LIKE 'character%';
```
