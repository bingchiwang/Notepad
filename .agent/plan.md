# 專案開發計劃 (Project Plan)

## 專案簡介：農曆萬年曆與雙軌待辦提醒記事 App (Lunar To-Do & Notepad App)

### 專案願景與核心目標 (Project Vision & Goals)
- **記事與分類全文搜尋**：提供支援草稿自動保存、釘選置頂、分類防重管理與標題／內文即時搜尋的記事系統。
- **雙軌任務與提醒管理**：獨立劃分 📋 **待做事件**（高／中／低優先級、截止日設定、進行中與已完成分離）與 🔔 **提醒事項**（跨年月自由多選提醒日期、精確系統通知、任務完成自動取消關聯提醒）。
- **離線農曆萬年曆**：完整離線演算 1900–2100 年農曆日、天干地支生肖、二十四節氣與傳統節慶，支援 6 款自訂月曆樣式。
- **即將到期特殊邊框警示**：針對明日到期的待辦事項，於卡片與月曆同步觸發橘紅呼吸動態發光虛線外框與 `⚠️ 明日到期！` 跳動徽章。
- **個人化設定與 WCAG 防重色保護**：支援淺色／深色／系統主題、自訂按鍵顏色、月曆樣式、4 段字體大小、通知開關與同步狀態管理；當文字與背景顏色相近或一致時自動切換高對比文字顏色。
- **本地優先 Firebase 雲端資料同步**：離線優先寫入本機 Room v5，連網時自動與 Firebase Cloud Firestore 雙向同步並採最新修改時間優先（LWW）智慧合併。
- **廣泛系統相容與自動化交付**：向下相容 **Android 8.0（API 26）**，採用 Facade 與 Repository 模式，每次修改檔案後通過測試並同步推送至 GitHub。

---

## 核心功能模組 (Core Feature Modules)
1. **記事管理與分類搜尋 (`NotesScreen` / `NoteEditorScreen`)**：唯一識別碼、空白標題／內文支援、釘選置頂、DataStore 草稿防遺失、分類防重與刪除轉移、標題與內文全文檢索。
2. **雙軌任務與多日提醒 (`TaskScreen` / `TaskDetailScreen` / `ReminderScheduler`)**：待辦非空驗證、高／中／低優先級、進行中與已完成分離、截止日與提醒日分離管理、完成任務連動取消未觸發提醒、跨年月多日提醒排程。
3. **離線農曆萬年曆 (`LunarCalendarFacade` / `CalendarScreen`)**：1900–2100 年農曆日期、干支生肖、二十四節氣與中西節慶離線換算，連動當日待辦與提醒。
4. **即將到期 UI 警示 (`breathingDashedBorder` / `BouncingBadge`)**：針對次日到期未完成項目觸發橘紅呼吸發光虛線外框與彈跳徽章。
5. **每日時間規劃表 (`ScheduleScreen`)**：時間軸視覺化日程安排、分類色彩標記、完成打勾與當日完成率進度條、一鍵連動番茄鐘。
6. **番茄鐘專注計時器 (`PomodoroScreen`)**：支援專注工作 (25m)、短暫休息 (5m)、深度休息 (15m)，搭配環狀動態倒數、目標任務綁定、今日番茄數統計與歷史紀錄。
7. **Firebase 雲端雙向同步 (`CloudSyncManager` / `FirebaseFirestoreDataSource`)**：Offline-First 本地優先儲存、離線刪除墓碑（`SyncTombstone`）、網路恢復自動上傳、`lastSyncAt` 記錄與 `SyncConflictResolver` 最新修改優先合併。
8. **個人化設定管理中心 (`AppSettingsDialog` / `ColorContrastUtils` / `CalendarThemeResolver`)**：自訂按鍵顏色、6 款月曆樣式、WCAG 自動文字對比防重色、4 段字體大小、通知開關與同步狀態儀表板。

---

