# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build debug APK (requires Android SDK)
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run lint checks
./gradlew lint

# Clean build
./gradlew clean assembleDebug
```

> On Windows use `gradlew.bat` instead of `./gradlew`. Android Studio can open this project directly and will auto-download the Gradle wrapper jar.

## Architecture

**MVVM** with a unidirectional data flow: `Room DB → DAO → Repository → ViewModel (StateFlow) → Compose UI`

### Data Layer (`data/`)
- **Entities** (`database/entity/`): `Device`, `Category`, `Staff`, `StockRecord`. Enums (`DeviceStatus`, `RecordType`, `DeviceCondition`) are stored as Strings via `Converters.kt` type converters.
- **DAOs** (`database/dao/`): Return `Flow<T>` for reactive queries; suspend functions for writes.
- **AppDatabase**: Singleton via `AppDatabase.getDatabase(context)`. Prepopulates 8 default categories (主机、显示器、鼠标、键盘…) on first creation via `RoomDatabase.Callback`.
- **Repositories**: Thin wrappers — delegate directly to DAOs, no extra logic.

### ViewModel Layer (`viewmodel/`)
Each ViewModel takes repositories as constructor parameters and exposes a single `StateFlow<XxxUiState>`. Factory pattern via companion object `fun factory(app: InventoryApp)` — no Hilt/DI framework used.

`DeviceViewModel` owns all device mutation logic (stockOut, returnDevice, startMaintenance, etc.) and writes both the `Device` update and the corresponding `StockRecord` atomically within `viewModelScope.launch`.

### UI Layer (`ui/`)
- **Navigation**: Single-activity, `NavHost` in `MainActivity`. Bottom nav routes: `dashboard`, `devices`, `records`, `more`. Sub-routes: `device_detail/{deviceId}`, `add_device`, `edit_device/{deviceId}`, `staff`, `categories`.
- **Screens** are self-contained Composables that obtain their ViewModel via `viewModel(factory = XxxViewModel.factory(app))` where `app = LocalContext.current.applicationContext as InventoryApp`.
- Operation dialogs (StockOut, Return, MaintenanceStart/End) are defined as top-level `@Composable` functions in `DeviceDetailScreen.kt` and called inline — no separate screen navigation.

### Key Design Decisions
- **Denormalized fields**: `StockRecord` stores `deviceName`, `deviceAssetCode`, `staffName` as snapshots to preserve history even if the device/staff is later edited.
- **Soft delete for Staff**: `isActive = false` instead of physical delete.
- **No foreign key constraints** in Room to avoid cascade complexity.
- **CSV export** uses `MediaStore.Downloads` API (Android 10+) or `Environment.DIRECTORY_DOWNLOADS` for older versions; both paths are in `MoreScreen.saveCSV()`.
- **Asset code auto-generation**: `DeviceViewModel.generateAssetCode()` produces prefix-based codes (e.g., `PC-001`, `MON-002`).

## Package & Config

- **Package**: `com.inventory.manager`
- **Min SDK**: 26 (Android 8.0) — safe to use adaptive icons without PNG fallbacks
- **Build tool**: KSP (not kapt) for Room annotation processing
- **Compose BOM**: `2024.02.00`, Kotlin `1.9.22`, Compose Compiler Extension `1.5.8`
