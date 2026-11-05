# <p align="center"><img src="https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/terminal/materialsymbolsoutlined/terminal_48px.svg" width="64" height="64" alt="MTermux Logo" /><br><b>MTermux — Material Termux</b></p>

<p align="center">
  <b>MTermux (`com.mtermux`)</b> — следующее поколение эмулятора терминала, SSH/SFTP-клиента и менеджера пакетов для Android в дизайне <b>Material 3 Expressive</b> с нативным вычислительным ядром на <b>ISO C++26</b>, <b>Kotlin 2.5.0-Beta2</b> и полной поддержкой <b>APK Signature Scheme v4</b>.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/C%2B%2B-26%20ISO%20Standard-blue.svg?style=for-the-badge&logo=c%2B%2B" alt="C++26" />
  <img src="https://img.shields.io/badge/Kotlin-2.5.0--Beta2-purple.svg?style=for-the-badge&logo=kotlin" alt="Kotlin 2.5" />
  <img src="https://img.shields.io/badge/Design-Material%203%20Expressive-pink.svg?style=for-the-badge&logo=google" alt="Material 3 Expressive" />
  <img src="https://img.shields.io/badge/Languages-RU%20%7C%20UK%20%7C%20EN%20%2B-green.svg?style=for-the-badge" alt="Localization" />
  <img src="https://img.shields.io/badge/APK%20Signing-Scheme%20v4%20(.idsig)-orange.svg?style=for-the-badge&logo=android" alt="APK v4" />
</p>

## Сравнение: MTermux vs Оригинальный Termux

| Возможность | Оригинальный Termux | MTermux (Material Termux) |
| :--- | :--- | :--- |
| **Полноэкранный терминал** | Чёрные полосы шторки и клавиатуры | 100% Fullscreen Edge-to-Edge: верхние вкладки и нижняя панель плавают поверх холста |
| **Интерфейс & Темизация** | Устаревший Holo / Material 1 (2015 г.) | **Material 3 Expressive** + динамический Monet с 4 стилями палитры и OLED-чёрным фоном |
| **Единый хаб утилит** | Отсутствует (только голая консоль) | Встроенный хаб **`SSH` • `Файлы` • `Пакеты`** в едином окне с жидкостной навигацией |
| **Движок анимаций** | Базовые скачкообразные перерисовки | **`MTermuxAnimEngine` (C++26)**: сплайн-интерполяция каретки, жидкостные таблетки, аккордеон |
| **Работа с SSH-серверами** | Только ручной ввод `ssh user@host` | Визуальный менеджер серверов с авто-пиногом, SFTP в один тап и авто-пробросом портов |
| **Хранилище SSH-ключей** | Ручное создание через CLI | Встроенный сейф ключей: генерация `Ed25519` / `RSA-4096`, импорт из PEM и автоподстановка |
| **Файловый менеджер (SFTP)** | Сторонние приложения | Инлайн-дерево папок, `chmod`-калькулятор, распаковка/упаковка `.zip`, подсчёт веса на C++26 |
| **Пакетный менеджер (`apt`)** | Консольный ввод `pkg install ...` | Графический менеджер с поиском, мульти-выбором для совместной установки и авто-лечением |
| **Редактор доп. клавиш** | Правка текстового файла `~/.termux/` | Визуальный Drag-and-Drop конструктор с перетаскиванием зажатием и каталогом спецклавиш |
| **Локализация интерфейса** | Частично английская | **Українська (`uk`)**, **Русский (`ru`)**, **English (`en`)** + глобальные языки |
| **Стандарт C++ ядра** | C++14 / C++17 | **ISO C++26** (`-std=c++26`, Clang 21, NEON/DotProd/Crypto оптимизации) |
| **Версия Kotlin & Java** | Kotlin 1.9 / Java 17 | **Kotlin 2.5.0-Beta2** + поддержка **Java 28** |
| **Подпись APK** | Scheme v2/v3 | **APK Signature Scheme v1 + v2 + v3 + v4 (`.idsig`)** |

## Интерактивный HUD и система анимаций