## 核心技術棧 (High-Level Tech Stack)
* **開發語言 (Language)**：Kotlin 2.0+ (Coroutines / Flow)
* **系統版本支援 (SDK Support)**：`minSdk = 26` (Android 8.0 Oreo) ~ `targetSdk = 37`
* **UI 框架 (UI Framework)**：Jetpack Compose (Material Design 3)
* **導航與自適應策略 (Navigation & Adaptive Strategy)**：**Jetpack Navigation 3** 與 **Compose Material Adaptive** (`NavigationSuiteScaffold`)
* **架構設計 (Architecture)**：**外觀模式 (Facade Pattern)** + **倉儲模式 (Repository Pattern)** + **Offline-First Cloud Sync**
* **資料持久化 (Persistence)**：
  * **Room Database v5**：包含 `Note`、`Category`、`ToDo`、`Reminder`、`TimeBlock`、`PomodoroRecord`、`SyncTombstone` 共 7 個實體與對應 DAO（含 `MIGRATION_3_4` 與 `MIGRATION_4_5`）。
  * **DataStore Preferences**：包含 `ThemePreferences`、`NotificationPreferences`、`NoteDraftPreferences`、`SyncPreferences`。
* **雲端服務 (Cloud Backend)**：Firebase BOM (`35.1.0`) + Cloud Firestore
* **品質驗證與 CI/CD**：16 個單元測試套件（74 項測試）、Android Lint 靜態檢查、GitHub Actions (`.github/workflows/android.yml`)

---

## 實作階段與任務紀錄 (Implementation Tasks)

### Task_1_DataLayer: 設定 Room 資料庫，包含 ToDo、Reminder、TimeBlock、PomodoroRecord 實體、DAO，並為雙軌任務與日程實作 Repository 模式。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):** 建立 Room Database，包含 `ToDo`、`Reminder`、`TimeBlock`、`PomodoroRecord`，並完成 `TaskRepository` 抽象化封裝。
- **驗收標準 (Acceptance Criteria):**
  - Room 資料庫設定完成且支援 migration
  - 各模組實體與 DAO 完整支援 CRUD 與響應式 Flow
  - Repository 統一提供存取操作

### Task_2_LunarCalendar: 實作離線農曆萬年曆引擎，並透過 Facade 模式封裝對外提供服務。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):** 離線農曆萬年曆引擎實作完成，支援 1900–2100 年農曆換算、天干地支生肖年份、二十四節氣與節慶。提供 `LunarCalendarFacade` 與日曆標籤文字提取接口。單元測試全數通過。
- **驗收標準 (Acceptance Criteria):**
  - 支援 1900–2100 年農曆日期、節氣與節慶之離線運算
  - Facade 封裝完整運算複雜度

### Task_3_UIComponents: 建立 Jetpack Compose 元件：高對比度色彩標籤、色彩選盤、即將到期之呼吸虛線與彈跳徽章動態卡片。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):** 完成 `ColorTag`、`ColorPickerRow`（8 款經典色票）、`breathingDashedBorder`、`BouncingBadge`、`DeadlineAlertCard`（支援打勾完成、刪除、明日到期發光動態）。
- **驗收標準 (Acceptance Criteria):**
  - 色彩標籤自動計算文字對比
  - 明日到期項目觸發橘紅呼吸虛線邊框與彈跳徽章動畫

### Task_4_ScreensNav: 實作畫面與導航：待辦清單、農曆月曆、每日時間規劃表、番茄鐘。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 待辦清單 (`TaskScreen`)：支援篩選狀態、新增待辦（設定到期日、色彩標籤、明日到期警示）。
  - 農曆月曆 (`CalendarScreen`)：月曆月份切換、日期網格、農曆/節氣/節慶顯示、當日待辦事項連動與打勾。
  - 每日時間規劃表 (`ScheduleScreen`)：日期切換、進度百分比、時間區塊清單、新增區塊、一鍵開啟番茄鐘。
  - 番茄鐘 (`PomodoroScreen`)：25/5/15 分鐘三模式、環狀進度倒數、任務連動、今日番茄數統計與歷史紀錄。
  - 導航系統 (`AppNavigation`)：使用 Navigation 3 + `NavigationSuiteScaffold`，支援自適應排版。
