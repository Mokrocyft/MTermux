package com.mtermux.app

import java.util.Locale

/**
 * MTermux Multi-Language Localization Engine (RU / UK / EN + Global Languages).
 * Provides O(1) dictionary lookup and dynamic template interpolation for Ukrainian (uk)
 * and major international languages while keeping full compatibility with tr(ru, en).
 */
object MTermuxI18n {

    data class LangOption(
        val index: Int,
        val code: String,
        val titleRu: String,
        val titleUk: String,
        val titleEn: String
    )

    val SUPPORTED_LANGUAGES: List<LangOption> = listOf(
        LangOption(0, "auto", "Системный (Авто)", "Системна (Авто)", "System default"),
        LangOption(1, "ru", "Русский", "Русский", "Русский"),
        LangOption(2, "uk", "Українська", "Українська", "Українська"),
        LangOption(3, "en", "English", "English", "English"),
        LangOption(4, "de", "Deutsch", "Deutsch", "Deutsch"),
        LangOption(5, "es", "Español", "Español", "Español"),
        LangOption(6, "fr", "Français", "Français", "Français"),
        LangOption(7, "pl", "Polski", "Polski", "Polski"),
        LangOption(8, "pt", "Português", "Português", "Português"),
        LangOption(9, "tr", "Türkçe", "Türkçe", "Türkçe"),
        LangOption(10, "zh", "中文 (简体)", "中文 (简体)", "中文 (简体)")
    )

    fun resolveLangCode(appLangPref: Int): String {
        return when (appLangPref) {
            1 -> "ru"
            2 -> "uk"
            3 -> "en"
            4 -> "de"
            5 -> "es"
            6 -> "fr"
            7 -> "pl"
            8 -> "pt"
            9 -> "tr"
            10 -> "zh"
            else -> {
                val sys = Locale.getDefault().language.lowercase(Locale.ROOT)
                when {
                    sys.startsWith("uk") -> "uk"
                    sys.startsWith("ru") || sys.startsWith("be") || sys.startsWith("kk") -> "ru"
                    sys.startsWith("de") -> "de"
                    sys.startsWith("es") -> "es"
                    sys.startsWith("fr") -> "fr"
                    sys.startsWith("pl") -> "pl"
                    sys.startsWith("pt") -> "pt"
                    sys.startsWith("tr") -> "tr"
                    sys.startsWith("zh") -> "zh"
                    else -> "en"
                }
            }
        }
    }

    fun languagePickerLabels(activeLangCode: String): List<String> {
        return SUPPORTED_LANGUAGES.map { opt ->
            if (opt.index == 0) {
                when (activeLangCode) {
                    "ru" -> opt.titleRu
                    "uk" -> opt.titleUk
                    "de" -> "Systemstandard (Auto)"
                    "es" -> "Predeterminado del sistema"
                    "fr" -> "Par défaut du système"
                    "pl" -> "Domyślny systemowy"
                    "pt" -> "Padrão do sistema"
                    "tr" -> "Sistem varsayılanı"
                    "zh" -> "跟随系统 (自动)"
                    else -> opt.titleEn
                }
            } else {
                opt.titleEn
            }
        }
    }

    fun translate(langCode: String, ru: String, en: String, uk: String? = null): String {
        return when (langCode) {
            "ru" -> ru
            "en" -> en
            "uk" -> uk ?: UK_STATIC[ru] ?: translateUkDynamic(ru) ?: en
            else -> OTHER_STATIC[langCode]?.get(en) ?: translateOtherDynamic(langCode, en) ?: en
        }
    }

