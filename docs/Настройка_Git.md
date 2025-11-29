# Настройка Git для Android проекта

## Проблема

После каждой отладки в Android Studio в коммит попадает 200+ файлов из папок `build/`, `.idea/` и других временных файлов.

## Решение

### 1. Обновлён `.gitignore`

Файл `.gitignore` уже обновлён и включает все необходимые правила для Android-проекта:
- Папки `build/` во всех модулях
- Папки `.idea/` (настройки Android Studio)
- Временные файлы компиляции
- APK, AAR и другие артефакты сборки

### 2. Удаление уже закоммиченных файлов

Если файлы из `build/` или `.idea/` уже были закоммичены, их нужно удалить из git (но оставить на диске):

```bash
# Удалить все файлы build/ из git (но оставить на диске)
git rm -r --cached app/build/
git rm -r --cached wit-sdk/build/
git rm -r --cached .idea/

# Если есть другие модули с build/
git rm -r --cached */build/

# Закоммитить удаление
git commit -m "Remove build artifacts and IDE files from git"
```

### 3. Проверка

После этого проверьте, что файлы игнорируются:

```bash
# Проверить, что build/ игнорируется
git status --ignored | grep build

# Проверить, что .idea/ игнорируется
git status --ignored | grep .idea
```

### 4. Что должно быть в коммитах

В коммиты должны попадать только:
- ✅ Исходный код (`.kt`, `.java`, `.xml`)
- ✅ Ресурсы (`res/`)
- ✅ Манифесты (`AndroidManifest.xml`)
- ✅ Gradle файлы (`build.gradle`, `gradle.properties`)
- ✅ Конфигурационные файлы (`.gitignore`, `README.md` и т.д.)

### 5. Что НЕ должно быть в коммитах

- ❌ Папки `build/` (артефакты сборки)
- ❌ Папки `.idea/` (настройки Android Studio)
- ❌ Файлы `.iml` (модули IntelliJ)
- ❌ APK файлы (`*.apk`)
- ❌ Локальные настройки (`local.properties`)

## Автоматическая проверка перед коммитом

Можно добавить pre-commit hook для проверки:

```bash
# Создать файл .git/hooks/pre-commit
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/sh
# Проверка, что не коммитятся файлы из build/
if git diff --cached --name-only | grep -E "(build/|\.idea/|\.iml)"; then
    echo "ERROR: Attempting to commit build artifacts or IDE files!"
    echo "These files should be in .gitignore"
    exit 1
fi
EOF

chmod +x .git/hooks/pre-commit
```

## Если проблема повторяется

1. Проверьте, что `.gitignore` в корне проекта (не в подпапках)
2. Убедитесь, что файлы не были добавлены в git до обновления `.gitignore`
3. Используйте `git status` перед каждым коммитом, чтобы проверить, что коммитятся только нужные файлы

