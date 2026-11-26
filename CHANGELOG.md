# 版本紀錄 (Changelog)

本文件依據專案實際異動與建置驗證紀錄維護。所有功能變更均遵循 [`AGENTS.md`](AGENTS.md) 之程式碼管理規範、操作規範與變更管理流程。

---

## [1.14.0] - 2026-11-26
### 新增與優化 (Added / Improved)
- **小工具月曆日期字體大小調整（`CalendarWidgetLogic` / `CalendarWidgetPreferences` / `CalendarWidgetProvider` / `CalendarWidgetConfigureActivity`）**：
  - 於 [CalendarWidgetLogic.kt](app/src/main/java/com/example/notepad/widget/CalendarWidgetLogic.kt) 新增 `CalendarWidgetDateFontSize` 四段字級列舉（`小 9.5sp`、`標準 11sp`、`大 13.5sp`、`特大 16sp`，並同步縮放農曆與節慶副標 `7sp`～`10sp`），透過 `CalendarWidgetPreferences` 持久化並由 [CalendarWidgetProvider.kt](app/src/main/java/com/example/notepad/widget/CalendarWidgetProvider.kt) 動態套用。
  - 支援從 **App 設定中心（「月曆與桌面小工具」分類）**、**長按桌面小工具設定** 或 **直接點擊小工具左上角月曆圖示** 開啟 [CalendarWidgetConfigureActivity.kt](app/src/main/java/com/example/notepad/widget/CalendarWidgetConfigureActivity.kt) 即時預覽並調整日期字體大小。
- **App 設定中心重構為摺疊式分類選單（`AppSettingsDialog` / `AppNavigation`）**：
  - 重構 [AppSettingsDialog.kt](app/src/main/java/com/example/notepad/ui/components/AppSettingsDialog.kt)，將所有設定歸納為五大摺疊分類（**「主題與按鍵外觀」**、**「月曆與桌面小工具」**、**「全局字體大小」**、**「提醒與系統通知」**、**「Google 雲端硬碟同步」**），預設收合並顯示目前設定摘要，點擊分類項目後才展開細部設定。
- **測試覆蓋擴充**：擴充 [CalendarWidgetLogicTest](app/src/test/java/com/example/notepad/widget/CalendarWidgetLogicTest.kt)（9 項），全專案單元測試達 **95 項**（20 個測試套件，0 失敗、0 略過、0 Lint 錯誤）。

---

## [1.13.0] - 2026-11-26
### 優化與調整 (Changed / Improved)
- **月曆標題、農曆台灣節慶、週日紅字與桌面小工具月曆同步改版（`LunarCalendarEngine` / `CalendarScreen` / `CalendarWidgetProvider`）**：
  - 將月曆分頁頂部標題「📅 農曆萬年曆」改為「📅 月曆」（[AppNavigation.kt](app/src/main/java/com/example/notepad/ui/app/AppNavigation.kt)），並將 App 月曆與桌面小工具月曆之星期日標題「日」與星期日日期數字統一改為高對比紅色（[CalendarThemeResolver.kt](app/src/main/java/com/example/notepad/ui/theme/CalendarThemeResolver.kt)、[widget_calendar.xml](app/src/main/res/layout/widget_calendar.xml)、[CalendarWidgetProvider.kt](app/src/main/java/com/example/notepad/widget/CalendarWidgetProvider.kt)）。
  - 全面將 [LunarCalendarEngine.kt](app/src/main/java/com/example/notepad/calendar/LunarCalendarEngine.kt) 與 [LunarCalendarFacade.kt](app/src/main/java/com/example/notepad/calendar/LunarCalendarFacade.kt) 農曆欄位改為純繁體中文（修正「驚蟄、穀雨、小滿、芒種、處暑」正體字與閏月年份推進邏輯），並改以台灣節慶為主（新增除夕、元旦、二二八和平紀念日、兒童節、母親節、勞動節、端午節、七夕、父親節、中元節、教師節、中秋節、重陽節、雙十國慶日、台灣光復節、尾牙、行憲紀念日等）。
  - 改造桌面小工具月曆與 App 月曆同步：於 App 月曆切換年月時自動同步小工具顯示月份（`CalendarWidgetUpdater.syncDisplayedMonth`）、將小工具年份與月份標題字體放大至 `18sp`、讓小工具週次列依當月實際週數（4／5／6 週）動態增減，並移除每日方框改以每週橫線區隔（[CalendarWidgetLogic.kt](app/src/main/java/com/example/notepad/widget/CalendarWidgetLogic.kt)、[widget_calendar.xml](app/src/main/res/layout/widget_calendar.xml)）。
