# 專案計劃 (Project Plan)

雙軌分類：獨立劃分 📋 待做事件（可設到期日、勾選完成）與 🔔 提醒事項（可選提醒日期）。
農曆萬年曆：完整離線演算 1900–2100 年農曆日、天干地支生肖、二十四節氣與傳統節慶。
自選色彩標記：提供 8 款經典色票與原生色彩選盤，膠囊標籤自動計算最佳字體對比度。
🚨 到期日前一天特殊邊框警示：針對明日到期的待辦事項，卡片與月曆同步觸發橘紅呼吸動態發光虛線外框與 ⚠️ 明日到期！ 跳動徽章。
做成手機軟體
用Facade 與 Repository規則寫
上傳到github，往後修改檔案後自動上傳到github

## 專案簡介 (Project Brief)

# 專案簡介：農曆萬年曆與雙軌任務提醒 App (Lunar To-Do & Reminder App)

## 核心功能 (Features)
1. **雙軌任務管理 (Dual-Track Task Management)**：獨立處理待辦事項（可設到期日與完成勾選框）與提醒事項（可指定通知時間）。
2. **離線農曆萬年曆 (Offline Lunar Perpetual Calendar)**：完整功能之離線萬年曆，支援計算 1900–2100 年農曆日期、天干地支生肖、二十四節氣與傳統節日。支援月曆視覺化網格、當月/當日待做事項清單、日期快速跳轉。
3. **智慧自訂色彩標記 (Smart Custom Color Tagging)**：提供 8 款經典預設色彩（經典藍、森林綠、琥珀橙、珊瑚紅、典雅紫、蒂芬妮、明亮黃、質感灰），膠囊標籤依背景明度自動計算並套用最佳文字對比度。
4. **即將到期 UI 警示 (Imminent Deadline UI Alerts)**：針對次日到期之項目提供進階視覺提示，任務卡片與月曆視圖同步觸發橘紅色「呼吸」發光虛線外框與彈跳「⚠️ 明日到期！」徽章。
5. **每日時間規劃表 (Daily Time Planner)**：時間軸視覺化日程安排，支援開始/結束時間區塊、分類標籤、完成打勾與每日進度百分比。
6. **番茄鐘專注計時器 (Pomodoro Timer)**：支援專注工作 (25m)、短暫休息 (5m)、深度休息 (15m)，搭配環狀動態倒數進度條、目標任務連動、今日番茄數統計與歷史紀錄。
7. **自動化 GitHub 工作流 (CI/CD)**：專案已推送到 GitHub 儲存庫，每次修改後自動 commit 並同步 push。

## 核心技術棧 (High-Level Tech Stack)
* **開發語言 (Language)**：Kotlin
* **UI 框架 (UI Framework)**：Jetpack Compose (Material 3)
* **導航與自適應策略 (Navigation & Adaptive Strategy)**：**Jetpack Navigation 3** 與 **Compose Material Adaptive** 函式庫。
* **架構設計 (Architecture)**：**外觀模式 (Facade Pattern)** 與 **倉儲模式 (Repository Pattern)**。
* **核心相依套件 (Core Dependencies)**：Kotlin Coroutines / Flow。
* **資料持久化 (Persistence)**：Room Database（包含 ToDo、Reminder、TimeBlock、PomodoroRecord 實體與 DAO）。
* **版本控制 (Version Control)**：Git 與 GitHub Actions。

## 實作步驟 (Implementation Steps)

### Task_1_DataLayer: 設定 Room 資料庫，包含 ToDo、Reminder、TimeBlock、PomodoroRecord 實體、DAO，並為雙軌任務與日程實作 Repository 模式。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):** 建立 Room Database (v2)，包含 ToDo (含到期日、色彩、標籤)、Reminder、TimeBlock (每日排程)、PomodoroRecord (番茄鐘紀錄)，並完成 TaskRepository 抽象化封裝。
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
  - 待辦清單 (TaskScreen)：支援篩選狀態、新增待辦（設定到期日、色彩標籤、明日到期警示）。
  - 農曆月曆 (CalendarScreen)：月曆月份切換、日期網格、農曆/節氣/節慶顯示、當日待辦事項連動與打勾。
  - 每日時間規劃表 (ScheduleScreen)：日期切換、進度百分比、時間區塊清單、新增區塊、一鍵開啟番茄鐘。
  - 番茄鐘 (PomodoroScreen)：25/5/15 分鐘三模式、環狀進度倒數、任務連動、今日番茄數統計與歷史紀錄。
  - 導航系統 (AppNavigation)：使用 Navigation 3 + NavigationSuiteScaffold，支援自適應排版。
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
  - 多日提醒與時間設定：支援一次勾選多天或自訂未來 14 天任選日期，結合自訂通知時/分與快速預設時段，透過 `AlarmManager` 與 `ReminderReceiver` 實現精確系統通知。
  - 事件編輯功能：支援待做事項（To-Do）、提醒事項（Reminder）與排程（TimeBlock）隨時編輯標題、備註、到期日期/時間、分類標籤與色彩標記。
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

