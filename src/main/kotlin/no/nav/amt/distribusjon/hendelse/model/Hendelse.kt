package no.nav.amt.distribusjon.hendelse.model

import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import java.time.LocalDateTime
import java.util.UUID

data class Hendelse(
    val id: UUID,
    val opprettet: LocalDateTime,
    val deltaker: HendelseDeltaker,
    val ansvarlig: HendelseAnsvarlig,
    val payload: HendelseType,
    val distribusjonskanal: Distribusjonskanal,
) {
    fun erEndringsVedtakSomSkalJournalfores(): Boolean {
        return when (payload) {
            is HendelseType.AvsluttDeltakelse,
            is HendelseType.EndreDeltakelsesmengde,
            is HendelseType.EndreSluttdato,
            is HendelseType.EndreStartdato,
            is HendelseType.ForlengDeltakelse,
            is HendelseType.IkkeAktuell,
            is HendelseType.EndreInnhold,
            is HendelseType.EndreBakgrunnsinformasjon,
            -> true

            is HendelseType.InnbyggerGodkjennUtkast,
            is HendelseType.NavGodkjennUtkast,
            is HendelseType.EndreSluttarsak,
            is HendelseType.EndreUtkast,
            is HendelseType.OpprettUtkast,
            is HendelseType.AvbrytUtkast,
            -> false
        }
    }
}