- **測試覆蓋擴充**：擴充 `LunarCalendarFacadeTest`、`CalendarWidgetLogicTest`（8 項）與 `ColorContrastUtilsTest`，全專案單元測試達 **94 項**（20 個測試套件，0 失敗、0 略過、0 Lint 錯誤）。

---

## [1.12.0] - 2026-11-25
### 新增 (Added)
- **記事內嵌核取方塊（Checklist）與一鍵轉待辦／今日規劃（`NoteChecklistLogic` / `NoteEditorScreen`）**：
  - 新增 [NoteChecklistLogic.kt](app/src/main/java/com/example/notepad/data/repository/NoteChecklistLogic.kt)，支援 Markdown 風格核取清單（`- [ ]`／`- [x]`）之解析、行勾選切換、游標與多行選取插入切換、Enter 自動延續下一項及空項結束清單。
  - 擴充 [NoteEditorScreen.kt](app/src/main/java/com/example/notepad/ui/screens/NoteEditorScreen.kt) 與 [AppNavigation.kt](app/src/main/java/com/example/notepad/ui/app/AppNavigation.kt)：於編輯器內建「插入核取項目 (`- [ ]`)」與「轉待辦 / 排程」快捷選單、選取文字即時浮現一鍵「轉換為待辦事項 (`ToDo`)」與「排入今日時間規劃 (`TimeBlock`)」行動列，並於下方呈現可即時點擊勾選／轉換的互動核取清單卡片。
  - 於 [ItemDialogs.kt](app/src/main/java/com/example/notepad/ui/components/ItemDialogs.kt) 抽取共用 `AddTimeBlockDialog`（並簡化重構 [ScheduleScreen.kt](app/src/main/java/com/example/notepad/ui/screens/ScheduleScreen.kt) 遵循 DRY），同步擴充 `AddToDoDialog` 支援 `initialTitle` 預填帶入。
- **測試覆蓋擴充**：新增 [NoteChecklistLogicTest](app/src/test/java/com/example/notepad/data/repository/NoteChecklistLogicTest.kt)（5 項），全專案單元測試擴充至 **93 項**（20 個測試套件，0 失敗、0 略過、0 Lint 錯誤）。

---

## [1.11.0] - 2026-11-25
### 新增 (Added)
- **自動化重複任務（Recurring Tasks，`RecurrenceRule` / `MIGRATION_5_6`）**：
  - 新增 [RecurrenceRule.kt](app/src/main/java/com/example/notepad/data/local/entity/RecurrenceRule.kt)（`RecurrenceType`：`NONE`／`DAILY`／`WEEKLY`／`MONTHLY` 與 `RecurringTaskLogic`），支援指定每週星期或每月日期（含月底 31 日於短月自動截斷且下期恢復原日期），並於 [ToDo.kt](app/src/main/java/com/example/notepad/data/local/entity/ToDo.kt)、[DatabaseConverters.kt](app/src/main/java/com/example/notepad/data/local/DatabaseConverters.kt) 與 [AppDatabase.kt](app/src/main/java/com/example/notepad/data/local/AppDatabase.kt) 實作無損資料庫升級 **Room v5 → v6（`MIGRATION_5_6`）**。
  - 擴充 `TaskRepository.completeToDoWithRecurrence`、`ReminderScheduler.updateToDoCompletionAndSyncReminders`／`formatCompletionFeedback` 與 `WidgetTaskActions.completeToDo`：無論於任務清單、任務詳情頁、農曆萬年曆或桌面小工具勾選完成當期重複任務時，皆自動產生下一期待辦項目、取消關聯提醒並防止重複衍生（`nextOccurrenceGenerated` 保護）；同步更新 `CloudSyncManager` 支援重複欄位跨裝置雲端同步。
  - 於 [ItemDialogs.kt](app/src/main/java/com/example/notepad/ui/components/ItemDialogs.kt) 抽取共用 `RecurrenceSelectorSection` 整合至 `AddToDoDialog` 與 `EditToDoDialog`，並於 [DeadlineAlertCard](app/src/main/java/com/example/notepad/ui/components/NotepadComponents.kt)、[TaskScreen.kt](app/src/main/java/com/example/notepad/ui/screens/TaskScreen.kt) 與 [CalendarScreen.kt](app/src/main/java/com/example/notepad/ui/screens/CalendarScreen.kt) 顯示重複週期徽章（如 `🔁 每週一`、`🔁 每月15日`）。
