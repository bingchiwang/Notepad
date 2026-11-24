# 專案規格書 (Project Specification)

## 1. 專案基本資訊
- **應用程式名稱**：Lunar To-Do & Notepad App（農曆萬年曆與雙軌待辦提醒記事）
- **套件識別碼 (`applicationId`)**：`com.example.notepad`
- **開發語言與框架**：Kotlin 2.0+、Jetpack Compose（Material Design 3）、Jetpack Navigation 3、Compose Material Adaptive
- **SDK 支援範圍**：`minSdk = 26`（Android 8.0 Oreo）、`compileSdk = 37`、`targetSdk = 37`
- **架構設計模式**：Facade Pattern（`LunarCalendarFacade`）+ Repository Pattern（`TaskRepository`、`NoteRepository`）+ DataStore Preferences + Offline-First Firebase Cloud Sync（`CloudSyncManager`）
- **目前應用版本**：`versionName = "1.0"`（`versionCode = 1`）／Room 資料庫版本：`version = 5`

---

## 2. 系統模組架構與資料模型

### 2.1 本機資料庫結構（Room Database v5 - `AppDatabase`）
| 資料表名稱 | 實體類別 | 主要欄位 | 索引與關聯規則 |
| :--- | :--- | :--- | :--- |
| `notes` | `Note` | `id` (PK), `title`, `content`, `isPinned`, `categoryId`, `color`, `createdAt`, `updatedAt`, `syncId`, `syncStatus` | 外鍵參照 `categories(id)`，`onDelete = ForeignKey.SET_NULL`；索引 `index_notes_categoryId`、唯一索引 `syncId` |
| `categories` | `Category` | `id` (PK), `name`, `color`, `createdAt`, `syncId`, `updatedAt`, `syncStatus` | `index_categories_name` 與 `syncId` 唯一索引（`unique = true`） |
| `todos` | `ToDo` | `id` (PK), `title`, `description`, `isCompleted`, `dueDate`, `tag`, `color`, `createdAt`, `priority`, `syncId`, `updatedAt`, `syncStatus` | `priority` 透過 `DatabaseConverters` 轉換（`HIGH=3`, `MEDIUM=2`, `LOW=1`）；唯一索引 `syncId` |
| `reminders` | `Reminder` | `id` (PK), `title`, `message`, `triggerAt`, `tag`, `color`, `isFired`, `createdAt`, `linkedToDoId`, `syncId`, `updatedAt`, `syncStatus` | `linkedToDoId` 用於待辦完成時連動取消未觸發通知；唯一索引 `syncId` |
| `time_blocks` | `TimeBlock` | `id` (PK), `date`, `startTime`, `endTime`, `title`, `description`, `isCompleted`, `tag`, `color`, `syncId`, `updatedAt`, `syncStatus` | 依日期儲存每日時間規劃區塊；唯一索引 `syncId` |
| `pomodoro_records` | `PomodoroRecord` | `id` (PK), `timestamp`, `durationMinutes`, `taskTitle`, `date`, `syncId`, `updatedAt`, `syncStatus` | 記錄每日專注完成次數與歷史紀錄；唯一索引 `syncId` |
| `sync_tombstones` | `SyncTombstone` | `id` (PK), `entityType`, `syncId`, `deletedAt` | 複合唯一索引 `(entityType, syncId)`，記錄離線刪除動作供連網後推播刪除與衝突比對 |

* **資料庫無損版本遷移（`MIGRATION_3_4` & `MIGRATION_4_5`）**：
  * `MIGRATION_3_4`：自 v3 無損升級至 v4，建立 `notes`、`categories` 資料表與 `todos.priority`、`reminders.linkedToDoId` 欄位。
  * `MIGRATION_4_5`：自 v4 無損升級至 v5，為上述 6 張資料表補齊 `syncId`（為舊資料自動產生唯一識別字串）、`updatedAt`、`syncStatus` 欄位與唯一索引，並建立 `sync_tombstones` 離線刪除墓碑表。

