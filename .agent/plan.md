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