- **測試覆蓋擴充**：新增 [RecurringTaskLogicTest](app/src/test/java/com/example/notepad/data/local/entity/RecurringTaskLogicTest.kt)（5 項）並擴充 `PriorityAndConvertersTest`（+1 項）與 `TaskRepositoryTest`（+1 項），全專案單元測試擴充至 **88 項**（19 個測試套件，0 失敗、0 略過、0 Lint 錯誤）。

---

## [1.10.1] - 2026-11-25
### 調整 (Changed)
- **精簡雲端同步對話框為純 Google 雲端硬碟同步介面（`CloudSyncDialog` / `MainActivity`）**：
  - 移除連結 Google 帳號底下的「進階設定（服務切換 / Firebase 備援 / 手動權杖）」與「雲端硬碟備份區別代號」欄位，僅保留 **Google 雲端硬碟帳號連結與雙向同步** 操作介面，並於 [GoogleDriveAuthHelper](app/src/main/java/com/example/notepad/data/sync/GoogleDriveAuthHelper.kt) 補充 `Context.findActivity()` 解包強化授權視窗啟動穩定性。

---

## [1.10.0] - 2026-11-24
### 新增與改版 (Added / Changed)
- **雲端同步全面支援 Google 雲端硬碟（Google Drive `appDataFolder`）並為主介面**：
  - 於 `libs.versions.toml` 與 `app/build.gradle.kts` 導入 `play-services-auth:21.3.0`，透過 Google Play Services `Identity.getAuthorizationClient` 申請 `https://www.googleapis.com/auth/drive.appdata` 專屬隱藏空間權限，不需危險通訊錄帳號權限即可一鍵連結與切換 Google 帳號，並透過 Drive `about` 端點自動帶入帳號信箱與顯示名稱。
  - 新增 [GoogleDriveSyncPayloadSerializer](app/src/main/java/com/example/notepad/data/sync/GoogleDriveSyncPayloadSerializer.kt)、[GoogleDriveAuthHelper](app/src/main/java/com/example/notepad/data/sync/GoogleDriveAuthHelper.kt)、[GoogleDriveRemoteDataSource](app/src/main/java/com/example/notepad/data/sync/GoogleDriveRemoteDataSource.kt) 與 [RoutingCloudRemoteDataSource](app/src/main/java/com/example/notepad/data/sync/RoutingCloudRemoteDataSource.kt)；於增量上傳時自動先讀回並保留遠端未變動（`NO_OP`）文件再執行合併寫入，完整保留離線優先、刪除墓碑（`SyncTombstone`）與最新修改時間優先（LWW `SyncConflictResolver`）雙向合併機制。
  - 改版 [CloudSyncDialog](app/src/main/java/com/example/notepad/ui/components/CloudSyncDialog.kt)、[AppSettingsDialog](app/src/main/java/com/example/notepad/ui/components/AppSettingsDialog.kt) 與 [AppNavigation](app/src/main/java/com/example/notepad/ui/app/AppNavigation.kt)，預設以 **Google 雲端硬碟同步** 為主操作面板，並保留可折疊的進階備援選項（手動 Access Token 或切換 Firebase Firestore）。
  - 擴充 [SyncPreferences](app/src/main/java/com/example/notepad/data/preferences/SyncPreferences.kt) 加入 `CloudBackendProvider`、`googleAccountEmail`、`googleDisplayName`、`googleDriveSpaceKey` 與 `googleDriveAccessToken` DataStore 持久化。
- **測試覆蓋擴充**：新增 `GoogleDriveSyncPayloadSerializerTest`（2 項）與 `GoogleDriveRemoteDataSourceTest`（3 項），全專案單元測試擴充至 **81 項**（18 個測試套件，0 失敗、0 略過、0 Lint 錯誤）。

