package no.nav.amt.distribusjon.outbox

import no.nav.amt.distribusjon.outbox.metrics.OutboxMeter
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

/**
 * Provides a high-level API for interacting with the outbox.
 * This service simplifies the creation and management of outbox events,
 * abstracting away the underlying repository details.
 */
@Service
class OutboxService(
    private val meter: OutboxMeter,
    private val repository: OutboxRepository,
    private val objectMapper: ObjectMapper,
) {
    /**
     * Creates a new outbox event and persists it to the database.
     *
     * @param K The type of the key being sent.
     * @param V The type of the value being sent.
     * @param key The key of the event, typically a unique identifier for the entity.
     * @param value The value (payload) of the event.
     * @param topic The Kafka topic to which the event will be published.
     * @return The created [OutboxRecord].
     */
    fun <K : Any, V : Any> insertRecord(
        key: K,
        value: V,
        topic: String,
    ): OutboxRecord {
        val event = NewOutboxRecord(
            key = key.toString(),
            valueType = value::class.java.simpleName,
            topic = topic,
            value = objectMapper.readTree(objectMapper.writeValueAsString(value)),
        )
        return repository.insertNewRecord(event).also { meter.incrementNewRecords(topic) }
    }

    /**
     * Finds unprocessed outbox events (with status PENDING or FAILED).
     *
     * @param limit The maximum number of events to return.
     * @return A list of unprocessed [OutboxRecord]s.
     */
    fun findUnprocessedRecords(limit: Int): List<OutboxRecord> = repository.findUnprocessedRecords(limit)

    /**
     * Marks an outbox record as processed.
     *
     * @param record The record to mark as processed.
     */
    fun markAsProcessed(record: OutboxRecord) {
        repository.markAsProcessed(record.id)
        meter.incrementProcessedRecords(record.topic, OutboxRecordStatus.PROCESSED)
    }

    /**
     * Marks an outbox record as failed.
     *
     * @param record The failed record.
     * @param errorMessage A message describing the reason for the failure.
     */
    fun markAsFailed(record: OutboxRecord, errorMessage: String) {
        repository.markAsFailed(record.id, errorMessage)
        meter.incrementProcessedRecords(record.topic, OutboxRecordStatus.FAILED)
    }

    fun getRecordsByTopicAndKey(topic: String, key: String): List<OutboxRecord> = repository.getRecordsByTopicAndKey(topic, key)
}
