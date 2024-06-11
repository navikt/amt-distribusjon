package no.nav.amt.distribusjon.varsel.model

import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.varsel.nowUTC
import no.nav.amt.distribusjon.varsel.skalVarslesEksternt
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.EksternVarslingBestilling
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID

data class Varsel(
    val id: UUID,
    val type: Type,
    val hendelser: List<UUID>,
    val status: Status,
    val erEksterntVarsel: Boolean,
    val revarselForVarsel: UUID?,
    val aktivFra: ZonedDateTime,
    val aktivTil: ZonedDateTime?,
    val deltakerId: UUID,
    val personident: String,
    val tekst: String,
    val sendt: ZonedDateTime?,
) {
    companion object {
        const val BESKJED_FORSINKELSE_MINUTTER = 30L

        val beskjedAktivLengde: Duration = Duration.ofDays(21).plusMinutes(BESKJED_FORSINKELSE_MINUTTER)

        fun nyOppgave(hendelse: Hendelse) = Varsel(
            id = UUID.randomUUID(),
            type = Type.OPPGAVE,
            hendelser = listOf(hendelse.id),
            status = Status.VENTER_PA_UTSENDELSE,
            erEksterntVarsel = true,
            aktivFra = nowUTC(),
            aktivTil = null,
            deltakerId = hendelse.deltaker.id,
            personident = hendelse.deltaker.personident,
            tekst = oppgaveTekst(hendelse),
            revarselForVarsel = null,
            sendt = null,
        )

        fun nyBeskjed(hendelse: Hendelse) = Varsel(
            id = UUID.randomUUID(),
            type = Type.BESKJED,
            hendelser = listOf(hendelse.id),
            status = Status.VENTER_PA_UTSENDELSE,
            erEksterntVarsel = hendelse.skalVarslesEksternt(),
            aktivFra = nesteUtsendingstidspunkt(),
            aktivTil = nesteUtsendingstidspunkt().plus(beskjedAktivLengde),
            deltakerId = hendelse.deltaker.id,
            personident = hendelse.deltaker.personident,
            tekst = beskjedTekst(hendelse),
            revarselForVarsel = null,
            sendt = null,
        )

        fun revarsel(varsel: Varsel): Varsel {
            require(varsel.skalRevarsles) {
                "Kan ikke lage et revarsel for varsel ${varsel.id}"
            }

            return varsel.copy(
                id = UUID.randomUUID(),
                status = Status.VENTER_PA_UTSENDELSE,
                aktivFra = nesteUtsendingstidspunkt(),
                aktivTil = nesteUtsendingstidspunkt().plus(beskjedAktivLengde),
                revarselForVarsel = varsel.id,
                sendt = null,
            )
        }

        fun nesteUtsendingstidspunkt() = nowUTC().plusMinutes(BESKJED_FORSINKELSE_MINUTTER)
    }

    val skalRevarsles: Boolean get() = type == Type.OPPGAVE && erEksterntVarsel && revarselForVarsel != null

    val erAktiv: Boolean get() = status == Status.AKTIV

    val venter: Boolean get() = status == Status.VENTER_PA_UTSENDELSE

    val erSendt: Boolean get() = sendt != null

    val kanRevarsles: Boolean
        get() {
            return status != Status.UTFORT &&
                erEksterntVarsel &&
                revarselForVarsel == null
        }

    fun merge(varsel: Varsel): Varsel {
        require(this.type == Type.BESKJED && status == Status.VENTER_PA_UTSENDELSE) {
            error("Kan ikke slå sammen andre varsler enn beskjeder som ikke er sendt")
        }

        return this.copy(
            hendelser = this.hendelser.plus(varsel.hendelser),
            erEksterntVarsel = this.erEksterntVarsel || varsel.erEksterntVarsel,
            aktivFra = nesteUtsendingstidspunkt(),
            aktivTil = nesteUtsendingstidspunkt().plus(beskjedAktivLengde),
        )
    }

    enum class Type {
        BESKJED,
        OPPGAVE,
    }

    enum class Status {
        VENTER_PA_UTSENDELSE,
        AKTIV,
        UTFORT,
        INAKTIVERT,
    }

    fun toOppgaveDto() = VarselActionBuilder.opprett {
        varselConfig(Varseltype.Oppgave)
    }

    fun toBeskjedDto() = VarselActionBuilder.opprett {
        varselConfig(Varseltype.Beskjed)
    }

    fun toInaktiverDto() = VarselActionBuilder.inaktiver {
        varselId = this@Varsel.id.toString()
        produsent = produsent()
    }

    private fun VarselActionBuilder.OpprettVarselInstance.varselConfig(varseltype: Varseltype) {
        varselId = this@Varsel.id.toString()
        type = varseltype
        sensitivitet = Sensitivitet.High
        ident = personident
        tekster += Tekst(
            spraakkode = "nb",
            tekst = this@Varsel.tekst,
            default = true,
        )
        aktivFremTil = aktivTil
        link = innbyggerDeltakerUrl(deltakerId)
        produsent = produsent()

        if (erEksterntVarsel) {
            eksternVarsling = EksternVarslingBestilling(prefererteKanaler = getPrefererteKanaler(varseltype))
        }
    }

    private fun getPrefererteKanaler(varseltype: Varseltype): List<EksternKanal> {
        return if (varseltype == Varseltype.Oppgave) {
            listOf(EksternKanal.SMS)
        } else {
            listOf(EksternKanal.EPOST)
        }
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
