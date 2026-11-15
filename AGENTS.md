# 專案程式碼與操作規範 (Project Guidelines)

本文件定義 **Lunar To-Do & Notepad App** 之最高開發準則、UX 操作規範、功能規格與變更管理流程。所有程式碼異動與文件維護均須嚴格遵守以下規範。

---

## 一、程式碼管理規範 (Code Management Guidelines)

1. **明確用途與職責**：每個類別（Class）及函式（Function）應有明確且專一的用途與職責（Single Responsibility Principle）。
2. **避免重複撰寫程式碼**：遵循 DRY 原則（Don't Repeat Yourself），重複出現的邏輯應抽離封裝為共用函式、元件或工具類別（如 `DateFormatter`、`ColorContrastUtils`、`CalendarThemeResolver`、`AppButtonThemeDefaults`、`SingleDatePickerSection`）。
3. **重要邏輯適當註解**：關鍵演算法、業務規則、非直觀處理與核心架構邏輯應加入適當且清晰的註解（優先使用繁體中文 KDoc 與行內說明）。
4. **變數命名明確具語意**：變數、常數、參數與屬性名稱必須具備明確意義，嚴格禁止使用無意義或混淆之縮寫與單字母命名。
5. **保護既有模組相依性**：不得隨意刪除或修改其他模組、畫面或組件正在引用的程式碼與資料結構；如需重構，需確保所有引用處同步調整且向下相容（含資料庫無損 Migration）。
6. **功能驗證與穩定性保證**：修改程式時必須確認相關功能仍可正常運作，修改完成後需執行單元測試與建置檢查（`./gradlew test` 與 `./gradlew build`）。
7. **每次修改檔案皆須上傳 GitHub**：遵守專案最高原則，每次修改檔案後皆執行 `git commit` 並推送（`git push`）至 GitHub 儲存庫。

---

## 二、操作規範 (Operation & UX Guidelines)

1. **所有主要功能應容易找到**：介面佈局清晰直觀，核心功能分區明確，並在首頁、頂部工具列及主要底部導覽直接呈現。
2. **新增記事按鈕應保持明顯**：提供高識別度的懸浮行動按鈕（`ExtendedFloatingActionButton`）或顯著行動呼籲（CTA），確保使用者能即時新增記事、待辦、提醒與行程。
3. **刪除資料前應顯示確認訊息**：所有刪除行為（記事、分類、待辦、提醒、關聯提醒、排程）皆需觸發二次確認對話框（`ConfirmDeleteDialog` 或分類刪除歸屬確認框），防止誤觸遺失資料。
4. **儲存成功時應提供提示**：資料新增、編輯儲存、狀態勾選或設定變更成功後，立即提供清晰的即時提示反饋（`showFeedback` Toast 提示）。
5. **錯誤發生時應顯示清楚訊息**：當輸入驗證未通過或執行異常時，應提供明確、易懂的行內錯誤訊息與 Toast 引導說明，避免無反饋或靜默失敗。
6. **不同頁面應保留一致的底部導覽方式**：採用全局統一的底部導覽（`NavigationSuiteScaffold`），在所有主要頁面保持相同標籤順序（`記事`、`待辦`、`月曆`、`排程`、`番茄鐘`）、圖示與導覽切換體驗。
7. **重要操作應避免過多點擊步驟**：優化使用者操作路徑，提供快速預設選項（如今天、明天、未來 7/30 天、常用時間段、一鍵勾選、一鍵開啟番茄鐘），減少繁瑣層級與點擊次數。

---

## 三、功能操作規範 (Functional & Operational Specifications)