- **驗收標準 (Acceptance Criteria):**
  - 月曆完整展示該月日曆與當日/當月待做事項
  - 每日時間規劃表與番茄鐘正常運作
  - 測試與除錯建置通過

### Task_5_GitHubVerify: 執行與驗證：設定 GitHub Actions CI/CD 工作流，驗證應用程式穩定性並確保自動推送至 GitHub。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):** 遠端儲存庫已綁定至 `https://github.com/bingchiwang/Notepad`，每次檔案異動皆自動執行 commit 並 push。測試與 APK 建置皆通過。
- **驗收標準 (Acceptance Criteria):**
  - GitHub Actions 工作流設定完成
  - 所有單元測試與 APK 建置通過
  - 專案檔案自動同步至 GitHub

### Task_6_RemindersAndEditConfirm: 實作多日精確提醒系統、事件內容與日期編輯功能、刪除前二次確認對話框。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 多日提醒與時間設定：支援跨年月自由多選日期，結合自訂通知時/分與快速預設時段，透過 `AlarmManager` 與 `ReminderReceiver` 實現精確系統通知。
  - 事件編輯功能：支援待做事項（`ToDo`）、提醒事項（`Reminder`）與排程（`TimeBlock`）隨時編輯標題、備註、到期日期/時間、分類標籤與色彩標記。
  - 刪除前二次確認：所有待做、提醒與行程刪除按鈕皆綁定 `ConfirmDeleteDialog`，防範誤觸刪除。
- **驗收標準 (Acceptance Criteria):**
  - 提醒事項支援多天多選與自訂時間
  - 到期觸發 Android 系統通知
  - 支援編輯事件內容與日期
  - 刪除前跳出確認對話框

### Task_7_DataLayerExpansion: 第一階段資料層擴充（Note / Category / ToDo 優先級 / ThemePreferences）。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 新增 `Note` 與 `Category` 資料實體、DAO (`NoteDao`, `CategoryDao`) 與 `NoteRepository`，支援空白標題/內文、釘選優先排序、修改時間自動更新、分類唯一索引防重與刪除分類連動處理、標題與內文全文搜尋排序。
  - 新增 `Priority`（高、中、低）列舉與 `DatabaseConverters`，於 `ToDo` 加入 `priority` 欄位並於 `AppDatabase` 升級為 v4（含 `MIGRATION_3_4` 遷移邏輯）。
  - 於 `Reminder` 加入可選欄位 `linkedToDoId`，為任務完成取消關聯提醒提供資料結構基礎。
  - 建立 `ThemePreferences`（DataStore Preferences 持久化 `ThemeMode`: 淺色、深色、跟隨系統）並串接至 `MainActivity`。
- **驗收標準 (Acceptance Criteria):**
  - Room v4 Schema 與遷移編譯成功
  - `NoteRepository`、`TaskRepository`、`PriorityAndConverters`、`ThemePreferences` 單元測試全數通過
  - `./gradlew build` 與 `./gradlew test` 驗證通過

### Task_8_Phase2UIAndUXIntegration: 第二階段介面與操作連動（記事管理／草稿不遺失／分類搜尋／To-Do優先級／通知總開關／主題切換／導覽列防遮擋）。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 建立記事管理畫面（`NotesScreen`）與草稿不遺失編輯器（`NoteEditorScreen` + `NoteDraftPreferences` DataStore 持久化），支援空白標題內文、一鍵釘選置頂、標題與內文全文搜尋（依修改時間排序、無結果提示）、分類增修防重與刪除分類連動歸類記事。
  - 建立待辦優先級（高、中、低）選取器與色彩徽章（`PrioritySelectorRow` / `PriorityBadge`），預設隔離進行中與已完成任務，並確保新增與編輯對話框均驗證任務名稱不得為空白。
  - 建立通知總開關持久化（`NotificationPreferences`）與系統權限管理（`AppSettingsDialog`），於 `ReminderScheduler` 與 `ReminderReceiver` 雙重攔截過期與關閉狀態通知，並在待辦任務完成時自動取消關聯之尚未觸發提醒。
  - 透過全局頂部工具列整合 `AppSettingsDialog` 支援淺色、深色與跟隨系統三模式即時同步所有畫面並持久化，搭配 `MainActivity` 動態更新系統狀態欄與導覽列對比色。
  - 於 `AppNavigation` 外層套用 `statusBarsPadding()`、`navigationBarsPadding()`、`displayCutoutPadding()` 與編輯器 `imePadding()`，全面防止手機系統導覽列、狀態欄、挖孔與鍵盤遮擋 APP 介面。
