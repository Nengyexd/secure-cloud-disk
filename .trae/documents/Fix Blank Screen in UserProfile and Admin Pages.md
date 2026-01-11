I have identified the causes of the "completely blank interface" on the **User Profile** and **Admin** pages. The issue is due to **missing variable definitions** and **undefined functions** in the `<script setup>` sections, which cause JavaScript runtime errors during rendering.

### **Diagnosis Report**

1.  **`UserProfile.vue` (Personal Center)**
    *   **Critical Error**: The variable `userInfo` is used in the template but is **not defined** in the script. The code `Object.assign(userInfo, res.data)` fails because `userInfo` does not exist.
    *   **Missing Variables**: Many reactive variables referenced in the template are missing from the script:
        *   `uploadUrl`, `uploadHeaders` (Avatar upload)
        *   `vipDaysRemaining`, `storagePercentage` (Computed properties)
        *   `editVisible`, `passwordVisible`, `loginLogsVisible`, `deleteAccountVisible` (Dialog visibility)
        *   `editForm`, `passwordForm`, `deleteAccountForm` (Form models)
        *   `saving`, `loadingLogs`, `deletingAccount` (Loading states)
    *   **Reason**: It appears some code was replaced with a placeholder comment `// ... 其他代码不变 ...` during a previous edit, causing the loss of these definitions.

2.  **`Admin.vue` (Admin Background)**
    *   **Critical Error**: The function `getAvatarUrl` is called in the template (Line 15) but is **not defined** in the script. This causes a `ReferenceError` preventing the entire page from rendering.

### **Fix Plan**

I will restore the missing code in both files to ensure all variables and functions are properly defined.

#### **Step 1: Fix `UserProfile.vue`**
*   **Define `userInfo`**: Initialize as a reactive object.
*   **Restore Computed Properties**: Add `vipDaysRemaining`, `storagePercentage`, `uploadUrl`, and `uploadHeaders`.
*   **Restore UI State**: Define all dialog visibility flags (`editVisible`, etc.) and form data objects (`editForm`, etc.).
*   **Restore Form Rules**: Add validation rules for editing profile and changing password.

#### **Step 2: Fix `Admin.vue`**
*   **Add `getAvatarUrl` Function**: Implement the helper function to correctly resolve avatar URLs (handling both absolute URLs and relative paths).

#### **Step 3: Verification**
*   After applying the fixes, the pages should render correctly without console errors.
*   I will verify that the data (user info, charts, tables) loads correctly.
