# Lunar To-Do & Notepad App (農曆萬年曆與雙軌待辦提醒記事)

[![Android CI](https://github.com/bingchiwang/Notepad/actions/workflows/android.yml/badge.svg)](https://github.com/bingchiwang/Notepad/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![Navigation 3](https://img.shields.io/badge/Navigation-3.0-orange.svg)](https://developer.android.com/guide/navigation)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-lightgrey.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-37-brightgreen.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 🌟 Overview / 專案概述

**Lunar To-Do & Notepad App** 是一款專為現代生活打造的高效生產力 Android 應用程式。本應用整合了**記事與分類全文搜尋**、**雙軌任務管理（待做事項優先級與多日提醒）**、**1900–2100 年完全離線農曆萬年曆**、**每日時間規劃表（Daily Schedule Planner）**、**番茄鐘專注計時器（Pomodoro Timer）**以及**本地優先（Offline-First）Firebase 雲端資料同步**。

全介面基於 **Jetpack Compose** 與 **Material Design 3** 打造，支援全面沉浸式體驗（Edge-to-Edge + 系統導覽列防遮擋）、動態色彩標籤、淺色／深色／跟隨系統主題切換、平板與摺疊機自適應佈局（Adaptive Layout）、以及 **Jetpack Navigation 3** 狀態驅動導航。

---

## 📋 Key Features / 核心功能特色

### 1. 📝 記事管理與草稿不遺失編輯器 (Notes, Categories & Full-Text Search)
- **彈性紀錄與釘選置頂**：每篇記事具唯一識別碼，標題與內容皆可空白；修改自動更新 `updatedAt`，支援一鍵釘選置頂排序。
- **草稿自動持久化**：透過 `NoteDraftPreferences` 即時保護未儲存內容，切換頁面或重開 App 皆不遺失草稿。
- **防重分類與全文搜尋**：分類具名稱防重檢查（大小寫無關），刪除分類時安全轉移或解除記事關聯；搜尋同時比對標題與內文並依修改時間降冪排列。

### 2. 📋 雙軌任務與自由多日提醒 (Dual-Track Tasks & Reminders)
- **待做事件 (To-Dos)**：支援高／中／低優先級徽章（`Priority`）、必填檢核、到期日設定、色彩標籤、進行中與已完成清單明確隔離。
- **自由多日提醒事項 (Multi-Day Reminders)**：
  - **打破天數限制**：內建跨月/跨年導覽與互動月曆網格，支援任選未來任意年份與月份的多個日期。
  - **精準時間排程**：自訂小時/分鐘與常用時間段快選，搭配 `NotificationPreferences` 全域通知總開關與 Android 13+ 權限引導。
  - **系統級推播通知**：整合 `AlarmManager.setExactAndAllowWhileIdle` 與 `BroadcastReceiver`；完成關聯待辦時自動取消未觸發通知排程。
  - **完整編輯與安全防護**：支援即時編輯提醒內容與日期時間，所有刪除前全面觸發二次確認對話框（`ConfirmDeleteDialog`）。

### 3. ☁️ Firebase 雲端雙向同步 (Offline-First Cloud Sync)
- **本地優先儲存**：新增、修改與刪除一律優先寫入本機 Room v5 / DataStore，確保離線零延遲使用體驗。
- **全資料與偏好雙向同步**：支援同步記事、分類、待辦事項、提醒、每日時間規劃、番茄鐘紀錄與主題／通知偏好設定；同時相容 `google-services.json` 自動初始化與介面手動設定 Firebase 參數（Project ID / App ID / Web API Key）。
- **斷線保留與恢復自動上傳**：離線期間完整維持 `PENDING_UPLOAD` 狀態與 `SyncTombstone` 刪除墓碑；網路恢復後透過 `AndroidNetworkMonitor` 自動上傳排隊變更。
- **智慧衝突解決與同步記錄**：成功同步即時記錄 `lastSyncAt` 最後同步時間；雙向衝突委派 `SyncConflictResolver` 依最新修改時間優先（`updatedAt` LWW）策略智慧合併。

### 4. 📅 離線農曆萬年曆 (Offline Lunar Perpetual Calendar)
- **完全離線換算**：支援 1900 年至 2100 年農曆演算法，無需聯網即時查詢。
- **豐富民俗資訊**：完整提供農曆月日、天干地支生肖紀年、二十四節氣與傳統民俗節慶。
- **月曆視覺化網格**：清楚標記當月所有日期、當日待辦事項連動、一鍵快速跳轉。

### 5. ⏰ 每日時間規劃表 (Daily Schedule Planner)
- **時間軸視覺化**：直觀呈現全天時間區塊（開始時間、結束時間、分類與備註）。
- **進度即時計算**：依打勾完成狀態自動計算並顯示當日進度百分比進度條。
- **一鍵專注**：點擊排程項目即可一鍵帶入番茄鐘進行深度專注工作。

### 6. 🍅 番茄鐘專注計時器 (Pomodoro Timer)
- **三大預設模式**：專注工作（25 分鐘）、短暫休息（5 分鐘）、深度休息（15 分鐘）。
- **動態倒數環**：流暢環狀進度條倒數與播放/暫停/重置控制。
- **歷史與統計**：任務完成即時納入今日番茄數統計，並記錄完成時間。

### 7. 🚨 即將到期動態警示 (Imminent Deadline Alerts)
- **呼吸發光虛線外框**：次日到期的任務卡片與月曆視圖同步觸發橘紅漸層呼吸發光動態。
- **跳動徽章**：伴隨 `⚠️ 明日到期！` 動態徽章，提醒使用者優先處理。

### 8. 🎨 專屬手繪風自適應圖示與個人化設定中心 (Adaptive Icon & Personalization)
- **符合 Android 官方規範**：手繪筆記本與鉛筆插圖，精準縮放置中於 72dp 安全可視區內，提供完整 `mdpi`–`xxxhdpi` 解析度資源。
- **個人化設定管理 (`AppSettingsDialog`)**：
  - **主題、按鍵顏色與月曆樣式自訂**：支援淺色／深色／跟隨系統模式、自訂按鍵色彩（12 款精選色票＋`#RRGGBB` 色碼輸入）與 6 款月曆樣式（`CLASSIC`、`INK_WASH`、`FOREST_ZEN`、`OCEAN_NIGHT`、`SUNSET_WARM`、`HIGH_CONTRAST`）。
  - **WCAG 自動防重色對比保護 (`ColorContrastUtils`)**：自動比對按鍵／月曆底色與文字、圖示、節慶標籤之對比度，若文字色接近或同色背景，即時翻轉為高對比深墨／純白文字並補齊邊框，維持清晰可讀。
  - **字體大小、通知開關與同步狀態**：支援 4 段全局字體縮放（85%–130%）、通知總開關與系統授權即時檢查，以及同步狀態檢視與一鍵雲端同步，全部設定並同步支援 DataStore 持久化與雲端跨裝置同步。

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
[ TaskRepository ]  [ NoteRepository ]  [ CloudSyncManager ]  [ DataStore Preferences ]
   │                    │                  │   ▲             (Theme/Notif/Draft/Sync)
   ▼                    ▼                  ▼   │
[ Room Database v5 (MIGRATION_4_5) & DAOs ]    [ FirebaseFirestoreDataSource ]
                        │                      [ SyncConflictResolver (updatedAt LWW) ]
                        ▼
             [ Facade: LunarCalendarFacade ]
```

1. **外觀模式 (Facade Pattern)**：`LunarCalendarFacade` 將繁複的 200 年農曆二進位壓縮位元資料與節氣運算隱藏在背後，提供簡潔高層接口。
2. **倉儲與離線優先同步模式 (Repository & Offline-First Sync)**：`TaskRepository` 與 `NoteRepository` 統一優先寫入本機 Room v5，異動自動透過 `CloudSyncManager`、`AndroidNetworkMonitor` 與 `SyncConflictResolver` 與 Firebase Firestore 雙向同步，離線期間保留 `PENDING_UPLOAD` 與 `SyncTombstone`。
3. **導航與適應性 (Navigation 3 & Adaptive Layout)**：採用最新版 `androidx.navigation3`，結合安全系統欄邊距（`statusBarsPadding`, `navigationBarsPadding`, `displayCutoutPadding`）實現手機與平板皆不遮擋的適應性佈局。

---

## 🛠️ Tech Stack / 技術棧

- **開發語言**：Kotlin 2.0+
- **UI 框架**：Jetpack Compose, Material Design 3, Compose Material Icons Extended
- **導航套件**：Jetpack Navigation 3 (`androidx.navigation3`)
- **自適應佈局**：Compose Material Adaptive (`NavigationSuiteScaffold`)
- **本機資料庫**：Room Persistence Library v5 (SQLite + `MIGRATION_3_4` + `MIGRATION_4_5`)
- **雲端同步後端**：Firebase BOM (`35.1.0`) + Cloud Firestore (`FirebaseFirestoreDataSource`) + `ConnectivityManager` 連線監測
- **背景與排程**：AlarmManager, BroadcastReceiver (`ReminderScheduler`, `ReminderReceiver`)
- **狀態持久化**：AndroidX DataStore Preferences（主題、通知開關、未保存草稿、雲端同步設定與 `lastSyncAt`）
- **建置工具**：Gradle Kotlin DSL (`build.gradle.kts`), Version Catalog (`libs.versions.toml`)
- **CI/CD**：GitHub Actions (`.github/workflows/android.yml`)

---

## 📂 Project Structure / 專案結構

```text
com.example.notepad/
├── MainActivity.kt             # 應用程式進入點、全螢幕設定、雲端同步生命週期初始化與動態主題同步
├── calendar/                   # 離線農曆計算引擎與外觀模式
│   ├── LunarCalendarEngine.kt  # 核心位元演算法 (1900–2100)
│   ├── LunarCalendarFacade.kt  # Facade 服務介面
│   └── LunarDate.kt            # 農曆日期與節氣節慶模型
├── data/                       # 資料持久層與雲端同步
│   ├── local/                  # Room 資料庫、DAOs、Entities、轉換器
│   │   ├── dao/                # NoteDao, CategoryDao, ToDoDao, ReminderDao, TimeBlockDao, PomodoroDao, SyncTombstoneDao
│   │   ├── entity/             # Note, Category, ToDo, Priority, Reminder, TimeBlock, PomodoroRecord, SyncTombstone, SyncMetadata
│   │   ├── DatabaseConverters.kt
│   │   └── AppDatabase.kt      # Room 主資料庫 (v5 + MIGRATION_3_4 + MIGRATION_4_5)
│   ├── preferences/            # DataStore 持久化
│   │   ├── ThemePreferences.kt
│   │   ├── NotificationPreferences.kt
│   │   ├── NoteDraftPreferences.kt
│   │   └── SyncPreferences.kt
│   ├── repository/             # NoteRepository, TaskRepository
│   └── sync/                   # Firebase 雲端雙向同步模組
│       ├── CloudRemoteDataSource.kt
│       ├── CloudSyncManager.kt
│       ├── CloudSyncModels.kt  # CloudSyncDocument & SyncConflictResolver
│       ├── FirebaseFirestoreDataSource.kt
│       └── NetworkMonitor.kt   # ConnectivityManager 連線與恢復觀察
├── reminder/                   # 系統通知與鬧鐘排程
│   ├── ReminderReceiver.kt     # 鬧鐘廣播接收器（含總開關與權限檢核）
│   └── ReminderScheduler.kt    # 精確鬧鐘排定、取消與任務連動取消
└── ui/                         # 視覺介面層 (Jetpack Compose)
    ├── app/                    # Navigation 3 導航與系統欄防遮擋安全容器
    ├── components/             # CloudSyncDialog、個人化設定管理、優先級徽章、色彩標籤、呼吸邊框、多日彈窗、反饋工具
    ├── screens/                # NotesScreen, NoteEditorScreen, TaskScreen, CalendarScreen, ScheduleScreen, PomodoroScreen
    └── theme/                  # M3 主題設定、ColorContrastUtils 自動防重色、CalendarThemeResolver 月曆樣式與字型縮放
```

---

## 📚 Documentation / 專案文件

- **[專案規格書 (SPECIFICATION.md)](SPECIFICATION.md)**：系統架構、Room v5 / DataStore / Firebase 雲端同步模型及詳細功能規格。
- **[版本紀錄 (CHANGELOG.md)](CHANGELOG.md)**：歷次功能擴充、錯誤修復與測試驗證紀錄。
- **[開發與操作規範 (AGENTS.md)](AGENTS.md)**：程式碼品質、UX 體驗、功能規範與變更管理工作流。
- **[專案開發計畫 (.agent/plan.md)](.agent/plan.md)**：分階段功能實作與驗收紀錄。

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
- Android SDK 26（Android 8.0） ~ 37

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