### 3.1 記事管理規範 (`Note` / `NoteRepository` / `NotesScreen` / `NoteEditorScreen`)
- 每篇記事必須具有唯一識別碼（Room `id` 主鍵與跨裝置同步 `syncId` UUID）。
- 標題（`title`）及內容（`content`）皆允許為空白。
- 新增記事後必須保存資料至本機資料庫。
- 修改記事後必須自動更新修改時間（`updatedAt`）。
- 刪除記事必須同步更新資料狀態（並清理對應未保存草稿與記錄離線刪除墓碑）。
- 釘選記事應顯示於列表前方（`ORDER BY isPinned DESC, updatedAt DESC`）。
- 不得因切換頁面而遺失尚未保存的內容（透過 `NoteDraftPreferences` 自動持久化草稿並支援一鍵還原）。

### 3.2 待辦事項規範 (`ToDo` / `Priority` / `TaskRepository` / `TaskScreen`)
- 每項任務必須具有唯一識別碼（Room `id` 主鍵與 `syncId` UUID）。
- 任務名稱（`title`）不得為空白（UI 表單驗證與 `TaskRepository.requireValidTitle` 雙重防禦）。
- 任務優先級分為高、中、低（`Priority.HIGH`, `Priority.MEDIUM`, `Priority.LOW`），並支援優先級排序與色彩徽章顯示。
- 完成任務後必須立即更新完成狀態（`isCompleted`）。
- 已完成任務不得繼續顯示為未完成（`activeToDos` 與 `completedToDos` 明確分流隔離）。
- 任務截止日期（`ToDo.dueDate`）及提醒日期（`Reminder.triggerAt`）應分開獨立管理，並可透過 `linkedToDoId` 選擇性綁定。

### 3.3 分類搜尋規範 (`Category` / `NoteRepository` / `CategoryManagerDialog`)
- 每個分類必須具有唯一識別碼（Room `id` 主鍵與 `syncId` UUID）。
- 分類名稱不得重複（資料庫 `Index(value = ["name"], unique = true)` 唯一約束與忽略大小寫／前後空白防重檢查）。
- 刪除分類時，必須妥善處理該分類下的記事（支援移至「未分類 `categoryId = null`」或批次轉移至指定分類，外鍵亦設為 `ON DELETE SET NULL`）。
- 搜尋必須同時支援標題及內文（`title LIKE %query% OR content LIKE %query%` 全欄位模糊比對）。
- 搜尋結果應依照最新修改時間排序（`ORDER BY updatedAt DESC`）。
- 搜尋無結果時必須顯示明確提示（Empty State 圖示、說明文字與「清除搜尋條件」按鈕）。

### 3.4 提醒通知規範 (`Reminder` / `ReminderScheduler` / `ReminderReceiver`)
- 提醒時間必須符合有效日期及時間格式。
- 提醒時間已過時，不應安排過去的通知（`triggerAt > System.currentTimeMillis()`）。
- 關閉通知功能後，不得繼續發送新提醒（提供 `NotificationPreferences` 全域通知總開關控制，關閉時立即取消待觸發鬧鐘）。
- 任務完成後應自動取消尚未觸發的相關提醒（`ReminderScheduler.updateToDoCompletionAndSyncReminders` 連動 `AlarmManager.cancel`）。
- 修改提醒時間時，必須更新原本的通知排程（先取消舊 `PendingIntent`，重置 `isFired = false` 並重新排定）。
- 必須妥善處理 Android 通知權限（Android 13+ `POST_NOTIFICATIONS` 動態權限申請、Android 8.0+ `areNotificationsEnabled()` 檢查與 `ON_RESUME` 自動重整）。

