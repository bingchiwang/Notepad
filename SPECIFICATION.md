# 專案規格書 (Project Specification)

## 1. 專案基本資訊
- **應用程式名稱**：Lunar To-Do & Notepad App（農曆萬年曆與雙軌待辦提醒記事）
- **套件識別碼 (`applicationId`)**：`com.example.notepad`
- **開發語言與框架**：Kotlin、Jetpack Compose（Material Design 3）、Jetpack Navigation 3、Compose Material Adaptive
- **SDK 支援範圍**：`minSdk = 30`、`compileSdk = 37`、`targetSdk = 37`
- **架構設計模式**：Facade Pattern（`LunarCalendarFacade`）+ Repository Pattern（`TaskRepository`、`NoteRepository`）+ DataStore Preferences
- **目前應用版本**：`versionName = "1.0"`（`versionCode = 1`）／Room 資料庫版本：`version = 4`

---

## 2. 系統模組架構與資料模型

### 2.1 本機資料庫結構（Room Database v4 - `AppDatabase`）
| 資料表名稱 | 實體類別 | 主要欄位 | 索引與關聯規則 |
| :--- | :--- | :--- | :--- |
| `notes` | `Note` | `id` (PK), `title`, `content`, `isPinned`, `categoryId`, `color`, `createdAt`, `updatedAt` | 外鍵參照 `categories(id)`，`onDelete = ForeignKey.SET_NULL`；索引 `index_notes_categoryId` |
| `categories` | `Category` | `id` (PK), `name`, `color`, `createdAt` | `index_categories_name` 唯一索引（`unique = true`） |
| `todos` | `ToDo` | `id` (PK), `title`, `description`, `isCompleted`, `dueDate`, `tag`, `color`, `createdAt`, `priority` | `priority` 透過 `DatabaseConverters` 轉換（`HIGH=3`, `MEDIUM=2`, `LOW=1`） |
| `reminders` | `Reminder` | `id` (PK), `title`, `message`, `triggerAt`, `tag`, `color`, `isFired`, `createdAt`, `linkedToDoId` | `linkedToDoId` 用於待辦完成時連動取消未觸發通知 |
| `time_blocks` | `TimeBlock` | `id` (PK), `date`, `startTime`, `endTime`, `title`, `description`, `isCompleted`, `tag`, `color` | 依日期儲存每日時間規劃區塊 |
| `pomodoro_records` | `PomodoroRecord` | `id` (PK), `timestamp`, `durationMinutes`, `taskTitle`, `date` | 記錄每日專注完成次數與歷史紀錄 |

* **資料庫版本遷移 (`MIGRATION_3_4`)**：自 v3 無損升級至 v4，保留既有待辦、提醒、時間規劃與番茄鐘紀錄，新增 `todos.priority`（預設 `2 = MEDIUM`）、`reminders.linkedToDoId`、以及 `categories` 與 `notes` 資料表。

### 2.2 偏好設定持久化（DataStore Preferences）
| 類別 | DataStore 檔名 | 儲存鍵值與預設狀態 | 功能說明 |
| :--- | :--- | :--- | :--- |
| `ThemePreferences` | `theme_preferences` | `theme_mode`（預設 `ThemeMode.SYSTEM`） | 支援 `LIGHT`（淺色）、`DARK`（深色）、`SYSTEM`（跟隨系統），跨頁面一致且重開 App 維持有效 |
| `NotificationPreferences` | `notification_preferences` | `notifications_enabled`（預設 `true`） | 全域通知總開關；關閉時取消所有待觸發鬧鐘且接收器不再顯示通知 |
| `NoteDraftPreferences` | `note_draft_preferences` | `NoteDraft`（`hasActiveDraft`, `editingNoteId`, `title`, `content`, `categoryId`, `color`, `isPinned`） | 即時保存尚未儲存之記事編輯內容，避免切換分頁或系統回收遺失草稿 |

---

## 3. 核心功能規格

### 3.1 記事管理與分類搜尋（`NotesScreen` / `NoteEditorScreen` / `NoteRepository`）
- **記事生命週期**：
  - 唯一識別碼 `id`（Room 自動遞增主鍵）。
  - 標題與內文皆允許為空白，支援無阻礙快速記錄。
  - 修改記事時自動更新 `updatedAt` 時間戳記。
  - 支援「釘選置頂」切換，列表依 `isPinned DESC, updatedAt DESC` 排序。
  - 刪除記事前必須經由 `ConfirmDeleteDialog` 二次確認。
  - 編輯器自動保存草稿至 `NoteDraftPreferences`，離開未儲存時列表上方顯示草稿恢復橫幅。
