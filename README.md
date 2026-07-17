# TripLog

Kotlin Multiplatform (KMP) приложение-дневник поездок с кроссплатформенным UI на Compose Multiplatform.

## Платформы

| Платформа | UI | БД | Карты |
|-----------|-----|-----|-------|
| Android | Compose Multiplatform | Room (SQLite) | OSMDroid MapView |
| Desktop (JVM) | Compose Multiplatform | SQLite JDBC | Leaflet.js + OSM (OpenStreetMap в браузере) |

---

## Android-версия

### Стек
- **Kotlin 1.9.22** + **Compose Multiplatform 1.5.12**
- **Room 2.6.1** — реактивная ORM для SQLite
- **OSMDroid 6.1.18** — нативная карта OpenStreetMap

### Архитектура

```
androidApp/src/main/kotlin/com/example/triplog/
├── MainActivity.kt          # Entry point, Compose Host
```

```
shared/src/androidMain/kotlin/com/example/triplog/
├── TripEntity.kt            # Room @Entity — таблица trips
├── TripDao.kt               # Room @Dao — insert + select
├── TripDatabase.kt          # RoomDatabase сpanion (Singleton)
├── TripRepositoryImpl.kt    # TripRepository: Room → Flow<Trip>
└── TripMapScreen.kt         # actual — OSMDroid MapView
```

### БД (Room)

Таблица `trips`:

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | ID поездки |
| `title` | `TEXT` | Название |
| `city` | `TEXT` | Город |
| `startDate` | `TEXT` | Дата начала (YYYY-MM-DD) |
| `endDate` | `TEXT` | Дата окончания (YYYY-MM-DD) |
| `notes` | `TEXT` | Заметки |
| `lat` | `REAL` | Широта |
| `lng` | `REAL` | Долгота |

KSP генерирует `TripDatabase_Impl` автоматически при сборке.

### Карты (OSMDroid)

- `actual fun TripMapScreen()` — Compose-обёртка над `MapView`
- Маркеры создаются по `lat/lng` из списка trips
- Карта центрируется на среднем положении всех точек
- `onRelease { map.onDetach() }` для корректной очистки

### Зависимости (androidApp)

```kotlin
implementation(project(":shared"))
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.activity:activity-compose:1.8.2")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
implementation(platform("androidx.compose:compose-bom:2024.01.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
```

---

## Desktop-версия (JVM)

### Стек
- **Kotlin 1.9.22** (JVM target) + **Compose Desktop 1.5.12**
- **SQLite JDBC 3.44.1.0** — файловая БД на диске
- **Leaflet.js 1.9.4** — карта в окне приложения через SwingPanel + JEditorPane

### Архитектура

```
desktopApp/src/jvmMain/kotlin/com/example/triplog/
├── Main.kt                 # Entry point, ComposeWindow
└── TripRepositoryImpl.kt   # JDBC: insert + select + StateFlow
```

```
shared/src/desktopMain/kotlin/com/example/triplog/
└── TripMapScreen.kt        # actual — HTML (Leaflet) + System Browser
```

### БД (SQLite JDBC)

Файл БД: `~/.trip_log/trips.db`

- При старте: `Class.forName("org.sqlite.JDBC")`
- Если файл не существует — создаёт таблицу `CREATE TABLE IF NOT EXISTS trips (...)`
- Все соединения обёрнуты в `try-with-resources` (auto-close)
- `insertTrip()` — `PreparedStatement` с `RETURN_GENERATED_KEYS`
- `getAllTrips()` — `SELECT * FROM trips ORDER BY id DESC` → `MutableStateFlow<List<Trip>>`

### Карты (Leaflet + OSM)

- `actual fun TripMapScreen()` — генерирует HTML с Leaflet.js
- При нажатии кнопки «Open Map» — создаёт временный `.html` файл
- Открывает его в системном браузере через `java.awt.Desktop.browse()`
- JS: загружает тайлы OSM, ставит маркеры по координатам, `fitBounds`

### Зависимости (desktopApp)

```kotlin
implementation(project(":shared"))
implementation(compose.desktop.currentOs)
implementation(compose.material3)
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
```

---

## Общее (shared)

### Ожидаемые декларации (expect/actual)

| expect (commonMain) | actual (androidMain) | actual (desktopMain) |
|---------------------|----------------------|----------------------|
| `TripMapScreen()` | OSMDroid `MapView` | HTML + Leaflet в браузере |

### Модель данных

```kotlin
data class Trip(
    val id: Int = 0,
    val title: String,
    val city: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val notes: String,
    val lat: Double,
    val lng: Double
)
```

### Репозиторий

```kotlin
interface TripRepository {
    suspend fun insertTrip(trip: Trip)
    fun getAllTrips(): Flow<List<Trip>>
}
```

### UI

- **Список поездок** — `LazyColumn` с `Card` для каждой поездки
- **Детали поездки** — заголовок, город, даты, координаты, заметки, карта
- **Добавление поездки** — форма с `DatePicker` (Material3) для выбора дат, текстовые поля
- **Настройки** — информационный экран

### Навигация

State-based навигация через `sealed class Screen`:
```
Screen.List → Screen.Detail(index)
Screen.List → Screen.Add → Screen.List
```

---

## Сборка и запуск

### Требования
- **JDK 17** (для обоих платформ)
- **Android SDK 34** (для Android-сборки)
- **Gradle 8.5** (встроен через wrapper)

### Команды

```bash
# Android APK
./gradlew :androidApp:assembleDebug

# Desktop JAR
./gradlew :desktopApp:jar

# Тесты
./gradlew :shared:test

# Полная сборка
./gradlew build
```

### Запуск

**Android:**
```bash
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n "com.example.triplog.android/com.example.triplog.MainActivity"
```

**Desktop:**
```bash
./gradlew :desktopApp:run
```

---

## Структура проекта

```
triplog/
├── build.gradle.kts              # Корневой — все плагины
├── settings.gradle.kts           # Модули: shared, androidApp, desktopApp
├── shared/
│   ├── build.gradle.kts          # KMP: commonMain, androidMain, desktopMain
│   └── src/
│       ├── commonMain/           # Trip, TripRepository, UI, App, expect
│       ├── commonTest/           # Unit-тесты (kotlin.test)
│       ├── androidMain/          # Room + OSMDroid actual
│       └── desktopMain/          # SQLite JDBC + Leaflet actual
├── androidApp/
│   ├── build.gradle.kts          # Android app + KSP
│   └── src/main/                 # MainActivity, AndroidManifest
└── desktopApp/
    ├── build.gradle.kts          # JVM app + Compose Desktop
    └── src/jvmMain/              # Main.kt + TripRepositoryImpl
```

---

## Лицензия

MIT
