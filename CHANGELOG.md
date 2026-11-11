# 版本紀錄 (Changelog)

本文件依據專案實際異動與建置驗證紀錄維護。所有功能變更均遵循專案程式碼管理規範、操作規範與變更管理流程。

---

## [1.4.1] - 2026-11-11
### 調整與修復 (Changed / Fixed)
- **向下支援 Android 8.0（API 26）**：將 `app/build.gradle.kts` 之 `minSdk` 由 `30` 調整為 `26`，驗證所有依賴套件（Firebase BOM 35.1.0、Room 2.8.4、Navigation 3、Compose M3 Adaptive、CameraX 等）與 Manifest 合併相容，且 Android Lint `NewApi` 相容性錯誤數為 `0`。
- **Android 8.0+ 通知權限與狀態連動優化**：
  - 於 `ReminderScheduler.hasNotificationPermission` 結合 `NotificationManagerCompat.areNotificationsEnabled()`，讓 Android 8.0–12（API 26–32）裝置亦可準確偵測系統通知開關狀態。
  - 於 `AppSettingsDialog` 加入 `ON_RESUME` 生命週期監聽，使用者從系統通知設定返回 App 時自動刷新授權狀態。
  - 於 `ReminderReceiver` 移除不再需要的 `SDK_INT >= Build.VERSION_CODES.O` 條件判斷（消除 Lint `ObsoleteSdkInt` 警告），統一以 `NotificationChannel` 派送高優先級通知。

---

## [1.4.0] - 2026-11-10
### 新增 (Added)
- **Firebase 雲端資料同步架構（本地優先 + 雙向合併）**：整合 Firebase BOM (`35.1.0`) 與 Cloud Firestore（支援有 `google-services.json` 自動初始化，亦可在 UI 手動輸入 Project ID / App ID / Web API Key 動態連線），同步範圍涵蓋記事、分類、待辦事項、提醒、每日時間規劃、番茄鐘紀錄及主題／通知偏好設定。
- **資料庫升級 v5 與同步詮釋資料**：新增 `SyncStatus`（`SYNCED`、`PENDING_UPLOAD`）、`SyncEntityType` 與 `SyncTombstone` 離線刪除墓碑表；為所有核心資料實體擴充唯一索引 `syncId`、`updatedAt` 與 `syncStatus`，並提供 `MIGRATION_4_5` 無損升級遷移。
- **離線保留、斷線重連自動上傳與同步狀態追蹤**：新增 `SyncPreferences`（DataStore 保存 `syncEnabled`、`syncAccountId` 與 `lastSyncAt` 最後同步時間）、`AndroidNetworkMonitor`（`ConnectivityManager` 連線恢復偵測）與 `CloudSyncManager`（於 `NoteRepository`、`TaskRepository` 與偏好設定異動時自動觸發節流同步；雲端拉回提醒時同步更新 `ReminderScheduler` 鬧鐘排程）。
- **智慧衝突解決器與雲端管理介面**：建立 `SyncConflictResolver`（依最新 `updatedAt` 時間戳優先策略解決雙端編輯與刪除墓碑衝突）與 `CloudSyncDialog`，並於 `AppNavigation` 頂部工具列加入即時同步狀態圖示與待同步數量徽章。

### 調整與修復 (Changed / Fixed)
- **建置記憶體配置與格式修正**：調整 `gradle.properties` 之 `org.gradle.jvmargs` 為 `-Xmx4096m -XX:MaxMetaspaceSize=1024m`，並修復檔案尾端編碼格式，確保 R8 混淆與 Dexing 流程穩定建置。
- **測試覆蓋擴充**：新增 `SyncConflictResolverTest`（8 項衝突決策測試）與 `CloudSyncManagerTest`（4 項雙向同步與離線恢復端到端模擬測試），全專案單元測試擴充至 43 項（皆使用記憶體式 Fake DAO／模擬遠端儲存）。

---

