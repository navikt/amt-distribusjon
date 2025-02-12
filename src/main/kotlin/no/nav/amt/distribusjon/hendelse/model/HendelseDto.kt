package no.nav.amt.distribusjon.hendelse.model

import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.lib.models.hendelse.HendelseDeltaker
import java.time.LocalDateTime
import java.util.UUID

data class HendelseDto(
    val id: UUID,
    val opprettet: LocalDateTime,
    val deltaker: HendelseDeltaker,
    val ansvarlig: HendelseAnsvarlig,
    val payload: HendelseType,
) {
    fun toModel(distribusjonskanal: Distribusjonskanal, manuellOppfolging: Boolean) = Hendelse(
        id,
        opprettet,
        deltaker,
        ansvarlig,
        payload,
        distribusjonskanal,
        manuellOppfolging,
    )
}