### 3.5 自訂主題與個人化設定管理規範 (`ThemePreferences` / `AppSettingsDialog` / `ColorContrastUtils`)
- **主題模式**：支援淺色模式（Light Mode）、深色模式（Dark Mode）與跟隨系統模式（System Default Mode），切換後所有頁面與系統狀態欄／導覽列保持一致。
- **自訂按鍵顏色與月曆樣式**：支援 12 款預設按鍵色票、`#RRGGBB` Hex 色碼自訂輸入、6 款月曆視覺樣式（`CalendarStyle`）與自訂月曆選取色。
- **文字與背景防重色保護**：依據 WCAG 2.1 相對明度演算法（`ColorContrastUtils` & `CalendarThemeResolver`），當自訂按鍵或月曆底色與文字、圖示、節慶或節氣標籤顏色相近或一致時，必須自動切換為高對比深墨色（`#121316`）或純白色（`#FFFFFF`）並於必要時補上細邊框，確保文字清晰可見。
- **字體大小調整**：支援 4 段全局字體縮放（`AppFontScale`：精巧小 85%、標準中 100%、舒適大 115%、特大清晰 130%）。
- **通知開關與同步狀態整合**：於個人化設定中心集中管理通知總開關與雲端同步狀態（顯示連線狀態、待同步筆數、最後同步時間與一鍵立即同步）。
- **設定持久化與跨裝置同步**：所有使用者設定必須透過 DataStore Preferences 持久化保存，重新開啟 App 後仍然有效，並支援納入雲端同步。

### 3.6 雲端資料同步規範 (`CloudSyncManager` / `FirebaseFirestoreDataSource` / `SyncConflictResolver`)
- **本地優先儲存（Offline-First）**：新增、修改或刪除記事與待辦等資料時，應先寫入本機資料庫（Room v5 / DataStore），確保離線操作順暢無延遲。
- **Firebase 雙向同步**：連線時將記事、分類、待辦事項、提醒、時間規劃、番茄鐘與個人化設定同步至 Firebase Cloud Firestore。
- **離線狀態與刪除墓碑保留**：無網路連線時，必須保留待同步狀態（`SyncStatus.PENDING_UPLOAD`）與離線刪除墓碑紀錄（`SyncTombstone`）。
- **網路恢復自動上傳**：透過 `AndroidNetworkMonitor` 監聽網路狀態，連線恢復後自動上傳尚未同步的變更。
- **記錄最後同步時間**：同步成功後即時更新並持久化 `lastSyncAt` 最後同步時間；離線或失敗時不得覆蓋既有成功時間。
- **智慧衝突解決**：當本機與雲端資料不一致時，透過 `SyncConflictResolver` 依最新修改時間優先（Last-Write-Wins, `updatedAt`）原則進行智慧合併。

### 3.7 系統相容性與介面安全規範 (`minSdk = 26` / `AppNavigation`)
- **Android 版本支援**：支援 **Android 8.0（API 26）** 至 **Android API 37**，不得引入未受版本保護之高版本專屬 API（Android Lint `NewApi` 錯誤必須為 `0`）。
- **防導覽列與挖孔遮擋**：全面啟用 Edge-to-Edge 並套用 `statusBarsPadding()`、`navigationBarsPadding()`、`displayCutoutPadding()` 與編輯器 `imePadding()`，確保介面與按鈕絕不遭手機導覽列、狀態欄、螢幕挖孔或鍵盤遮擋。

---

## 四、功能異動與變更管理規範 (Change Management Guidelines)

如果需要增加或修改功能，必須嚴格依序執行以下五個步驟：

1. **確認修改目的**：明確釐清需求目標、使用情境與預期產出，避免無目標之改動。
2. **確認是否影響其他功能**：盤點目標模組的上下游相依性（資料庫 Schema、Repository 介面、共用元件、導航與既有功能），評估並防範回歸風險。
3. **更新相關程式碼**：遵循程式碼管理規範與操作規範實作變更，保持既有模組向下相容並補齊必要繁體中文註解。
4. **執行必要測試**：針對異動與受影響範圍撰寫或更新單元測試，並執行 `./gradlew test` 與 `./gradlew build` 驗證通過（0 失敗、0 Lint 錯誤）。
5. **更新專案規格書及版本紀錄**：同步更新 [`SPECIFICATION.md`](SPECIFICATION.md)、[`CHANGELOG.md`](CHANGELOG.md)、[`README.md`](README.md) 與 [`.agent/plan.md`](.agent/plan.md)，最後執行 Commit 並推送至 GitHub。