## 程式碼管理規範 (Code Management Guidelines)
1. **每個類別及函式應有明確用途**：保持單一職責，確保每個組件結構清晰且功能專一。
2. **避免重複撰寫相同程式碼**：遵循 DRY 原則，重複出現的邏輯抽象為共用函式或元件。
3. **重要邏輯應加入適當註解**：在關鍵計算、狀態轉換、業務規則及演算法處加上清晰的繁體中文註解。
4. **不得使用不明確的變數名稱**：嚴禁使用無語意的單字母或模糊縮寫，命名需能明確表達變數用途。
5. **不得隨意刪除其他模組正在使用的程式碼**：尊重現有程式相依性，避免破壞其他模組或功能。
6. **修改程式時必須確認相關功能仍可正常運作**：異動後執行測試與編譯檢查，確保回歸測試通過、穩定運作。
7. **每次修改檔案皆須上傳 GitHub**：遵守專案規範，每次修改檔案後皆執行 commit 並推送至 GitHub 儲存庫。

## 操作規範 (Operation & UX Guidelines)
1. **所有主要功能應容易找到**：介面佈局清晰直觀，核心功能分區明確，並在首頁及主要導覽直接呈現。
2. **新增記事按鈕應保持明顯**：提供高識別度的懸浮按鈕（Floating Action Button）或顯著行動呼籲（CTA），確保使用者能即時新增待辦、提醒與行程。
3. **刪除資料前應顯示確認訊息**：所有刪除行為（待辦、提醒、排程）皆需觸發二次確認對話框（`ConfirmDeleteDialog`），防止誤觸遺失資料。
4. **儲存成功時應提供提示**：資料新增或編輯儲存成功後，立即提供清晰的即時提示反饋（如 Toast 或 Snackbar）。
5. **錯誤發生時應顯示清楚訊息**：當輸入驗證未通過或執行異常時，應提供明確、易懂的錯誤訊息與引導說明，避免無反饋或靜默失敗。
6. **不同頁面應保留一致的底部導覽方式**：採用全局統一的底部導覽（`NavigationSuiteScaffold`），在所有主要頁面保持相同標籤順序、圖示與導覽切換體驗。
7. **重要操作應避免過多點擊步驟**：優化使用者操作路徑，提供快速預設選項（如今天、明天、常用時間段、一鍵勾選），減少繁瑣層級與點擊次數。

## 功能操作規範 (Functional & Operational Specifications)
### 記事管理規範
- 每篇記事必須具有唯一識別碼。
- 標題及內容可以為空白。
- 新增記事後必須保存資料。
- 修改記事後必須更新修改時間。
- 刪除記事必須同步更新資料狀態。
- 釘選記事應顯示於列表前方。
- 不得因切換頁面而遺失尚未保存的內容。

### 待辦事項規範
- 每項任務必須具有唯一識別碼。
- 任務名稱不得為空白。
- 任務優先級分為高、中、低。
- 完成任務後必須更新完成狀態。
- 已完成任務不得繼續顯示為未完成。
- 任務截止日期及提醒日期應分開管理。

### 分類搜尋規範
- 每個分類必須具有唯一識別碼。
- 分類名稱不得重複。
- 刪除分類時，必須處理該分類下的記事。
- 搜尋必須支援標題及內文。
- 搜尋結果應依照最新修改時間排序。
- 搜尋無結果時必須顯示提示。

### 提醒通知規範
- 提醒時間必須符合有效日期及時間格式。
- 提醒時間已過時，不應安排過去的通知。
- 關閉通知功能後，不得繼續發送新提醒。
- 任務完成後應取消尚未觸發的相關提醒。
- 修改提醒時間時，必須更新原本的通知排程。
- 必須處理 Android 通知權限。

### 自訂主題規範
- 支援淺色模式。
- 支援深色模式。
- 支援跟隨系統模式。
- 主題切換後所有頁面必須保持一致。
- 使用者設定必須保存，重新開啟 App 後仍然有效。

## 功能異動與變更管理規範 (Change Management Guidelines)
如果需要增加或修改功能，必須：
1. **確認修改目的**
2. **確認是否影響其他功能**
3. **更新相關程式碼**
4. **執行必要測試**
5. **更新專案規格書及版本紀錄**（同步更新 `SPECIFICATION.md`、`CHANGELOG.md` 與 `README.md` 並推送 GitHub）





