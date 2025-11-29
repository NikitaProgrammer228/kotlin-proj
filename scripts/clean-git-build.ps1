# Скрипт для удаления файлов build/ и .idea/ из git индекса

Write-Host "Удаление файлов build/ и .idea/ из git индекса..." -ForegroundColor Yellow

# Удалить все папки build/ из git
$buildDirs = Get-ChildItem -Path . -Directory -Recurse -Filter "build" -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notmatch "\.git" }
foreach ($dir in $buildDirs) {
    $relativePath = $dir.FullName.Replace((Get-Location).Path + "\", "").Replace("\", "/")
    Write-Host "Удаление: $relativePath" -ForegroundColor Cyan
    git rm -r --cached "$relativePath" 2>&1 | Out-Null
}

# Удалить .idea/ если существует
if (Test-Path ".idea") {
    Write-Host "Удаление: .idea/" -ForegroundColor Cyan
    git rm -r --cached .idea/ 2>&1 | Out-Null
}

# Удалить .iml файлы
$imlFiles = Get-ChildItem -Path . -Filter "*.iml" -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notmatch "\.git" }
foreach ($file in $imlFiles) {
    $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "").Replace("\", "/")
    Write-Host "Удаление: $relativePath" -ForegroundColor Cyan
    git rm --cached "$relativePath" 2>&1 | Out-Null
}

Write-Host "`nПроверка результата..." -ForegroundColor Yellow
$remaining = git status --short | Select-String -Pattern "(build/|\.idea/|\.iml)" | Measure-Object
Write-Host "Осталось файлов с build/ или .idea/: $($remaining.Count)" -ForegroundColor $(if ($remaining.Count -eq 0) { "Green" } else { "Red" })

Write-Host "`nГотово! Теперь закоммитьте изменения:" -ForegroundColor Green
Write-Host "  git commit -m 'Remove build artifacts and IDE files from git'" -ForegroundColor Cyan

