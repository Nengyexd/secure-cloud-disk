# Upload Project to GitHub Plan

I will upload your complete project to a new GitHub repository, ensuring all configuration and file filtering rules are strictly followed.

## 1. Environment & Initialization
- **Action**: Initialize Git repository in the project root.
- **Command**: `git init`
- **Verification**: Check for `.git` directory creation.

## 2. Configure .gitignore
- **Action**: Create `.gitignore` file in the root directory.
- **Content**:
  - **Required**: `.env`, `.env.local`, `/dist`, `/build`, `/node_modules`, `.idea`, `.vscode`, `Thumbs.db`, `.DS_Store`, `*.log`.
  - **Project Specific**: `backend/target/` (Java build), `minio/data/` (runtime data), `frontend/dist/` (frontend build).
- **Verification**: Use `git check-ignore` to validate rules.

## 3. Stage Project Files
- **Action**: Stage all project files while respecting `.gitignore`.
- **Note on Batch Files**: The specific files `启动服务.bat` and `诊断服务.bat` were not found in the root. I will instead stage the existing `deploy/start-all.bat` and `deploy/stop-all.bat` along with the rest of the source code (`backend/`, `frontend/`, etc.).
- **Command**: `git add .` (followed by `git status` to verify).

## 4. Initial Commit
- **Action**: Create the first commit.
- **Command**: `git commit -m "feat: 初始项目提交"`

## 5. Create GitHub Repository
- **Action**: Create a public GitHub repository named `secure-cloud-disk` (derived from your project configuration) using the GitHub API.
- **License**: MIT License.

## 6. Push to Remote
- **Action**: Link local repository to remote and push.
- **Commands**:
  - `git remote add origin [SSH_URL]`
  - `git push -u origin main`
- **Fallback**: If local Git authentication is missing, I will provide the exact commands for you to complete the push manually.

## 7. Final Verification
- **Action**: Verify the repository structure and file list on GitHub via API.
