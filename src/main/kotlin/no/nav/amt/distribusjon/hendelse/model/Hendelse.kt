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
    val manuellOppfolging: Boolean,
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
            is HendelseType.LeggTilOppstartsdato,
            -> true

            is HendelseType.InnbyggerGodkjennUtkast,
            is HendelseType.NavGodkjennUtkast,
            is HendelseType.EndreSluttarsak,
            is HendelseType.EndreUtkast,
            is HendelseType.OpprettUtkast,
            is HendelseType.AvbrytUtkast,
            is HendelseType.DeltakerSistBesokt,
            is HendelseType.ReaktiverDeltakelse,
            -> false
        }
    }

    fun getBegrunnelseForHovedvedtak(): String? {
        return when (payload) {
            is HendelseType.AvsluttDeltakelse,
            is HendelseType.EndreDeltakelsesmengde,
            is HendelseType.EndreSluttdato,
            is HendelseType.EndreStartdato,
            is HendelseType.ForlengDeltakelse,
            is HendelseType.IkkeAktuell,
            is HendelseType.EndreInnhold,
            is HendelseType.EndreBakgrunnsinformasjon,
            is HendelseType.InnbyggerGodkjennUtkast,
            is HendelseType.NavGodkjennUtkast,
            is HendelseType.EndreSluttarsak,
            is HendelseType.EndreUtkast,
            is HendelseType.OpprettUtkast,
            is HendelseType.AvbrytUtkast,
            is HendelseType.DeltakerSistBesokt,
            is HendelseType.LeggTilOppstartsdato,
            -> null
            is HendelseType.ReaktiverDeltakelse,
            -> payload.begrunnelseFraNav
        }
    }

    fun tillattEndringUtenAktivOppfolgingsperiode() = when (payload) {
        is HendelseType.AvsluttDeltakelse,
        is HendelseType.EndreSluttarsak,
        is HendelseType.EndreSluttdato,
        is HendelseType.IkkeAktuell,
        -> true

        is HendelseType.InnbyggerGodkjennUtkast,
        is HendelseType.NavGodkjennUtkast,
        is HendelseType.EndreUtkast,
        is HendelseType.OpprettUtkast,
        is HendelseType.AvbrytUtkast,
        is HendelseType.DeltakerSistBesokt,
        is HendelseType.ReaktiverDeltakelse,
        is HendelseType.EndreDeltakelsesmengde,
        is HendelseType.EndreStartdato,
        is HendelseType.ForlengDeltakelse,
        is HendelseType.EndreInnhold,
        is HendelseType.EndreBakgrunnsinformasjon,
        is HendelseType.LeggTilOppstartsdato,
        -> false
    }
}