### 2.2 偏好設定持久化（DataStore Preferences）
| 類別 | DataStore 檔名 | 儲存資料結構與預設狀態 | 功能說明 |
| :--- | :--- | :--- | :--- |
| `ThemePreferences` | `theme_preferences` | `PersonalizationSettings`（`themeMode = SYSTEM`, `buttonColorLong = null`, `calendarStyle = CLASSIC`, `calendarAccentColorLong = null`, `fontScale = MEDIUM`） | 支援淺／深／系統模式、自訂按鍵顏色、6 款月曆樣式／選色及全局字體大小倍率（`0.85x ~ 1.30x`），跨頁面一致且重開 App 維持有效 |
| `NotificationPreferences` | `notification_preferences` | `notifications_enabled`（預設 `true`） | 全域通知總開關；關閉時取消所有待觸發鬧鐘且接收器不再顯示通知 |
| `NoteDraftPreferences` | `note_draft_preferences` | `NoteDraft`（`hasActiveDraft`, `editingNoteId`, `title`, `content`, `categoryId`, `color`, `isPinned`） | 即時保存尚未儲存之記事編輯內容，避免切換分頁或系統回收遺失草稿 |
| `SyncPreferences` | `cloud_sync_preferences` | `SyncSettings`（`syncEnabled`, `syncAccountId`, `firebaseProjectId`, `firebaseAppId`, `firebaseApiKey`, `lastSyncAt`, `lastSyncMessage`, `settingsUpdatedAt`, `settingsSyncPending`） | 保存雲端同步開關、跨裝置空間代碼、選填手動 Firebase 參數、待同步設定時間戳與最後同步時間 (`lastSyncAt`) |

---

## 3. 核心功能與規範規格

### 3.1 記事管理與分類搜尋（`NotesScreen` / `NoteEditorScreen` / `NoteRepository`）
- **記事生命週期**：
  - 每篇記事具唯一識別碼 `id`（Room 自動遞增主鍵）與 `syncId`（UUID）。
  - 標題與內文皆允許為空白，支援無阻礙快速記錄。
  - 修改記事時自動更新 `updatedAt` 時間戳記並標記 `SyncStatus.PENDING_UPLOAD`。
  - 支援「釘選置頂」切換，列表依 `isPinned DESC, updatedAt DESC` 排序。
  - 刪除記事前必須經由 `ConfirmDeleteDialog` 二次確認；確認刪除後同步清理對應草稿並寫入 `SyncTombstone`。
  - 編輯器自動保存未儲存變更至 `NoteDraftPreferences`，離開未儲存時於記事列表頂部顯示草稿恢復橫幅。
- **分類管理**：
  - 每個分類具有唯一 `id`，新增與編輯時透過資料庫唯一索引及忽略大小寫／前後空白比對（`CategoryDao.getCategoryByName`）防止名稱重複。
  - 刪除分類時提供確認對話框，讓使用者選擇將該分類下記事轉移至「未分類（`categoryId = null`）」或移轉至其他既有分類。
- **全文搜尋**：
  - 支援同時模糊比對標題（`title`）與內文（`content`）。
  - 搜尋結果依最新修改時間（`updatedAt DESC`）排列。
  - 無搜尋結果時顯示明確的 Empty State 圖示、提示文字與一鍵清除搜尋按鈕。

### 3.2 雙軌待辦事項與優先級管理（`TaskScreen` / `TaskDetailScreen` / `TaskRepository`）
- **任務檢核與優先級**：
  - 強制檢核任務名稱不可為空白（UI 行內錯誤提示 + `TaskRepository.requireValidTitle` 前置防禦）。
  - 優先級分為 **高（`HIGH`, level=3）**、**中（`MEDIUM`, level=2）**、**低（`LOW`, level=1）**，提供選取器與高對比色彩徽章（`PrioritySelectorRow` / `PriorityBadge`），列表預設依未完成、高優先級與到期日排序。
  - 進行中（`activeToDos`）與已完成（`completedToDos`）清單明確隔離，已完成任務不會繼續顯示於未完成列表。
  - `ToDo.dueDate`（截止日期）與 `Reminder.triggerAt`（通知排程）分離管理，可獨立設定並透過 `linkedToDoId` 建立關聯；於 `TaskDetailScreen` 內亦可直接新增、編輯或經二次確認刪除關聯提醒。
  - 次日到期且未完成之任務同步於清單與月曆觸發橘紅呼吸虛線外框與跳動「`⚠️ 明日到期！`」徽章。