- **驗收標準 (Acceptance Criteria):**
  - 所有主要功能皆可由統一底部導覽及頂部快捷列即時存取
  - 31 項單元測試與 `./gradlew build`（95 項任務）全部執行通過

### Task_9_FirebaseCloudSync: 新增 Firebase 雲端資料同步功能（本地優先 + 離線恢復自動上傳 + 最新修改優先衝突合併）。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 擴充 Room Database 至 v5（`MIGRATION_4_5`）：為 `Note`、`Category`、`ToDo`、`Reminder`、`TimeBlock`、`PomodoroRecord` 加入 `syncId`、`updatedAt`、`syncStatus` 欄位，並新增 `SyncTombstone`（`sync_tombstones`）記錄離線刪除動作。
  - 建立 `SyncPreferences`（保存同步空間代碼、`lastSyncAt` 與手動 Firebase 設定）、`AndroidNetworkMonitor`（網路恢復自動偵測）、`FirebaseFirestoreDataSource`（支援 `google-services.json` 或 UI 手動填入 `Project ID / App ID / API Key`）與 `CloudSyncManager`（雙向同步全部資料與設定、離線保留待上傳狀態、連網自動上傳及雲端提醒排程連動）。
  - 實作 `SyncConflictResolver` 採智慧合併與最新修改時間優先（`updatedAt` LWW）處理衝突，並建立 `CloudSyncDialog` 與頂部狀態徽章供檢視同步摘要、修改設定與一鍵手動同步。
  - 更新 `NoteRepositoryTest`、`TaskRepositoryTest` 假物件相容新 DAO 接口，新增 `SyncConflictResolverTest`（8 項）與 `CloudSyncManagerTest`（4 項），共計 43 項單元測試全數通過。
- **驗收標準 (Acceptance Criteria):**
  - 新增／修改／刪除優先寫入本機並保留待同步狀態，有網即時同步至 Firebase Firestore，離線恢復後自動上傳
  - 成功同步後更新 `lastSyncAt` 且衝突時依最新修改時間優先智慧合併
  - 43 項單元測試與 `./gradlew build`（含 `lint` 與 `assembleRelease` R8）全部執行通過

### Task_10_Android8Compatibility: 向下支援 Android 8.0（API 26）並驗證相容性與通知授權行為。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 將 `app/build.gradle.kts` 之 `minSdk` 由 `30` 調整為 `26`（Android 8.0 Oreo），確認所有依賴套件清單合併與執行期相容性無礙。
  - 移除 `ReminderReceiver` 過時的 `SDK_INT >= O` 判斷（消除 `ObsoleteSdkInt` Lint 警告），並於 `ReminderScheduler.hasNotificationPermission` 整合 `NotificationManagerCompat.areNotificationsEnabled()` 以涵蓋 Android 8.0–12 系統通知開關檢測。
  - 於 `AppSettingsDialog` 加入 `ON_RESUME` 生命週期刷新通知授權狀態，提升從系統設定返回時的使用體驗。
- **驗收標準 (Acceptance Criteria):**
  - Android Lint SARIF 報告之 `NewApi` 與 `ObsoleteSdkInt` 問題數為 `0`
  - 43 項單元測試與 `./gradlew build`（Debug / Release APK + R8 + Lint）全數通過

