我将继续执行 **Phase 2: 文件列表与操作体验升级**。

### 本次任务：文件图标、上传体验与右键菜单优化

1.  **彩色 SVG 图标系统**:
    *   目前使用的是 Element Plus 默认的单色图标。我将引入一套彩色 SVG 图标（Folder, Image, Video, Audio, Document, PDF, Zip 等），通过 `getColorfulFileIcon` 方法根据文件类型动态渲染。
    *   为了不引入额外的图标库依赖，我将直接在代码中定义几个精美的 SVG 组件。

2.  **上传体验升级**:
    *   **全局拖拽**: 优化 `drag-upload-overlay` 样式，使用浅粉色背景 + 虚线框，增加 Bilibili 风格的插画或动画。
    *   **上传队列**: 重构 `.upload-view`，使其更加美观。增加“加密中”状态的可视化反馈（例如双色进度条）。

3.  **右键菜单美化**:
    *   重写 `.context-menu` 样式，使用 `var(--shadow-lg)` 和圆角，菜单项增加悬浮渐变背景。
    *   图标颜色与主题色统一。

4.  **文件列表细节**:
    *   优化 `el-table` 的行样式，使其看起来更像一个个独立的卡片（悬浮时）。

### 实施步骤
1.  **创建图标组件**: 在 `src/components/icons` 下新建 `FileIcons.vue` (或类似)，封装彩色 SVG 图标。
2.  **修改 `Home.vue`**:
    *   引入彩色图标组件，替换 `getFileIcon` 逻辑。
    *   更新上传视图和拖拽蒙层的 CSS。
    *   更新右键菜单 CSS。