### 3.3 提醒通知排程（`ReminderScheduler` / `ReminderReceiver` / `BootCompletedReceiver`）
- 支援自選任意未來年份與月份的多日提醒（`AddMultiDayReminderDialog`）及日期時間編輯（`EditReminderDialog`）。
- 嚴格攔截過去時間（`triggerAt > System.currentTimeMillis()`），不安排已過期的系統鬧鐘。
- 整合通知總開關（`NotificationPreferences`）：關閉時立即取消待觸發鬧鐘且廣播端不再發出通知；重新開啟時自動補排尚未過期之未來提醒。
- 當關聯之待辦任務完成時，透過 `ReminderScheduler.updateToDoCompletionAndSyncReminders` 自動取消所有尚未觸發之關聯提醒排程並標記已處理。
- 修改提醒時間時先取消舊 `PendingIntent`，再以最新時間重新排程並重置 `isFired = false`。
- **重開機與 App 更新自動復原排程**：透過已註冊於 `AndroidManifest.xml` 之 `BootCompletedReceiver` 監聽 `ACTION_BOOT_COMPLETED`、`ACTION_MY_PACKAGE_REPLACED` 與快速開機事件，於開機或更新完成後非同步呼叫 `ReminderScheduler.rescheduleUpcomingReminders`，從 Room 資料庫（`ReminderDao.getUpcomingUnfiredRemindersOnce`）撈取所有未觸發、未過期且關聯待辦未完成之提醒重新向 `AlarmManager` 排程，並同步刷新桌面小工具。
- 完整處理 Android 8.0+（API 26+）通知頻道與權限：Android 13+ 動態申請 `POST_NOTIFICATIONS`，並結合 `NotificationManagerCompat.areNotificationsEnabled()` 與 `AppSettingsDialog` 的 `ON_RESUME` 生命週期監聽即時反映系統授權狀態。

### 3.4 離線農曆萬年曆、每日排程與番茄鐘
- **離線農曆萬年曆（`LunarCalendarFacade` / `CalendarScreen`）**：支援 1900–2100 年農曆日、天干地支生肖、二十四節氣與傳統民俗節日演算，並連動當日待辦與提醒項目。
- **每日時間規畫與內嵌番茄鐘（`ScheduleScreen` / `PomodoroScreen`）**：於「時間規畫表」內整合「`⏰ 時間規畫`」與「`🍅 番茄鐘`」雙分頁，以日期切換檢視時間區塊、計算當日完成率進度條，並可一鍵將排程或待辦任務帶入番茄鐘（專注工作 25 分鐘、短暫休息 5 分鐘、深度休息 15 分鐘）自動寫入 `PomodoroRecord`。

### 3.5 全局導航、系統欄安全防護與操作規範
- **導航架構（`AppNavigation`）**：採用 Jetpack Navigation 3 與 `NavigationSuiteScaffold`，全應用所有主要頁面維持一致的 4 項底部導覽（`記事`、`待辦`、`月曆`、`時間規畫表`；番茄鐘內嵌於「時間規畫表」內部切換分頁）及頂部雲端同步／通知／主題設定快捷工具列。
- **系統欄與挖孔防護**：外層容器套用 `statusBarsPadding()`、`navigationBarsPadding()`、`displayCutoutPadding()`，編輯器套用 `imePadding()`，防止手機系統導覽列、狀態欄、鏡頭挖孔或鍵盤遮擋介面。
- **操作反饋與二次確認**：所有新增、更新、刪除、狀態勾選、設定變更與雲端同步結果皆透過 `showFeedback` 提供清楚即時反饋；所有刪除操作皆經由 `ConfirmDeleteDialog` 二次確認防護。