    private val UK_STATIC: HashMap<String, String> = hashMapOf(
        "+ Своя клавиша" to "+ Власна клавіша",
        "1 ряд (Компактная)" to "1 рядок (Компактна)",
        "2 ряда (Расширенная)" to "2 рядки (Розширена)",
        "SSH-агент (-A)" to "SSH-агент (-A)",
        "SSH-ключ" to "SSH-ключ",
        "SSH-ключи" to "SSH-ключі",
        "SSH-сервер" to "SSH-сервер",
        "Автозапуск команды при входе" to "Автозапуск команди під час входу",
        "Автокопирование при выделении" to "Автокопіювання при виділенні",
        "Автоматически копировать выделенный текст в буфер обмена" to "Автоматично копіювати виділений текст у буфер обміну",
        "Автоскрытие при прокрутке" to "Автоприховування під час прокрутки",
        "Активно • Полный доступ ко всем папкам /sdcard" to "Активно • Повний доступ до всіх папок /sdcard",
        "Активно • Сессии и SSH не закрываются в фоне" to "Активно • Сесії та SSH не закриваються у фоні",
        "Активно • Стандартный доступ к медиа и файлам" to "Активно • Стандартний доступ до медіа та файлів",
        "Активно • Фоновые сессии и SSH защищены от выгрузки" to "Активно • Фонові сесії та SSH захищені від вивантаження",
        "Анимация мигания" to "Анімація блимання",
        "Базовый оттенок" to "Базовий відтінок",
        "Блок (Скруглённый)" to "Блок (Заокруглений)",
        "Буфер прокрутки (Scrollback)" to "Буфер прокрутки (Scrollback)",
        "Быстрый выбор из Загрузок (Download)" to "Швидкий вибір із Завантажень (Download)",
        "Быстрый импорт из команды" to "Швидкий імпорт із команди",
        "Ввод и отклик" to "Введення та відгук",
        "Вертикальный луч (Bar)" to "Вертикальний промінь (Bar)",
        "Вес (размер)" to "Вага (розмір)",
        "Вибрация активна" to "Вібрацію увімкнено",
        "Вибрация отключена" to "Вібрацію вимкнено",
        "Владелец" to "Власник",
        "Все" to "Усі",
        "Всего" to "Усього",
        "Вставить" to "Вставити",
        "Вставить из буфера" to "Вставити з буфера",
        "Вставьте корректный приватный ключ" to "Вставте коректний приватний ключ",
        "Встроенный системный моноширинный шрифт" to "Вбудований системний моноширинний шрифт",
        "Выберите SSH-ключ" to "Оберіть SSH-ключ",
        "Выберите SSH-сервер" to "Оберіть SSH-сервер",
        "Выберите ряд для настройки кнопок" to "Оберіть рядок для налаштування кнопок",
        "Выберите тип разрешения для работы с внутренней памятью (/sdcard) в терминале и проводнике SFTP." to "Оберіть тип дозволу для роботи з внутрішньою пам'яттю (/sdcard) у терміналі та провіднику SFTP.",
        "Выберите шаблон нижней панели клавиш" to "Оберіть шаблон нижньої панелі клавіш",
        "Выбрать всё" to "Виділити все",
        "Выбрать файл ключа через проводник" to "Обрати файл ключа через провідник",
        "Выбрать через проводник Android" to "Обрати через провідник Android",
        "Выключено" to "Вимкнено",
        "Вырезать" to "Вирізати",
        "Выход" to "Вихід",
        "Геометрия отображения каретки в терминале" to "Геометрія відображення каретки в терміналі",
        "Готовые шаблоны (нажмите для подстановки):" to "Готові шаблони (натисніть для підстановки):",
        "Готовый профиль раскладки" to "Готовий профіль розкладки",
        "Группа" to "Група",
        "Динамический SOCKS5 прокси (-D)" to "Динамічний SOCKS5 проксі (-D)",
        "Длительность импульса" to "Тривалість імпульсу",
        "Длительность скольжения" to "Тривалість ковзання",
        "Добавить" to "Додати",
        "Дополнительно" to "Додатково",
        "Дополнительные флаги SSH" to "Додаткові прапорці SSH",
        "Доступ к файлам" to "Доступ до файлів",
        "Доступ к файлам устройства" to "Доступ до файлів пристрою",
        "Жирное начертание (700)" to "Жирне накреслення (700)",
        "Загрузка файла…" to "Завантаження файлу…",
        "Закрыть" to "Закрити",
        "Закрыть сессию" to "Закрити сесію",
        "Запись (W)" to "Запис (W)",
        "Запросить системное разрешение приложения" to "Запитати системний дозвіл застосунку",
        "Запуск (X)" to "Виконання (X)",
        "Изменить" to "Змінити",
        "Изменить текст" to "Змінити текст",
        "Изменён" to "Змінено",
        "Или вставьте приватный ключ (PEM / OpenSSH)" to "Або вставте приватний ключ (PEM / OpenSSH)",
        "Импорт SSH-ключа в хранилище" to "Імпорт SSH-ключа у сховище",
        "Импорт файла" to "Імпорт файлу",
        "Импортировать" to "Імпортувати",
        "Импортировать или создать ключ вручную" to "Імпортувати або створити ключ вручну",
        "Имя ключа в хранилище" to "Ім'я ключа у сховищі",
        "Имя папки (поддерживается вложенность a/b)" to "Ім'я папки (підтримується вкладеність a/b)",
        "Имя файла" to "Ім'я файлу",
        "Имя файла ключа в ~/.ssh" to "Ім'я файлу ключа в ~/.ssh",
        "Инициализация..." to "Ініціалізація...",
        "Использовать локальные ключи на удалённом сервере" to "Використовувати локальні ключі на віддаленому сервері",
        "Использовать системную палитру обоев Android" to "Використовувати системну палітру шпалер Android",
        "Используйте нижнюю панель, чтобы открыть хранилище «SSH-ключи» или добавить новый сервер." to "Використовуйте нижню панель, щоб відкрити сховище «SSH-ключі» або додати новий сервер.",
        "История вывода" to "Історія виводу",
        "История сессии" to "Історія сесії",
        "Источник цвета" to "Джерело кольору",
        "Итоговая команда подключения" to "Підсумкова команда підключення",
        "Каждые 120 сек" to "Кожні 120 сек",
        "Каждые 15 сек" to "Кожні 15 сек",
        "Каждые 30 сек" to "Кожні 30 сек",
        "Каждые 60 сек" to "Кожні 60 сек",
        "Каталог специальных клавиш" to "Каталог спеціальних клавіш",
        "Клавиатура и бинды" to "Клавіатура та бінди",
        "Клавиша" to "Клавіша",
        "Клавиши сброшены по умолчанию" to "Клавіші скинуто за замовчуванням",
        "Классическое начертание (400)" to "Класичне накреслення (400)",
        "Ключ" to "Ключ",
        "Ключ id_ed25519 создан и выбран" to "Ключ id_ed25519 створено та обрано",
        "Ключ добавлен в хранилище" to "Ключ додано до сховища",
        "Код клавиши (напр. CTRL+C, ALT+B, F5)" to "Код клавіші (напр. CTRL+C, ALT+B, F5)",
        "Количество сохраняемых строк истории вывода" to "Кількість збережених рядків історії виводу",
        "Команда с авто-Enter (напр. htop)" to "Команда з авто-Enter (напр. htop)",
        "Команда ↵" to "Команда ↵",
        "Комментарий (-C)" to "Коментар (-C)",
        "Конструктор клавиши / макроса" to "Конструктор клавіші / макросу",
        "Копировать" to "Копіювати",
        "Копировать всё" to "Копіювати все",
        "Кривая затухания и пульсации каретки" to "Крива згасання та пульсації каретки",
        "Курсор и анимации" to "Курсор та анімації",
        "Лигатуры шрифта" to "Лігатури шрифту",
        "Лок. порт" to "Лок. порт",
        "Локальный проброс портов (-L)" to "Локальне перенаправлення портів (-L)",
        "Масштаб и интервалы" to "Масштаб та інтервали",
        "Масштаб шрифта" to "Масштаб шрифту",
        "Межстрочный интервал" to "Міжрядковий інтервал",
        "Модификаторы и ввод" to "Модифікатори та введення",
        "Мой сервер" to "Мій сервер",
        "Мощность импульса" to "Потужність імпульсу",
        "Мощный (14 мс • 90%)" to "Потужний (14 мс • 90%)",
        "Мягкий (6 мс • 30%)" to "М'який (6 мс • 30%)",
        "Навигация и курсор" to "Навігація та курсор",
        "Надпись на кнопке (напр. top, git, C-c)" to "Напис на кнопці (напр. top, git, C-c)",
        "Нажмите на любую клавишу, чтобы добавить её в выбранный ряд" to "Натисніть будь-яку клавішу, щоб додати її у вибраний рядок",
        "Название" to "Назва",
        "Напр. -X или -o StrictHostKeyChecking=no" to "Напр. -X або -o StrictHostKeyChecking=no",
        "Настройка SSH-сервера" to "Налаштування SSH-сервера",
        "Настройка мощности и длительности вибрации" to "Налаштування потужності та тривалості вібрації",
        "Настройка рядов нижней панели и макросов" to "Налаштування рядків нижньої панелі та макросів",
        "Настройки" to "Налаштування",
        "Насыщенная палитра с высокой хроматичностью акцентов" to "Насичена палітра з високою хроматичністю акцентів",
        "Начальное содержимое" to "Початковий вміст",
        "Не в сети" to "Не в мережі",
        "Не выключать экран" to "Не вимикати екран",
        "Не удалось подключиться по SFTP/SSH" to "Не вдалося підключитися через SFTP/SSH",
        "Не удалось распознать приватный ключ" to "Не вдалося розпізнати приватний ключ",
        "Недопустимое имя" to "Неприпустиме ім'я",
        "Необязательно (для авто-входа)" to "Необов'язково (для автовходу)",
        "Необязательно…" to "Необов'язково…",
        "Непрозрачность курсора" to "Непрозорість курсора",
        "Нет серверов для пинга" to "Немає серверів для пінгу",
        "Нет сохранённых SSH-серверов" to "Немає збережених SSH-серверів",
        "Нижняя линия (Underline)" to "Нижня лінія (Underline)",
        "Новая папка" to "Нова папка",
        "Новый SSH-сервер" to "Новий SSH-сервер",
        "Новый файл" to "Новий файл",
        "ОК" to "ОК",
        "Объединять операторы !=, ->, =>, :: в единые символы" to "Об'єднувати оператори !=, ->, =>, :: в єдині символи",
        "Обычное разрешение приложения" to "Звичайний дозвіл застосунку",
        "Остальные" to "Інші",
        "Отключить оптимизацию батареи для фоновой работы" to "Вимкнути оптимізацію батареї для фонової роботи",
        "Открыть SFTP" to "Відкрити SFTP",
        "Открыть папку в терминале" to "Відкрити папку в терміналі",
        "Открыть системное меню выбора любого файла" to "Відкрити системне меню вибору будь-якого файлу",
        "Открыть хранилище ключей SSH…" to "Відкрити сховище ключів SSH…",
        "Отмена" to "Скасувати",
        "Оформление и интерфейс" to "Оформлення та інтерфейс",
        "Оффлайн" to "Офлайн",
        "Очистить" to "Очистити",
        "Очистить экран (Ctrl+L)" to "Очистити екран (Ctrl+L)",
        "Ошибка подключения SFTP" to "Помилка підключення SFTP",
        "Пакеты" to "Пакети",
        "Память и фоновый режим" to "Пам'ять та фоновий режим",
        "Папка" to "Папка",
        "Папка пуста" to "Папка порожня",
        "Папка • подсчёт веса…" to "Папка • підрахунок розміру…",
        "Параметры анимации" to "Параметри анімації",
        "Параметры соединения" to "Параметри з'єднання",
        "Пароль" to "Пароль",
        "Переименовать" to "Перейменувати",
        "Перейти" to "Перейти",
        "Перейти по пути (поддерживается root /)" to "Перейти за шляхом (підтримується root /)",
        "Пинг" to "Пінг",
        "Пингануть сервер" to "Пінганути сервер",
        "Пинг…" to "Пінг…",
        "Плавно скрывать панель клавиш при прокрутке истории вверх" to "Плавно приховувати панель клавіш під час прокрутки історії вгору",
        "Плавное скольжение (Smooth Caret)" to "Плавне ковзання (Smooth Caret)",
        "Поведение каретки" to "Поведінка каретки",
        "Поведение терминала" to "Поведінка термінала",
        "Повторить попытку" to "Повторити спробу",
        "Поддерживаются OpenSSH, PEM, id_ed25519, id_rsa" to "Підтримуються OpenSSH, PEM, id_ed25519, id_rsa",
        "Поддержка соединения (Keep-Alive)" to "Підтримка з'єднання (Keep-Alive)",
        "Подключаться строго по протоколу IPv4" to "Підключатися виключно за протоколом IPv4",
        "Подключение для проброса портов без запуска оболочки" to "Підключення для перенаправлення портів без запуску оболонки",
        "Подключение и авторизация" to "Підключення та авторизація",
        "Подключиться" to "Підключитися",
        "Подсчёт размера папки…" to "Підрахунок розміру папки…",
        "Поделиться" to "Поділитися",
        "Поиск в папке…" to "Пошук у папці…",
        "Полный путь" to "Повний шлях",
        "Полужирное начертание (600)" to "Напівжирне накреслення (600)",
        "Полый контур (Outline)" to "Порожнистий контур (Outline)",
        "Пользователь" to "Користувач",
        "Пользовательский" to "Користувацький",
        "Порт" to "Порт",
        "Права доступа (chmod)" to "Права доступу (chmod)",
        "Проброс SSH-агента (-A)" to "Перенаправлення SSH-агента (-A)",
        "Проброс портов и туннелирование" to "Перенаправлення портів і тунелювання",
        "Промежуточный сервер ProxyJump (-J)" to "Проміжний сервер ProxyJump (-J)",
        "Профиль вибрации" to "Профіль вібрації",
        "Пружинная интерполяция движения каретки как в VS Code" to "Пружинна інтерполяція руху каретки як у VS Code",
        "Путь" to "Шлях",
        "Работа в фоне без ограничений" to "Робота у фоні без обмежень",
        "Радиус скругления углов" to "Радіус заокруглення кутів",
        "Разобрать" to "Розібрати",
        "Разрешить работу в фоне без ограничений батареи" to "Дозволити роботу у фоні без обмежень батареї",
        "Раскладка панели" to "Розкладка панелі",
        "Распаковать здесь" to "Розпакувати тут",
        "Редактируемый ряд" to "Редагований рядок",
        "Рекомендуется для терминала, SFTP и скриптов" to "Рекомендується для термінала, SFTP та скриптів",
        "Родительская папка" to "Батьківська папка",
        "Ряд пуст — выберите клавиши из каталога ниже" to "Рядок порожній — оберіть клавіші з каталогу нижче",
        "Сбалансированная палитра с мягкими тональными переходами" to "Збалансована палітра з м'якими тональними переходами",
        "Сброс" to "Скидання",
        "Свойства" to "Властивості",
        "Сжатие трафика (-C)" to "Стиснення трафіку (-C)",
        "Сигналы и функции" to "Сигнали та функції",
        "Символы терминала" to "Символи термінала",
        "Системные цвета (По умолчанию)" to "Системні кольори (За замовчуванням)",
        "Системные цвета и стили палитры" to "Системні кольори та стилі палітри",
        "Системный (Авто)" to "Системна (Авто)",
        "Системный моноширинный" to "Системний моноширинний",
        "Системный проводник недоступен" to "Системний провідник недоступний",
        "Системный шрифт Android, JetBrains Mono или свой .ttf/.otf" to "Системний шрифт Android, JetBrains Mono або власний .ttf/.otf",
        "Системный шрифт устройства" to "Системний шрифт пристрою",
        "Скопировано" to "Скопійовано",
        "Скопировать команду" to "Скопіювати команду",
        "Скопировать путь" to "Скопіювати шлях",
        "Скорость затухания" to "Швидкість згасання",
        "Скрыть нижнюю панель" to "Приховати нижню панель",
        "Сначала добавьте SSH-сервер во вкладке SSH" to "Спочатку додайте SSH-сервер у вкладці SSH",
        "Создайте кнопку для мгновенного ввода команды, текста или сочетания клавиш" to "Створіть кнопку для миттєвого введення команди, тексту або комбінації клавіш",
        "Создайте новый ключ Ed25519 / RSA 4096 или импортируйте готовый ключ. Все ключи из хранилища доступны при создании SSH-сервера." to "Створіть новий ключ Ed25519 / RSA 4096 або імпортуйте готовий ключ. Усі ключі зі сховища доступні під час створення SSH-сервера.",
        "Создать" to "Створити",
        "Создать или импортировать ключ…" to "Створити або імпортувати ключ…",
        "Создать ключ" to "Створити ключ",
        "Создать ключ Ed25519 автоматически" to "Створити ключ Ed25519 автоматично",
        "Создать новый SSH-ключ" to "Створити новий SSH-ключ",
        "Создать новый или импортировать ключ" to "Створити новий або імпортувати ключ",
        "Сохранение…" to "Збереження…",
        "Сохранить" to "Зберегти",
        "Сохранить в Загрузки (/sdcard/Download)" to "Зберегти в Завантаження (/sdcard/Download)",
        "Сохранить в хранилище" to "Зберегти у сховище",
        "Спец. доступ ко всем файлам и обычное разрешение хранилища" to "Спец. доступ до всіх файлів та звичайний дозвіл сховища",
        "Спец. разрешение «Доступ ко всем файлам»" to "Спец. дозвіл «Доступ до всіх файлів»",
        "Спокойная минималистичная схема с деликатным оттенком" to "Спокійна мінімалістична схема з делікатним відтінком",
        "Способ авторизации" to "Спосіб авторизації",
        "Средняя насыщенность (500)" to "Середня насиченість (500)",
        "Стиль палитры" to "Стиль палітри",
        "Тактильный отклик" to "Тактильний відгук",
        "Текст" to "Текст",
        "Текст без Enter (напр. ssh root@)" to "Текст без Enter (напр. ssh root@)",
        "Терминал" to "Термінал",
        "Только IPv4 (-4)" to "Лише IPv4 (-4)",
        "Только туннель портов (-N)" to "Лише тунель портів (-N)",
        "Тонкое начертание (300)" to "Тонке накреслення (300)",
        "Уверены?" to "Впевнені?",
        "Удал. порт" to "Віддал. порт",
        "Удалить" to "Видалити",
        "Удалить сервер" to "Видалити сервер",
        "Удалённый проброс портов (-R)" to "Віддалене перенаправлення портів (-R)",
        "Удерживайте клавишу для перетаскивания • Нажмите ✕ для удаления" to "Утримуйте клавішу для перетягування • Натисніть ✕ для видалення",
        "Удерживать подсветку экрана во время работы терминала" to "Утримувати підсвічування екрана під час роботи термінала",
        "Укажите адрес сервера (Host / IP)" to "Вкажіть адресу сервера (Host / IP)",
        "Упаковать в .zip" to "Запакувати в .zip",
        "Упругий (8 мс • 55%)" to "Пружний (8 мс • 55%)",
        "Ускоряет передачу текста на медленных сетях" to "Прискорює передачу тексту в повільних мережах",
        "Файл" to "Файл",
        "Файл / Ключ" to "Файл / Ключ",
        "Файл не содержит приватного SSH-ключа" to "Файл не містить приватного SSH-ключа",
        "Файл превышает 512 КБ" to "Файл перевищує 512 КБ",
        "Файлы" to "Файли",
        "Форма каретки, плавное скольжение и затухание" to "Форма каретки, плавне ковзання та згасання",
        "Форма курсора" to "Форма курсора",
        "Характер тактильного отклика" to "Характер тактильного відгуку",
        "Хост (IP или домен)" to "Хост (IP або домен)",
        "Хранилище SSH-ключей пусто" to "Сховище SSH-ключів порожнє",
        "Хранилище ключей SSH" to "Сховище ключів SSH",
        "Цвета и палитра" to "Кольори та палітра",
        "Чтение (R)" to "Читання (R)",
        "Шрифт вашей системы Android по умолчанию" to "Шрифт вашої системи Android за замовчуванням",
        "Шрифт и терминал" to "Шрифт і термінал",
        "Шрифт и язык" to "Шрифт і мова",
        "Шрифт терминала" to "Шрифт термінала",
        "Шрифт, язык, масштаб и параметры терминала" to "Шрифт, мова, масштаб і параметри термінала",
        "Язык всех меню, диалогов и уведомлений" to "Мова всіх меню, діалогів та сповіщень",
        "Язык интерфейса" to "Мова інтерфейсу",
        "Буфер пуст." to "Буфер порожній.",
        "Выразительное сочетание оттенков с глубоким контрастом" to "Виразне поєднання відтінків із глибоким контрастом",
        "Ed25519 (Быстрый)" to "Ed25519 (Швидкий)",
        "байт" to "байт",
        "мс" to "мс",
        "файл(ов)" to "файл(ів)"
    )