## [1.3.0] - 2026-11-10
### 新增 (Added)
- **記事管理與草稿不遺失系統**：新增 `Note` 資料實體、`NoteDao`、`NoteRepository`、`NotesScreen` 與 `NoteEditorScreen`，並透過 `NoteDraftPreferences`（DataStore）保存未儲存草稿，防止切換分頁或重開 App 遺失內容。
- **分類與全文搜尋**：新增 `Category` 資料實體、`CategoryDao`（名稱唯一索引與大小寫防重）、分類管理對話框（刪除分類連動重新歸類記事）、頂部標題／內文全文搜尋列（依 `updatedAt` 降冪排序）與無結果 Empty State 提示。
- **待辦事項優先級管理**：新增 `Priority`（高、中、低）列舉與 `DatabaseConverters`，建立共用 `PrioritySelectorRow`、`PriorityBadge` 與 `AddToDoDialog`，並強制隔離進行中與已完成清單及檢核名稱不得空白。
- **通知總開關與任務連動取消**：新增 `NotificationPreferences` 與 `AppSettingsDialog`，於 `ReminderScheduler` 與 `ReminderReceiver` 雙重驗證開關狀態與權限；待辦任務完成時透過 `updateToDoCompletionAndSyncReminders` 自動取消關聯之尚未觸發提醒。
- **自訂主題切換介面**：建立 `ThemePreferences`（DataStore），支援「淺色模式」、「深色模式」與「跟隨系統模式」，並於全局頂部列提供 1 鍵切換與即時系統欄圖示對比更新（`MainActivity`）。
- **資料庫升級 v4**：`AppDatabase` 升級為 `version = 4`，加入 `MIGRATION_3_4` 無損遷移。
- **專案規範文件化**：建立專案規格書 `SPECIFICATION.md`、版本紀錄 `CHANGELOG.md`，並完善 `AGENTS.md` 中的程式碼管理、操作、功能操作及變更管理規範。

### 調整與修復 (Changed / Fixed)
- **手機導覽列安全防遮擋**：於 `AppNavigation` 外層套用 `statusBarsPadding()`、`navigationBarsPadding()`、`displayCutoutPadding()`，並於編輯器加上 `imePadding()` 及各清單底部安全間距，全面避免底部系統導覽列、狀態欄、挖孔與鍵盤遮擋介面。
- **操作反饋與錯誤捕捉**：於 `NotepadComponents` 新增 `showFeedback`，在待辦、提醒、排程、記事與分類的新增、更新、刪除及異常發生時顯示即時明確提示。
- **測試覆蓋擴充**：新增 `ThemePreferencesTest`、`PriorityAndConvertersTest`、`NoteRepositoryTest`、`TaskRepositoryTest`、`NotificationPreferencesTest`、`NoteDraftPreferencesTest` 與 `ReminderSchedulerLogicTest`，累計 31 項單元測試全數通過。

---

## [1.2.0] - 2026-10-18
### 新增 (Added)
- **手繪風自適應應用程式圖示**：更新筆記本與鉛筆插圖為全解析度（`mdpi` ~ `xxxhdpi`）App Icon 與自適應前景/背景圖層。
- **自由跨月／跨年多日提醒**：升級 `AddMultiDayReminderDialog` 與編輯對話框，支援透過 `MonthNavigatorHeader` 與 `MonthCalendarGrid` 自由挑選任意未來年份與月份的多天提醒。

### 修復 (Fixed)
- **修復 GitHub Actions Android Lint 錯誤**：抽取 `DateFormatter` 工具物件移除 Composable 內非響應式 `Locale.getDefault()` 呼叫，並調整 Gradle Lint 設定確保 CI 順利通過。

---

## [1.1.0] - 2026-10-18
### 新增 (Added)
- **多日提醒、事件編輯與刪除二次確認**：加入 `AlarmManager` 精確排程與 `ReminderReceiver` 系統通知；支援待辦、提醒、時間區塊內容與日期編輯；所有刪除動作皆綁定 `ConfirmDeleteDialog`。
- **農曆月曆、每日排程與番茄鐘**：新增 `CalendarScreen` 月曆網格與當日任務連動、`ScheduleScreen` 每日時間規劃表、以及 `PomodoroScreen` 25/5/15 分鐘番茄鐘計時與統計。
- **頂部狀態欄安全間距**：加入 `statusBarsPadding()` 與 `displayCutoutPadding()` 避開鏡頭挖孔遮擋。
- **自動化同步腳本**：新增 `sync.ps1` 簡化 Git Commit 與 GitHub 推送流程。

---

## [1.0.0] - 2026-10-18
### 新增 (Added)
- **專案初始版本**：建立 Lunar To-Do & Reminder App 基礎架構，包含 Room 資料庫、Repository 模式、`LunarCalendarFacade`（1900–2100 離線農曆演算法引擎）、Jetpack Navigation 3 導航與呼吸警示邊框動態卡片。
