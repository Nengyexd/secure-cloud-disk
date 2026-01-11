通过代码分析，发现头像上传失败的根本原因在于前端发送上传请求时，**Authorization Header 格式不正确**。

### 问题分析
1. **Token 存储方式**：
   - 在前端 `store/user.js` 中，登录成功后直接将后端返回的 JWT Token 存储到 localStorage 中：`localStorage.setItem('token', res.data.token)`。
   - 后端返回的 `LoginVO` 中包含 `tokenType = "Bearer"`，但前端并未将 `tokenType` 和 `token` 拼接后存储，而是仅存储了 `token` 字符串。

2. **后端验证逻辑**：
   - 后端 `JwtAuthenticationFilter.java` 的 `doFilterInternal` 方法中，明确要求 Header 必须以 `tokenPrefix + " "` (即 "Bearer ") 开头：
     ```java
     if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(tokenPrefix + " ")) {
         filterChain.doFilter(request, response);
         return;
     }
     ```
   - 如果 Header 不符合该格式，请求将不被认证，SecurityContext 为空，最终导致后续的 `authenticated()` 拦截器拒绝访问（返回 403 Forbidden）。

3. **前端上传组件 Header**：
   - 在 `UserProfile.vue` 中，上传组件的 Header 构造如下：
     ```javascript
     const uploadHeaders = computed(() => ({
       Authorization: localStorage.getItem('token') || ''
     }))
     ```
   - 这里直接使用了 localStorage 中的 token，缺少了 "Bearer " 前缀。

### 修复方案
修改前端 `UserProfile.vue` 中的 `uploadHeaders` 计算属性，手动添加 "Bearer " 前缀。

### 实施步骤
1. 修改 `frontend/src/views/UserProfile.vue`。
2. 将 `uploadHeaders` 的计算逻辑修改为：
   ```javascript
   const uploadHeaders = computed(() => {
     const token = localStorage.getItem('token')
     return {
       Authorization: token ? `Bearer ${token}` : ''
     }
   })
   ```
3. 无需修改后端代码，因为后端逻辑是正确的（符合标准 JWT 规范）。

### 验证
修改后，前端上传请求的 Authorization Header 将变为 `Bearer <token>`，能够通过后端的校验，从而成功上传头像。