---

## [1.9.5] - 2026-11-24
### 新增 (Added)
- **開機完成與 App 更新後自動恢復提醒排程（`BootCompletedReceiver` / `ReminderScheduler`）**：
  - 新增並於 `AndroidManifest.xml` 註冊 [BootCompletedReceiver](app/src/main/java/com/example/notepad/reminder/BootCompletedReceiver.kt)（`android:exported="true"`），監聽 `ACTION_BOOT_COMPLETED`、`ACTION_MY_PACKAGE_REPLACED` 及快速開機廣播 `QUICKBOOT_POWERON`。
  - 擴充 `ReminderDao.getUpcomingUnfiredRemindersOnce` 與 `ReminderScheduler.rescheduleUpcomingReminders` / `filterRemindersToReschedule`，於重開機或應用程式更新後自動核對通知總開關，從本機 Room 資料庫撈取所有未觸發（`isFired = 0`）、未過期且關聯待辦未完成之未來提醒重新向 `AlarmManager` 排程，並同步刷新桌面待辦、今日行程與月曆小工具。
- **測試覆蓋擴充**：擴充 `ReminderSchedulerLogicTest`（新增開機／更新重排篩選與廣播動作白名單驗證），全專案單元測試擴充至 **76 項**（16 個測試套件，0 失敗、0 略過）。

---

## [1.9.4] - 2026-11-16
### 優化 (Improved)
- **全面升級四大桌面小工具圖示與預覽插圖視覺設計（`res/drawable/`）**：
  - 重新繪製四大桌面小工具選單圖示（[ic_widget_picker_note.xml](app/src/main/res/drawable/ic_widget_picker_note.xml)、[ic_widget_picker_todo.xml](app/src/main/res/drawable/ic_widget_picker_todo.xml)、[ic_widget_picker_today.xml](app/src/main/res/drawable/ic_widget_picker_today.xml)、[ic_widget_picker_calendar.xml](app/src/main/res/drawable/ic_widget_picker_calendar.xml)），導入垂直色彩漸層（`<gradient>`）、內緣微高光與專屬功能前景插畫（折頁便箋配畫筆圓章、雙卡打勾清單、暖陽進度儀表、雙環月曆網格配今日高亮與兩色標籤），徹底區分各小工具辨識度。
  - 將小工具頂部導覽圖示（[ic_widget_note.xml](app/src/main/res/drawable/ic_widget_note.xml)、[ic_widget_checklist.xml](app/src/main/res/drawable/ic_widget_checklist.xml)、[ic_widget_today.xml](app/src/main/res/drawable/ic_widget_today.xml)、[ic_widget_calendar.xml](app/src/main/res/drawable/ic_widget_calendar.xml)）升級為圓角雙色識別徽章風格，在深／淺色桌面主題下皆保持高對比一致美感。
  - 翻新全部四款桌面選單預覽插圖（`widget_preview_*.xml`），改用圓角卡片、膠囊按鈕、分區進度條與置中靠上日期＋農曆提示條呈現真實排版。

---

## [1.9.3] - 2026-11-16
### 修復與打包驗證 (Fixed / Verified)
- **修復 Release R8 執行期閃退並完成模擬器實機安裝啟動驗證（`build.gradle.kts` / `proguard-rules.pro`）**：
  - 於 `Pixel_6a` 模擬器實測 Release APK 時發現原先 `packageScope` R8 設定會造成 `Room` 初始化拋出 `IllegalAccessError`（`kotlin.text` 跨套件存取權限異常）。
  - 將 `app/build.gradle.kts` 之 `release` 建置類型調整為標準 `isMinifyEnabled = true`、`isShrinkResources = true` 與 `proguard-android-optimize.txt`，並新增 [app/proguard-rules.pro](app/proguard-rules.pro) 保護 Room、Navigation 3 `@Serializable` 路由、桌面小工具與 `BroadcastReceiver`；Release 產物同步縮減至 **4.01 MB**（已簽章版）。
  - 透過 `apksigner` 完成 APK v2/v3 簽章驗證，並於 `Pixel_6a` 模擬器完成冷啟動與各桌面小工具深度連結導航實測，確認無任何閃退且全部 74 項單元測試與 Lint 通過。

---

