package no.nav.amt.distribusjon.outbox

import org.postgresql.util.PGobject
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet

@Repository
class JdbcOutboxRepository(
    private val template: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) : OutboxRepository {
    private val rowMapper = RowMapper { rs: ResultSet, _: Int -> mapRow(rs) }

    private fun mapRow(rs: ResultSet) = OutboxRecord(
        id = OutboxRecordId(rs.getLong("id")),
        key = rs.getString("key"),
        value = objectMapper.readTree(rs.getString("value")),
        valueType = rs.getString("value_type"),
        topic = rs.getString("topic"),
        createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
        processedAt = rs.getTimestamp("processed_at")?.toLocalDateTime(),
        status = OutboxRecordStatus.valueOf(rs.getString("status")),
        retryCount = rs.getInt("retry_count"),
        retriedAt = rs.getTimestamp("retried_at")?.toLocalDateTime(),
        errorMessage = rs.getString("error_message"),
    )

    override fun insertNewRecord(record: NewOutboxRecord): OutboxRecord {
        val sql =
            """
            insert into outbox_record (
                key, value, value_type, topic, status, retry_count
            ) values (
                :key, :value, :value_type, :topic, :status, :retry_count
            )
            returning *
            """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("key", record.key)
            .addValue("value", toPGObject(record.value))
            .addValue("value_type", record.valueType)
            .addValue("topic", record.topic)
            .addValue("status", OutboxRecordStatus.PENDING.name)
            .addValue("retry_count", 0)

        return template.queryForObject(sql, params, rowMapper)
    }

    override fun findUnprocessedRecords(limit: Int): List<OutboxRecord> {
        val sql =
            """
            select * from outbox_record
            where status in (:pending, :failed)
            order by created_at
            limit :limit
            """.trimIndent()

        val params = mapOf(
            "pending" to OutboxRecordStatus.PENDING.name,
            "failed" to OutboxRecordStatus.FAILED.name,
            "limit" to limit,
        )

        return template.query(sql, params, rowMapper)
    }

    override fun markAsProcessed(recordId: OutboxRecordId): Int {
        val sql =
            """
            update outbox_record
            set processed_at = current_timestamp,
                status = :processed,
                modified_at = current_timestamp
            where id = :id
            """.trimIndent()

        val params = mapOf(
            "id" to recordId.value,
            "processed" to OutboxRecordStatus.PROCESSED.name,
        )

        return template.update(sql, params)
    }

    override fun markAsFailed(recordId: OutboxRecordId, errorMessage: String): Int {
        val sql =
            """
            update outbox_record
            set status = :failed,
                error_message = :error_message,
                retry_count = retry_count + 1,
                modified_at = current_timestamp,
                retried_at = current_timestamp
            where id = :id
            """.trimIndent()

        val params = mapOf(
            "id" to recordId.value,
            "error_message" to errorMessage,
            "failed" to OutboxRecordStatus.FAILED.name,
        )

        return template.update(sql, params)
    }

    override fun get(id: OutboxRecordId): OutboxRecord? {
        val sql = "select * from outbox_record where id = :id"
        val params = mapOf("id" to id.value)
        return template.query(sql, params, rowMapper).firstOrNull()
    }

    override fun getRecordsByTopicAndKey(topic: String, key: String): List<OutboxRecord> {
        val sql = "select * from outbox_record where topic = :topic and key = :key"
        val params = mapOf("topic" to topic, "key" to key)
        return template.query(sql, params, rowMapper)
    }

    private fun toPGObject(value: Any?): PGobject = PGobject().also {
        it.type = "json"
        it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
    }
}
