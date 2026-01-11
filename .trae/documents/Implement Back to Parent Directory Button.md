I will implement the "Back to Parent Directory" button in the `frontend/src/views/Home.vue` file.

**Plan:**

1. **Modify Template (`frontend/src/views/Home.vue`)**:

   * In the `.breadcrumb-bar` section, create a wrapper `div` to group the new "Back" button and the existing breadcrumb navigation.

   * Insert an `<el-button>` component before the breadcrumb.

   * **Design**:

     * Icon: `ArrowLeft`.

     * Text: "返回上一级" (Back to Parent).

     * Style: Use `text` or `link` type with a background hover effect (`bg` prop in Element Plus) for a modern look.

     * **Visibility/State**: The button will remain visible but become **disabled** when in the root directory (checked via `breadcrumb.length <= 1`) to maintain layout stability.

   * Add a vertical divider (`<el-divider direction="vertical" />`) between the button and the breadcrumb for visual separation.

2. **Implement Logic (`<script setup>`)**:

   * Create a `handleGoBack` function.

   * **Logic**: Check if `breadcrumb.length > 1`. If true, retrieve the parent folder (the second to last item in the breadcrumb array) and call the existing `navigateToFolder(parent.id)` function. This ensures data loading and path updates are consistent with the current system.

3. **Styling & Responsiveness**:

   * Add CSS to align the button and breadcrumb horizontally (`display: flex`, `align-items: center`).

   * Add `transition` properties to the button to ensure smooth hover effects and state changes (enabled/disabled).

   * Ensure the button adapts to different screen sizes (flex layout will handle this naturally).

**Outcome**:

* A clear "Back" button will appear in the top navigation bar.

* Clicking it will navigate to the parent folder.

* It will be disabled in the root directory.

* The interaction will be smooth and consistent with the existing breadcrumb navigation.

