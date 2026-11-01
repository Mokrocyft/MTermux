# Termuxx (`com.termuxx` · C++23 · 165Hz · Pure Dark AMOLED)

Минималистичный терминал для **OnePlus 15 (Snapdragon 8 Elite Gen 5 / ARMv9.2-A / Android 17 API 37 / 165 Гц)** в стиле **Pure Dark AMOLED (`#000000`)** со шрифтом **JetBrains Mono** и полной поддержкой `apt`, `pkg` и Root (`su`) в отдельном пакете `com.termuxx`.

## Архитектура

1. **Почти вся логика на C++23 (`libtermux-core.so` + `libtermuxx-redirect.so`)**:
   - **Поток чтения PTY на C++23 (`pthread` + `poll`)**: чтение из `/dev/ptmx`, декодирование UTF-8, парсинг VT520/TrueColor (с ускорением ARM NEON), отправка клавиш/мыши и упаковка видимых глифов строки (`packRowRender`) выполняются полностью в нативном C++23 коде без промежуточных Java-потоков.
   - **Прозрачная поддержка `apt` и `pkg` в пакете `com.termuxx`**:
     - Нативный 16-байтовый патчер (`"com.termux/files"` → `"com.termuxx/file"`) переносит все ELF-бинарники и скрипты `bootstrap-aarch64.zip` без смещения таблиц символов.
     - Библиотека `libtermuxx-redirect.so` (`LD_PRELOAD`) на лету перенаправляет системные вызовы `dpkg` и `apt` из `/data/data/com.termux/files` в `/data/data/com.termuxx/file` и автоматически патчит новые пакеты при распаковке.
2. **Точная привязка глифов и курсора к сетке (`JetBrainsMono[wght].ttf`)**:
   - При сборке автоматически загружается официальный шрифт [`JetBrainsMono[wght].ttf`](https://raw.githubusercontent.com/google/fonts/main/ofl/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf).
   - Каждый символ строки отрисовывается строго по своей координате ячейки `x = col * mCellWidthPx`, поэтому курсор никогда не смещается относительно набранного текста даже при перехвате системных шрифтов оболочкой ColorOS.
3. **Минималистичный интерфейс Pure Dark AMOLED (`#000000`)**:
   - Абсолютно чёрный фон `#000000` (отключённые пиксели OLED), компактная монохромная строка вкладок (`1·sh`, `2·su`, `+`, `#`, `cp`, `ps`, `kb`), безрамочная двухрядная панель клавиш и классическая ч/б иконка Termux.
4. **Предварительно подписанные `.apk` (`testkey_untrusted.jks`)**:
   - И `debug`, и `release` сборки автоматически подписываются встроенным ключом (схемы V1 + V2 + V3).