### 3.6 Firebase 雲端資料同步與衝突解決（`CloudSyncManager` / `FirebaseFirestoreDataSource` / `SyncConflictResolver`）
1. **本地優先寫入（Local-First）**：新增、修改或刪除記事、分類、待辦、提醒、時間規劃、番茄鐘與偏好設定時，一律優先寫入本機 Room 資料庫或 DataStore，確保零網路延遲與離線可用性。
2. **全資料 Firebase 雲端同步**：於連線且配置就緒時將全部資料類型（含關聯之分類 `categorySyncId` 與連結待辦 `linkedToDoSyncId`）雙向同步至 Firebase Firestore 集合 `notepad_sync/{accountId}/items/{ENTITYTYPE_SYNCID}`；支援自動識別 `google-services.json` 或由使用者於 `CloudSyncDialog` 手動輸入 Firebase Project ID / App ID / Web API Key 動態初始化。
3. **離線異動與刪除墓碑保留**：無網路期間，本機異動維持 `SyncStatus.PENDING_UPLOAD`，刪除資料則即時寫入 `sync_tombstones`（`SyncTombstone`），避免唯一索引（如同名分類重建）衝突同時完整保存待同步刪除操作。
4. **網路恢復自動上傳**：透過 `AndroidNetworkMonitor`（`ConnectivityManager.NetworkCallback`）偵測網路連線恢復，自動節流觸發 `CloudSyncManager.syncNow` 上傳排隊異動；雲端拉回提醒異動時透過 `onReminderSyncedFromCloud` 同步重整 `ReminderScheduler` 鬧鐘排程。
5. **最後同步時間記錄**：同步成功後立即寫入 `SyncPreferences.lastSyncAt` 與狀態摘要，並將本機狀態重設為 `SYNCED` 與清除已推播的墓碑；若離線或發生錯誤，保留既有的成功同步時間不覆寫。
6. **智慧合併與最新修改優先衝突解決（`SyncConflictResolver`）**：雙向比對 `updatedAt` 時間戳記（與刪除時間 `deletedAt`），以最新修改時間勝出（Last-Write-Wins）；若離線雙邊修改同一資料且本機於雲端刪除後仍有更新編輯，智慧保留本機修改並復原雲端版本。

### 3.7 個人化設定管理與 WCAG 防重色演算法（`AppSettingsDialog` / `ColorContrastUtils` / `CalendarThemeResolver`）
- **WCAG 2.1 相對明度與對比度公式**：
  - 對任意 RGB 色彩通道進行伽瑪線性化後計算相對明度 $L = 0.2126 R + 0.7152 G + 0.0722 B$，並以 $CR = \frac{\max(L_1, L_2) + 0.05}{\min(L_1, L_2) + 0.05}$ 計算對比度。
  - 當前景文字與背景色之對比度 $CR < 4.5$ 時，`ColorContrastUtils.ensureReadableTextColor` 自動將文字切換為高對比深墨色（`#121316`）或純白色（`#FFFFFF`）；當按鍵底色與卡片表面對比度 $CR < 1.6$ 時，自動啟用細邊框保護（`needsSubtleBorder`）。
- **主題設定與按鍵顏色自訂**：支援淺色／深色／跟隨系統模式，並可自訂全應用按鍵顏色（12 款色票或 `#RRGGBB` 色碼，亦可恢復主題預設）。
- **月曆樣式自訂**：支援 `CalendarStyle` 6 款視覺風格（`CLASSIC`, `INK_WASH`, `FOREST_ZEN`, `OCEAN_NIGHT`, `SUNSET_WARM`, `HIGH_CONTRAST`）與可選自訂選日底色，透過 `CalendarThemeResolver` 自動保護日期數字、週末標頭、節慶與節氣文字之可讀性。
- **字體大小**：透過 `AppFontScale` 提供 `0.85x`、`1.0x`、`1.15x`、`1.30x` 四段即時縮放倍率，於 `NotepadTheme` 注入 `LocalDensity.fontScale` 一致生效至全頁面。
- **通知開關與同步狀態統整**：於同一設定中心內直接切換全域通知總開關並檢視／觸發雲端同步，所有個人化偏好皆持久化至 `ThemePreferences` 並納入 `CloudSyncManager` 跨裝置同步。

### 3.8 快速記事桌面小工具（`QuickNoteWidgetProvider` / `QuickNoteWidgetService` / `QuickNoteWidgetConfigureActivity`）
- **最近記事顯示**：透過 `RemoteViewsService`（`QuickNoteWidgetService`）讀取本機 Room `Note` 與 `Category` 清單，依 `isPinned DESC, updatedAt DESC` 排序並顯示最多 12 筆最近記事之釘選徽章、左側色條、分類標籤、更新時間與標題／摘要預覽；支援淺色與深色桌面主題切換（`values/` + `values-night/`）與 `horizontal|vertical` 縮放。
- **一鍵新增與編輯導航**：透過 `QuickNoteWidgetContract` 建立明確指向 `MainActivity` 之深層連結，配合 `AppNavigation` 的 `LaunchedEffect(pendingWidgetDeepLink)` 與 `NoteEditorRoute(noteId, initialCategoryId)`：
  - 點擊小工具「+ 新增」按鈕或空狀態提示時，直接開啟空白記事編輯器，並於綁定特定分類時自動預帶 `initialCategoryId`。
  - 點擊小工具內任一記事卡片時（以 `PendingIntent.FLAG_MUTABLE` Template 搭配 `FillInIntent` 實作），直接開啟該 `noteId` 對應之記事編輯畫面。
