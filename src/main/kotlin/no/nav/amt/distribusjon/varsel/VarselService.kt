package no.nav.amt.distribusjon.varsel

import io.getunleash.Unleash
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.distribusjonskanal.skalDistribueresDigitalt
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.varsel.model.PAMELDING_TEKST
import no.nav.amt.distribusjon.varsel.model.PLACEHOLDER_BESKJED_TEKST
import no.nav.amt.distribusjon.varsel.model.Varsel
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
        val beskjedAktivLengde: Duration = Duration.ofDays(14)
    }

    fun handleHendelse(hendelse: Hendelse) {
        if (!unleash.isEnabled(Environment.VARSEL_TOGGLE)) {
            log.info("Varsler er togglet av, håndterer ikke hendelse for deltaker ${hendelse.deltaker.id}.")
            return
        }

        if (!hendelse.distribusjonskanal.skalDistribueresDigitalt()) return

        when (hendelse.payload) {
            is HendelseType.OpprettUtkast -> opprettPameldingsoppgave(hendelse)
            is HendelseType.AvbrytUtkast -> inaktiverVarsel(hendelse.deltaker, Varsel.Type.OPPGAVE)
            is HendelseType.InnbyggerGodkjennUtkast -> inaktiverVarsel(hendelse.deltaker, Varsel.Type.OPPGAVE)
            is HendelseType.NavGodkjennUtkast -> {
                inaktiverVarsel(hendelse.deltaker, Varsel.Type.OPPGAVE)
                opprettBeskjed(hendelse)
            }

            is HendelseType.EndreStartdato,
            is HendelseType.EndreSluttdato,
            is HendelseType.ForlengDeltakelse,
            is HendelseType.AvsluttDeltakelse,
            is HendelseType.IkkeAktuell,
            -> opprettBeskjed(hendelse)

            is HendelseType.EndreUtkast,
            is HendelseType.EndreDeltakelsesmengde,
            is HendelseType.EndreBakgrunnsinformasjon,
            is HendelseType.EndreInnhold,
            is HendelseType.EndreSluttarsak,
            -> {
                log.info("Oppretter ikke varsel for hendelse ${hendelse.payload::class} for deltaker ${hendelse.deltaker.id}")
            }
        }
    }

    private fun opprettBeskjed(hendelse: Hendelse) = opprettVarsel(
        hendelse = hendelse,
        type = Varsel.Type.BESKJED,
        aktivTil = nowUTC().plus(beskjedAktivLengde),
        tekst = PLACEHOLDER_BESKJED_TEKST,
    )

    private fun opprettPameldingsoppgave(hendelse: Hendelse) = opprettVarsel(
        hendelse = hendelse,
        type = Varsel.Type.OPPGAVE,
        aktivTil = null,
        tekst = PAMELDING_TEKST,
    )

    private fun opprettVarsel(
        hendelse: Hendelse,
        type: Varsel.Type,
        aktivTil: ZonedDateTime?,
        tekst: String,
    ) {
        repository.getByHendelseId(hendelse.id).onSuccess {
            log.info("Varsel for hendelse ${hendelse.id} er allerede opprettet. Oppretter ikke nytt varsel.")
            return
        }

        val forrigeVarsel = repository.getSisteVarsel(hendelse.deltaker.id, type).getOrNull()
        if (forrigeVarsel?.erAktiv == true) {
            log.info(
                "Forrige varsel for deltaker ${hendelse.deltaker.id} av type $type er fortsatt aktivt. " +
                    "Oppretter ikke nytt varsel.",
            )
            return
        }

        val varsel = Varsel(
            id = UUID.randomUUID(),
            type = type,
            hendelseId = hendelse.id,
            aktivFra = nowUTC(),
            aktivTil = aktivTil,
            deltakerId = hendelse.deltaker.id,
            personident = hendelse.deltaker.personident,
            tekst = tekst,
            skalVarsleEksternt = hendelse.skalVarslesEksternt(),
        )

        repository.upsert(varsel)

        log.info("Opprettet varsel for deltaker ${hendelse.deltaker.id} av type $type")

        when (type) {
            Varsel.Type.OPPGAVE -> producer.opprettOppgave(varsel)
            Varsel.Type.BESKJED -> producer.opprettBeskjed(varsel)
        }
    }

    private fun inaktiverVarsel(deltaker: HendelseDeltaker, type: Varsel.Type) {
        repository.getSisteVarsel(deltaker.id, type).onSuccess { varsel ->
            if (varsel.erAktiv) {
                repository.upsert(varsel.copy(aktivTil = nowUTC()))
                producer.inaktiver(varsel)
            }
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
    is HendelseType.InnbyggerGodkjennUtkast,
    -> false

    is HendelseType.ForlengDeltakelse,
    is HendelseType.EndreSluttdato,
    is HendelseType.IkkeAktuell,
    is HendelseType.NavGodkjennUtkast,
    is HendelseType.OpprettUtkast,
    is HendelseType.AvsluttDeltakelse,
    -> true
}
