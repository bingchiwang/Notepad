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
2. **離線農曆萬年曆 (Offline Lunar Perpetual Calendar)**：完整功能之離線萬年曆，支援計算 1900–2100 年農曆日期、天干地支生肖、二十四節氣與傳統節日。
3. **智慧自訂色彩標記 (Smart Custom Color Tagging)**：使用者可使用 8 款經典預設色彩或自選色彩盤為項目加上標籤，膠囊標籤會依背景明度自動計算並套用最佳文字對比度以確保易讀性。
4. **即將到期 UI 警示 (Imminent Deadline UI Alerts)**：針對次日到期之項目提供進階視覺提示，任務卡片與月曆視圖同步觸發動態效果，包含橘紅色「呼吸」發光虛線外框與彈跳「⚠️ 明日到期！」徽章。
5. **自動化 GitHub 工作流 (CI/CD)**：內建設定以自動上傳專案，並將未來的程式碼修改直接同步至 GitHub 儲存庫。

## 核心技術棧 (High-Level Tech Stack)
* **開發語言 (Language)**：Kotlin
* **UI 框架 (UI Framework)**：Jetpack Compose
* **導航與自適應策略 (Navigation & Adaptive Strategy)**：**Jetpack Navigation 3**（狀態驅動）與 **Compose Material Adaptive** 函式庫（支援所有響應式版面配置）。
* **架構設計 (Architecture)**：**外觀模式 (Facade Pattern)**（簡化底層複雜日曆運算）與 **倉儲模式 (Repository Pattern)**（清晰的資料存取抽象層）。
* **核心相依套件 (Core Dependencies)**：Kotlin Coroutines（非同步運算與 UI 狀態更新）。
* **資料持久化 (Persistence)**：Room Database（本機儲存雙軌待辦事項、提醒與自訂色彩標記）。
* **版本控制 (Version Control)**：Git 與 GitHub Actions（處理自動推送與同步需求）。

## 實作步驟 (Implementation Steps)

### Task_1_DataLayer: 設定 Room 資料庫，包含 ToDo 與 Reminder 實體、DAO，並為雙軌任務實作 Repository 模式。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):** `coder_agent` 已成功建立 Room 資料庫、`ToDo` 與 `Reminder` 實體、DAO（`ToDoDao`, `ReminderDao`），以及用於雙軌任務的 `TaskRepository`。專案已正常建置。
- **驗收標準 (Acceptance Criteria):**
  - Room 資料庫設定完成
  - 建立 ToDo 與 Reminder 實體
  - Repository 成功處理資料存取操作

### Task_2_LunarCalendar: 實作離線農曆萬年曆引擎，並透過 Facade 模式封裝對外提供服務。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):** 離線農曆萬年曆引擎已成功實作。`LunarCalendarEngine` 支援 1900–2100 年農曆日期、二十四節氣與節慶之演算。`LunarCalendarFacade` 提供簡潔的封裝介面。單元測試確認計算正確。
- **驗收標準 (Acceptance Criteria):**
  - 支援 1900–2100 年農曆日期、二十四節氣與傳統節慶之離線運算
  - Facade 類別成功抽象化運算複雜度

### Task_3_UIComponents: 建立 Jetpack Compose 元件：高對比度文字自訂色彩標籤、即將到期之 UI 警示動態卡片。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):** `coder_agent` 已成功建立 UI 元件。`ColorTag` 依亮度自動判定文字對比色。`DeadlineAlertCard` 包含自訂呼吸動態虛線邊框 Modifier 以及用於明日到期項目的彈跳徽章動畫。預覽元件正常可用。
- **驗收標準 (Acceptance Criteria):**
  - 色彩標籤元件能自動計算文字對比以保持清晰易讀
  - 明日到期之項目能觸發呼吸發光虛線邊框與彈跳徽章動畫

### Task_4_ScreensNav: 使用 Jetpack Navigation 3 與 Compose Material Adaptive 佈局實作各畫面，整合 Repository 與 Calendar Facade。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):** `coder_agent` 已成功建構應用程式畫面。App 採用 `NavigationSuiteScaffold` 進行自適應導航（底部導航列 vs 側邊導航軌），並使用 `ListDetailSceneStrategy` 在大螢幕上提供響應式雙欄分割視圖。使用 Jetpack Navigation 3 在日曆與任務畫面間路由。已成功整合 Repository 與 Facade。專案建置成功。
- **驗收標準 (Acceptance Criteria):**
  - 畫面採用 Adaptive 自適應佈局以支援不同螢幕尺寸
  - Navigation 3 負責日曆與任務畫面之路由切換
  - 正確顯示任務與農曆資訊

### Task_5_GitHubVerify: 執行與驗證：設定 GitHub Actions CI/CD 工作流，驗證應用程式穩定性並確保符合所有需求。
- **狀態 (Status):** COMPLETED (已完成)
- **更新記錄 (Updates):** 已成功於 GitHub 建立遠端儲存庫 `https://github.com/bingchiwang/Notepad`，完成首次推送，並設定 CI/CD 工作流與本機同步腳本 `sync.ps1`。每次修改檔案後將自動 commit 並 push 至 GitHub。
- **驗收標準 (Acceptance Criteria):**
  - 成功建立並綁定 GitHub 儲存庫
  - GitHub Actions 工作流設定檔已建立（推送自動觸發測試與建置）
  - 本機與遠端 main 分支完全同步
  - 建立一鍵同步腳本支援後續自動更新
- **耗時 (Duration):** COMPLETED
