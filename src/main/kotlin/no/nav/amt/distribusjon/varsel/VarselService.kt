package no.nav.amt.distribusjon.varsel

import io.getunleash.Unleash
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.digitalbruker.DigitalBrukerService
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.varsel.model.Varsel
import no.nav.amt.distribusjon.varsel.model.beskjedTekst
import no.nav.amt.distribusjon.varsel.model.oppgaveTekst
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class VarselService(
    private val repository: VarselRepository,
    private val producer: VarselProducer,
    private val unleash: Unleash,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        val beskjedAktivLengde: Duration = Duration.ofDays(21).plusMinutes(30)
    }

    fun handleHendelse(hendelse: Hendelse) {
        if (!unleash.isEnabled(Environment.VARSEL_TOGGLE)) {
            log.info("Varsler er togglet av, håndterer ikke hendelse for deltaker ${hendelse.deltaker.id}.")
            return
        }

        if (!DigitalBrukerService.skalDistribueresDigitalt(hendelse.distribusjonskanal, hendelse.manuellOppfolging)) return

        when (hendelse.payload) {
            is HendelseType.OpprettUtkast -> opprettPameldingsoppgave(hendelse)
                .onSuccess { sendVarsel(it) }
            is HendelseType.AvbrytUtkast -> inaktiverOppgave(hendelse.deltaker)
            is HendelseType.InnbyggerGodkjennUtkast -> inaktiverOppgave(hendelse.deltaker)
            is HendelseType.NavGodkjennUtkast -> {
                inaktiverOppgave(hendelse.deltaker)
                opprettBeskjed(hendelse)
                    .onSuccess { sendVarsel(it) }
            }

            is HendelseType.EndreBakgrunnsinformasjon,
            is HendelseType.EndreDeltakelsesmengde,
            is HendelseType.EndreInnhold,
            is HendelseType.EndreStartdato,
            is HendelseType.EndreSluttdato,
            is HendelseType.ForlengDeltakelse,
            is HendelseType.AvsluttDeltakelse,
            is HendelseType.IkkeAktuell,
            -> opprettBeskjed(hendelse)

            is HendelseType.EndreUtkast,
            is HendelseType.EndreSluttarsak,
            -> {
                log.info("Oppretter ikke varsel for hendelse ${hendelse.payload::class} for deltaker ${hendelse.deltaker.id}")
            }
        }
    }

    private fun opprettBeskjed(hendelse: Hendelse) = opprettVarsel(
        hendelse = hendelse,
        type = Varsel.Type.BESKJED,
    )

    private fun opprettPameldingsoppgave(hendelse: Hendelse) = opprettVarsel(
        hendelse = hendelse,
        type = Varsel.Type.OPPGAVE,
    )

    private fun sendVarsel(varsel: Varsel) {
        val aktivtVarsel = repository.getAktivt(varsel.deltakerId).getOrNull()

        if (aktivtVarsel?.erAktiv == true) {
            inaktiverVarsel(aktivtVarsel)
        }

        when (varsel.type) {
            Varsel.Type.BESKJED -> producer.opprettBeskjed(varsel)
            Varsel.Type.OPPGAVE -> producer.opprettOppgave(varsel)
        }

        repository.upsert(varsel.copy(aktivFra = nowUTC(), erSendt = true))
        log.info("Sendte varsel ${varsel.id} for deltaker ${varsel.deltakerId}")
    }

    private fun opprettVarsel(hendelse: Hendelse, type: Varsel.Type): Result<Varsel> {
        repository.getByHendelseId(hendelse.id).onSuccess {
            val msg = "Varsel for hendelse ${hendelse.id} er allerede opprettet. Oppretter ikke nytt varsel."
            log.info(msg)
            return Result.failure(IllegalStateException(msg))
        }

        val forrigeVarsel = repository.getIkkeSendt(hendelse.deltaker.id).getOrNull()

        val varsel = if (forrigeVarsel != null) {
            val nyType = if (type == Varsel.Type.OPPGAVE) Varsel.Type.OPPGAVE else forrigeVarsel.type
            val eksternVarsling = forrigeVarsel.skalVarsleEksternt || hendelse.skalVarslesEksternt()

            forrigeVarsel.copy(
                type = nyType,
                hendelser = forrigeVarsel.hendelser.plus(hendelse.id),
                tekst = varselTekst(nyType, hendelse),
                skalVarsleEksternt = eksternVarsling,
                aktivFra = nesteUtsendingstidspunkt(),
                aktivTil = aktivTilTidspunkt(nyType),
            )
        } else {
            Varsel(
                id = UUID.randomUUID(),
                type = type,
                hendelser = listOf(hendelse.id),
                aktivFra = nesteUtsendingstidspunkt(),
                aktivTil = aktivTilTidspunkt(type),
                deltakerId = hendelse.deltaker.id,
                personident = hendelse.deltaker.personident,
                tekst = varselTekst(type, hendelse),
                skalVarsleEksternt = hendelse.skalVarslesEksternt(),
                erSendt = false,
            )
        }

        repository.upsert(varsel)
        log.info("Lagret varsel ${varsel.id} for hendelse ${hendelse.id}")
        return Result.success(varsel)
    }

    private fun varselTekst(type: Varsel.Type, hendelse: Hendelse) = when (type) {
        Varsel.Type.BESKJED -> beskjedTekst(hendelse)
        Varsel.Type.OPPGAVE -> oppgaveTekst(hendelse)
    }

    private fun aktivTilTidspunkt(type: Varsel.Type) = when (type) {
        Varsel.Type.BESKJED -> nowUTC().plus(beskjedAktivLengde)
        Varsel.Type.OPPGAVE -> null
    }

    private fun inaktiverOppgave(deltaker: HendelseDeltaker) {
        repository.getSisteVarsel(deltaker.id, Varsel.Type.OPPGAVE).onSuccess { varsel ->
            inaktiverVarsel(varsel)
        }
    }

    private fun inaktiverVarsel(varsel: Varsel) {
        if (varsel.erAktiv) {
            repository.upsert(varsel.copy(aktivTil = nowUTC()))
            producer.inaktiver(varsel)
            log.info("Inaktiverte varsel ${varsel.id} for deltaker ${varsel.deltakerId}")
        }
    }

    fun inaktiverBeskjed(varsel: Varsel) {
        require(varsel.type == Varsel.Type.BESKJED) {
            "Varsel er ikke av type ${Varsel.Type.BESKJED}, kan ikke inaktivere beskjed"
        }
        log.info("Inaktiverer beskjed ${varsel.id}")
        repository.upsert(varsel.copy(aktivTil = nowUTC()))
    }

    fun get(varselId: UUID) = repository.get(varselId)

    fun sendVentendeVarsler() {
        val varsler = repository.getVentende()
        require(varsler.size == varsler.distinctBy { it.deltakerId }.size) {
            "Det finnes flere enn et ventende varsel for en eller flere deltakere"
        }
        varsler.forEach {
            sendVarsel(it)
        }
    }
}

fun nowUTC(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("Z"))

fun nesteUtsendingstidspunkt() = nowUTC().plusMinutes(30)

fun Hendelse.skalVarslesEksternt() = when (payload) {
    is HendelseType.AvbrytUtkast,
    is HendelseType.EndreBakgrunnsinformasjon,
    is HendelseType.EndreDeltakelsesmengde,
    is HendelseType.EndreInnhold,
    is HendelseType.EndreSluttarsak,
    is HendelseType.EndreStartdato,
    is HendelseType.EndreUtkast,
    is HendelseType.ForlengDeltakelse,
    is HendelseType.InnbyggerGodkjennUtkast,
    -> false

    is HendelseType.EndreSluttdato,
    is HendelseType.IkkeAktuell,
    is HendelseType.NavGodkjennUtkast,
    is HendelseType.OpprettUtkast,
    is HendelseType.AvsluttDeltakelse,
    -> true
}