## [1.9.2] - 2026-11-16
### 修復 (Fixed)
- **修復二十四節氣跨時區計算與 GitHub CI 測試一致性（`LunarCalendarEngine`）**：
  - 修正 `LunarCalendarEngine.getSolarTerm` 原先使用 JVM 預設時區判定節氣所屬日期的問題，統一以東亞標準時間（`GMT+8`）計算節氣落點，避免於 GitHub Actions（`UTC` 時區）執行時因跨午夜偏移導致如 2026 年「雨水」（GMT+8 `2/19 06:25`／UTC `2/18 22:25`）錯位至前一日。
  - 同步優化 `LunarCalendarEngine.getLunarDate` 日差運算改為提取本地年月日後統一以 `UTC` 計算整數天數差，杜絕全球各時區夏令時間（DST）造成的一日誤差。
  - 已於 `UTC` 時區與本地時區完成 16 個測試套件共 **74 項單元測試** 與 `./gradlew build` 驗證通過。

---

## [1.9.1] - 2026-11-16
### 調整與優化 (Changed / Improved)
- **整合「時間規畫表」與內嵌「番茄鐘」導覽架構（`AppNavigation` / `ScheduleScreen`）**：
  - 將原先底部導覽中的「排程」與獨立「番茄鐘」欄位整合為單一「**時間規畫表**」導覽項目（底部導覽精簡為 `記事`、`待辦`、`月曆`、`時間規畫表` 四大主入口）。
  - 於 `ScheduleScreen` 頂部建立一致的雙分頁切換列（`⏰ 時間規畫` ／ `🍅 番茄鐘`），將 `PomodoroScreen` 整合放置於「時間規畫表」內部；從待辦清單或時間規劃區塊點擊「專注」時，皆自動切換至「時間規畫表＞番茄鐘」分頁並帶入對應任務名稱。

---

## [1.9.0] - 2026-11-15
### 新增 (Added)
- **月曆桌面小工具（`widget/`）**：
  - **完整月份與星期網格顯示**：新增 `CalendarWidgetProvider`、`CalendarWidgetLogic`、`CalendarWidgetPreferences`、`CalendarWidgetConfigureActivity` 與雙主題自適應佈局（`widget_calendar.xml`、`widget_calendar_day_cell.xml`、`calendar_widget_info.xml`），顯示月曆圖示（`ic_widget_calendar.xml`）、完整年月標題、星期列（`日`～`六`）與 6 週 × 7 天（42 格）月曆格網，並支援依個別小工具實例點擊左右箭頭切換上一個月／下一個月（點擊年月標題可快速回到當月）。
  - **日期置中靠上排版與可選農曆日期**：每個日期格之國曆日號採「置中靠上（`top|center_horizontal`）」排版，並可透過小工具頂部「`農曆 ✓`」一鍵切換鈕或長按重設配置頁（`CalendarWidgetConfigureActivity`）自由選擇是否於日號下方顯示農曆日期、傳統節慶與二十四節氣。
  - **今天日期明顯標示**：今天日期格採用專屬藍色背景與藍色外框（`widget_cal_today_bg.xml`）及高對比藍字日號標示，清晰區分當月、非當月與今日。
  - **格內直接顯示圖示＋文字與多筆折疊規則（無小圓點）**：廢除傳統小圓點標記，直接於日期格內顯示附帶專屬小圖示（記事文件圖示 `ic_widget_cal_event_note.xml`、待辦勾選圖示 `ic_widget_cal_event_todo.xml`／已完成核取圖示 `ic_widget_cal_event_todo_done.xml`）的文字膠囊，並以不同底色區分記事（暖琥珀色 `widget_cal_note_pill_bg`）與待辦事項（湛藍色 `widget_cal_todo_pill_bg`／已完成翠綠色 `widget_cal_todo_done_pill_bg`）；每格最多直接顯示 2 筆事項，單行超出寬度自動省略（`ellipsize="end"`），超過 2 筆時自動顯示「`＋N 項`」（如 5 筆顯示 2 筆＋「`＋3 項`」）。
  - **五項完整點擊互動**：
    - 點擊日期格或「`＋N 項`」：直接開啟 `CalendarScreen` 並跳轉至該日期，檢視該日的記事、待辦與提醒清單。
    - 點擊格內記事標籤：直接進入 `NoteEditorScreen` 編輯該篇記事。
    - 點擊格內待辦標籤：直接進入 `TaskDetailScreen` 查看或修改該筆待辦事項。
    - 點擊左右箭頭：切換上一個月／下一個月。
    - 點擊頂部「`+ 新增`」按鈕：開啟月曆快速建立選單，一鍵選擇為該日期快速建立記事（自動帶入該日日期）或待辦事項。
