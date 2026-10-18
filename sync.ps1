# Automatic GitHub synchronization script
param(
    [string]$message = "Update project files"
)

Write-Host "[1/3] Checking Git status..." -ForegroundColor Cyan
git status -s

Write-Host "`n[2/3] Staging changes (git add .)..." -ForegroundColor Cyan
git add .

$status = git status --porcelain
if ($status) {
    Write-Host "Creating commit: $message" -ForegroundColor Cyan
    git commit -m "$message"
} else {
    Write-Host "No changes to commit." -ForegroundColor Yellow
}

Write-Host "`n[3/3] Pushing to GitHub (git push origin main)..." -ForegroundColor Green
git push origin main
if ($LASTEXITCODE -eq 0) {
    Write-Host "Successfully synced to GitHub!" -ForegroundColor Green
} else {
    Write-Host "Failed to push to GitHub. Please check network or credentials." -ForegroundColor Red
}
