<div align="center">

<img src="https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/terminal/materialsymbolsoutlined/terminal_48px.svg" width="84" height="84" alt="MTermux Emblem" />

# ✦ MTermux — Material 3 Expressive Terminal

**Эмулятор терминала, SSH/SFTP-проводник и менеджер пакетов нового поколения для Android 17 (API 37)**  
*Нативное ядро ISO C++26 • Жидкостный интерфейс Material 3 Expressive • Kotlin 2.5.0-Beta2 • Автономность Zero-Drain*

<p align="center">
  <img src="https://img.shields.io/badge/Design-Material%203%20Expressive-D0BCFF?style=for-the-badge&logo=materialdesign&logoColor=141218" alt="Material 3 Expressive" />
  <img src="https://img.shields.io/badge/Core-ISO%20C%2B%2B26%20SIMD-A8C7FA?style=for-the-badge&logo=c%2B%2B&logoColor=062E6F" alt="ISO C++26" />
  <img src="https://img.shields.io/badge/Kotlin-2.5.0--Beta2-B69DF8?style=for-the-badge&logo=kotlin&logoColor=21005D" alt="Kotlin 2.5.0-Beta2" />
  <img src="https://img.shields.io/badge/Android-17%20(API%2037)-80D8FF?style=for-the-badge&logo=android&logoColor=003544" alt="Android 17" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/CI%20Runner-Ubuntu%2026.04-E95420?style=flat-square&logo=ubuntu&logoColor=white" alt="Ubuntu 26.04" />
  <img src="https://img.shields.io/badge/NDK-30.0.16248370%20(16KB%20Pages)-4CAF50?style=flat-square&logo=llvm&logoColor=white" alt="NDK r30" />
  <img src="https://img.shields.io/badge/CCache-2GB%20Accelerated-FFB74D?style=flat-square&logo=speedtest&logoColor=black" alt="CCache" />
  <img src="https://img.shields.io/badge/APK%20Signing-Scheme%20v1%20%7C%20v2%20%7C%20v3%20%7C%20v4%20(.idsig)-FF8A65?style=flat-square&logo=googleplay&logoColor=black" alt="APK v4" />
  <img src="https://img.shields.io/badge/I18n-RU%20%7C%20UK%20%7C%20EN%20%2B%207%20Languages-81C784?style=flat-square" alt="Languages" />
</p>

</div>

---

## ✦ Философия дизайна: Material 3 Expressive