- **可選擇顯示指定分類**：透過 `QuickNoteWidgetConfigureActivity`（`reconfigurable`）與 `QuickNoteWidgetPreferences` 為每個 `appWidgetId` 獨立保存 `WidgetCategoryFilter`（`All`、`Uncategorized`、`CategoryFilter(id)`）；若綁定分類已遭刪除，`QuickNoteWidgetLogic.resolveWidgetState` 自動退回顯示「全部記事」避免空白卡死。
- **自動即時更新**：`NoteRepository.onLocalDataChanged` 與 `CloudSyncManager.onRemoteDataSynced` 皆連動 `QuickNoteWidgetUpdater.notifyDataChanged`，於本機變更或雲端拉取完成後立即重整所有桌面小工具實例。

### 3.9 待辦事項桌面小工具（`ToDoWidgetProvider` / `ToDoWidgetService` / `ToDoWidgetLogic`）
- **僅顯示尚未完成任務**：透過 `ToDoWidgetLogic.resolveActiveTasks` 嚴格過濾 `isCompleted = false` 之待辦事項，依 `priority.level` 降冪、`dueDate` 升冪與 `createdAt` 降冪排序，並於頂部膠囊即時顯示待辦總數。
- **直接勾選完成並連動提醒取消**：點擊任務核取方塊透過 `ToDoWidgetProvider` 發送廣播動作呼叫 `ReminderScheduler.updateToDoCompletionAndSyncReminders`，將任務設為已完成、標記 `SyncStatus.PENDING_UPLOAD` 並自動取消所有尚未觸發的關聯系統鬧鐘；Android 12+（API 31+）採用 `RemoteViews.RemoteCollectionItems` 與共用 `ToDoWidgetRowBinder`，Android 8.0–11（API 26–30）採用 `ToDoWidgetService`（`RemoteViewsService`），確保跨版本零權限或 BAL 例外。
- **截止日期緊急度標示**：依據本地時區午夜邊界計算 `DueDateUrgency`（`OVERDUE` 已逾期、`TODAY` 今天到期、`TOMORROW` 明天到期、`UPCOMING` 未來日期、`NONE` 未設截止日），套用對應提醒字色。
- **高、中、低優先級識別與深度連結**：每筆任務同時顯示左側優先級色條（紅／橘／綠）與「`高優先`／`中優先`／`低優先`」高對比色彩徽章（支援淺／深色主題）；點擊頂部「+ 新增」直接進入 `TaskRoute` 並開啟 `AddToDoDialog`，點擊任務內容列直接進入 `TaskDetailRoute`。
- **即時自動重新整理**：`TaskRepository.onLocalDataChanged` 與 `CloudSyncManager.onRemoteDataSynced` 皆連動 `ToDoWidgetUpdater.notifyDataChanged`，確保主應用異動與雲端拉回後即時刷新桌面小工具。

### 3.10 今日行程桌面小工具（`TodayScheduleWidgetProvider` / `TodayScheduleWidgetService` / `TodayScheduleWidgetLogic`）
- **今日完成進度統計**：透過 `TodayScheduleWidgetLogic.resolveTodaySchedule` 彙整今日所有 `TimeBlock`（`date` 落於當日）與今天到期的 `ToDo`（`dueDate` 落於當日），計算已完成項數／總項數與百分比，以頂部進度條（`ProgressBar`）與摘要文字即時呈現；點擊進度卡片或日期膠囊直接開啟 `ScheduleRoute`。
- **今天到期的待辦**：重用 `ToDoWidgetLogic` 與 `ToDoWidgetRowBinder` 呈現今天到期且未完成的任務清單，並透過共用 `WidgetTaskActions.completeToDo` 支援直接在桌面勾選完成、取消關聯提醒並同步重整「待辦事項小工具」、「今日行程小工具」與「月曆小工具」；當今日到期任務全數完成時顯示「今日到期任務已全數完成 ✓」。
- **即將到來的提醒**：過濾尚未觸發且未過期的 `Reminder`（`!isFired && triggerAt > now`），依觸發時間升冪排序並依距離呈現「`今天 HH:mm`」、「`明天 HH:mm`」或「`MM/dd HH:mm`」；點擊可直達 `TaskRoute` 之「提醒事項」分軌。
- **即時聯動刷新**：`ReminderReceiver`、`TaskRepository.onLocalDataChanged` 與 `CloudSyncManager.onRemoteDataSynced` 皆即時觸發 `TodayScheduleWidgetUpdater.notifyDataChanged`。

