package com.example.notepad.data.repository

import com.example.notepad.data.local.dao.PomodoroDao
import com.example.notepad.data.local.dao.ReminderDao
import com.example.notepad.data.local.dao.SyncTombstoneDao
import com.example.notepad.data.local.dao.TimeBlockDao
import com.example.notepad.data.local.dao.ToDoDao
import com.example.notepad.data.local.entity.PomodoroRecord
import com.example.notepad.data.local.entity.Priority
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.SyncEntityType
import com.example.notepad.data.local.entity.SyncStatus
import com.example.notepad.data.local.entity.SyncTombstone
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.local.entity.ToDo
import kotlinx.coroutines.flow.Flow

/**
 * 待辦事項、提醒、每日時間規劃與番茄鐘資料倉儲（含離線優先待同步狀態追蹤）
 */
class TaskRepository(
    private val toDoDao: ToDoDao,
    private val reminderDao: ReminderDao,
    private val timeBlockDao: TimeBlockDao,
    private val pomodoroDao: PomodoroDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) {
    /** 本機資料異動通知鉤子，供雲端同步協調器即時觸發同步 */
    var onLocalDataChanged: (() -> Unit)? = null

    // === ToDo Operations ===
    val allToDos: Flow<List<ToDo>> = toDoDao.getAllToDos()

    /** 僅未完成任務串流（規範 4.2） */
    val activeToDos: Flow<List<ToDo>> = toDoDao.getActiveToDos()

    /** 已完成任務串流（規範 4.2） */
    val completedToDos: Flow<List<ToDo>> = toDoDao.getCompletedToDos()

    fun getToDosByPriority(priority: Priority): Flow<List<ToDo>> {
        return toDoDao.getToDosByPriority(priority)
    }

    fun getToDosByDate(date: Long): Flow<List<ToDo>> {
        return toDoDao.getToDosByDate(date)
    }

    fun getToDosByDateRange(startDate: Long, endDate: Long): Flow<List<ToDo>> {
        return toDoDao.getToDosByDateRange(startDate, endDate)
    }

    fun getToDoById(id: Int): Flow<ToDo?> {
        return toDoDao.getToDoById(id)
    }

    /**
     * 新增待辦事項（優先本機保存，名稱不得為空白，並標記待同步）
     */
    suspend fun insertToDo(toDo: ToDo) {
        require(toDo.title.isNotBlank()) { "任務名稱不得為空白" }
        val now = System.currentTimeMillis()
        syncTombstoneDao?.deleteTombstone(SyncEntityType.TODO, toDo.syncId)
        toDoDao.insertToDo(
            toDo.copy(
                title = toDo.title.trim(),
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        onLocalDataChanged?.invoke()
    }

    /**
     * 更新待辦事項（更新修改時間並標記待同步）
     */
    suspend fun updateToDo(toDo: ToDo) {
        require(toDo.title.isNotBlank()) { "任務名稱不得為空白" }
        toDoDao.updateToDo(
            toDo.copy(
                title = toDo.title.trim(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        onLocalDataChanged?.invoke()
    }

    suspend fun deleteToDo(toDo: ToDo) {
        syncTombstoneDao?.upsertTombstone(
            SyncTombstone(
                entityType = SyncEntityType.TODO,
                syncId = toDo.syncId,
                deletedAt = System.currentTimeMillis()
            )
        )
        toDoDao.deleteToDo(toDo)
        onLocalDataChanged?.invoke()
    }

    // === Reminder Operations ===
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()

    fun getRemindersByDate(startMillis: Long, endMillis: Long): Flow<List<Reminder>> {
        return reminderDao.getRemindersByDate(startMillis, endMillis)
    }

    suspend fun getPendingRemindersByToDoId(toDoId: Int): List<Reminder> {
        return reminderDao.getPendingRemindersByToDoId(toDoId)
    }

    suspend fun insertReminder(reminder: Reminder): Long {
        val now = System.currentTimeMillis()
        syncTombstoneDao?.deleteTombstone(SyncEntityType.REMINDER, reminder.syncId)
        val id = reminderDao.insertReminder(
            reminder.copy(
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        onLocalDataChanged?.invoke()
        return id
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(
            reminder.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        onLocalDataChanged?.invoke()
    }

    suspend fun deleteReminder(reminder: Reminder) {
        syncTombstoneDao?.upsertTombstone(
            SyncTombstone(
                entityType = SyncEntityType.REMINDER,
                syncId = reminder.syncId,
                deletedAt = System.currentTimeMillis()
            )
        )
        reminderDao.deleteReminder(reminder)
        onLocalDataChanged?.invoke()
    }

    suspend fun markReminderAsFired(id: Int) {
        reminderDao.markAsFired(id, System.currentTimeMillis())
        onLocalDataChanged?.invoke()
    }

    // === TimeBlock Operations (Daily Schedule) ===
    val allTimeBlocks: Flow<List<TimeBlock>> = timeBlockDao.getAllTimeBlocks()

    fun getTimeBlocksByDate(date: Long): Flow<List<TimeBlock>> {
        return timeBlockDao.getTimeBlocksByDate(date)
    }

    suspend fun insertTimeBlock(timeBlock: TimeBlock) {
        val now = System.currentTimeMillis()
        syncTombstoneDao?.deleteTombstone(SyncEntityType.TIME_BLOCK, timeBlock.syncId)
        timeBlockDao.insertTimeBlock(
            timeBlock.copy(
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        onLocalDataChanged?.invoke()
    }

    suspend fun updateTimeBlock(timeBlock: TimeBlock) {
        timeBlockDao.updateTimeBlock(
            timeBlock.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        onLocalDataChanged?.invoke()
    }

    suspend fun deleteTimeBlock(timeBlock: TimeBlock) {
        syncTombstoneDao?.upsertTombstone(
            SyncTombstone(
                entityType = SyncEntityType.TIME_BLOCK,
                syncId = timeBlock.syncId,
                deletedAt = System.currentTimeMillis()
            )
        )
        timeBlockDao.deleteTimeBlock(timeBlock)
        onLocalDataChanged?.invoke()
    }

    // === Pomodoro Operations ===
    fun getTodayPomodoroCount(date: Long): Flow<Int> {
        return pomodoroDao.getTodayCount(date)
    }

    fun getPomodoroRecordsByDate(date: Long): Flow<List<PomodoroRecord>> {
        return pomodoroDao.getRecordsByDate(date)
    }

    suspend fun insertPomodoroRecord(record: PomodoroRecord) {
        val now = System.currentTimeMillis()
        pomodoroDao.insertRecord(
            record.copy(
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        onLocalDataChanged?.invoke()
    }
}
