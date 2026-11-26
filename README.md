# Lunar To-Do & Notepad App（農曆萬年曆與雙軌待辦提醒記事）

[![Android CI](https://github.com/bingchiwang/Notepad/actions/workflows/android.yml/badge.svg)](https://github.com/bingchiwang/Notepad/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Navigation 3](https://img.shields.io/badge/Navigation-3.0-orange.svg)](https://developer.android.com/guide/navigation)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-31%20(Android%2012)-teal.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-37-brightgreen.svg)](https://developer.android.com)
[![Unit Tests](https://img.shields.io/badge/Unit%20Tests-98%20Passed-success.svg)](app/src/test/java/com/example/notepad)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 📑 快速導覽目錄 (Table of Contents)

- [🌟 專案概述 (Overview)](#-專案概述-overview)
- [✨ 核心功能特色 (Key Features)](#-核心功能特色-key-features)
- [🏗️ 系統架構與設計模式 (Architecture)](#-系統架構與設計模式-architecture)
- [🛠️ 技術棧總覽 (Tech Stack)](#-技術棧總覽-tech-stack)
- [📂 專案目錄結構 (Project Structure)](#-專案目錄結構-project-structure)
- [🧪 測試覆蓋與品質保證 (Testing & QA)](#-測試覆蓋與品質保證-testing--qa)
- [📜 專案開發與操作規範摘要 (Guidelines)](#-專案開發與操作規範摘要-guidelines)
- [📚 專案文件導覽 (Documentation)](#-專案文件導覽-documentation)
- [🏁 快速開始與建置指令 (Getting Started)](#-快速開始與建置指令-getting-started)

---

## 🌟 專案概述 (Overview)

**Lunar To-Do & Notepad App** 是一款專為日常高效生產力設計的現代化 Android 應用程式（支援 **Android 12 API 31** 以上裝置）。本應用將六大核心工作流整合於單一清爽介面中：

1. **📝 記事管理與分類全文搜尋**：支援草稿自動保存、釘選置頂、防重複分類管理與標題／內文即時搜尋。
2. **📋 雙軌待辦事項與多日提醒**：將「待做任務（含高／中／低優先級與截止日）」與「自由跨年月多日提醒（精確系統鬧鐘推播）」分軌管理並支援完成連動取消。
3. **📅 1900–2100 離線農曆萬年曆**：純離線位元演算法推算農曆月日、天干地支生肖、二十四節氣與傳統／國曆節慶，支援 6 款自訂月曆視覺風格。
4. **⏰ 每日時間規劃表**：直觀管理每日時段區塊、即時計算完成進度百分比，並可一鍵帶入番茄鐘。
5. **🍅 番茄鐘專注計時器**：內建 25 / 5 / 15 分鐘三段式專注循環、待辦目標綁定與每日完成統計。
6. **☁️ 本地優先 Google 雲端硬碟同步與個人化設定中心**：離線優先寫入 Room v5，連網自動與 Google 雲端硬碟 `appDataFolder` 雙向智慧合併（LWW）；內建 WCAG 自動防重色對比引擎、自訂按鍵色、月曆樣式與 4 段全局字體大小調整。
7. **📲 四大生產力桌面小工具（快速記事／待辦事項／今日行程／月曆）**：於手機桌面直接瀏覽與操作最近記事（分類切換、一鍵新增／編輯）、未完成待辦（直接勾選完成、截止日、高中低優先級）、今日行程概覽（今天到期待辦、今日完成進度條、即將到來的提醒）與完整月份月曆（藍色標示今日、格內直接顯示記事與待辦文字、超過 2 筆顯示 `＋N 項`、切換月份與快速新增）。

---

## ✨ 核心功能特色 (Key Features)

### 1. 📝 記事管理與草稿不遺失編輯器 (`NotesScreen` / `NoteEditorScreen`)
- **無阻礙快速記事**：每篇記事具備唯一 `id` 與跨裝置 `syncId`，標題與內文均允許留白；儲存或編輯時自動更新 `updatedAt` 時間戳。
- **釘選置頂排序**：支援一鍵釘選／取消釘選，釘選記事固定置於列表最前方（`isPinned DESC, updatedAt DESC`）。
- **草稿即時持久化**：透過 `NoteDraftPreferences`（DataStore）於輸入期間即時備份未保存內容；切換分頁或系統回收後返回，皆可由列表頂部橫幅一鍵恢復編輯。
- **內嵌核取方塊清單與一鍵轉換**：編輯器支援 Markdown 核取清單（`- [ ]`／`- [x]`）即時勾選、換行自動延續，以及選取文字或清單項目一鍵「轉為待辦事項（ToDo）」或「排入今日時間規劃（TimeBlock）」。
- **防重分類與全文檢索**：分類具資料庫唯一索引與不分大小寫防重檢查；刪除分類時可選擇將所屬記事移至「未分類」或轉移至其他分類。全文搜尋同時比對標題與內文，查無結果時提供清晰 Empty State 與一鍵重置。

### 2. 📋 雙軌任務與自由多日提醒 (`TaskScreen` / `TaskDetailScreen` / `ReminderScheduler`)
- **待辦事項（To-Dos）**：
  - 強制檢核任務名稱不得為空白，支援 **高（`HIGH`）／中（`MEDIUM`）／低（`LOW`）** 三段優先級選擇與高對比色彩徽章。
  - 「進行中」與「已完成」清單嚴格分離，已完成任務絕不混入未完成視圖。
  - **自動化重複任務（Recurring Tasks）**：支援每天、每週（一～日）與每月（1～31 日含月底自動截斷不偏移）例行規則設定；於主應用或桌面小工具勾選完成當期任務時，自動產生下一期待辦並防止重複衍生。
  - 任務截止日期（`dueDate`）與提醒通知時間（`Reminder.triggerAt`）分離管理，可獨立排定或透過 `linkedToDoId` 綁定。
  - **明日到期動態警示**：次日到期且未完成的任務，於任務列表與萬年曆同步觸發橘紅呼吸虛線外框（`breathingDashedBorder`）與彈跳「`⚠️ 明日到期！`」徽章（`BouncingBadge`）。
- **提醒通知（Reminders）**：
  - **跨年月自由多選**：內建 `MonthNavigatorHeader` 與 `MonthCalendarGrid`，可跨任意年份與月份勾選多天提醒並設定時／分。
  - **智慧排程與連動取消**：嚴格攔截過去時間（不安排過期通知）；支援全域「通知總開關」（`NotificationPreferences`）；當關聯待辦勾選完成時，自動呼叫 `AlarmManager.cancel` 取消尚未觸發的提醒通知。

### 3. 🎨 摺疊式分類設定中心與 WCAG 防重色引擎 (`AppSettingsDialog` / `ColorContrastUtils`)
- **五大摺疊分類選單（點擊展開設定）**：將設定歸納為「主題與按鍵外觀」、「月曆與桌面小工具」、「全局字體大小」、「提醒與系統通知」、「Google 雲端硬碟同步」五大分類，預設收合並顯示摘要，點擊分類後展開對應設定。
- **主題與自訂按鍵配色**：支援淺色、深色、跟隨系統三模式；提供 12 款精選按鍵色票與 `#RRGGBB` 色碼自訂輸入。
- **6 款月曆視覺樣式（`CalendarStyle`）**：內建「經典簡約」、「水墨東方」、「青翠森境」、「湛藍晴空」、「暖陽暮靄」、「極簡高對比」6 種風格，並可自訂選日強調色。
- **WCAG 2.1 自動文字對比保護**：當使用者選擇與文字顏色相近或一致的按鍵／月曆底色（如純白、亮黃、珊瑚紅或曜石黑）時，`ColorContrastUtils` 與 `CalendarThemeResolver` 會即時計算相對明度與對比度（$\ge 4.5:1$），自動將文字、圖示、節慶紅字或節氣綠字切換為高對比深墨色（`#121316`）或純白色（`#FFFFFF`），並為近底色按鍵補上細邊框。
- **4 段全局字體大小（`AppFontScale`）**：支援 `85%`（精巧小）、`100%`（標準中）、`115%`（舒適大）、`130%`（特大清晰）即時縮放。
- **通知開關與同步狀態儀表板**：於同一設定視窗內整合通知權限狀態（支援 `ON_RESUME` 自動刷新）與雲端同步儀表板。

### 4. ☁️ 本地優先 Google 雲端硬碟雙向同步 (`CloudSyncManager` / `GoogleDriveRemoteDataSource`)
- **Offline-First 本地優先**：所有新增、修改、刪除操作皆優先寫入本機 Room v5 / DataStore，離線操作零延遲。
- **Google 帳號一鍵授權與全類型同步**：透過 Google Play Services 申請 `drive.appdata` 隱藏空間權限，自動於 Google 雲端硬碟保存與合併記事、分類、待辦、提醒、時間規劃、番茄鐘紀錄與個人化／通知設定；亦於進階設定保留切換至 Firebase Firestore 的彈性。
- **離線墓碑與斷線自動補傳**：離線刪除自動寫入 `SyncTombstone`（`sync_tombstones` 表），待網路恢復時由 `AndroidNetworkMonitor` 自動觸發背景同步並記錄 `lastSyncAt` 最後同步時間。
- **智慧衝突解決（`SyncConflictResolver`）**：雙端異動衝突時採最新修改時間優先（Last-Write-Wins, `updatedAt`）智慧合併。

### 5. 📅 離線農曆萬年曆、⏰ 時間規畫表（含內嵌 🍅 番茄鐘）
- **離線農曆萬年曆（`CalendarScreen`）**：離線推算 1900–2100 年農曆日期、干支生肖年、二十四節氣與傳統節慶，同頁檢視並管理當日記事、待辦與提醒。
- **時間規畫表與番茄鐘整合（`ScheduleScreen` + `PomodoroScreen`）**：底部導覽簡化為四大核心入口（`記事`、`待辦`、`月曆`、`時間規畫表`），於「時間規畫表」內建雙分頁切換「`⏰ 時間規畫`」（依日期檢視時段行程、即時計算完成率進度條）與「`🍅 番茄鐘`」（25m／5m／15m 環狀動畫倒數、目標任務綁定與今日專注統計）。

### 6. 📲 快速記事桌面小工具 (`QuickNoteWidgetProvider` / `QuickNoteWidgetConfigureActivity`)
- **最近記事即時預覽**：於手機桌面直接瀏覽釘選與最新修改記事，顯示標題、摘要預覽、色條識別、分類標籤與修改時間，支援淺／深色桌面主題與雙向調整大小。
- **一鍵新增與直達編輯**：點擊「+ 新增」或空狀態提示即可透過深度連結直接開啟記事編輯頁（若為特定分類小工具則自動預選該分類）；點擊清單中任一記事可直接開啟該篇編輯畫面。
- **彈性篩選指定分類**：放置或點擊頂部分類標籤時可隨時切換顯示「全部記事」、「未分類」或任一既有分類，本機異動與雲端同步拉回後自動更新桌面清單。

### 7. ✅ 待辦事項桌面小工具 (`ToDoWidgetProvider` / `ToDoWidgetService` / `ToDoWidgetLogic`)
- **僅顯示未完成任務**：嚴格隔離已完成項目，依高＞中＞低優先級與截止日期由近到遠排序，頂部即時顯示待辦數量。
- **直接勾選完成與提醒連動**：直接在桌面點擊任務左側核取方塊即可完成待辦、標記雲端待同步並自動取消尚未觸發的關聯提醒鬧鐘。
- **截止日緊迫度與高／中／低優先級標示**：明確區分「已逾期」、「今天到期」、「明天到期」、「截止日期」與「未設截止日」，並透過左側色條與色彩徽章清楚標示高、中、低優先級。
- **快速新增與詳情導航**：支援一鍵從桌面彈出新增待辦視窗或點擊任務列直達 `TaskDetailScreen`。

### 8. 🗓️ 今日行程桌面小工具 (`TodayScheduleWidgetProvider` / `TodayScheduleWidgetService` / `TodayScheduleWidgetLogic`)
- **今天到期的待辦**：篩選截止日為今天的未完成待辦並標示高／中／低優先級，支援直接在桌面勾選完成並即時消除關聯鬧鐘提醒。
- **今日完成進度進度條**：彙整今日所有每日時間規劃與今天到期之待辦事項，顯示即時進度百分比條與完成統計。
- **即將到來的提醒**：列出尚未觸發且未過期的最近提醒，標示「今天」、「明天」或跨日時間並可一鍵直達提醒管理頁面。

### 9. 📆 月曆桌面小工具 (`CalendarWidgetProvider` / `CalendarWidgetConfigureActivity` / `CalendarWidgetLogic`)
- **動態週數、每週橫線區隔與可調日期字體大小**：顯示 `18sp` 放大年月標題、週日紅字星期列與依當月實際週數（4～6 週）動態增減之週次列（每週以橫線區隔）；支援調整 **小／標準／大／特大** 四段日期字體大小（可於 App 設定中心、長按小工具設定或點擊小工具左上角圖示即時調整），並支援一鍵切換台灣農曆、節慶與二十四節氣。
- **格內直接顯示圖示＋文字與 `＋N 項` 摺疊（不使用小圓點）**：有記事或待辦事項的日期直接在格子內顯示附帶專屬小圖示的單行省略文字，並以暖琥珀色與湛藍色底區分記事與待辦；每格最多顯示 2 筆，超過 2 筆時自動顯示「`＋3 項`」等剩餘筆數標籤。
- **五維度直覺點擊互動與擬真選單預覽**：點擊日期格開啟該日記事與待辦清單、點擊記事進入編輯頁、點擊待辦查看或修改詳情、點擊左右箭頭切換月份、點擊「`+ 新增`」快速選擇建立該日記事或待辦；四大桌面小工具均配備 Android 12+ 擬真即時預覽版面（`previewLayout`）與迷你卡片選單圖示，於「新增小工具」選單即所見即所得。

---

## 🏗️ 系統架構與設計模式 (Architecture)

```text
┌───────────────────────────────────────────────────────────────────────────┐
│              UI Layer (Jetpack Compose + Material 3 Adaptive)             │
│   AppNavigation (Navigation 3) + 5 Bottom Nav Tabs + AppSettingsDialog    │
│   Theme.kt + ColorContrastUtils (WCAG) + CalendarThemeResolver            │
└───────────────┬───────────────────────────────┬───────────────────────────┘
                │                               │
                ▼                               ▼
┌───────────────────────────────┐ ┌─────────────────────────────────────────┐
│     Domain & Facade Layer     │ │     Sync & Preferences Coordinator      │
│  - LunarCalendarFacade        │ │  - CloudSyncManager + NetworkMonitor    │
│  - LunarCalendarEngine        │ │  - SyncConflictResolver (updatedAt LWW) │
│  - ReminderScheduler          │ │  - Theme / Notif / Draft / Sync Prefs   │
└───────────────┬───────────────┘ └─────────────┬───────────────────────────┘
                │                               │
                ▼                               ▼
┌───────────────────────────────────────────────┐ ┌─────────────────────────┐
│    Local Persistence (Room Database v5)       │ │   Remote Cloud Store    │
│  notes | categories | todos | reminders       │ │  Firebase Firestore     │
│  time_blocks | pomodoro_records | tombstones  │ │  (Offline-First Sync)   │
└───────────────────────────────────────────────┘ └─────────────────────────┘
```

---

## 🛠️ 技術棧總覽 (Tech Stack)

| 領域 | 採用技術與版本 | 用途說明 |
| :--- | :--- | :--- |
| **開發語言** | Kotlin `2.0+` | Coroutines、Flow 非同步資料流與序列化 |
| **系統相容版本** | `minSdk = 26` / `targetSdk = 37` | 完整支援 Android 8.0 (Oreo) 至最新 Android 版本 |
| **UI 框架** | Jetpack Compose + Material 3 | 宣告式 UI、動態主題、自訂 Canvas 呼吸邊框與深淺色適應 |
| **自適應導覽** | Jetpack Navigation 3 + M3 Adaptive | `NavigationSuiteScaffold` 全局一致底部導覽與平板雙欄支援 |
| **本機資料庫** | Room Persistence Library `2.8.4` (v5) | 7 張核心資料表，含 `MIGRATION_3_4` 與 `MIGRATION_4_5` 無損升級 |
| **偏好設定** | AndroidX DataStore Preferences `1.1.7` | 持久化儲存個人化主題、通知總開關、記事草稿與雲端同步設定 |
| **雲端同步** | Firebase BOM `35.1.0` + Firestore | 本地優先雙向雲端同步、離線墓碑追蹤與網路恢復自動上傳 |
| **系統提醒排程** | `AlarmManager` + `BroadcastReceiver` | 精確喚醒鬧鐘排程、通知頻道管理與待辦完成連動取消 |
| **持續整合** | GitHub Actions (`.github/workflows/android.yml`) | 自動執行 JDK 17 編譯、Android Lint 靜態分析與單元測試 |

---

## 📂 專案目錄結構 (Project Structure)

```text
com.example.notepad/
├── MainActivity.kt                         # 應用程式入口、Edge-to-Edge、資料庫/Repository/雲端同步初始化
├── calendar/                               # 離線農曆萬年曆模組 (Facade Pattern)
│   ├── LunarCalendarEngine.kt              # 1900–2100 年農曆大小月位元遮罩與二十四節氣核心演算法
│   ├── LunarCalendarFacade.kt              # 封裝農曆、節氣、節慶查詢與午夜時戳正規化介面
│   └── LunarDate.kt                        # 農曆年月日、天干地支生肖與中文格式化資料模型
├── data/                                   # 資料持久層、偏好設定與雲端同步層
│   ├── local/
│   │   ├── AppDatabase.kt                  # Room v5 資料庫定義與 MIGRATION_3_4 / MIGRATION_4_5
│   │   ├── DatabaseConverters.kt           # Priority、SyncStatus、SyncEntityType 型別轉換器
│   │   ├── dao/                            # NoteDao, CategoryDao, ToDoDao, ReminderDao, TimeBlockDao, PomodoroDao, SyncTombstoneDao
│   │   └── entity/                         # Note, Category, ToDo, Priority, Reminder, TimeBlock, PomodoroRecord, SyncTombstone, SyncMetadata
│   ├── preferences/                        # DataStore 持久化封裝
│   │   ├── ThemePreferences.kt             # 主題模式、按鍵色、月曆樣式、字體大小 (PersonalizationSettings)
│   │   ├── NotificationPreferences.kt      # 全域通知總開關持久化
│   │   ├── NoteDraftPreferences.kt         # 未保存記事草稿即時持久化
│   │   └── SyncPreferences.kt              # 雲端同步開關、帳號識別碼、Firebase 參數與 lastSyncAt
│   ├── repository/                         # 倉儲層 (Repository Pattern)
│   │   ├── NoteRepository.kt               # 記事與分類 CRUD、防重檢核、全文搜尋與離線墓碑寫入
│   │   └── TaskRepository.kt               # 待辦、提醒、時間區塊與番茄鐘 CRUD 及離線墓碑寫入
│   └── sync/                               # 本地優先雲端雙向同步模組
│       ├── CloudRemoteDataSource.kt        # 雲端遠端資料源抽象介面
│       ├── FirebaseFirestoreDataSource.kt  # Firebase Cloud Firestore 實作（支援自動與手動設定）
│       ├── CloudSyncManager.kt             # 雙向同步協調器、離線恢復自動上傳與設定同步
│       ├── CloudSyncModels.kt              # 同步文件模型與 SyncConflictResolver (updatedAt LWW)
│       └── NetworkMonitor.kt               # ConnectivityManager 網路狀態監聽器
├── reminder/                               # 提醒推播與鬧鐘排程模組
│   ├── ReminderScheduler.kt                # 精確鬧鐘排定、過期攔截、總開關連動與開機更新重排協調器
│   ├── ReminderReceiver.kt                 # 鬧鐘廣播接收器與高優先級 NotificationChannel 推播
│   └── BootCompletedReceiver.kt            # 開機完成與 App 更新後自動撈取 Room 提醒並重新排程之廣播接收器
├── widget/                                 # 快速記事與待辦事項桌面小工具模組
│   ├── QuickNoteWidgetContract.kt          # 小工具（記事與待辦）深度連結動作與 Intent 解析契約
│   ├── QuickNoteWidgetLogic.kt             # 記事小工具分類篩選編解碼、排序與摘要格式化邏輯
│   ├── QuickNoteWidgetPreferences.kt       # 依 appWidgetId 保存記事小工具分類篩選設定
│   ├── QuickNoteWidgetProvider.kt          # 記事小工具 AppWidgetProvider 與即時重整協調器
│   ├── QuickNoteWidgetService.kt           # 記事小工具 RemoteViewsService 與清單工廠
│   ├── QuickNoteWidgetConfigureActivity.kt # 記事小工具分類選擇與重新配置 Compose 介面
│   ├── ToDoWidgetLogic.kt                  # 待辦小工具未完成過濾、優先級排序與截止日緊迫度邏輯
│   ├── ToDoWidgetRowBinder.kt              # 待辦列元件 RemoteViews 共用繫結器（DRY）
│   ├── ToDoWidgetService.kt                # Android 8.0–11 待辦清單 RemoteViewsService
│   ├── ToDoWidgetProvider.kt               # 待辦小工具提供者（直接勾選完成／提醒取消）與重整協調器
│   ├── TodayScheduleWidgetLogic.kt         # 今日行程進度統計、今日到期篩選與提醒排序邏輯
│   ├── TodayScheduleWidgetRowBinder.kt     # 今日行程多型態清單列（分區標題／待辦／提醒）繫結器
│   ├── TodayScheduleWidgetService.kt       # Android 8.0–11 今日行程 RemoteViewsService
│   ├── TodayScheduleWidgetProvider.kt      # 今日行程小工具提供者與即時重整協調器
│   ├── CalendarWidgetLogic.kt              # 月曆小工具 42 格月份排程、記事/待辦排序與 ＋N 項截斷邏輯
│   ├── CalendarWidgetPreferences.kt        # 依 appWidgetId 保存月曆小工具顯示年月與農曆開關
│   ├── CalendarWidgetProvider.kt           # 月曆小工具提供者（月份／農曆切換、格內事件直達）與重整協調器
│   ├── CalendarWidgetConfigureActivity.kt  # 月曆小工具農曆日期顯示配置與即時預覽 Compose 介面
│   └── WidgetTaskActions.kt                # 跨小工具共用待辦完成與雙向刷新協調器（DRY）
└── ui/                                     # Jetpack Compose 視覺介面層
    ├── app/                                # Navigation 3 多堆疊導覽狀態與系統欄防遮擋容器
    │   ├── AppNavigation.kt
    │   ├── NavigationKeys.kt
    │   ├── NavigationState.kt
    │   └── Navigator.kt
    ├── components/                         # 共用 UI 元件與對話框
    │   ├── AppSettingsDialog.kt            # 個人化設定管理中心（主題/按鍵色/月曆樣式/字體/通知/同步）
    │   ├── CloudSyncDialog.kt              # Google 雲端硬碟帳號連結與雙向同步對話框
    │   ├── ItemDialogs.kt                  # 待辦、多日提醒、時間區塊新增/編輯對話框與共用月曆選日器
    │   └── NotepadComponents.kt            # 色彩標籤、優先級徽章、呼吸邊框、二次確認對話框、AppButtonThemeDefaults
    ├── screens/                            # 五大核心畫面與子詳情頁
    │   ├── NotesScreen.kt                  # 記事列表、全文搜尋列、分類篩選與管理、草稿恢復橫幅
    │   ├── NoteEditorScreen.kt             # 記事新增/編輯器（支援空白標題內文与草稿即時保存）
    │   ├── TaskScreen.kt                   # 雙軌待辦/提醒主畫面與 TaskDetailScreen 待辦詳情頁
    │   ├── CalendarScreen.kt               # 農曆萬年曆畫面（套用 6 款自訂月曆樣式與當日任務管理）
    │   ├── ScheduleScreen.kt               # 時間規畫表畫面（含雙分頁切換每日時間規畫與內嵌番茄鐘）
    │   └── PomodoroScreen.kt               # 番茄鐘專注計時子分頁畫面（內嵌於時間規畫表）
    └── theme/                              # 主題與 WCAG 對比度引擎
        ├── Theme.kt                        # NotepadTheme、自訂按鍵色彩與全局字體縮放注入
        ├── ColorContrastUtils.kt           # WCAG 2.1 相對明度、對比度計算與自動深淺字色切換
        ├── CalendarThemeResolver.kt        # 6 款月曆樣式調色盤與節慶/節氣文字防重色解析器
        ├── Color.kt
        └── Type.kt
```

---

## 🧪 測試覆蓋與品質保證 (Testing & QA)

本專案包含 **20 個單元測試套件、共 98 項自動化測試案例**，涵蓋所有核心演算法、自動化重複任務計算、記事核取方塊解析、資料倉儲、偏好設定、雲端同步衝突解決、Google Drive API 序列化與合併、WCAG 色彩對比計算與桌面小工具邏輯：

| 測試套件類別 | 測試數量 | 驗證範圍說明 |
| :--- | :---: | :--- |
| `LunarCalendarFacadeTest` | 7 | 農曆全年換算、2024–2028 全部 120 個 VSOP87D 天文二十四節氣、閏月初一標籤、2060 位元表修正、台灣節慶與明日到期判定 |
| `PriorityAndConvertersTest` | 5 | 優先級權重排序與 Room `DatabaseConverters`（含 `RecurrenceType`）雙向轉換 |
| `RecurringTaskLogicTest` | 5 | 每天／每週／每月（含月底 31 日保留不偏移）下期計算、標籤格式與防重生成保護 |
| `NoteRepositoryTest` | 9 | 記事空白標題/內文保存、`updatedAt` 更新、釘選排序、分類防重、刪除分類記事歸屬、全文搜尋與離線刪除墓碑 |
| `NoteChecklistLogicTest` | 5 | 記事內嵌 Markdown 核取清單（`- [ ]` / `- [x]`）解析、切換、游標插入、換行延續與選字轉待辦標題萃取 |
| `TaskRepositoryTest` | 5 | 待辦空白名稱攔截、進行中/已完成分離、優先級排序、重複任務完成自動建下期與防重複衍生、離線刪除墓碑 |
| `ReminderSchedulerLogicTest` | 5 | 過期時間攔截、未來時間允許、通知總開關關閉攔截與開機／更新後未過期提醒重排篩選測試 |
| `ThemePreferencesTest` | 6 | 淺色/深色/系統模式、自訂按鍵色、月曆樣式、字體大小持久化與一鍵重設預設值 |
| `NotificationPreferencesTest` | 1 | 全域通知總開關預設值與切換持久化驗證 |
| `NoteDraftPreferencesTest` | 1 | 未保存記事草稿寫入、跨頁面讀取還原與儲存後清除驗證 |
| `SyncConflictResolverTest` | 8 | 本機與雲端雙向新增、LWW 時間戳比對、本機/雲端刪除墓碑衝突與離線編輯復活決策 |
| `CloudSyncManagerTest` | 4 | 本地優先上傳、雲端拉取合併、離線墓碑排隊與網路恢復自動上傳、個人化設定雲端同步 |
| `GoogleDriveSyncPayloadSerializerTest` | 2 | Google 雲端硬碟 JSON 快照之 64 位元整數、空值與各實體／墓碑來回序列化與異常防護 |
| `GoogleDriveRemoteDataSourceTest` | 3 | Google Drive `appDataFolder` 建立與增量更新保留既有文件、HTTP 401 提示與路由分派 |
| `ColorContrastUtilsTest` | 5 | WCAG 相對明度與對比度、淺底深字/深底白字自動切換、同色邊框判定、Hex 色碼解析、6 款月曆樣式防重色與週日紅字保持 |
| `QuickNoteWidgetLogicTest` | 7 | 記事小工具分類編解碼、釘選與更新時間排序、分類刪除退回、空白標題／多行摘要格式化與深度連結解析 |
| `ToDoWidgetLogicTest` | 5 | 待辦小工具未完成過濾、優先級與截止日排序、徽章標籤格式、逾期／今日／明日判別與深度連結解析 |
| `TodayScheduleWidgetLogicTest` | 5 | 今日行程小工具今日到期待辦、綜合完成進度計算、即將到來提醒排序與格式及深度連結解析 |
| `CalendarWidgetLogicTest` | 9 | 月曆小工具跨年月份切換、4／5／6 週動態週數與週日標記、四段日期字體大小解析與傳遞、記事與待辦歸類、超過 2 筆顯示 `＋3 項`、台灣節慶農曆切換與深度連結解析 |
| `ExampleUnitTest` | 1 | 基礎 JUnit 測試環境驗證 |

---

## 📜 專案開發與操作規範摘要 (Guidelines)

本專案嚴格遵循 [`AGENTS.md`](AGENTS.md) 定義之四大規範體系：
1. **程式碼管理規範**：單一職責、DRY 共用封裝、關鍵邏輯繁體中文註解、具語意命名、向下相容保護、修改後必跑 `./gradlew test` & `./gradlew build`，且每次修改皆上傳 GitHub。
2. **操作與 UX 規範**：主要功能直覺易找、明顯新增按鈕（FAB）、所有刪除前必經 `ConfirmDeleteDialog` 二次確認、儲存成功與錯誤皆有即時清晰反饋、全局統一 `NavigationSuiteScaffold` 底部導覽、提供快捷預設減少點擊步驟。
3. **功能操作規範**：完整落實記事管理、待辦事項、分類搜尋、提醒通知、個人化主題與防重色、本地優先 Firebase 雲端同步及 Android 8.0（API 26）向下相容規範。
4. **變更管理五步驟**：確認修改目的 → 確認是否影響其他功能 → 更新相關程式碼 → 執行必要測試 → 更新規格書及版本紀錄並推送 GitHub。

---

## 📚 專案文件導覽 (Documentation)

- **[開發與操作規範 (`AGENTS.md`)](AGENTS.md)**：程式碼管理、操作體驗、功能操作規範與變更管理標準流程。
- **[專案技術規格書 (`SPECIFICATION.md`)](SPECIFICATION.md)**：Room v5 資料表結構、DataStore 模型、WCAG 對比度公式、雲端同步協議與測試規格。
- **[版本變更紀錄 (`CHANGELOG.md`)](CHANGELOG.md)**：從 `v1.0.0` 至最新版本的完整功能演進、架構升級與修復紀錄。
- **[專案實作計畫 (`.agent/plan.md`)](.agent/plan.md)**：各開發階段（`Task_1` ~ `Task_16`）之實作細節與驗收標準。

---

## 🏁 快速開始與建置指令 (Getting Started)

### 系統環境需求
- **IDE**：Android Studio Ladybug (2024.2.1) 或更新版本
- **JDK**：JDK 17（Android Studio 內建 JBR 21 亦完全相容）
- **Android SDK**：`minSdk = 31`（Android 12）~ `compileSdk = 37`

### 常用 Gradle 指令
```bash
# 1. 執行全部 98 項單元測試
./gradlew testDebugUnitTest

# 2. 建置 Debug APK
./gradlew assembleDebug

# 3. 執行完整專案建置（含 Debug/Release 編譯、R8 壓縮與 Android Lint 檢查）
./gradlew build
```

---

## 📄 授權條款 (License)

本專案採用 [MIT License](LICENSE) 開源授權條款。
