您希望实现 **VIP会员体系**，具体需求如下：
1.  **存储空间差异**：普通用户 5GB，VIP会员 50GB 以上。
2.  **VIP特权**：不限速上传下载、更大存储空间、更长分享有效期、更大单文件上传限制、专属身份标识。
3.  **会员中心**：查看VIP权益、到期时间、在线续费。

### 现状分析
1.  **数据库**：`users` 表中已经包含 `userType` (0-普通, 1-VIP)、`vipExpireTime` (VIP到期时间)、`storageQuota` (存储配额) 等字段，这为 VIP 功能提供了很好的基础。
2.  **后端逻辑**：`UserService` 中已有注册时设置默认配额的逻辑 (`normalQuota`)。配置文件中有 `business.storage.vip-quota` 配置项。
3.  **前端**：目前还没有专门的“会员中心”页面，需要在前端添加。

### 实施计划

#### 1. 后端功能开发
*   **开通/续费 VIP 接口**：我们需要一个接口来模拟支付并升级用户为 VIP。
    *   在 `UserService` 中添加 `upgradeToVip(Long userId, Integer days)` 方法。
    *   逻辑：
        *   修改 `userType` 为 1。
        *   更新 `vipExpireTime`（如果是续费则累加，如果是新开通则从当前时间开始）。
        *   更新 `storageQuota` 为 VIP 配额（例如 50GB 或更多）。
        *   返回更新后的用户信息。
    *   在 `UserController` 中暴露 `POST /user/vip/upgrade` 接口。

*   **VIP 权益检查**：
    *   虽然需求提到了“不限速上传下载”，但这通常需要在文件流传输层面做限速控制，实现起来较复杂且依赖具体部署环境（如 Nginx 限速或 Java 层限速）。考虑到项目规模，我们可以先实现逻辑上的权益（如存储空间、单文件大小限制），对于限速可以暂不实现或仅做标记。
    *   **更大单文件上传限制**：在上传文件接口 (`FileService.uploadFile`) 中，根据用户类型判断文件大小限制（普通用户 100MB，VIP 2GB）。
    *   **更长分享有效期**：在创建分享接口 (`ShareService.createShare`) 中，允许 VIP 设置更长的有效期或永久有效。

#### 2. 前端功能开发
*   **新建会员中心页面 (`frontend/src/views/VipCenter.vue`)**：
    *   展示当前会员状态（是否 VIP，到期时间）。
    *   展示 VIP 特权对比列表（普通用户 vs VIP会员）。
    *   提供“开通/续费”按钮和套餐选择（例如：月卡、年卡）。
*   **路由配置**：在 `frontend/src/router/index.js` 中添加 `/vip` 路由。
*   **导航栏入口**：在主页侧边栏或顶部导航添加“会员中心”入口。
*   **全局状态同步**：开通成功后，需要更新 Pinia store 中的用户信息（存储空间、VIP 状态等）。

#### 3. 详细设计

**后端接口定义**:
*   `POST /user/vip/upgrade`: 参数 `{ duration: 30 }` (天数)，模拟支付成功后调用。

**前端页面设计**:
*   **特权展示区**：使用卡片或列表展示 5 大特权。
*   **套餐选择区**：
    *   月卡（30天）：￥10
    *   季卡（90天）：￥25
    *   年卡（365天）：￥80
*   **支付模拟**：点击购买后，弹出二维码（模拟），点击“支付完成”后调用后端接口。

### 待办事项列表
1.  **后端**：
    *   修改 `UserService.java`，添加 `upgradeToVip` 方法。
    *   修改 `UserController.java`，添加 VIP 升级接口。
    *   (可选) 修改 `FileService.java`，在上传时检查 VIP 单文件大小限制。
2.  **前端**：
    *   创建 `src/api/vip.js`，封装 VIP 相关接口。
    *   创建 `src/views/VipCenter.vue`，实现会员中心 UI。
    *   修改 `src/router/index.js`，注册路由。
    *   修改 `src/views/Home.vue`，添加菜单入口。

我将优先实现核心的 VIP 开通与续费流程，并展示权益。关于具体的“限速”和“文件限制”逻辑，将在后续步骤中完善。