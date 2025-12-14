package no.nav.amt.distribusjon.varsel

import no.nav.amt.distribusjon.varsel.model.Varsel
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.util.UUID

@Service
class VarselRepository(
    private val template: NamedParameterJdbcTemplate,
) {
    fun upsert(varsel: Varsel): Int {
        val sql =
            """
            insert into varsel (
                id,
                type,
                hendelser,
                status,
                tekst,
                aktiv_fra,
                aktiv_til,
                deltaker_id,
                personident,
                er_eksternt_varsel,
                revarsel_for_varsel,
                revarsles
            )
            values (
                :id,
                :type,
                :hendelser,
                :status,
                :tekst,
                :aktiv_fra,
                :aktiv_til,
                :deltaker_id,
                :personident,
                :er_eksternt_varsel,
                :revarsel_for_varsel,
                :revarsles
            )
            on conflict (id) do update set
                type = :type,
                hendelser = :hendelser,
                status = :status,
                tekst = :tekst,
                aktiv_fra = :aktiv_fra,
                aktiv_til = :aktiv_til,
                er_eksternt_varsel = :er_eksternt_varsel,
                revarsel_for_varsel = :revarsel_for_varsel,
                revarsles = :revarsles,
                modified_at = current_timestamp
            """.trimIndent()

        val params = mapOf(
            "id" to varsel.id,
            "type" to varsel.type.name,
            "hendelser" to varsel.hendelser.toTypedArray(),
            "status" to varsel.status.name,
            "tekst" to varsel.tekst,
            "aktiv_fra" to varsel.aktivFra.toLocalDateTime(),
            "aktiv_til" to varsel.aktivTil?.toLocalDateTime(),
            "deltaker_id" to varsel.deltakerId,
            "personident" to varsel.personident,
            "er_eksternt_varsel" to varsel.erEksterntVarsel,
            "revarsel_for_varsel" to varsel.revarselForVarsel,
            "revarsles" to varsel.revarsles?.toLocalDateTime(),
        )

        return template.update(sql, params)
    }

    fun getSisteVarsel(deltakerId: UUID, type: Varsel.Type): Result<Varsel> {
        val sql =
            """
            select *
            from varsel
            where deltaker_id = :deltaker_id
              and type = :type
            order by aktiv_fra desc
            limit 1
            """.trimIndent()

        val result = template.query(
            sql,
            mapOf(
                "deltaker_id" to deltakerId,
                "type" to type.name,
            ),
            varselRowMapper,
        )

        return result
            .firstOrNull()
            ?.let { Result.success(it) }
            ?: Result.failure(
                NoSuchElementException("Fant ingen varsel av type $type for deltaker $deltakerId"),
            )
    }

    fun getAktiveEllerVentendeBeskjeder(deltakerId: UUID): List<Varsel> {
        val sql =
            """
            select *
            from varsel
            where deltaker_id = :deltaker_id
              and type = 'BESKJED'
              and status in ('VENTER_PA_UTSENDELSE', 'AKTIV')
            """.trimIndent()

        return template.query(
            sql,
            mapOf("deltaker_id" to deltakerId),
            varselRowMapper,
        )
    }

    fun getAktivt(deltakerId: UUID): Result<Varsel> {
        val sql =
            """
            select *
            from varsel
            where deltaker_id = :deltaker_id
              and status = 'AKTIV'
            """.trimIndent()

        val result = template.query(
            sql,
            mapOf("deltaker_id" to deltakerId),
            varselRowMapper,
        )

        return result
            .firstOrNull()
            ?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException())
    }

    fun get(id: UUID): Result<Varsel> {
        val sql =
            """
            select *
            from varsel
            where id = :id
            """.trimIndent()

        val result = template.query(
            sql,
            mapOf("id" to id),
            varselRowMapper,
        )

        return result
            .firstOrNull()
            ?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Fant ikke varsel $id"))
    }

    fun getByHendelseId(hendelseId: UUID): Result<Varsel> {
        val sql =
            """
            select *
            from varsel
            where hendelser @> ARRAY[:hendelse_id]::uuid[]
            """.trimIndent()

        val result = template.query(
            sql,
            mapOf("hendelse_id" to hendelseId),
            varselRowMapper,
        )

        return result
            .firstOrNull()
            ?.let { Result.success(it) }
            ?: Result.failure(
                NoSuchElementException("Fant ikke varsel for hendelse $hendelseId"),
            )
    }

    fun getVentendeVarsel(deltakerId: UUID): Result<Varsel> {
        val sql =
            """
            select *
            from varsel
            where deltaker_id = :deltaker_id
              and status = 'VENTER_PA_UTSENDELSE'
            """.trimIndent()

        val result = template.query(
            sql,
            mapOf("deltaker_id" to deltakerId),
            varselRowMapper,
        )

        return result
            .firstOrNull()
            ?.let { Result.success(it) }
            ?: Result.failure(
                NoSuchElementException("Fant ikke varsel som ikke var sendt for deltaker $deltakerId"),
            )
    }

    fun getVarslerSomSkalSendes(): List<Varsel> {
        val sql =
            """
            select *
            from varsel
            where status = 'VENTER_PA_UTSENDELSE'
              and aktiv_fra at time zone 'UTC' < current_timestamp at time zone 'UTC'
            """.trimIndent()

        return template.query(sql, varselRowMapper)
    }

    fun getVarslerSomSkalRevarsles(): List<Varsel> {
        val sql =
            """
            select *
            from varsel
            where revarsles < current_timestamp
            """.trimIndent()

        return template.query(sql, varselRowMapper)
    }

    fun stoppRevarsler(deltakerId: UUID): Int {
        val sql =
            """
            update varsel
            set revarsles = null
            where deltaker_id = :deltaker_id
              and revarsles is not null
            """.trimIndent()

        return template.update(sql, mapOf("deltaker_id" to deltakerId))
    }

    companion object {
        private val varselRowMapper = RowMapper { rs, _ ->
            Varsel(
                id = UUID.fromString(rs.getString("id")),
                type = Varsel.Type.valueOf(rs.getString("type")),
                hendelser = rs
                    .getArray("hendelser")
                    .array
                    .let { it as Array<UUID> }
                    .toList(),
                status = Varsel.Status.valueOf(rs.getString("status")),
                aktivFra = rs
                    .getTimestamp("aktiv_fra")
                    .toInstant()
                    .atZone(ZoneId.of("Z")),
                aktivTil = rs
                    .getTimestamp("aktiv_til")
                    ?.toInstant()
                    ?.atZone(ZoneId.of("Z")),
                deltakerId = UUID.fromString(rs.getString("deltaker_id")),
                personident = rs.getString("personident"),
                tekst = rs.getString("tekst"),
                erEksterntVarsel = rs.getBoolean("er_eksternt_varsel"),
                revarselForVarsel = rs.getString("revarsel_for_varsel")?.let(UUID::fromString),
                revarsles = rs
                    .getTimestamp("revarsles")
                    ?.toInstant()
                    ?.atZone(ZoneId.of("Z")),
            )
        }
    }
}