### 3.11 月曆桌面小工具與全小工具圖示系統（`CalendarWidgetProvider` / `CalendarWidgetLogic` / `CalendarWidgetPreferences` / `CalendarWidgetConfigureActivity`）
- **完整月份與星期顯示（日期置中靠上＋可選農曆日期）**：透過 `CalendarWidgetLogic.resolveMonthGrid` 產生固定 6 週 × 7 天（42 格，週日為首日）月曆格網與「`YYYY年 M月`」標題（搭配 `ic_widget_calendar.xml` 標頭圖示），今天日期以藍色底色與藍色外框（`widget_cal_today_bg.xml`）及藍字日號標示；格內國曆日號採「置中靠上（`top|center_horizontal`）」對齊，並透過 `CalendarWidgetPreferences` 依 `appWidgetId` 獨立保存顯示年月與是否顯示農曆開關（`showLunarDate`），支援點擊頂部「`農曆 ✓`」按鈕一鍵切換，或於加入／重設配置頁（`CalendarWidgetConfigureActivity`）預覽與選擇是否顯示農曆日期、節慶與二十四節氣。
- **直接顯示圖示＋文字標籤與多筆截斷規則（不使用小圓點）**：
  - 將該日建立的 `Note`（依 `createdAt` 歸屬當日）與該日到期的 `ToDo`（依 `dueDate` 歸屬當日）直接以附帶類型圖示（`ic_widget_cal_event_note` / `ic_widget_cal_event_todo` / `ic_widget_cal_event_todo_done`）之單行省略文字膠囊（`maxLines = 1, ellipsize = end`）呈現於日期格內，並以不同底色區分記事（暖琥珀底色）與待辦事項（湛藍底色／已完成翠綠底色）。
  - 每個日期格最多直接顯示 2 筆事項（`MAX_VISIBLE_EVENTS = 2`）；當同一天超過 2 筆事項時，自動計算剩餘筆數並顯示「`＋N 項`」標籤（例如共 5 筆時顯示前 2 筆及「`＋3 項`」）。
- **四大桌面小工具圖示與預覽圖（Widget Picker Icons & Previews）**：
  - 於 `AndroidManifest.xml` 為四大桌面小工具配置獨立高識別度圖示（`ic_widget_picker_note`、`ic_widget_picker_todo`、`ic_widget_picker_today`、`ic_widget_picker_calendar`），並於 `res/xml/*_widget_info.xml` 配置對應向量預覽圖（`widget_preview_*`）；小工具內部同步配備分類、記事文件、時間、截止日、標籤與進度統計圖示。
- **點擊互動與深層連結**：
  - 點擊日期格或「`＋N 項`」：透過 `WidgetDeepLink.OpenCalendarDay(dateMillis, openQuickAdd = false)` 開啟 `CalendarRoute` 並跳轉至該日期，同時顯示該日記事清單、待辦清單與提醒清單。
  - 點擊格內記事膠囊：透過 `WidgetDeepLink.EditNote(noteId)` 直接開啟 `NoteEditorRoute` 編輯該篇記事。
  - 點擊格內待辦膠囊：透過 `WidgetDeepLink.OpenToDoDetail(taskId)` 直接開啟 `TaskDetailRoute` 查看或修改該筆待辦事項。
  - 點擊左右箭頭：發送廣播 `ACTION_PREV_MONTH` / `ACTION_NEXT_MONTH` 切換顯示月份。
  - 點擊「`+ 新增`」按鈕：透過 `WidgetDeepLink.OpenCalendarDay(dateMillis, openQuickAdd = true)` 開啟月曆快速建立對話框，一鍵選擇建立該日記事（帶入 `initialCreatedAtMillis`）或建立該日待辦事項。