- **四大桌面小工具專屬圖示與桌面選單預覽圖全面升級**：
  - 為 `QuickNoteWidgetProvider`、`ToDoWidgetProvider`、`TodayScheduleWidgetProvider` 與 `CalendarWidgetProvider` 新增桌面小工具選單專屬識別圖示（`ic_widget_picker_note.xml`、`ic_widget_picker_todo.xml`、`ic_widget_picker_today.xml`、`ic_widget_picker_calendar.xml`）與高解析度向量預覽插圖（`widget_preview_quick_note.xml`、`widget_preview_todo.xml`、`widget_preview_today_schedule.xml`、`widget_preview_calendar.xml`）。
  - 於各桌面小工具內部元件補齊語意圖示：快速記事分類資料夾圖示（`ic_widget_category.xml`）、單篇記事文件圖示（`ic_widget_note_doc.xml`）、修改時間時鐘圖示（`ic_widget_clock.xml`）、待辦截止日曆圖示（`ic_widget_due_calendar.xml`）、標籤圖示（`ic_widget_tag.xml`）、今日進度統計圖示（`ic_widget_progress.xml`）與今日行程分區圖示。
- **萬年曆畫面整合當日記事清單與雙向即時刷新**：擴充 `CalendarScreen`（新增「📝 該日記事」區塊、點擊記事編輯、點擊待辦進詳情頁、快速新增記事／待辦選單對話框）、`NoteEditorRoute`（支援 `initialCreatedAtMillis`）、`WidgetTaskActions` 與 `MainActivity`（記事、待辦本機異動與雲端同步拉回後即時觸發 `CalendarWidgetUpdater.notifyDataChanged`）。
- **測試覆蓋擴充**：新增 `CalendarWidgetLogicTest`（7 項跨年月份進退位、42 格週日對齊與今日標示、非本月預設新增日、記事與待辦同日歸類排序、超過 2 筆截斷與「`＋3 項`」標籤計算、農曆顯示切換與節慶／初一標記、深度連結解析測試），全專案單元測試擴充至 **74 項**（16 個測試套件，0 失敗、0 略過）。

---

## [1.8.0] - 2026-11-15
### 新增 (Added)
- **今日行程桌面小工具（`widget/`）**：
  - **顯示今天到期的待辦**：新增 `TodayScheduleWidgetProvider`、`TodayScheduleWidgetService`、`TodayScheduleWidgetLogic`、`TodayScheduleWidgetRowBinder` 與雙主題佈局（`widget_today_schedule.xml`、`widget_today_section_header.xml`、`widget_today_reminder_item.xml`、`widget_today_empty_row.xml`、`today_schedule_widget_info.xml`），重用 `ToDoWidgetLogic` 與 `ToDoWidgetRowBinder` 顯示今日到期的未完成待辦（含高／中／低優先級與直接勾選完成）。
  - **顯示今日完成進度**：於頂部固定進度卡片（`widget_progress_drawable.xml` 水平 `ProgressBar`）彙整今日時間規劃區塊（`TimeBlock`）與今日到期待辦（`ToDo`）之完成數與百分比；抽離共用 `WidgetTaskActions` 確保從任一小工具勾選任務完成皆同步取消關聯提醒並即時刷新進度。
  - **顯示即將到來的提醒**：過濾尚未觸發（`isFired = false`）且晚於目前時間的提醒通知，依時間升冪排序並區分「`今天 HH:mm`」、「`明天 HH:mm`」與跨日時間顯示；點擊可直接跳轉至 `TaskScreen` 提醒事項分軌。
