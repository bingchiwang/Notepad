# 自動同步專案至 GitHub
param(
    [string]$message = "Update project files"
)

Write-Host "🔍 檢查 Git 狀態..." -ForegroundColor Cyan
git status -s

Write-Host "`n📦 暫存所有變更 (git add .)..." -ForegroundColor Cyan
git add .

# 檢查是否有任何變更需要 commit
$status = git status --porcelain
if ($status) {
    Write-Host "📝 建立提交 (git commit -m '$message')..." -ForegroundColor Cyan
    git commit -m "$message"
} else {
    Write-Host "ℹ️ 沒有需要提交的變更。" -ForegroundColor Yellow
}

# 檢查是否有設定 remote origin
$remote = git remote get-url origin 2>$null
if ($remote) {
    Write-Host "🚀 推送至 GitHub (git push origin main)..." -ForegroundColor Green
    git push origin main
    Write-Host "✅ 成功同步至 GitHub！" -ForegroundColor Green
} else {
    Write-Host "⚠️ 尚未設定 GitHub 遠端儲存庫 (remote origin)。" -ForegroundColor Red
    Write-Host "請先執行: git remote add origin <你的GitHub專案網址.git>" -ForegroundColor Yellow
}