    private fun translateUkDynamic(ru: String): String? {
        return when {
            ru.endsWith(" строк") -> ru.removeSuffix(" строк") + " рядків"
            ru.startsWith("Сессия ") -> "Сесія " + ru.removePrefix("Сессия ")
            ru.startsWith("Архив создан: ") -> "Архів створено: " + ru.removePrefix("Архив создан: ")
            ru.startsWith("Создан архив: ") -> "Створено архів: " + ru.removePrefix("Создан архив: ")
            ru.startsWith("Загружено: ") -> "Завантажено: " + ru.removePrefix("Загружено: ")
            ru.startsWith("Загрузка ") -> {
                val rest = ru.removePrefix("Загрузка ")
                "Вивантаження " + rest.replace(" на ", " на ")
            }
            ru.startsWith("Скачивание ") -> "Завантаження " + ru.removePrefix("Скачивание ")
            ru.startsWith("Выгрузить файл на ") -> "Вивантажити файл на " + ru.removePrefix("Выгрузить файл на ")
            ru.startsWith("Выбрать из хранилища (") -> "Обрати зі сховища (" + ru.removePrefix("Выбрать из хранилища (")
            ru.startsWith("Сохранённые ключи (") -> "Збережені ключі (" + ru.removePrefix("Сохранённые ключи (")
            ru.startsWith("Установленные пакеты (") -> "Встановлені пакети (" + ru.removePrefix("Установленные пакеты (")
            ru.startsWith("Системные пакеты (") -> "Системні пакети (" + ru.removePrefix("Системные пакеты (")
            ru.startsWith("Вставить (") -> "Вставити (" + ru.removePrefix("Вставить (")
            ru.startsWith("Применить права (chmod ") -> "Застосувати права (chmod " + ru.removePrefix("Применить права (chmod ")
            ru.startsWith("Ряд 1 (") -> "Рядок 1 (" + ru.removePrefix("Ряд 1 (")
            ru.startsWith("Ряд 2 (") -> "Рядок 2 (" + ru.removePrefix("Ряд 2 (")
            ru.startsWith("Кнопки ряда ") -> "Кнопки рядка " + ru.removePrefix("Кнопки ряда ")
            ru.startsWith("Максимум ") && ru.endsWith(" клавиш в одном ряду") ->
                "Максимум " + ru.removePrefix("Максимум ").removeSuffix(" клавиш в одном ряду") + " клавіш в одному рядку"
            ru.startsWith("Нет файлов по запросу «") -> "Немає файлів за запитом «" + ru.removePrefix("Нет файлов по запросу «")
            ru.startsWith("Генерация ключа ") -> "Генерація ключа " + ru.removePrefix("Генерация ключа ")
            ru.startsWith("Ключ создан: ") -> "Ключ створено: " + ru.removePrefix("Ключ создан: ")
            ru.startsWith("Ключ сохранён в хранилище: ") -> "Ключ збережено у сховищі: " + ru.removePrefix("Ключ сохранён в хранилище: ")
            ru.startsWith("Ключ сохранён: ") -> "Ключ збережено: " + ru.removePrefix("Ключ сохранён: ")
            ru.startsWith("Импортирован ключ: ") -> "Імпортовано ключ: " + ru.removePrefix("Импортирован ключ: ")
            ru.startsWith("Импортирован профиль из ") -> "Імпортовано профіль з " + ru.removePrefix("Импортирован профиль из ")
            ru.startsWith("Импортировано: ") -> "Імпортовано: " + ru.removePrefix("Импортировано: ")
            ru.startsWith("Распаковано: ") -> "Розпаковано: " + ru.removePrefix("Распаковано: ")
            ru.startsWith("Сохранено на ") -> "Збережено на " + ru.removePrefix("Сохранено на ")
            ru.startsWith("Сохранено: ") -> "Збережено: " + ru.removePrefix("Сохранено: ")
            ru.startsWith("Файл: ") -> ru
            else -> null
        }
    }

