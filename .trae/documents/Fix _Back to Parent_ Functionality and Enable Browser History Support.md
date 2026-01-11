I will fix the "Back to Parent Directory" functionality and ensure it works with browser history (forward/back buttons) by synchronizing the folder state with the URL query parameters.

### Technical Implementation Steps

1.  **Import `useRoute`**:
    -   Add `useRoute` to the imports from `vue-router` in `frontend/src/views/Home.vue`.

2.  **Synchronize URL with Folder State**:
    -   Initialize `route` object: `const route = useRoute();`.
    -   Modify `navigateToFolder(folderId)` to use `router.push({ query: { ...route.query, folderId } })` instead of directly updating `currentFolderId` and calling `loadFileList`. This ensures every navigation pushes a history entry.

3.  **Handle Browser History Navigation**:
    -   Add a `watch` on `() => route.query.folderId` to detect URL changes (including browser Back/Forward).
    -   Inside the watcher:
        -   Update `currentFolderId` based on the new query parameter (default to 0 if missing).
        -   Call `loadFileList()` to refresh the view.
    -   Update `onMounted` to initialize `currentFolderId` from the URL query parameter before loading the list.

4.  **Refine `handleGoBack`**:
    -   The existing logic to find the parent folder from `breadcrumb` is correct (`breadcrumb.value[breadcrumb.value.length - 2]`).
    -   Ensure it calls the updated `navigateToFolder` to trigger the route change.

5.  **Robustness & Error Handling**:
    -   Ensure `currentFolderId` is parsed as an integer from the query.
    -   Verify that navigation only occurs when `currentView` is 'all-files'.

### Expected Behavior
-   Clicking "Back to Parent" will update the URL (e.g., `?folderId=10` -> `?folderId=5`) and load the parent folder.
-   Clicking the browser's "Back" button will correctly navigate to the previous folder.
-   Refreshing the page will remain in the current folder instead of resetting to the root.
