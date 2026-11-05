# MTermux — Material Termux (C++26 & Kotlin 2.5 Edition)

<p align="center">
  <b>MTermux (`com.mtermux`)</b> — высокопроизводительный терминальный эмулятор, SSH/SFTP-клиент и менеджер пакетов для Android в дизайне <b>Material 3 Expressive</b> с нативным ядром на <b>ISO C++26</b> и <b>Kotlin 2.5.0-Beta2</b>.
</p>

## Ключевые особенности

* **Полноэкранный терминал с плавающим HUD (Floating Glass UI):**
  * Терминал рендерится на 100% высоты и ширины экрана; верхняя панель сессий (с лимитом до 4 видимых вкладок в рамке и горизонтальной прокруткой) и нижняя двухрядная панель клавиш плавают поверх холста терминала.
  * Drag-and-drop редактор дополнительных клавиш с поддержкой перетаскивания зажатием.
  * Отсутствие инерции соседних сессий при переключении и стабильный скроллбуфер при максимальном масштабировании (`pinch-to-zoom`).
* **Единый хаб утилит (`SSH` • `Файлы` • `Пакеты`) на базе Material 3 Expressive:**
  * Универсальная раскрывающаяся таблетка **`>_ Терминал`** (`[ >_ | Termux | SD | Root | SSH | ✕ ]`) с жидкостной морфинг-анимацией сворачивания и разворачивания, а также контекстной кнопкой дерева папок `[ 📁 ▸ ]` во вкладке `Файлы`.
  * **SSH-клиент и Хранилище ключей (`SSH-ключи`):** генерация ключей `Ed25519` / `RSA-4096` через нативный `ssh-keygen`, импорт приватных ключей, автоподстановка ключей при создании сервера, `SFTP` в один тап и ICMP/TCP пинг сервера.
  * **Файлы (Локальный проводник + SFTP + Root):** бесшовное переключение между `Termux` (`/data/data/com.mtermux/file`), `SD`, `Root` (`su`) и удалёнными `SSH/SFTP` серверами, инлайн-дерево подпапок с плавной анимацией без перезагрузки списка, плавное аккордеон-раскрытие скрытых файлов (`.` dot-files), `chmod`-калькулятор (`rwx` + восьмеричный код), упаковка и распаковка `.zip`, подсчёт точного веса папок и файлов на C++26.
  * **Менеджер пакетов (`apt` / `dpkg` / `pkg`):** быстрый поиск, фильтрация, пакетное выделение для совместной установки или удаления пакетов, автоисправление `dpkg --configure -a` и очистка кэша.
* **Единый движок анимаций `MTermuxAnimEngine` (C++26 JNI):**
  * Все UI-переходы (жидкостные segmented-переключатели, разворачивание/сворачивание меню `"Терминал"`, аккордеон скрытых файлов и дерева каталогов, шторка базовых оттенков в настройках темы, клавиатурный отступ IME) работают через единый централизованный движок `MTermuxAnimEngine`, использующий C++26 кривые (`nativeEvalAnimCurve` и `nativeComputeLiquidPillStep`).
* **100% совместимость с `apt`, `pkg` и репозиториями Termux:**
  * Двухуровневая система совместимости путей `/data/data/com.mtermux/file/usr`:
    1. **Скрипт сборки `scripts/update_bootstrap.py`** выполняет побайтовый 16-байтный патч ELF-бинарей и скриптов (`com.termux/files` → `com.mtermux/file`, `com.termux/cache` → `com.mtermux/cach`) и создаёт конфиг `/etc/apt/apt.conf.d/00mtermux`.
    2. **Нативный перехватчик `libmtermux-redirect.so` (`LD_PRELOAD`, C++26)** прозрачно перенаправляет системные вызовы `openat`, `statx`, `execve`, `faccessat`, `readlinkat` со старых путей `/data/data/com.termux/files` на `/data/data/com.mtermux/file` во время выполнения любых свежеустановленных пакетов из `apt`.

## Технологический стек и архитектура

| Компонент | Версия / Стандарт | Описание |
| :--- | :--- | :--- |
| **Application ID** | `com.mtermux` | Пакет приложения **MTermux (Material Termux)** |
| **Ядро C++** | **ISO C++26** (`-std=c++26`) | `libmtermux-core.so` (PTY, JNI, ANSI/VT100, SFTP/SSH, `chmod`, `opendir` вес папок, кривые анимаций) + `libmtermux-redirect.so` (`LD_PRELOAD` редиректор для `apt`/`dpkg`) |
| **Kotlin** | **2.5.0-Beta2** (с авто-фоллбэком) | K2 Compiler, корутины `1.10.2`, единый UI и `MTermuxAnimEngine` |
| **Java / JVM Toolchain** | **Java 28** (с авто-фоллбэком) | Настроен на новейший JDK 28 (`28-ea`) с автоматическим переключением на доступный JDK ранера |
| **Android SDK / NDK** | API **36** / NDK **29.0.13113456** | 16 KB page alignment (`-Wl,-z,max-page-size=16384`), LTO, `-O3`, `arm64-v8a` |
| **Подпись APK** | **APK Signature Scheme v1 + v2 + v3 + v4** | Включён `enableV4Signing = true` в Gradle и генерация `.apk.idsig` через `apksigner --v4-signing-enabled true` |

## Структура проекта

```text
MTermux/
├── .github/workflows/build.yml          # CI/CD: Java 28, C++26, кэширование NDK/Bootstrap, подпись APK v4
├── app/
│   ├── build.gradle.kts                 # Сборка com.mtermux, C++26 флаги, подпись v1/v2/v3/v4, авто-выбор Kotlin/Java
│   ├── proguard-rules.pro               # Правила оптимизации R8 Full Mode для com.mtermux
│   └── src/main/
│       ├── AndroidManifest.xml          # Манифест MTermux (Material Termux)
│       ├── cpp/
│       │   ├── CMakeLists.txt           # CMake 4.0+, стандарт C++26 (-std=c++26), LTO, O3
│       │   ├── termux_core.cpp          # Нативное ядро терминала, PTY, окружение apt/pkg и C++26 движок расчётов/анимаций
│       │   └── mtermux_redirect.cpp     # C++26 LD_PRELOAD перехватчик системных вызовов для полной совместимости с apt
│       ├── java/com/mtermux/app/
│       │   └── TermuxActivity.kt        # Полный интерфейс MTermux и единый движок MTermuxAnimEngine
│       └── res/                         # Ресурсы и локализации (RU / EN)
├── gradle/libs.versions.toml            # Каталог версий (Kotlin 2.5.0-Beta2, AGP, Coroutines 1.10.2)
├── scripts/update_bootstrap.py          # Скачивание и 16-байтная бинарная адаптация Termux bootstrap под com.mtermux
├── build.gradle.kts                     # Корневой конфигурационный файл сборки
└── settings.gradle.kts                  # Резолвер плагинов с поддержкой preview/dev репозиториев Kotlin и JDK toolchains
```

## Быстрая сборка

1. **Подготовка Bootstrap-окружения (`aarch64`):**
   ```bash
   python3 scripts/update_bootstrap.py
   ```
2. **Сборка APK (Debug & Release с C++26 и подписью v4):**
   ```bash
   ./gradlew assembleDebug assembleRelease --build-cache --parallel
   ```
