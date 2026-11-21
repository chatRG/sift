package app.clearsms.testing

import app.clearsms.data.db.ReminderDao
import app.clearsms.data.db.ReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [ReminderDao] for tests that touch a collaborator holding one
 * (work scheduling, settings screens) without caring about reminder rows.
 * Behaves like the real queries over a list held in [rows].
 */
class FakeReminderDao(
    initial: List<ReminderEntity> = emptyList(),
) : ReminderDao {
    val rows = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<ReminderEntity>> = rows

    override fun observeUpcoming(nowMs: Long): Flow<List<ReminderEntity>> =
        rows.map { all -> all.filter { it.dismissedAt == null && it.dueDate != null && it.dueDate >= nowMs } }

    override suspend fun findByRawSmsId(rawSmsId: Long): ReminderEntity? = rows.value.firstOrNull { it.rawSmsId == rawSmsId }

    override suspend fun getAll(): List<ReminderEntity> = rows.value

    override suspend fun insert(reminder: ReminderEntity): Long {
        rows.value = rows.value + reminder
        return reminder.id
    }

    override suspend fun insertAll(reminders: List<ReminderEntity>) {
        rows.value = rows.value + reminders
    }

    override suspend fun setDismissed(
        ids: List<Long>,
        dismissedAt: Long?,
    ) {
        rows.value = rows.value.map { if (it.id in ids) it.copy(dismissedAt = dismissedAt) else it }
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        rows.value = rows.value.filterNot { it.id in ids }
    }

    override suspend fun deleteByRawSmsId(rawSmsId: Long) {
        rows.value = rows.value.filterNot { it.rawSmsId == rawSmsId }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}