- **通知觸發與雙向即時刷新**：串接 `ReminderReceiver`（提醒觸發標記已處理時）、`TaskRepository.onLocalDataChanged` 與 `CloudSyncManager.onRemoteDataSynced` 至 `TodayScheduleWidgetUpdater.notifyDataChanged`，維持桌面資訊即時同步。
- **測試覆蓋擴充**：新增 `TodayScheduleWidgetLogicTest`（5 項今天到期待辦篩選、今日綜合完成進度計算、即將到來提醒排序與格式、深度連結解析測試），全專案單元測試擴充至 **67 項**（15 個測試套件，0 失敗、0 略過）。

---

## [1.7.0] - 2026-11-15
### 新增 (Added)
- **待辦事項桌面小工具（`widget/`）**：
  - **顯示尚未完成的任務**：新增 `ToDoWidgetProvider`、`ToDoWidgetService`、`ToDoWidgetLogic` 與 `ToDoWidgetRowBinder`，過濾僅顯示未完成待辦（`isCompleted = false`），並依「高＞中＞低優先級、截止日由近到遠、最新建立優先」排序，於頂部膠囊即時統計未完成筆數。
  - **直接勾選完成項目**：點擊任務左側核取方塊即透過背景廣播觸發 `ReminderScheduler.updateToDoCompletionAndSyncReminders` 完成該任務、標記雲端待上傳、取消尚未觸發之關聯提醒鬧鐘，並即時從桌面清單移除已完成項目；支援 Android 12+ `RemoteCollectionItems` 與 Android 8.0–11 `RemoteViewsService` 雙軌相容。
  - **顯示截止日期**：於 `ToDoWidgetLogic.resolveDueDateInfo` 依本地午夜時間判別 `已逾期`（紅字）、`今天到期` / `明天到期`（琥珀警示色）、`截止 MM/dd` 與 `未設截止日` 狀態。
  - **標示高、中、低優先級**：每列待辦皆透過左側高對比識別色條與高／中／低優先級色彩徽章（`高優先`／`中優先`／`低優先`）清楚分級，完整適配淺／深色桌面主題。
- **深度連結與雙向即時刷新**：擴充 `WidgetDeepLink`（`OpenToDoTab`、`OpenToDoDetail`）、`TaskScreen`（支援一鍵開啟 `AddToDoDialog`）、`AppNavigation`、`TaskRepository.onLocalDataChanged` 與 `CloudSyncManager.onRemoteDataSynced` 至 `ToDoWidgetUpdater.notifyDataChanged`，實現桌面與主應用雙向連動。
- **測試覆蓋擴充**：新增 `ToDoWidgetLogicTest`（5 項未完成過濾、優先級與截止日排序、高中低徽章格式、逾期/今日/明日/未來判別、深度連結解析測試），全專案單元測試擴充至 **62 項**（14 個測試套件，0 失敗、0 略過）。

---

## [1.6.0] - 2026-11-15
### 新增 (Added)
- **快速記事桌面小工具（`widget/`）**：
  - **顯示最近記事內容**：新增 `QuickNoteWidgetProvider`、`QuickNoteWidgetService`（`RemoteViewsService`）與自適應淺／深色佈局（`widget_quick_note.xml`、`widget_quick_note_item.xml`、`values-night/colors.xml`），於桌面直接呈現釘選徽章、分類標籤、色條識別、標題／多行摘要預覽與最近修改時間，支援水平與垂直縮放。
  - **點擊即可新增記事**：點擊小工具頂部「+ 新增」按鈕或空狀態提示區，透過 `QuickNoteWidgetContract` 與 `MainActivity` / `AppNavigation` 深度連結直接開啟 `NoteEditorScreen` 新增記事；若小工具已綁定特定分類，自動預帶該分類標籤（`initialCategoryId`）。
  - **點擊記事可進入編輯頁面**：透過 `RemoteViews` `PendingIntentTemplate` + `FillInIntent`（符合 Android 12+/14+ `PendingIntent` 規範），點擊小工具內任一記事卡片即可直接開啟該篇記事的編輯頁面。
  - **可選擇顯示指定分類**：新增 Jetpack Compose `QuickNoteWidgetConfigureActivity` 與 `QuickNoteWidgetPreferences`，於加入桌面時或隨時點擊小工具頂部分類膠囊，選擇顯示「全部記事」、「未分類」或任一既有分類；若原綁定分類遭刪除，由 `QuickNoteWidgetLogic` 安全退回「全部記事」。