- **即時聯動刷新**：`NoteRepository.onLocalDataChanged`、`TaskRepository.onLocalDataChanged`、`WidgetTaskActions.completeToDo` 與 `CloudSyncManager.onRemoteDataSynced` 皆連動 `CalendarWidgetUpdater.notifyDataChanged` / `refreshAllBlocking`。

---

## 4. 單元測試與驗證矩陣

全專案共計 **16 個測試類別、76 項單元測試**，每次異動均透過 `./gradlew test` 與 `./gradlew build` 驗證通過：

1. `LunarCalendarFacadeTest`（4 項）：驗證農曆日期換算、節氣、節慶與明日判定。
2. `PriorityAndConvertersTest`（4 項）：驗證優先級權重與 Room 型別轉換器。
3. `NoteRepositoryTest`（9 項）：驗證記事 CRUD、釘選排序、分類防重、分類刪除記事重歸類、全文搜尋與離線墓碑。
4. `TaskRepositoryTest`（4 項）：驗證待辦非空檢核、完成狀態分離、優先級排序、截止日與提醒分離及離線墓碑。
5. `ReminderSchedulerLogicTest`（5 項）：驗證過期時間攔截、通知總開關、開機／更新後重排篩選與系統廣播動作白名單。
6. `ThemePreferencesTest`（6 項）：驗證主題模式、按鍵色、月曆樣式、字體大小持久化與重設預設值。
7. `NotificationPreferencesTest`（1 項）：驗證通知總開關 DataStore 持久化。
8. `NoteDraftPreferencesTest`（1 項）：驗證未儲存記事草稿保存、讀取與清除。
9. `SyncConflictResolverTest`（8 項）：驗證 LWW 雙向合併與刪除墓碑衝突決策。
10. `CloudSyncManagerTest`（4 項）：驗證本地優先上傳、雲端下載合併、離線恢復補傳與個人化設定同步。
11. `ColorContrastUtilsTest`（5 項）：驗證 WCAG 對比度計算、同色防隱形文字翻轉、Hex 解析與 6 款月曆樣式對比度。
12. `ExampleUnitTest`（1 項）：驗證基礎測試框架運作。
13. `QuickNoteWidgetLogicTest`（7 項）：驗證桌面記事小工具分類篩選編解碼、釘選與更新時間排序、分類遺失安全退回、空白標題／多行摘要格式化與新增／編輯深度連結解析。
14. `ToDoWidgetLogicTest`（5 項）：驗證待辦小工具未完成任務過濾、高中低優先級與截止日排序、徽章與標籤格式、逾期／今日／明日／未來截止日判別與深度連結解析。
15. `TodayScheduleWidgetLogicTest`（5 項）：驗證今日行程小工具今天到期待辦過濾、今日完成進度統計（含時間規劃與到期任務）、即將到來提醒篩選與時間格式及深度連結解析。
16. `CalendarWidgetLogicTest`（7 項）：驗證月曆小工具跨年月份進退位、42 格週日對齊與今日標示、非當月快速新增預設日、記事與待辦同日歸類排序、超過 2 筆截斷與「`＋3 項`」顯示規則、農曆顯示切換與節慶標記及深度連結解析。

---

## 5. 變更管理規範（Change Management）
凡新增或修改功能，須嚴格依序完成以下五個步驟：
1. **確認修改目的**：釐清需求目標與使用情境。
2. **確認是否影響其他功能**：檢查資料結構、Repository、UI 與導覽相依性。
3. **更新相關程式碼**：遵循程式碼管理規範與操作規範實作，並補齊繁體中文註解。
4. **執行必要測試**：執行 `./gradlew test` 與 `./gradlew build` 確保 100% 通過且無 Lint 錯誤。
5. **更新專案規格書及版本紀錄**：同步更新 [`SPECIFICATION.md`](SPECIFICATION.md)、[`CHANGELOG.md`](CHANGELOG.md)、[`README.md`](README.md) 與 [`.agent/plan.md`](.agent/plan.md)，並執行 Commit 推送至 GitHub。
