package no.nav.amt.distribusjon.varsel

import io.getunleash.Unleash
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.digitalbruker.DigitalBrukerService
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.varsel.model.Varsel
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class VarselService(
    private val repository: VarselRepository,
    private val producer: VarselProducer,
    private val unleash: Unleash,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handleHendelse(hendelse: Hendelse) {
        if (skalIkkeVarsles(hendelse)) return

        when (hendelse.payload) {
            is HendelseType.OpprettUtkast -> handleVarsel(Varsel.nyOppgave(hendelse), true)

            is HendelseType.AvbrytUtkast -> intaktiverOppgave(hendelse.deltaker)
            is HendelseType.InnbyggerGodkjennUtkast -> utforOppgave(hendelse.deltaker)
            is HendelseType.NavGodkjennUtkast -> {
                intaktiverOppgave(hendelse.deltaker)
                val beskjed = slaSammenMedVentendeVarsel(Varsel.nyBeskjed(hendelse))
                handleVarsel(beskjed, true)
            }

            is HendelseType.EndreBakgrunnsinformasjon,
            is HendelseType.EndreDeltakelsesmengde,
            is HendelseType.EndreInnhold,
            is HendelseType.EndreStartdato,
            is HendelseType.EndreSluttdato,
            is HendelseType.ForlengDeltakelse,
            is HendelseType.AvsluttDeltakelse,
            is HendelseType.IkkeAktuell,
            -> handleVarsel(slaSammenMedVentendeVarsel(Varsel.nyBeskjed(hendelse)))

            is HendelseType.EndreUtkast,
            is HendelseType.EndreSluttarsak,
            -> {
                log.info("Oppretter ikke varsel for hendelse ${hendelse.payload::class} for deltaker ${hendelse.deltaker.id}")
            }

            is HendelseType.DeltakerSistBesokt -> utforBeskjed(hendelse.deltaker, hendelse.payload.sistBesokt)
        }
    }

    private fun skalIkkeVarsles(hendelse: Hendelse): Boolean {
        return if (!unleash.isEnabled(Environment.VARSEL_TOGGLE)) {
            log.info("Varsler er togglet av, håndterer ikke hendelse for deltaker ${hendelse.deltaker.id}.")
            true
        } else if (repository.getByHendelseId(hendelse.id).isSuccess) {
            log.info("Varsel for hendelse ${hendelse.id} er allerede opprettet. Oppretter ikke nytt varsel.")
            true
        } else {
            !DigitalBrukerService.skalDistribueresDigitalt(hendelse.distribusjonskanal, hendelse.manuellOppfolging)
        }
    }

    private fun slaSammenMedVentendeVarsel(nyttVarsel: Varsel): Varsel {
        val varsel = repository.getVentendeVarsel(nyttVarsel.deltakerId).fold(
            onSuccess = { it.merge(nyttVarsel) },
            onFailure = { nyttVarsel },
        )

        return varsel
    }

    private fun handleVarsel(varsel: Varsel, sendUmiddelbart: Boolean = false) {
        if (varsel.kanRevarsles) {
            repository.stoppRevarsler(varsel.deltakerId)
        }

        if (sendUmiddelbart) {
            sendVarsel(varsel)
        } else {
            repository.upsert(varsel)
            log.info("Legger varsel ${varsel.id} klar til utsending ${varsel.aktivFra}")
        }
    }

    private fun sendVarsel(varsel: Varsel) {
        inaktiverTidligereBeskjed(varsel.deltakerId)

        val oppdatertVarsel = varsel.copy(aktivFra = nowUTC(), status = Varsel.Status.AKTIV)
        repository.upsert(oppdatertVarsel)

        when (varsel.type) {
            Varsel.Type.BESKJED -> producer.opprettBeskjed(oppdatertVarsel)
            Varsel.Type.OPPGAVE -> producer.opprettOppgave(oppdatertVarsel)
        }

        log.info("Sendte varsel ${varsel.id} for deltaker ${varsel.deltakerId}")
    }

    private fun ferdigstillSendtVarsel(varsel: Varsel, nyStatus: Varsel.Status) {
        if (varsel.erAktiv) {
            repository.upsert(varsel.copy(aktivTil = nowUTC(), status = nyStatus))
            producer.inaktiver(varsel)
            log.info("Endret status på varsel ${varsel.id} til $nyStatus for deltaker ${varsel.deltakerId}")
        }
    }

    private fun inaktiverTidligereBeskjed(deltakerId: UUID) {
        val varsel = repository.getAktivt(deltakerId).getOrNull()
        require(varsel?.type != Varsel.Type.OPPGAVE) {
            "Kan ikke inaktivere oppgave ${varsel?.id} som om den var en beskjed"
        }

        if (varsel?.erAktiv == true) {
            ferdigstillSendtVarsel(varsel, Varsel.Status.INAKTIVERT)
        }
    }

    private fun intaktiverOppgave(deltaker: HendelseDeltaker) {
        repository.getSisteVarsel(deltaker.id, Varsel.Type.OPPGAVE).onSuccess { varsel ->
            ferdigstillSendtVarsel(varsel, Varsel.Status.INAKTIVERT)
        }
    }

    private fun utforOppgave(deltaker: HendelseDeltaker) {
        repository.getSisteVarsel(deltaker.id, Varsel.Type.OPPGAVE).onSuccess { varsel ->
            ferdigstillSendtVarsel(varsel, Varsel.Status.UTFORT)
        }
    }

    private fun utforBeskjed(deltaker: HendelseDeltaker, sistBesokt: ZonedDateTime) {
        val sisteBeskjed = repository.getSisteVarsel(deltaker.id, Varsel.Type.BESKJED).getOrNull() ?: return

        if (erBesokTidligereEnnBeskjed(sistBesokt, sisteBeskjed)) {
            return
        }

        when (sisteBeskjed.status) {
            Varsel.Status.VENTER_PA_UTSENDELSE -> {
                val now = nowUTC()
                repository.upsert(sisteBeskjed.copy(aktivFra = now, aktivTil = now, status = Varsel.Status.UTFORT))
            }

            Varsel.Status.AKTIV -> {
                ferdigstillSendtVarsel(sisteBeskjed, Varsel.Status.UTFORT)
            }

            Varsel.Status.UTFORT,
            Varsel.Status.INAKTIVERT,
            -> {
            }
        }
    }

    private fun erBesokTidligereEnnBeskjed(sistBesokt: ZonedDateTime, sisteBeskjed: Varsel): Boolean {
        val besokForSendt = sistBesokt.withZoneSameInstant(ZoneOffset.UTC) < sisteBeskjed.aktivFra && sisteBeskjed.erAktiv
        val besokForIkkeSendt = sistBesokt.withZoneSameInstant(
            ZoneId.of("Z"),
        ) < sisteBeskjed.aktivFra.minusMinutes(Varsel.BESKJED_FORSINKELSE_MINUTTER) && sisteBeskjed.venterPaUsendelse

        return besokForSendt || besokForIkkeSendt
    }

    fun get(varselId: UUID) = repository.get(varselId)

    fun sendVentendeVarsler() {
        val varsler = repository.getVarslerSomSkalSendes()
        require(varsler.size == varsler.distinctBy { it.deltakerId }.size) {
            "Det finnes flere enn et ventende varsel for en eller flere deltakere"
        }
        varsler.forEach {
            sendVarsel(it)
        }
    }

    fun getVarslerSomSkalRevarsles() = repository.getVarslerSomSkalRevarsles()
}

fun nowUTC(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("Z"))

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
    is HendelseType.DeltakerSistBesokt,
    -> false

    is HendelseType.EndreSluttdato,
    is HendelseType.IkkeAktuell,
    is HendelseType.NavGodkjennUtkast,
    is HendelseType.OpprettUtkast,
    is HendelseType.AvsluttDeltakelse,
    -> true
}