**MTermux (`com.mtermux`)** спроектирован с нуля вокруг концепции **Material 3 Expressive (ColorSpec 2026)** и **жидкостной пространственной физики (Liquid Spatial Physics)**:

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [ $ 1 | # 2 | + ]                                      [ ⊞ | ⚙ | ✕ ]      │  ◄ Матовые парящие капсулы поверх 100% холста
├────────────────────────────────────────────────────────────────────────────┤
│  ░░░░░░░░░░░░░░░░░ Глубокое матовое затемнение (Vignette) ░░░░░░░░░░░░░░░  │
│                                                                            │
│  ~ $ pkg install clang neovim python                                       │  ◄ Рендеринг каждой строки в отдельный GPU RenderNode
│  ~ $ ssh prod-cluster                                                      │    с пружинной интерполяцией каретки (VS Code Smooth)
│                                                                            │
│  ░░░░░░░░░░░░░░░░░ Плавное нижнее затемнение с авто-скрытием ░░░░░░░░░░░░  │
│  [ ESC | TAB | CTRL | ALT |  —  |  /  |  |  ]                              │  ◄ Синхронное 120 Гц парение над системной клавиатурой
│  [  HOME  |  ↑  |  END  |  PGUP |  ←  |  ↓  |  →  ]                        │    с защитой от спама и настраиваемым тактильным откликом
└────────────────────────────────────────────────────────────────────────────┘
```

### Ключевые визуальные решения M3 Expressive
* **Жидкостная трансформация капсулы `>_ Терминал`**: в разделе «Файлы» верхняя капсула `>_ Терминал [ 💾 ▸ ]` с упругим вертикальным натяжением (`scaleY` волна) и каскадным раскрытием превращается в переключатель источников `[ >_ ] [ Termux | SD | Root | SSH ] [ ✕ ]` и мягко собирается обратно без срывов и обрезания текста.
* **Экран первого запуска в стиле M3 Expressive**: во время первичной распаковки окружения Linux отрисовывается объёмная карточка с морфинг-полигоном на C++26 (`nativeComputeM3LoadingPolygon`), эмблемой `>_` и волновым индикатором прогресса.
* **Адаптивная группировка `ListItemDefaults.segmentedShapes`**: списки настроек, SSH-серверов, файлов и пакетов формируют единые сегментированные колонки с внешними скруглениями `20–24dp` и внутренними стыками `5dp`. При выборе пункта в диалогах выбора активная карточка плавно скругляется до полной капсулы `22dp`.
* **Отзывчивые Bento-переключатели**: нижняя плавающая док-панель в проводнике (`[ 🔍 | 🌿 | 👁 | 📁+ | ⬆️ ]`) сжимает соседние сегменты с сохранением общей ширины при удержании и плавно раскрывается в панель мгновенного поиска с фильтрами `Все` / `Папки` / `Файлы` и сортировкой `⇅ Имя` / `⇅ Размер` / `⇅ Дата`.

---

## ✦ Сравнение: Оригинальный Termux vs MTermux

| Характеристика | Оригинальный Termux | ✦ MTermux (Material 3 Expressive) |
| :--- | :--- | :--- |
| **Дизайн-система** | Устаревший Material 1 / Holo | **Material 3 Expressive** + генератор палитр **ColorSpec 2026** в C++26 |
| **Геометрия экрана** | Обрезанный холст с рамками | **100% Full-Bleed AMOLED** с парящими капсулами и двойной виньеткой |
| **Анимационный движок** | Мгновенные скачки без переходов | **`MTermuxAnimEngine` (C++26)**: жидкостные таблетки, аккордеон, пружинная каретка |
| **Энергопотребление в фоне** | Постоянный `PARTIAL_WAKE_LOCK` + Wi-Fi Lock | **Zero-Drain Autonomy**: 0% нагрузки на UI в фоне, авто-сон каретки через 4 с |
| **Рендеринг терминала** | Перерисовка всего экрана на каждый кадр | Построчное аппаратное кэширование **`android.graphics.RenderNode`** + фильтрация грязных строк |
| **Встроенный хаб инструментов** | Отсутствует | Единый центр **`SSH` • `Файлы` • `Пакеты`** с поддержкой Predictive Back (Android 14–17) |
| **Проводник (Local / Root / SFTP)** | Нет встроенного UI | Мгновенный поиск **0 мс в памяти**, древовидный режим, `chmod`-калькулятор, `.zip` архиватор |
| **Хранилище SSH-ключей** | Только через CLI | Визуальный сейф ключей с генерацией `Ed25519` / `RSA-4096` и импортом из FileZilla XML / PEM |
| **Буфер обмена и выделение** | Лишние системные уведомления буфера | Проверка метаданных `ClipDescription` без ложных срабатываний тоста Android 12–17 |
| **Выход по жесту «Назад»** | Закрытие активности и сброс сессии | Безопасный перевод задачи в фон (`moveTaskToBack`) с **100% сохранением сессии и истории** |

---

## ✦ Архитектура производительности и автономности (Zero-Drain Optimization)

1. **Кэширование на уровне аппаратных `RenderNode`**:
   * Каждая строка терминала записывается в собственный аппаратный дисплей-лист `RenderNode`. При вводе текста или мигании курсора перерисовываются только изменившиеся строки (`nativeIsRowDirty`).
2. **Устранение лишних JNI-вызовов в `onDraw`**:
   * Статус готовности `bootstrapReady` и глубина буфера прокрутки `cachedMaxScrollback` кэшируются на стороне Kotlin — в состоянии покоя `onDraw` не создаёт ни одной JNI-строки и не вызывает лишних проверок буфера `OSC 52`.
   * Отрисовка дополнительных строк над верхней панелью и под нижней панелью выполняется строго при наличии реального скроллбэка.
3. **Двухфазный VSYNC-цикл каретки и авто-сон**:
   * Во время скольжения каретки между символами (`stillMoving == true`) интерполяция работает на частоте экрана **120 Гц**.
   * Когда каретка неподвижна, частота плавного дыхания снижается до экономичных **~28 Гц**, а через **4 секунды** бездействия переходит в режим **Idle Freeze (0% CPU / GPU)**.
4. **Многократно увеличенная автономность в фоне**:
   * В `onPause()` поверхность мгновенно отвязывается от нативного потока чтения (`nativeRegisterSurface(null)`), исключая пробуждение UI-потока при фоновом выводе.
   * Фоновый сервис `KeepAliveService` удерживает процесс и PTY-сессии в памяти без принудительного круглосуточного удержания `PARTIAL_WAKE_LOCK` и `WIFI_MODE_FULL_LOW_LATENCY`, позволяя процессору смартфона уходить в глубокий сон (`Deep Doze / Suspend-to-RAM`).

---

## ✦ Сборка в GitHub Actions и телеметрия релиза (`BUILD-INFO.md`)

Сборка выполняется автоматически на раннере **`ubuntu-26.04`** с использованием **CCache (2 ГБ)**, кэширования Gradle/CXX и предсобранного `bootstrap-aarch64.zip`.

В каждый артефакт сборки **`MTermux-release`** (а также на страницу **GitHub Actions Step Summary**) автоматически прикладывается файл **`BUILD-INFO.md`**, содержащий:
* **Информацию о выходных артефактах**: точный размер и `SHA-256` хэш `MTermux-release.apk`, статус `MTermux-release.apk.idsig` (APK Signature Scheme v4).
* **Версии компиляторов и зависимостей**: версия ОС и ядра раннера, `JDK 28-ea / 25`, `Kotlin 2.5.0-Beta2`, `AGP 9.6.0-alpha02`, `Android SDK 37`, `NDK 30.0.16248370`, флаги `ISO C++26`.
* **Состав Bootstrap-окружения**: точные версии предустановленных пакетов `aarch64` (`bash`, `apt`, `dpkg`, `openssh`, `openssl`, `coreutils`, `ncurses`, `readline`, `zlib` и др.), извлечённые напрямую из `var/lib/dpkg/status`.
* **Отчёт `ccache -s`**: статистика попаданий в кэш C++26 объектных файлов и время компиляции.