### Task_11_PersonalizationSettings: 實作個人化設定管理（自訂按鍵顏色／月曆樣式＋自動文字對比防護／字體大小／通知開關／同步狀態）。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 建立 `ColorContrastUtils` 與 `CalendarThemeResolver`，提供 WCAG 相對亮度對比偵測：無論使用者選擇明亮黃、純白、深靛或曜石黑作為按鍵或月曆底色，文字、圖示、優先級徽章與農曆／節慶／節氣標籤皆自動維持高對比清晰可見。
  - 擴充 `ThemePreferences` 與 `NotepadTheme`，持久化並全域套用 `PersonalizationSettings`（自訂按鍵顏色、`CalendarStyle` 6 款月曆樣式與可選底色、`AppFontScale` 4 段字體縮放倍率 `0.85x ~ 1.30x`）。
  - 升級 `AppSettingsDialog` 為整合「主題設定（按鍵色／月曆樣式即時預覽）」、「字體大小」、「通知開關」與「同步狀態（含一鍵立即同步與連線設定入口）」之個人化設定管理中心，並將個人化欄位納入 `CloudSyncManager` 雲端雙向同步。
  - 更新 `NoteRepositoryTest`、`ThemePreferencesTest`、`CloudSyncManagerTest` 並新增 `ColorContrastUtilsTest`（5 項），全專案 12 個測試套件共 50 項單元測試與 `./gradlew build` 皆通過。
- **驗收標準 (Acceptance Criteria):**
  - 按鍵顏色與月曆樣式切換即時套用全應用且任何同色文字皆自動轉為清晰可讀高對比色
  - 字體大小、通知開關與同步狀態集中管理並支援 DataStore 持久化與雲端同步
  - 50 項單元測試與 `./gradlew build` 全數通過

### Task_12_RuleAuditAndMarkdownOptimization: 全專案規則合規性檢查、程式碼註解與操作回饋強化、以及全部 Markdown 文件優化。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 逐一檢視全專案 Kotlin 原始碼，補齊 `LunarCalendarEngine`、`LunarCalendarFacade`、`LunarDate`、實體、DAO、導覽與共用 UI 元件之繁體中文 KDoc 註解並移除多餘匯入。
  - 於 `TaskDetailScreen` 補齊關聯提醒之直接編輯與刪除二次確認對話框，並強化 `ScheduleScreen`、`CalendarScreen`、`PomodoroScreen` 狀態變更即時 `showFeedback` 回饋。
  - 全面翻新與優化 `AGENTS.md`、`README.md`、`SPECIFICATION.md`、`CHANGELOG.md` 與 `.agent/plan.md`，確保所有規範、架構圖、資料模型與 50 項測試矩陣同步一致。
- **驗收標準 (Acceptance Criteria):**
  - 全專案原始碼與 `.md` 文件 100% 符合 `AGENTS.md` 所有規範
  - 50 項單元測試與 `./gradlew build` 驗證通過並推送至 GitHub

### Task_13_QuickNoteDesktopWidget: 實作快速記事桌面小工具（顯示最近記事、一鍵新增、點擊記事編輯與指定分類篩選）。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 建立 `widget/` 套件（`QuickNoteWidgetProvider`、`QuickNoteWidgetService`、`QuickNoteWidgetConfigureActivity`、`QuickNoteWidgetPreferences`、`QuickNoteWidgetLogic`、`QuickNoteWidgetContract`）與淺／深色自適應佈局及 `appwidget-provider` 中繼定義（`quick_note_widget_info.xml`）。
  - 擴充 `NoteEditorRoute`、`NoteEditorScreen`、`AppNavigation`、`MainActivity` 與 `CloudSyncManager.onRemoteDataSynced`，支援小工具「一鍵新增（自動預帶篩選分類）」、「點擊記事直達編輯」與「本機／雲端同步自動重整小工具」。
  - 新增 `QuickNoteWidgetLogicTest` 7 項單元測試並同步更新技術規格書、版本紀錄與 README。
- **驗收標準 (Acceptance Criteria):**
  - 支援 Android 8.0+（`minSdk = 26`）與 Android 12+/14+ `PendingIntent` 規範，Android Lint `NewApi` 與測試錯誤均為 0
  - 全專案 57 項單元測試與 `./gradlew build` 驗證通過並推送至 GitHub