- **分類管理**：
  - 每個分類具有唯一 `id`，新增與編輯時透過資料庫唯一索引及忽略大小寫／空白比對（`CategoryDao.getCategoryByName`）防止名稱重複。
  - 刪除分類時提供確認對話框，讓使用者選擇將該分類下記事轉移至「未分類（`categoryId = null`）」或移轉至其他既有分類。
- **全文搜尋**：
  - 支援同時模糊比對標題（`title`）與內文（`content`）。
  - 搜尋結果依最新修改時間（`updatedAt DESC`）排列。
  - 無搜尋結果時顯示明確的 Empty State 圖示、提示文字與一鍵清除按鈕。

### 3.2 雙軌待辦事項與優先級管理（`TaskScreen` / `TaskDetailScreen` / `TaskRepository`）
- **任務檢核與優先級**：
  - 強制檢核任務名稱不可為空白（UI 行內錯誤提示 + `TaskRepository` 前置防禦）。
  - 優先級分為 **高（`HIGH`）**、**中（`MEDIUM`）**、**低（`LOW`）**，提供選取器與色彩徽章（`PrioritySelectorRow` / `PriorityBadge`），列表預設依未完成、高優先級與到期日排序。
  - 進行中與已完成分頁明確隔離，已完成任務不會繼續顯示於未完成列表。
  - `ToDo.dueDate`（截止日期）與 `Reminder.triggerAt`（通知排程）分離管理，可獨立設定並透過 `linkedToDoId` 建立關聯。
  - 次日到期且未完成之任務同步於清單與月曆觸發橘紅呼吸虛線外框與跳動「⚠️ 明日到期！」徽章。

### 3.3 提醒通知排程（`ReminderScheduler` / `ReminderReceiver`）
- 支援自選任意未來年份與月份的多日提醒（`AddMultiDayReminderDialog`）及日期時間編輯（`EditReminderDialog`）。
- 嚴格攔截過去時間（`triggerAt > System.currentTimeMillis()`），不安排已過期的系統鬧鐘。
- 整合通知總開關（`NotificationPreferences`）：關閉時立即取消待觸發鬧鐘且廣播端不再發出通知；重新開啟時自動補排尚未過期之未來提醒。
- 當關聯之待辦任務完成時，透過 `ReminderScheduler.updateToDoCompletionAndSyncReminders` 自動取消所有尚未觸發之關聯提醒排程。
- 修改提醒時間時先取消舊 `PendingIntent`，再以最新時間重新排程並重置 `isFired = false`。
- 處理 Android 13+ `POST_NOTIFICATIONS` 權限申請與系統通知設定導引（`AppSettingsDialog`）。

### 3.4 離線農曆萬年曆、每日排程與番茄鐘
- **離線農曆萬年曆（`LunarCalendarFacade` / `CalendarScreen`）**：支援 1900–2100 年農曆日、天干地支生肖、二十四節氣與傳統民俗節日演算，並連動當日待辦與提醒項目。
- **每日時間規劃表（`ScheduleScreen`）**：以日期切換檢視時間區塊、計算當日完成進度百分比，並可一鍵帶入番茄鐘。
- **番茄鐘計時器（`PomodoroScreen`）**：提供專注工作（25 分鐘）、短暫休息（5 分鐘）、深度休息（15 分鐘），累計今日完成次數與歷史紀錄。

### 3.5 全局導航、系統欄安全防護與操作規範
- **導航架構（`AppNavigation`）**：採用 Jetpack Navigation 3 與 `NavigationSuiteScaffold`，全應用所有主要頁面維持一致的 5 項底部導覽（`記事`、`待辦`、`月曆`、`排程`、`番茄鐘`）及頂部主題／通知快捷工具列。
- **系統欄與挖孔防護**：外層容器套用 `statusBarsPadding()`、`navigationBarsPadding()`、`displayCutoutPadding()`，編輯器套用 `imePadding()`，防止手機系統導覽列、狀態欄、鏡頭挖孔或鍵盤遮擋介面。
- **操作反饋機制**：所有新增、更新、刪除成功及異常錯誤皆透過 `showFeedback` 或行內訊息提供清楚即時反饋，所有刪除操作皆經由二次確認對話框防護。

---

## 4. 變更管理規範（Change Management）
凡新增或修改功能，須依序完成：
1. 確認修改目的。
2. 確認是否影響其他功能與既有模組相依性。
3. 更新相關程式碼並維持規範品質。
4. 執行必要測試（`./gradlew test` 與 `./gradlew build`）。
5. 同步更新本規格書（`SPECIFICATION.md`）、版本紀錄（`CHANGELOG.md`）、專案說明（`README.md`）與專案計畫（`.agent/plan.md`），並提交推送至 GitHub。