    private val OTHER_STATIC: Map<String, Map<String, String>> = mapOf(
        "de" to mapOf(
            "Terminal" to "Terminal",
            "Files" to "Dateien",
            "Packages" to "Pakete",
            "Settings" to "Einstellungen",
            "SSH Keys" to "SSH-Schlüssel",
            "SSH Key Vault" to "SSH-Schlüsseltresor",
            "Appearance & UI" to "Darstellung & UI",
            "Colors & palette" to "Farben & Palette",
            "Font & terminal" to "Schrift & Terminal",
            "Font & language" to "Schrift & Sprache",
            "App language" to "App-Sprache",
            "Language of all menus, dialogs, and notifications" to "Sprache aller Menüs, Dialoge und Benachrichtigungen",
            "Input & feedback" to "Eingabe & Feedback",
            "Cursor & animations" to "Cursor & Animationen",
            "Keyboard & bindings" to "Tastatur & Tasten",
            "Haptic feedback" to "Haptisches Feedback",
            "Storage & background" to "Speicher & Hintergrund",
            "File access permissions" to "Dateizugriffsberechtigungen",
            "Unrestricted background execution" to "Unbeschränkte Hintergrundausführung",
            "Connect" to "Verbinden",
            "Cancel" to "Abbrechen",
            "Save" to "Speichern",
            "Delete" to "Löschen",
            "Remove" to "Entfernen",
            "Edit" to "Bearbeiten",
            "Close" to "Schließen",
            "Copy" to "Kopieren",
            "Cut" to "Ausschneiden",
            "Properties" to "Eigenschaften",
            "Permissions (chmod)" to "Berechtigungen (chmod)",
            "Rename" to "Umbenennen",
            "Compress to .zip" to "Als .zip packen",
            "Extract here" to "Hier entpacken",
            "New folder" to "Neuer Ordner",
            "New file" to "Neue Datei",
            "Folder" to "Ordner",
            "File" to "Datei",
            "Search in folder…" to "Im Ordner suchen…",
            "Ping server" to "Server anpingen",
            "Exit" to "Beenden"
        ),
        "es" to mapOf(
            "Terminal" to "Terminal",
            "Files" to "Archivos",
            "Packages" to "Paquetes",
            "Settings" to "Ajustes",
            "SSH Keys" to "Claves SSH",
            "SSH Key Vault" to "Almacén de claves SSH",
            "Appearance & UI" to "Apariencia e interfaz",
            "Colors & palette" to "Colores y paleta",
            "Font & terminal" to "Fuente y terminal",
            "Font & language" to "Fuente e idioma",
            "App language" to "Idioma de la aplicación",
            "Language of all menus, dialogs, and notifications" to "Idioma de todos los menús, diálogos y notificaciones",
            "Input & feedback" to "Entrada y respuesta",
            "Cursor & animations" to "Cursor y animaciones",
            "Keyboard & bindings" to "Teclado y atajos",
            "Haptic feedback" to "Respuesta háptica",
            "Storage & background" to "Almacenamiento y segundo plano",
            "File access permissions" to "Permisos de acceso a archivos",
            "Unrestricted background execution" to "Ejecución en segundo plano sin restricciones",
            "Connect" to "Conectar",
            "Cancel" to "Cancelar",
            "Save" to "Guardar",
            "Delete" to "Eliminar",
            "Remove" to "Quitar",
            "Edit" to "Editar",
            "Close" to "Cerrar",
            "Copy" to "Copiar",
            "Cut" to "Cortar",
            "Properties" to "Propiedades",
            "Permissions (chmod)" to "Permisos (chmod)",
            "Rename" to "Renombrar",
            "Compress to .zip" to "Comprimir en .zip",
            "Extract here" to "Extraer aquí",
            "New folder" to "Nueva carpeta",
            "New file" to "Nuevo archivo",
            "Folder" to "Carpeta",
            "File" to "Archivo",
            "Search in folder…" to "Buscar en carpeta…",
            "Ping server" to "Hacer ping al servidor",
            "Exit" to "Salir"
        ),
        "fr" to mapOf(
            "Terminal" to "Terminal",
            "Files" to "Fichiers",
            "Packages" to "Paquets",
            "Settings" to "Paramètres",
            "SSH Keys" to "Clés SSH",
            "SSH Key Vault" to "Coffre de clés SSH",
            "Appearance & UI" to "Apparence et interface",
            "Colors & palette" to "Couleurs et palette",
            "Font & terminal" to "Police et terminal",
            "Font & language" to "Police et langue",
            "App language" to "Langue de l'application",
            "Language of all menus, dialogs, and notifications" to "Langue de tous les menus, dialogues et notifications",
            "Input & feedback" to "Saisie et retour",
            "Cursor & animations" to "Curseur et animations",
            "Keyboard & bindings" to "Clavier et raccourcis",
            "Haptic feedback" to "Retour haptique",
            "Storage & background" to "Stockage et arrière-plan",
            "File access permissions" to "Autorisations d'accès aux fichiers",
            "Unrestricted background execution" to "Exécution en arrière-plan sans restriction",
            "Connect" to "Connecter",
            "Cancel" to "Annuler",
            "Save" to "Enregistrer",
            "Delete" to "Supprimer",
            "Remove" to "Retirer",
            "Edit" to "Modifier",
            "Close" to "Fermer",
            "Copy" to "Copier",
            "Cut" to "Couper",
            "Properties" to "Propriétés",
            "Permissions (chmod)" to "Permissions (chmod)",
            "Rename" to "Renommer",
            "Compress to .zip" to "Compresser en .zip",
            "Extract here" to "Extraire ici",
            "New folder" to "Nouveau dossier",
            "New file" to "Nouveau fichier",
            "Folder" to "Dossier",
            "File" to "Fichier",
            "Search in folder…" to "Rechercher dans le dossier…",
            "Ping server" to "Ping du serveur",
            "Exit" to "Quitter"
        ),
        "pl" to mapOf(
            "Terminal" to "Terminal",
            "Files" to "Pliki",
            "Packages" to "Pakiety",
            "Settings" to "Ustawienia",
            "SSH Keys" to "Klucze SSH",
            "SSH Key Vault" to "Magazyn kluczy SSH",
            "Appearance & UI" to "Wygląd i interfejs",
            "Colors & palette" to "Kolory i paleta",
            "Font & terminal" to "Czcionka i terminal",
            "Font & language" to "Czcionka i język",
            "App language" to "Język aplikacji",
            "Language of all menus, dialogs, and notifications" to "Język wszystkich menu, okien dialogowych i powiadomień",
            "Input & feedback" to "Wprowadzanie i wibracje",
            "Cursor & animations" to "Kursor i animacje",
            "Keyboard & bindings" to "Klawiatura i skróty",
            "Haptic feedback" to "Wibracje haptyczne",
            "Storage & background" to "Pamięć i praca w tle",
            "File access permissions" to "Uprawnienia dostępu do plików",
            "Unrestricted background execution" to "Praca w tle bez ograniczeń",
            "Connect" to "Połącz",
            "Cancel" to "Anuluj",
            "Save" to "Zapisz",
            "Delete" to "Usuń",
            "Remove" to "Usuń",
            "Edit" to "Edytuj",
            "Close" to "Zamknij",
            "Copy" to "Kopiuj",
            "Cut" to "Wytnij",
            "Properties" to "Właściwości",
            "Permissions (chmod)" to "Uprawnienia (chmod)",
            "Rename" to "Zmień nazwę",
            "Compress to .zip" to "Spakuj do .zip",
            "Extract here" to "Rozpakuj tutaj",
            "New folder" to "Nowy folder",
            "New file" to "Nowy plik",
            "Folder" to "Folder",
            "File" to "Plik",
            "Search in folder…" to "Szukaj w folderze…",
            "Ping server" to "Pinguj serwer",
            "Exit" to "Wyjdź"
        ),
        "pt" to mapOf(
            "Terminal" to "Terminal",
            "Files" to "Arquivos",
            "Packages" to "Pacotes",
            "Settings" to "Configurações",
            "SSH Keys" to "Chaves SSH",
            "SSH Key Vault" to "Cofre de chaves SSH",
            "Appearance & UI" to "Aparência e interface",
            "Colors & palette" to "Cores e paleta",
            "Font & terminal" to "Fonte e terminal",
            "Font & language" to "Fonte e idioma",
            "App language" to "Idioma do aplicativo",
            "Language of all menus, dialogs, and notifications" to "Idioma de todos os menus, diálogos e notificações",
            "Connect" to "Conectar",
            "Cancel" to "Cancelar",
            "Save" to "Salvar",
            "Delete" to "Excluir",
            "Remove" to "Remover",
            "Edit" to "Editar",
            "Close" to "Fechar",
            "Copy" to "Copiar",
            "Cut" to "Recortar",
            "Properties" to "Propriedades",
            "Permissions (chmod)" to "Permissões (chmod)",
            "Rename" to "Renomear",
            "Compress to .zip" to "Compactar em .zip",
            "Extract here" to "Extrair aqui",
            "New folder" to "Nova pasta",
            "New file" to "Novo arquivo",
            "Folder" to "Pasta",
            "File" to "Arquivo",
            "Ping server" to "Pingar servidor",
            "Exit" to "Sair"
        ),
        "tr" to mapOf(
            "Terminal" to "Terminal",
            "Files" to "Dosyalar",
            "Packages" to "Paketler",
            "Settings" to "Ayarlar",
            "SSH Keys" to "SSH Anahtarları",
            "SSH Key Vault" to "SSH Anahtar Kasası",
            "Appearance & UI" to "Görünüm ve Arayüz",
            "Colors & palette" to "Renkler ve palet",
            "Font & terminal" to "Yazı tipi ve terminal",
            "Font & language" to "Yazı tipi ve dil",
            "App language" to "Uygulama dili",
            "Language of all menus, dialogs, and notifications" to "Tüm menü, iletişim kutusu ve bildirimlerin dili",
            "Connect" to "Bağlan",
            "Cancel" to "İptal",
            "Save" to "Kaydet",
            "Delete" to "Sil",
            "Remove" to "Kaldır",
            "Edit" to "Düzenle",
            "Close" to "Kapat",
            "Copy" to "Kopyala",
            "Cut" to "Kes",
            "Properties" to "Özellikler",
            "Permissions (chmod)" to "İzinler (chmod)",
            "Rename" to "Yeniden adlandır",
            "Compress to .zip" to ".zip olarak sıkıştır",
            "Extract here" to "Buraya çıkart",
            "New folder" to "Yeni klasör",
            "New file" to "Yeni dosya",
            "Folder" to "Klasör",
            "File" to "Dosya",
            "Ping server" to "Sunucuya ping at",
            "Exit" to "Çıkış"
        ),
        "zh" to mapOf(
            "Terminal" to "终端",
            "Files" to "文件",
            "Packages" to "软件包",
            "Settings" to "设置",
            "SSH Keys" to "SSH 密钥",
            "SSH Key Vault" to "SSH 密钥库",
            "Appearance & UI" to "外观与界面",
            "Colors & palette" to "颜色与调色板",
            "Font & terminal" to "字体与终端",
            "Font & language" to "字体与语言",
            "App language" to "界面语言",
            "Language of all menus, dialogs, and notifications" to "所有菜单、对话框和通知的显示语言",
            "Input & feedback" to "输入与反馈",
            "Cursor & animations" to "光标与动画",
            "Keyboard & bindings" to "键盘与快捷键",
            "Haptic feedback" to "触觉反馈",
            "Storage & background" to "存储与后台",
            "File access permissions" to "文件访问权限",
            "Unrestricted background execution" to "无限制后台运行",
            "Connect" to "连接",
            "Cancel" to "取消",
            "Save" to "保存",
            "Delete" to "删除",
            "Remove" to "移除",
            "Edit" to "编辑",
            "Close" to "关闭",
            "Copy" to "复制",
            "Cut" to "剪切",
            "Properties" to "属性",
            "Permissions (chmod)" to "权限 (chmod)",
            "Rename" to "重命名",
            "Compress to .zip" to "压缩为 .zip",
            "Extract here" to "解压到此处",
            "New folder" to "新建文件夹",
            "New file" to "新建文件",
            "Folder" to "文件夹",
            "File" to "文件",
            "Search in folder…" to "在文件夹中搜索…",
            "Ping server" to "Ping 服务器",
            "Exit" to "退出"
        )
    )

    private fun translateOtherDynamic(langCode: String, en: String): String? {
        if (!en.startsWith("Session ")) return null
        val num = en.removePrefix("Session ")
        return when (langCode) {
            "de" -> "Sitzung $num"
            "es" -> "Sesión $num"
            "fr" -> "Session $num"
            "pl" -> "Sesja $num"
            "pt" -> "Sessão $num"
            "tr" -> "Oturum $num"
            "zh" -> "会话 $num"
            else -> null
        }
    }
}
