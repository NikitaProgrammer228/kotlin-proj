# Быстрая очистка git от файлов build/ и .idea/

Write-Host "=== Очистка Git от временных файлов ===" -ForegroundColor Cyan
Write-Host ""

# Проверка текущего статуса
$before = (git status --short 2>&1 | Measure-Object -Line).Count
Write-Host "Файлов в staging area до очистки: $before" -ForegroundColor Yellow

# Удаление всех build/ из git
Write-Host "`nУдаление папок build/..." -ForegroundColor Yellow
Get-ChildItem -Path . -Directory -Recurse -Filter "build" -ErrorAction SilentlyContinue | 
    Where-Object { $_.FullName -notmatch "\.git" } | 
    ForEach-Object {
        $relativePath = $_.FullName.Replace((Get-Location).Path + "\", "").Replace("\", "/")
        Write-Host "  - $relativePath" -ForegroundColor Gray
        git rm -r --cached "$relativePath" 2>&1 | Out-Null
    }

# Удаление .idea/
if (Test-Path ".idea") {
    Write-Host "`nУдаление .idea/..." -ForegroundColor Yellow
    git rm -r --cached .idea/ 2>&1 | Out-Null
}

# Удаление .iml файлов
Write-Host "`nУдаление .iml файлов..." -ForegroundColor Yellow
Get-ChildItem -Path . -Filter "*.iml" -Recurse -ErrorAction SilentlyContinue | 
    Where-Object { $_.FullName -notmatch "\.git" } | 
    ForEach-Object {
        $relativePath = $_.FullName.Replace((Get-Location).Path + "\", "").Replace("\", "/")
        Write-Host "  - $relativePath" -ForegroundColor Gray
        git rm --cached "$relativePath" 2>&1 | Out-Null
    }

# Проверка результата
$after = (git status --short 2>&1 | Measure-Object -Line).Count
Write-Host "`n=== Результат ===" -ForegroundColor Cyan
Write-Host "Файлов в staging area после очистки: $after" -ForegroundColor $(if ($after -lt $before) { "Green" } else { "Yellow" })

# Проверка оставшихся файлов build/
$remaining = git status --short 2>&1 | Select-String -Pattern "(build/|\.idea/|\.iml)"
if ($remaining) {
    Write-Host "`n⚠ ВНИМАНИЕ: Остались файлы с build/ или .idea/:" -ForegroundColor Red
    $remaining | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
} else {
    Write-Host "`n✅ Все файлы build/ и .idea/ удалены из git!" -ForegroundColor Green
}

Write-Host "`nСледующий шаг:" -ForegroundColor Cyan
Write-Host "  git commit -m 'Remove build artifacts and IDE files from git'" -ForegroundColor Yellow

