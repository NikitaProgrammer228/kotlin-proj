# Как очистить Git от файлов build/

## Проблема
В коммитах показывается 252 файла из папок `build/` и `.idea/`, которые не должны быть в git.

## Решение (выполните в терминале)

### Шаг 1: Удалить все файлы build/ из git индекса

```powershell
# В PowerShell (в корне проекта)
cd C:\Users\nikit\Desktop\kotlin-proj

# Удалить все папки build/ из git (но оставить на диске)
git rm -r --cached app/build/
git rm -r --cached wit-sdk/build/
git rm -r --cached .idea/

# Если есть другие модули с build/, удалите их тоже
# git rm -r --cached <module-name>/build/
```

### Шаг 2: Проверить результат

```powershell
# Посмотреть, сколько файлов осталось
git status --short | Select-String "build" | Measure-Object

# Должно быть 0 или очень мало
```

### Шаг 3: Закоммитить удаление

```powershell
git commit -m "Remove build artifacts and IDE files from git"
```

### Шаг 4: Проверить, что новые файлы build/ игнорируются

```powershell
# Сделать clean build
.\gradlew.bat clean

# Проверить git status - не должно быть файлов из build/
git status
```

## Альтернативный способ (через скрипт)

Запустите скрипт:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/clean-git-build.ps1
```

Затем закоммитьте изменения:

```powershell
git commit -m "Remove build artifacts and IDE files from git"
```

## Что должно быть в коммитах после очистки

✅ Только исходные файлы:
- `.kt`, `.java` (исходный код)
- `.xml` (макеты, ресурсы)
- `build.gradle`, `gradle.properties` (конфигурация)
- `AndroidManifest.xml`
- `.gitignore`, `README.md` и т.д.

❌ НЕ должно быть:
- `build/` (любые папки build)
- `.idea/` (настройки Android Studio)
- `*.iml` (файлы модулей IntelliJ)
- `*.apk`, `*.aar` (артефакты сборки)

## Если проблема повторяется

1. Убедитесь, что `.gitignore` в корне проекта
2. Проверьте, что в `.gitignore` есть строки:
   ```
   build/
   .idea/
   *.iml
   ```
3. Перед каждым коммитом проверяйте `git status` - не должно быть файлов из `build/`

