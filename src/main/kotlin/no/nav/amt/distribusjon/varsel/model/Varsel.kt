package no.nav.amt.distribusjon.varsel.model

import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.varsel.nowUTC
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.EksternVarslingBestilling
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.time.ZonedDateTime
import java.util.UUID

data class Varsel(
    val id: UUID,
    val type: Type,
    val aktivFra: ZonedDateTime,
    val aktivTil: ZonedDateTime?,
    val deltakerId: UUID,
    val personident: String,
    val tekst: String,
) {
    val erAktiv: Boolean get() {
        val now = nowUTC()
        return aktivFra <= now && (aktivTil == null || aktivTil >= now)
    }

    enum class Type {
        PAMELDING, // opprett-utkast, avbryt-utkast, godkjenn-utkast
        OPPSTART, // legg-til-oppstartsdato, endre-oppstartsdato
        AVSLUTNING, // avslutt-deltakelse, ikke-aktuell, forleng-deltakelse

        // deltakelsesmengde?
    }

    fun toOppgaveDto(skalVarslesEksternt: Boolean) = VarselActionBuilder.opprett {
        type = Varseltype.Oppgave
        link = innbyggerDeltakerUrl(deltakerId)

        commonVarselConfig()

        if (skalVarslesEksternt) {
            eksternVarsling = EksternVarslingBestilling(prefererteKanaler = listOf(EksternKanal.SMS))
        }
    }

    fun toBeskjedDto(skalVarslesEksternt: Boolean) = VarselActionBuilder.opprett {
        type = Varseltype.Beskjed
        link = innbyggerDeltakerUrl(deltakerId) // skal beskjeder ha link?
        commonVarselConfig()

        if (skalVarslesEksternt) {
            eksternVarsling = EksternVarslingBestilling(prefererteKanaler = listOf(EksternKanal.SMS))
        }
    }

    fun toInaktiverDto() = VarselActionBuilder.inaktiver {
        varselId = this@Varsel.id.toString()
        produsent = produsent()
    }

    private fun VarselActionBuilder.OpprettVarselInstance.commonVarselConfig() {
        varselId = this@Varsel.id.toString()
        sensitivitet = Sensitivitet.High
        ident = personident
        tekster += Tekst(
            spraakkode = "nb",
            tekst = this@Varsel.tekst,
            default = true,
        )
        aktivFremTil = aktivTil
        produsent = produsent()
    }

    private fun produsent() = Produsent(
        cluster = Environment.cluster,
        namespace = Environment.namespace,
        appnavn = Environment.appName,
    )
}

fun innbyggerDeltakerUrl(deltakerId: UUID): String {
    return if (Environment.isProd()) {
        "https://www.nav.no/arbeidsmarkedstiltak/$deltakerId"
    } else {
        "https://amt.intern.dev.nav.no/arbeidsmarkedstiltak/$deltakerId"
    }
}