### Task_14_ToDoDesktopWidget: 實作待辦事項桌面小工具（顯示未完成任務、直接勾選完成、顯示截止日期與標示高／中／低優先級）。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 建立 `ToDoWidgetLogic`、`ToDoWidgetRowBinder`、`ToDoWidgetService`、`ToDoWidgetProvider`（含 `ToDoWidgetUpdater`）與淺／深色雙主題優先級／截止日佈局資源（`widget_todo.xml`、`widget_todo_item.xml`、`todo_widget_info.xml`、`ic_widget_checklist.xml`、`ic_widget_checkbox_unchecked.xml`、`widget_priority_*_bg.xml`）。
  - 擴充 `QuickNoteWidgetContract`（`OpenToDoTab`、`OpenToDoDetail`）、`TaskScreen`（支援 `openAddToDoOnLaunch` 自動彈出新增對話框）、`AppNavigation` 與 `MainActivity`（連動 `taskRepository.onLocalDataChanged` 與 `CloudSyncManager.onRemoteDataSynced`）。
  - 支援直接於桌面點擊核取方塊完成任務，同步呼叫 `ReminderScheduler.updateToDoCompletionAndSyncReminders` 取消尚未觸發的關聯提醒並立即更新桌面列表。
  - 新增 `ToDoWidgetLogicTest` 5 項單元測試並同步更新 `SPECIFICATION.md`、`CHANGELOG.md` 與 `README.md`。
- **驗收標準 (Acceptance Criteria):**
  - 支援 Android 8.0–16（`minSdk = 26`、`targetSdk = 37`；API 31+ `RemoteCollectionItems` 與 API <31 `RemoteViewsService` 雙軌支援），Android Lint `NewApi` 與測試錯誤均為 0
  - 全專案 62 項單元測試與 `./gradlew build` 驗證通過並推送至 GitHub

### Task_15_TodayScheduleDesktopWidget: 實作今日行程桌面小工具（顯示今天到期的待辦、今日完成進度與即將到來的提醒）。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 建立 `TodayScheduleWidgetLogic`、`TodayScheduleWidgetRowBinder`、`TodayScheduleWidgetService`、`TodayScheduleWidgetProvider`（含 `TodayScheduleWidgetUpdater`）、共用 `WidgetTaskActions` 及對應佈局／進度條資源（`widget_today_schedule.xml`、`widget_today_section_header.xml`、`widget_today_reminder_item.xml`、`widget_today_empty_row.xml`、`widget_progress_drawable.xml`、`today_schedule_widget_info.xml`）。
  - 擴充 `QuickNoteWidgetContract`（`OpenScheduleTab`、`OpenRemindersTrack`）、`TaskScreen`（支援 `initialTrackOnLaunch` 直接切換提醒事項分軌）、`AppNavigation`、`ReminderReceiver` 與 `MainActivity` 同步刷新所有桌面小工具。
  - 新增 `TodayScheduleWidgetLogicTest` 5 項單元測試並同步更新技術規格書、版本紀錄與 README。
- **驗收標準 (Acceptance Criteria):**
  - 支援 Android 8.0–16（`minSdk = 26`、`targetSdk = 37`），Android Lint 錯誤為 0
  - 全專案 67 項單元測試與 `./gradlew build` 驗證通過並推送至 GitHub

