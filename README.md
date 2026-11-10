# Lunar To-Do & Reminder App (農曆萬年曆與雙軌待辦提醒軟體)

[![Android CI](https://github.com/bingchiwang/notepad/actions/workflows/android.yml/badge.svg)](https://github.com/bingchiwang/notepad/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![Navigation 3](https://img.shields.io/badge/Navigation-3.0-orange.svg)](https://developer.android.com/guide/navigation)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 🌟 Overview / 專案概述

**Lunar To-Do & Reminder App (農曆萬年曆與雙軌待辦提醒軟體)** 是一款結合現代 Android 頂尖技術與傳統農曆文化的生產力應用程式。本應用專為華人生活圈設計，不僅提供靈活的雙軌待辦與提醒管理，更內建高效能的 **1900–2100 離線農曆萬年曆引擎**，支援干支紀年、二十四節氣與傳統節日計算。

App 採用純 Jetpack Compose 與 Material Design 3 打造，支援全面全螢幕體驗 (Edge-to-Edge)、動態色彩 (Dynamic Color)、自適應平板雙窗格佈局 (Compose Material Adaptive) 以及 Jetpack Navigation 3 狀態驅動導航。

---

## 📋 Key Features / 核心功能特色

### 1. 📋 雙軌待辦管理 (Dual-Track Task Management)
- **待辦事項 (To-Dos)**：支援完整勾選狀態、截止日期設定、優先級與分類標籤。
- **提醒事項 (Reminders)**：獨立的提醒追蹤系統，支援 `AlarmManager` 推播通知與時間排程。
- **番茄鐘專注與時間區塊 (Pomodoro & Time Boxing)**：內建專注計時與時間區塊管理，提升日常工作效率。

### 2. 📅 離線農曆萬年曆 (Offline Lunar Perpetual Calendar)
- **完全離線計算**：覆蓋 1900 至 2100 年的農曆轉換引擎，無需網路連線即可隨時查詢。
- **豐富曆法資訊**：精準提供農曆年月日、干支紀年 (天干地支)、二十四節氣以及傳統重要民俗節日。
- **日曆與任務聯動**：點選日曆日期即可直接檢視當天對應的待辦與提醒事項。

### 3. 🎨 智慧自定義顏色標籤 (Smart Custom Color Tagging)
- **多樣色彩選擇**：提供 8 種經典預設色彩與原生顏色選擇器。
- **智慧對比度計算**：膠囊標籤 (Capsule Labels) 自動計算文字與背景的最佳對比度，確保在任何主題下皆清晰易讀。

### 4. 🚨 即將到期警報 (Imminent Deadline Alerts)
- **呼吸燈動態邊框**：針對即將到期的任務呈現具呼吸感的橘紅虛線邊框。
- **醒目提醒徽章**：即時同步顯示「⚠️ Due Tomorrow!」或逾期徽章，跨卡片與日曆檢視無縫同步。

---

## 🏗️ Architecture & Design Patterns / 架構與設計模式

本專案嚴格遵循 Android 官方架構指南 (MAD) 與 Clean Architecture 原則，採用以下核心設計模式：

```
[ UI Layer: Jetpack Compose + Nav 3 ]
              │
              ▼
[ ViewModel Layer: StateFlow & UI State ]
              │
              ▼
[ Repository Pattern: TaskRepository ] ───► [ Facade Pattern: LunarCalendarFacade ]
              │                                      │
              ▼                                      ▼
[ Local Data Source: Room DB & DAOs ]       [ Algorithm Engine: LunarCalendarEngine ]
```

1. **Facade Pattern (外觀模式)**：
   - 透過 `LunarCalendarFacade` 與 `LunarCalendarEngine`，封裝複雜且高效能的農曆位元運算與曆法轉換邏輯，向 UI 層提供簡潔易用的 API。
2. **Repository Pattern (儲存庫模式)**：
   - `TaskRepository` 統一協調 Room Database (`AppDatabase`、各 Dao) 與 Kotlin Flows，提供非同步、響應式資料流。
3. **Jetpack Navigation 3 & Compose Material Adaptive**：
   - 使用 `@Serializable` NavKeys 與 `NavDisplay` 進行狀態驅動導航。
   - 透過 `ListDetailPaneScaffold` 實現手機與平板/摺疊機的響應式適應性佈局。

---

## 🛠️ Tech Stack / 技術堆疊

- **UI Framework**: Jetpack Compose, Material Design 3, Material Icons Extended
- **Navigation**: Jetpack Navigation 3 (`androidx.navigation3`)
- **Adaptive UI**: Compose Material Adaptive (`ListDetailPaneScaffold`)
- **Architecture**: MVVM, Kotlin Coroutines & Flows
- **Database**: Room Persistence Library (SQLite)
- **Background Tasks**: AlarmManager, BroadcastReceiver (`ReminderScheduler`, `ReminderReceiver`)
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`), Version Catalogs (`libs.versions.toml`)

---

## 📂 Project Structure / 專案目錄結構

```text
com.example.notepad/
├── calendar/               # 離線農曆計算引擎與外觀模式
│   ├── LunarCalendarEngine.kt
│   ├── LunarCalendarFacade.kt
│   └── LunarDate.kt
├── data/                   # 資料層 (Room DB & Repository)
│   ├── local/              # AppDatabase, DAOs, Entities
│   └── repository/         # TaskRepository
├── reminder/               # 鬧鐘與通知排程
│   ├── ReminderReceiver.kt
│   └── ReminderScheduler.kt
└── ui/                     # 呈現層 (Jetpack Compose)
    ├── app/                # Navigation 3 導航與狀態管理
    ├── components/         # 共用 UI 元件與對話框
    ├── screens/            # 各功能主畫面 (Task, Schedule, Calendar, Pomodoro)
    └── theme/              # Color, Theme, Type (M3 樣式與動態色彩)
```

---

## 🚀 CI/CD & GitHub Integration / 持續整合

本專案配置了完整的 GitHub Actions 工作流程 (`.github/workflows/android.yml`)，確保每次 Push 與 Pull Request 的程式碼品質：
- **環境設定**：自動安裝 Temurin JDK 17 與 Gradle 快取。
- **自動建置**：執行 `./gradlew build` 確保編譯無誤。
- **自動測試**：執行 `./gradlew test` 運行單元測試與業務邏輯驗證。

---

## 🏁 Getting Started & Building / 快速開始與建置

### 系統需求
- Android Studio Ladybug 或更新版本
- JDK 17
- Android SDK 34+ (支援 SDK 35)

### 建置與運行指令
在專案根目錄下執行以下指令：

```bash
# 建置 Debug APK
./gradlew assembleDebug

# 執行單元測試
./gradlew testDebugUnitTest

# 執行儀器化測試 (需連接實機或模擬器)
./gradlew connectedDebugAndroidTest
```

---

## 📄 License / 授權條款

本專案採用 [MIT License](LICENSE) 授權條款。
