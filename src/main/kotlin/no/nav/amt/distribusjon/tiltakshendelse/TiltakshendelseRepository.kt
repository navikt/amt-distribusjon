package no.nav.amt.distribusjon.tiltakshendelse

import no.nav.amt.distribusjon.tiltakshendelse.model.Tiltakshendelse
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TiltakshendelseRepository(
    private val template: NamedParameterJdbcTemplate,
) {
    fun upsert(tiltakshendelse: Tiltakshendelse): Int {
        val sql =
            """
            insert into tiltakshendelse (
                id,
                type,
                deltaker_id,
                forslag_id,
                hendelser,
                personident,
                aktiv,
                tekst,
                tiltakskode
            )
            values (
                :id,
                :type,
                :deltaker_id,
                :forslag_id,
                :hendelser,
                :personident,
                :aktiv,
                :tekst,
                :tiltakskode
            )
            on conflict (id) do update set
                hendelser = :hendelser,
                personident = :personident,
                aktiv = :aktiv,
                tekst = :tekst,
                modified_at = current_timestamp
            """.trimIndent()

        val params = mapOf(
            "id" to tiltakshendelse.id,
            "type" to tiltakshendelse.type.name,
            "deltaker_id" to tiltakshendelse.deltakerId,
            "forslag_id" to tiltakshendelse.forslagId,
            "hendelser" to tiltakshendelse.hendelser.toTypedArray(),
            "personident" to tiltakshendelse.personident,
            "aktiv" to tiltakshendelse.aktiv,
            "tekst" to tiltakshendelse.tekst,
            "tiltakskode" to tiltakshendelse.tiltakskode.name,
        )

        return template.update(sql, params)
    }

    fun get(id: UUID): Result<Tiltakshendelse> {
        val sql =
            """
            select *
            from tiltakshendelse
            where id = :id
            """.trimIndent()

        val result = template.query(
            sql,
            mapOf("id" to id),
            tiltakshendelseRowMapper,
        )

        return result
            .firstOrNull()
            ?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Fant ikke tiltakshendelse $id"))
    }

    fun getHendelse(deltakerId: UUID, hendelseType: Tiltakshendelse.Type): Result<Tiltakshendelse> {
        val sql =
            """
            select *
            from tiltakshendelse
            where deltaker_id = :deltaker_id
              and type = :type
            """.trimIndent()

        val params = mapOf(
            "deltaker_id" to deltakerId,
            "type" to hendelseType.name,
        )

        val result = template.query(sql, params, tiltakshendelseRowMapper)

        return result
            .firstOrNull()
            ?.let { Result.success(it) }
            ?: Result.failure(
                NoSuchElementException("Fant ikke tiltakshendelse for deltaker $deltakerId"),
            )
    }

    fun getForslagHendelse(forslagId: UUID): Result<Tiltakshendelse> {
        val sql =
            """
            select *
            from tiltakshendelse
            where forslag_id = :forslag_id
            """.trimIndent()

        val result = template.query(
            sql,
            mapOf("forslag_id" to forslagId),
            tiltakshendelseRowMapper,
        )

        return result
            .firstOrNull()
            ?.let { Result.success(it) }
            ?: Result.failure(
                NoSuchElementException("Fant ikke tiltakshendelse med forslagId $forslagId"),
            )
    }

    fun getByHendelseId(hendelseId: UUID): Result<Tiltakshendelse> {
        val sql =
            """
            select *
            from tiltakshendelse
            where hendelser @> ARRAY[:hendelse_id]::uuid[]
            """.trimIndent()

        val result = template.query(
            sql,
            mapOf("hendelse_id" to hendelseId),
            tiltakshendelseRowMapper,
        )

        return result
            .firstOrNull()
            ?.let { Result.success(it) }
            ?: Result.failure(
                NoSuchElementException("Fant ikke tiltakshendelse for hendelse $hendelseId"),
            )
    }

    companion object {
        private val tiltakshendelseRowMapper = RowMapper { rs, _ ->
            Tiltakshendelse(
                id = UUID.fromString(rs.getString("id")),
                type = Tiltakshendelse.Type.valueOf(rs.getString("type")),
                deltakerId = UUID.fromString(rs.getString("deltaker_id")),
                forslagId = rs.getString("forslag_id")?.let(UUID::fromString),
                hendelser = rs
                    .getArray("hendelser")
                    .array
                    .let { it as Array<UUID> }
                    .toList(),
                personident = rs.getString("personident"),
                aktiv = rs.getBoolean("aktiv"),
                tekst = rs.getString("tekst"),
                tiltakskode = Tiltakskode.valueOf(rs.getString("tiltakskode")),
                opprettet = rs.getTimestamp("created_at").toLocalDateTime(),
            )
        }
    }
}