- **本機異動與雲端同步即時重新整理小工具**：串接 `NoteRepository.onLocalDataChanged` 與 `CloudSyncManager.onRemoteDataSynced` 至 `QuickNoteWidgetUpdater.notifyDataChanged`，確保新增／編輯／刪除記事或分類異動與雲端同步拉回後桌面小工具即時更新。
- **測試覆蓋擴充**：新增 `QuickNoteWidgetLogicTest`（7 項分類編解碼、排序與分類遺失退回、空白標題／多行超長摘要、深度連結解析測試），全專案單元測試擴充至 **57 項**（13 個測試套件，0 失敗、0 略過）。

---

## [1.5.1] - 2026-11-15
### 優化與規範稽核 (Changed / Improved)
- **全專案規範合規稽核與 UX 回饋強化**：
  - 於 `TaskDetailScreen` 補齊關聯提醒之直接編輯（`EditReminderDialog`）與刪除前二次確認對話框（`ConfirmDeleteDialog`），確保詳情頁操作體驗與主列表完全一致。
  - 於 `ScheduleScreen`、`CalendarScreen` 與 `PomodoroScreen` 完善勾選完成狀態切換與番茄鐘專注完成之即時 `showFeedback` 提示與異常保護。
  - 為 `LunarCalendarEngine`、`LunarCalendarFacade`、`LunarDate`、各資料實體、DAO、導覽元件與共用 UI 元件補齊結構化繁體中文 KDoc 註解，並清理未使用的多餘匯入。
- **全面優化專案 Markdown 文件體系**：
  - 更新 [`AGENTS.md`](AGENTS.md)：完整納入「自訂主題與個人化設定管理規範（含 WCAG 文字防重色）」、「雲端資料同步規範（6 大準則）」與「Android 8.0 (API 26) 系統相容與防遮擋規範」。
  - 優化 [`README.md`](README.md)、[`SPECIFICATION.md`](SPECIFICATION.md)、[`CHANGELOG.md`](CHANGELOG.md) 與 [`.agent/plan.md`](.agent/plan.md)：新增快速導覽目錄、WCAG 對比度公式說明、12 個測試套件／50 項單元測試驗證矩陣，並修正 `.agent/plan.md` 重複標題與過期技術棧描述。

---

## [1.5.0] - 2026-11-11
### 新增 (Added)
- **個人化設定管理中心（`AppSettingsDialog`）**：整合「主題設定」、「字體大小」、「通知開關」與「雲端同步狀態」四大管理區段，可於頂部導覽列一鍵開啟，並支援「重設」為系統預設風格。
- **自訂按鍵顏色與自動高對比防重色引擎（`ColorContrastUtils` / `Theme.kt` / `AppButtonThemeDefaults`）**：提供 12 款精選按鍵色票（含純白、明亮黃、深靛與曜石黑）及 `#RRGGBB` Hex 色碼自訂輸入；依據 WCAG 相對亮度演算法自動切換按鍵內文、圖示、優先級徽章及表面連結之墨黑／純白文字（對比度低於閾值時自動加註外框並調整表面重點色），徹底杜絕文字與背景同色隱形問題。
- **自訂月曆樣式（`CalendarThemeResolver`）**：新增 `CalendarStyle` 六款月曆視覺風格（經典簡約、水墨東方、青翠森境、湛藍晴空、暖陽暮靄、極簡高對比）與可選自訂選日底色，同步套用至 `CalendarScreen` 與多日提醒選日網格（`MonthCalendarGrid`），並對星期／節慶／節氣與農曆字色進行自動高對比反色計算。
- **全局字體大小設定（`AppFontScale`）**：支援「精巧小 (85%)」、「標準中 (100%)」、「舒適大 (115%)」與「特大清晰 (130%)」四段縮放，透過 `LocalDensity` 即時套用至全應用頁面與對話框。
- **個人化設定雲端同步**：擴充 `CloudSyncManager` 與 `AppNavigation`，使用者修改按鍵色、月曆樣式或字體大小時自動標記待同步並跨裝置雙向合併。
- **測試覆蓋擴充**：新增 `ColorContrastUtilsTest`（5 項對比度、Hex 解析與月曆調色測試）並擴充 `ThemePreferencesTest` 與 `CloudSyncManagerTest`，全專案單元測試擴充至 **50 項**（12 個測試套件，0 失敗、0 略過）。

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