### 1. Единый нативный движок анимаций `MTermuxAnimEngine`
Под капотом приложения работает централизованный движок анимаций, синхронизированный с C++26 ядром (`nativeEvalAnimCurve` и `nativeComputeLiquidPillStep`):
* **Fluid Caret Spring Dynamics**: пружинная интерполяция движения каретки с физикой затухания, аналогичная современным редакторам кода.
* **Liquid Segmented Bento**: плавная деформация и перетекание активной таблетки переключателя между режимами (`SSH`, `Файлы`, `Пакеты`, `chmod`).
* **Universal Term Pill Morphing**: универсальная таблетка **`>_ Терминал`** плавно раскрывается в меню быстрых переходов `[ >_ | Termux | SD | Root | SSH | ✕ ]` и сворачивается обратно без мерцания и рывков.
* **Folder Tree & Hidden Files Accordion**: раскрытие подкаталогов дерева и показ скрытых файлов (`.` dot-files) анимируются по высоте на месте без полной перезагрузки списка.

### 2. Полноэкранный терминал с плавающим HUD
* Терминал занимает честные 100% экрана, прорисовываясь под строкой состояния и закруглениями дисплея.
* Верхняя панель сессий поддерживает лимит в 4 видимые сессии с горизонтальной прокруткой и плавной анимацией закрытия.
* Нижняя двухрядная панель клавиш автоматически приподнимается при появлении клавиатуры IME с плавной кривой ускорения.

## Архитектура и совместимость с `apt` / Termux

MTermux использует собственный идентификатор пакета **`com.mtermux`** и путь окружения **`/data/data/com.mtermux/file`**. Для обеспечения 100% бинарной совместимости со всеми официальными репозиториями Termux внедрена двухуровневая система:
1. **ELF Patching (`update_bootstrap.py`)**: 16-байтная замена токенов (`"com.termux/files"` → `"com.mtermux/file"`), сохраняющая строгие смещения заголовков ELF без повреждения бинарников `apt`, `dpkg`, `bash` и `ssh`.
2. **C++26 VFS & Exec Redirector (`libmtermux-redirect.so`)**: нативный перехватчик системных вызовов (`LD_PRELOAD`), перенаправляющий любые пути `/data/data/com.termux/files` на лету при запуске свежеустановленных из `apt` утилит.

## Технологический стек

```text
MTermux/
├── .github/workflows/build.yml     # Высокоскоростной CI/CD (Node, Python, Java 28/25, CCache, APK v4)
├── .gitattributes                  # Корректная разметка языков для GitHub Linguist (Kotlin 75%, C++ 23%)
├── app/
│   ├── build.gradle.kts            # Конфигурация Android API 37, C++26, R8 Full Mode, APK Signing v4
│   ├── src/main/
│   │   ├── AndroidManifest.xml     # Манифест с поддержкой Android 13+ App Language (@xml/locales_config)
│   │   ├── cpp/
│   │   │   ├── CMakeLists.txt      # Сборка mtermux-core и mtermux-redirect с флагами -std=c++26
│   │   │   ├── termux_core.cpp     # C++26 ядро терминала, PTY, кривые анимаций, lstat подсчёт размера
│   │   │   └── mtermux_redirect.cpp# C++26 LD_PRELOAD редиректор файловых системных вызовов
│   │   ├── java/com/mtermux/app/
│   │   │   ├── TermuxActivity.kt   # Главная активность, UI Material 3 Expressive, MTermuxAnimEngine
│   │   │   └── MTermuxI18n.kt      # Движок локализации: Українська, Русский, English + Global
│   │   └── res/xml/locales_config.xml # Системные языковые настройки Android
│   └── testkey_untrusted.jks       # Ключ подписи release APK
└── scripts/update_bootstrap.py     # Подготовка и бинарная адаптация официального bootstrap-окружения
```

## Сборка проекта

1. **Подготовка окружения bootstrap:**
   ```bash
   python3 scripts/update_bootstrap.py
   ```
2. **Сборка релизного APK с нативным ядром C++26 и подписью v4:**
   ```bash
   ./gradlew assembleRelease --build-cache --parallel
   ```
   Готовый подписанный APK и файл подписи `.idsig` будут находиться в каталоге `app/build/outputs/apk/release/`.
