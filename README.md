# Lunar To-Do & Notepad App (農曆萬年曆與雙軌待辦提醒記事)

[![Android CI](https://github.com/bingchiwang/Notepad/actions/workflows/android.yml/badge.svg)](https://github.com/bingchiwang/Notepad/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![Navigation 3](https://img.shields.io/badge/Navigation-3.0-orange.svg)](https://developer.android.com/guide/navigation)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-30-lightgrey.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-37-brightgreen.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 🌟 Overview / 專案概述

**Lunar To-Do & Notepad App** 是一款專為現代生活打造的高效生產力 Android 應用程式。本應用整合了**雙軌任務管理（待做事項與多日提醒）**、**1900–2100 年完全離線農曆萬年曆**、**每日時間規劃表（Daily Schedule Planner）**與**番茄鐘專注計時器（Pomodoro Timer）**。

全介面基於 **Jetpack Compose** 與 **Material Design 3** 打造，支援全面沉浸式體驗（Edge-to-Edge）、動態色彩標籤、平板與摺疊機自適應佈局（Adaptive Layout）、以及 **Jetpack Navigation 3** 狀態驅動導航。

---

## 📋 Key Features / 核心功能特色

### 1. 📋 雙軌任務與自由多日提醒 (Dual-Track Tasks & Reminders)
- **待做事件 (To-Dos)**：支援標題說明、到期日設定、色彩標籤、完成打勾狀態更新與過濾（全部 / 進行中 / 已完成）。
- **自由多日提醒事項 (Multi-Day Reminders)**：
  - **打破天數限制**：內建跨月/跨年導覽與互動月曆網格，支援任選未來任意年份與月份的多個日期。
  - **精準時間排程**：自訂小時/分鐘與常用時間段快選（08:00, 09:00, 12:00 等）。
  - **系統級推播通知**：整合 `AlarmManager.setExactAndAllowWhileIdle` 與 `BroadcastReceiver`，即使休眠亦能準時發出系統通知。
  - **完整編輯與安全防護**：支援即時編輯提醒內容與日期時間，刪除前全面觸發二次確認對話框（`ConfirmDeleteDialog`）。

### 2. 📅 離線農曆萬年曆 (Offline Lunar Perpetual Calendar)
- **完全離線換算**：支援 1900 年至 2100 年農曆演算法，無需聯網即時查詢。
- **豐富民俗資訊**：完整提供農曆月日、天干地支生肖紀年、二十四節氣與傳統民俗節慶。
- **月曆視覺化網格**：清楚標記當月所有日期、當日待辦事項連動、一鍵快速跳轉。

### 3. ⏰ 每日時間規劃表 (Daily Schedule Planner)
- **時間軸視覺化**：直觀呈現全天時間區塊（開始時間、結束時間、分類與備註）。
- **進度即時計算**：依打勾完成狀態自動計算並顯示當日進度百分比進度條。
- **一鍵專注**：點擊排程項目即可一鍵帶入番茄鐘進行深度專注工作。

### 4. 🍅 番茄鐘專注計時器 (Pomodoro Timer)
- **三大預設模式**：專注工作（25 分鐘）、短暫休息（5 分鐘）、深度休息（15 分鐘）。
- **動態倒數環**：流暢環狀進度條倒數與播放/暫停/重置控制。
- **歷史與統計**：任務完成即時納入今日番茄數統計，並記錄完成時間。

### 5. 🚨 即將到期動態警示 (Imminent Deadline Alerts)
- **呼吸發光虛線外框**：次日到期的任務卡片與月曆視圖同步觸發橘紅漸層呼吸發光動態。
- **跳動徽章**：伴隨 `⚠️ 明日到期！` 動態徽章，提醒使用者優先處理。

### 6. 🎨 專屬手繪風自適應圖示 (Adaptive App Icon)
- **符合 Android 官方規範**：手繪筆記本與鉛筆插圖，精準縮放置中於 72dp 安全可視區內。
- **全解析度支援**：完整提供 `mdpi`、`hdpi`、`xhdpi`、`xxhdpi`、`xxxhdpi` 各尺寸資源與傳統圓形圖示（Round Icon）。

---

## 📜 專案開發與操作規範 (Project Guidelines)

本專案遵循嚴格的代碼管理與操作體驗標準（詳見 [`AGENTS.md`](AGENTS.md)）：

### 1. 程式碼管理規範
1. **明確用途與職責**：每個類別及函式保持單一職責（Single Responsibility）。
2. **避免重複撰寫**：遵循 DRY 原則，重複邏輯抽離為共用模組或工具類。
3. **重要邏輯適當註解**：關鍵演算法、架構與業務規則加入繁體中文註解。
4. **命名明確具語意**：禁止無意義簡稱或單字母命名。
5. **保護既有模組相依性**：禁止隨意修改或刪除其他模組依賴的公開介面。
6. **功能驗證與穩定性保證**：修改完成後必須通過編譯檢查與單元測試。
7. **每次修改檔案皆須上傳 GitHub**：專案核心原則，異動後即刻同步推送。

### 2. 操作與 UX 規範
1. **主要功能易於尋找**：核心分區明確，首頁及底部導航直接呈現。
2. **新增按鈕保持明顯**：提供高識別度的懸浮按鈕（FAB）。
3. **刪除資料前二次確認**：所有刪除行為皆需二次彈窗確認。
4. **儲存成功即時提示**：新增或編輯成功後提供清晰的反饋訊息（`showFeedback`）。
5. **錯誤發生時清楚反饋**：異常或驗證未過時提供清楚訊息，避免無響應。
6. **各頁面統一底部導航**：使用全局一致的 `NavigationSuiteScaffold`。
7. **減少繁瑣點擊步驟**：提供常用時間預設、快速切換與一鍵勾選。

### 3. 功能操作規範
- **記事管理**：唯一識別碼、標題內文可空白、儲存更新時間、釘選置頂優先、草稿持久化保護。
- **待辦事項**：名稱不得空白、優先級分級（高/中/低）、完成狀態即時同步、截止日與提醒日分離。
- **分類搜尋**：唯一 ID、分類名不重複、刪除分類安全連動、標題與內文全欄位搜尋、依修改時間排序。
- **提醒通知**：格式檢核、過期不安排、通知總開關、任務完成取消排程、時間變更重新排程、Android 13+ 權限完備。
- **自訂主題**：支援淺色 / 深色 / 跟隨系統，全局頁面一致，DataStore Preferences 持久化。

### 4. 功能異動與變更管理規範
如果需要增加或修改功能，必須依序執行：
1. **確認修改目的**
2. **確認是否影響其他功能**
3. **更新相關程式碼**
4. **執行必要測試**（`./gradlew test` 與 `./gradlew build`）
5. **更新專案規格書及版本紀錄**（[`SPECIFICATION.md`](SPECIFICATION.md) 與 [`CHANGELOG.md`](CHANGELOG.md)）

---

## 🏗️ Architecture & Design Patterns / 架構與設計模式

```
[ UI Layer: Jetpack Compose + Navigation 3 + M3 Adaptive ]
                        │
                        ▼
   ┌────────────────────┼────────────────────┐
   ▼                    ▼                    ▼
[ TaskRepository ]  [ NoteRepository ]  [ DataStore Preferences ]
   │                    │               (Theme / Notification / Draft)
   ▼                    ▼
[ Room Database v4 (MIGRATION_3_4) & DAOs ]   [ Facade: LunarCalendarFacade ]
```

1. **外觀模式 (Facade Pattern)**：`LunarCalendarFacade` 將繁複的 200 年農曆二進位壓縮位元資料與節氣運算隱藏在背後，提供簡潔高層接口。
2. **倉儲模式 (Repository Pattern)**：`TaskRepository` 與 `NoteRepository` 分別負責雙軌待辦排程與記事分類管理，向 UI 層暴露響應式 Kotlin Flow。
3. **導航與適應性 (Navigation 3 & Adaptive Layout)**：採用最新版 `androidx.navigation3`，結合安全系統欄邊距（`statusBarsPadding`, `navigationBarsPadding`, `displayCutoutPadding`）實現手機與平板皆不遮擋的適應性佈局。

---

## 🛠️ Tech Stack / 技術棧

- **開發語言**：Kotlin 2.0+
- **UI 框架**：Jetpack Compose, Material Design 3, Compose Material Icons Extended
- **導航套件**：Jetpack Navigation 3 (`androidx.navigation3`)
- **自適應佈局**：Compose Material Adaptive (`NavigationSuiteScaffold`)
- **本機資料庫**：Room Persistence Library v4 (SQLite + `MIGRATION_3_4`)
- **背景與排程**：AlarmManager, BroadcastReceiver (`ReminderScheduler`, `ReminderReceiver`)
- **狀態持久化**：AndroidX DataStore Preferences（主題、通知開關、未保存草稿）
- **建置工具**：Gradle Kotlin DSL (`build.gradle.kts`), Version Catalog (`libs.versions.toml`)
- **CI/CD**：GitHub Actions (`.github/workflows/android.yml`)

---

## 📂 Project Structure / 專案結構

```text
com.example.notepad/
├── MainActivity.kt             # 應用程式進入點、全螢幕設定與動態主題對比度同步
├── calendar/                   # 離線農曆計算引擎與外觀模式
│   ├── LunarCalendarEngine.kt  # 核心位元演算法 (1900–2100)
│   ├── LunarCalendarFacade.kt  # Facade 服務介面
│   └── LunarDate.kt            # 農曆日期與節氣節慶模型
├── data/                       # 資料持久層
│   ├── local/                  # Room 資料庫、DAOs、Entities、轉換器
│   │   ├── dao/                # NoteDao, CategoryDao, ToDoDao, ReminderDao, TimeBlockDao, PomodoroDao
│   │   ├── entity/             # Note, Category, ToDo, Priority, Reminder, TimeBlock, PomodoroRecord
│   │   ├── DatabaseConverters.kt
│   │   └── AppDatabase.kt      # Room 主資料庫 (v4 + MIGRATION_3_4)
│   ├── preferences/            # DataStore 持久化
│   │   ├── ThemePreferences.kt
│   │   ├── NotificationPreferences.kt
│   │   └── NoteDraftPreferences.kt
│   └── repository/             # NoteRepository, TaskRepository
├── reminder/                   # 系統通知與鬧鐘排程
│   ├── ReminderReceiver.kt     # 鬧鐘廣播接收器（含總開關與權限檢核）
│   └── ReminderScheduler.kt    # 精確鬧鐘排定、取消與任務連動取消
└── ui/                         # 視覺介面層 (Jetpack Compose)
    ├── app/                    # Navigation 3 導航與系統欄防遮擋安全容器
    ├── components/             # 優先級徽章、設定彈窗、色彩標籤、呼吸邊框、多日彈窗、反饋工具
    ├── screens/                # NotesScreen, NoteEditorScreen, TaskScreen, CalendarScreen, ScheduleScreen, PomodoroScreen
    └── theme/                  # M3 主題設定、色彩調色盤與字型規範
```

---

## 📚 Documentation / 專案文件

- **[專案規格書 (SPECIFICATION.md)](SPECIFICATION.md)**：系統架構、Room v4 / DataStore 資料模型及詳細功能規格。
- **[版本紀錄 (CHANGELOG.md)](CHANGELOG.md)**：歷次功能擴充、錯誤修復與測試驗證紀錄。
- **[開發與操作規範 (AGENTS.md)](AGENTS.md)**：程式碼品質、UX 體驗、功能規範與變更管理工作流。

---

## 🚀 CI/CD & GitHub Integration / 持續整合

本專案配置 GitHub Actions 自動化工作流（`.github/workflows/android.yml`）：
- **JDK 17 Temurin**：自動化編譯環境與 Gradle 快取。
- **Android Lint 靜態檢查**：驗證 Composable 代碼品質與規範。
- **單元測試**：自動執行 `./gradlew test`，確保每次提交皆穩定可靠。

---

## 🏁 Getting Started & Building / 快速開始與建置

### 系統需求
- Android Studio Ladybug (2024.2.1) 或更新版本
- JDK 17
- Android SDK 30 ~ 37

### 建置指令
```bash
# 建置 Debug APK
./gradlew assembleDebug

# 執行單元測試
./gradlew testDebugUnitTest

# 執行完整專案檢查與建置
./gradlew build
```

---

## 📄 License / 授權條款

本專案採用 [MIT License](LICENSE) 開源授權條款。