### Task_16_CalendarDesktopWidget: 實作月曆桌面小工具（完整月份與星期、今日藍色標示、格內直接顯示記事與待辦文字、超過 2 筆顯示「＋N 項」、上下月切換與五項點擊互動）。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 建立 `CalendarWidgetLogic`、`CalendarWidgetPreferences`、`CalendarWidgetProvider`（含 `CalendarWidgetUpdater`）與淺／深色自適應月曆格網佈局（`widget_calendar.xml`、`widget_calendar_day_cell.xml`、`calendar_widget_info.xml`、`widget_cal_today_bg.xml`、`widget_cal_note_pill_bg.xml`、`widget_cal_todo_pill_bg.xml`、`widget_cal_todo_done_pill_bg.xml`、`widget_cal_more_pill_bg.xml`）。
  - 廢除小圓點標示，改於每日格子內直接顯示最多 2 筆附帶類型小圖示（`ic_widget_cal_event_*`）之單行省略文字標籤並以不同底色區分記事與待辦；超過 2 筆時自動顯示「`＋N 項`」（如 5 筆顯示前 2 筆與「`＋3 項`」）。
  - 為四大桌面小工具補齊桌面選單專屬圖示（`ic_widget_picker_*`）、預覽插圖（`widget_preview_*`）及小工具內部語意圖示（分類、文件、時鐘、截止日曆、標籤、進度圖示）。
  - 擴充 `QuickNoteWidgetContract`（`OpenCalendarDay`、`buildEditNoteIntent`）、`NoteEditorRoute`（`initialCreatedAtMillis`）、`NoteEditorScreen`、`CalendarScreen`（整合「📝 該日記事」區塊、點擊記事編輯、點擊待辦詳情與快速建立記事／待辦對話框）、`AppNavigation`、`WidgetTaskActions` 與 `MainActivity` 雙向即時刷新。
  - 將日期格內國曆日號調整為「置中靠上（`top|center_horizontal`）」對齊，並擴充 `CalendarWidgetLogic`、`CalendarWidgetPreferences`、`CalendarWidgetProvider`（頂部一鍵切換 `ACTION_TOGGLE_LUNAR`）與 `CalendarWidgetConfigureActivity`（`reconfigurable` 即時預覽設定頁），支援依個別小工具選擇是否於日號下方顯示農曆日期、節慶與二十四節氣。
  - 新增 `CalendarWidgetLogicTest` 7 項單元測試並同步更新 `SPECIFICATION.md`、`CHANGELOG.md` 與 `README.md`。
- **驗收標準 (Acceptance Criteria):**
  - 支援 Android 8.0–16（`minSdk = 26`、`targetSdk = 37`），Android Lint 錯誤為 0
  - 全專案 74 項單元測試與 `./gradlew build` 驗證通過並推送至 GitHub

### Task_17_UnifiedSchedulePomodoroTab: 將底部導覽之「番茄鐘」與「排程」整併改為「時間規畫表」，並將番茄鐘整合內嵌於時間規畫表分頁中。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 調整 `AppNavigation` 之頂層導覽為四大分頁（`記事`、`待辦`、`月曆`、`時間規畫表`），移除底部導覽列獨立之「番茄鐘」欄位。
  - 擴充 `ScheduleScreen` 加入 `⏰ 時間規畫` 與 `🍅 番茄鐘` 雙分頁切換（`PrimaryTabRow`），並將所有任務／時間段「一鍵進入番茄鐘」連動導向 `ScheduleRoute` 之番茄鐘分頁。
- **驗收標準 (Acceptance Criteria):**
  - 底部導覽列維持 4 項一致入口，且「時間規畫表」內可流暢切換時間規畫與番茄鐘功能
  - 全專案 74 項單元測試與 `./gradlew build` 驗證通過並推送至 GitHub

### Task_18_SolarTermTimezoneFix: 修復農曆二十四節氣於 UTC／跨時區環境之計算一致性（修復 GitHub Actions CI）。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 調整 `LunarCalendarEngine.getSolarTerm` 統一採用 `GMT+8`（東亞標準時間）比對二十四節氣日期，並於 `getLunarDate` 統一以 UTC 計算跨日整數天數差以消除夏令時間／時區偏移造成的誤判。
  - 於 `UTC` 時區模擬 GitHub Actions Runner 驗證全專案 74 項單元測試與 `./gradlew build` 全數通過。
- **驗收標準 (Acceptance Criteria):**
  - 全專案 74 項單元測試於 UTC 與 GMT+8 環境均 100% 通過
  - GitHub Actions `Android CI / build` 執行成功

