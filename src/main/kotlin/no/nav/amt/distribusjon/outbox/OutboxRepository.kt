package no.nav.amt.distribusjon.outbox

interface OutboxRepository {
    fun insertNewRecord(record: NewOutboxRecord): OutboxRecord

    fun findUnprocessedRecords(limit: Int): List<OutboxRecord>

    fun markAsProcessed(recordId: OutboxRecordId): Int

    fun markAsFailed(recordId: OutboxRecordId, errorMessage: String): Int

    fun get(id: OutboxRecordId): OutboxRecord?

    fun getRecordsByTopicAndKey(topic: String, key: String): List<OutboxRecord>
}