### Task_19_ReleaseR8AndApkVerification: 修復 Release R8 混淆設定並完成 Debug／Release APK 簽章與模擬器實機執行驗證。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 修正 `app/build.gradle.kts` 移除造成 Room 初始化 `IllegalAccessError` 的實驗性 `packageScope` R8 設定，改為標準 `isMinifyEnabled = true`、`isShrinkResources = true` 搭配 `app/proguard-rules.pro`。
  - 透過 `zipalign` 與 `apksigner` 簽署產生可直接安裝執行的 `app-release-signed.apk`（4.01 MB）與 `app-debug.apk`（30.51 MB），並於 `Pixel_6a` 模擬器完成實機安裝、冷啟動與多頁深層連結測試（0 閃退）。
- **驗收標準 (Acceptance Criteria):**
  - Debug 與 Release APK 皆通過 `apksigner` 驗證且於 Android 模擬器實測正常啟動執行
  - 全專案 74 項單元測試與 `./gradlew build` 驗證通過並推送至 GitHub

### Task_20_WidgetIconPolish: 優化四大桌面小工具選單圖示、頂部徽章圖示與預覽插圖。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 升級 `ic_widget_picker_*` 四款桌面小工具選單圖示為漸層圓角方塊搭配微陰影多層次向量插畫，明確區隔快速記事、待辦事項、今日行程與月曆小工具。
  - 將 `ic_widget_note`、`ic_widget_checklist`、`ic_widget_today`、`ic_widget_calendar` 升級為與選單視覺一致的圓角雙色識別徽章。
  - 全面將 `widget_preview_*` 預覽圖從直角矩形升級為圓角現代化排版示意圖。
- **驗收標準 (Acceptance Criteria):**
  - 所有向量 XML 與漸層屬性於 Android 8.0–16（`minSdk = 26`、`targetSdk = 37`）無任何 AAPT2 或 Lint 錯誤
  - 全專案 74 項單元測試與 `./gradlew build` 驗證通過並推送至 GitHub

### Task_21_BootAndPackageUpdateReminderRecovery: 補齊並驗證開機／App 更新後自動從 Room 資料庫恢復未過期提醒排程（`BootCompletedReceiver`）。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):**
  - 檢查發現專案雖已於 `AndroidManifest.xml` 宣告 `RECEIVE_BOOT_COMPLETED` 權限，但尚未建立對應的系統開機與更新廣播接收器。
  - 新增 `BootCompletedReceiver`（監聽 `ACTION_BOOT_COMPLETED`、`ACTION_MY_PACKAGE_REPLACED`、`QUICKBOOT_POWERON`）並註冊於 `AndroidManifest.xml`；擴充 `ReminderDao.getUpcomingUnfiredRemindersOnce` 與 `ReminderScheduler.rescheduleUpcomingReminders`／`filterRemindersToReschedule`，確保通知總開關開啟時於背景撈取 Room 中所有未觸發、未過期且關聯待辦未完成之提醒重新排程。
  - 擴充 `ReminderSchedulerLogicTest`（共 5 項）並更新技術規格書、版本紀錄與 README，全專案達 76 項單元測試。
- **驗收標準 (Acceptance Criteria):**
  - `AndroidManifest.xml` 完整註冊 `BOOT_COMPLETED` 與 `MY_PACKAGE_REPLACED` 接收器並符合 Android 12+ `exported` 與安全檢核
  - 全專案 76 項單元測試與 `./gradlew build` 驗證通過並推送至 GitHub

---

## 專案規範索引 (Guidelines Reference)

本專案之完整規範請參閱根目錄 [`AGENTS.md`](../AGENTS.md)：
- **一、程式碼管理規範**：單一職責、DRY 共用封裝、繁體中文註解、具語意命名、相依性保護、測試驗證、每次修改必上傳 GitHub。
- **二、操作規範**：主要功能易找、明顯新增按鈕、刪除前二次確認、儲存成功即時提示、錯誤清楚提示、統一底部導覽、減少點擊步驟。
- **三、功能操作規範**：涵蓋記事管理、待辦事項、分類搜尋、提醒通知、個人化設定與防重色、Firebase 雲端資料同步、Android 8.0（API 26）相容性。
- **四、功能異動與變更管理規範**：1. 確認修改目的 → 2. 確認是否影響其他功能 → 3. 更新相關程式碼 → 4. 執行必要測試 → 5. 更新專案規格書及版本紀錄並推送 GitHub。
