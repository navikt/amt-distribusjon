package no.nav.amt.distribusjon.varsel

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
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        val beskjedAktivLengde = Duration.ofDays(14)
    }

    fun handleHendelse(hendelse: Hendelse) {
        when (hendelse.payload) {
            is HendelseType.OpprettUtkast -> opprettPameldingsoppgave(hendelse)
            is HendelseType.AvbrytUtkast -> inaktiverVarsel(hendelse.deltaker, Varsel.Type.OPPGAVE)
            is HendelseType.InnbyggerGodkjennUtkast -> inaktiverVarsel(hendelse.deltaker, Varsel.Type.OPPGAVE)
            is HendelseType.NavGodkjennUtkast -> {
                inaktiverVarsel(hendelse.deltaker, Varsel.Type.OPPGAVE)
                opprettBeskjed(hendelse, true)
            }

            is HendelseType.EndreSluttdato,
            is HendelseType.ForlengDeltakelse,
            is HendelseType.AvsluttDeltakelse,
            is HendelseType.IkkeAktuell,
            -> opprettBeskjed(hendelse, true)

            is HendelseType.EndreStartdato,
            -> opprettBeskjed(hendelse, false)

            is HendelseType.EndreDeltakelsesmengde,
            is HendelseType.EndreBakgrunnsinformasjon,
            is HendelseType.EndreInnhold,
            is HendelseType.EndreSluttarsak,
            -> {
                log.info("Oppretter ikke varsel for hendelse ${hendelse.payload::class} for deltaker ${hendelse.deltaker.id}")
            }
        }
    }

    private fun opprettBeskjed(hendelse: Hendelse, skalVarsleEksternt: Boolean) = opprettVarsel(
        hendelse = hendelse,
        type = Varsel.Type.BESKJED,
        aktivTil = nowUTC().plus(beskjedAktivLengde),
        tekst = PLACEHOLDER_BESKJED_TEKST,
        skalVarsleEksternt = skalVarsleEksternt,
    )

    private fun opprettPameldingsoppgave(hendelse: Hendelse) = opprettVarsel(
        hendelse = hendelse,
        type = Varsel.Type.OPPGAVE,
        aktivTil = null,
        tekst = PAMELDING_TEKST,
        skalVarsleEksternt = true,
    )

    private fun opprettVarsel(
        hendelse: Hendelse,
        type: Varsel.Type,
        aktivTil: ZonedDateTime?,
        tekst: String,
        skalVarsleEksternt: Boolean,
    ) {
        val forrigeVarsel = repository.getSisteVarsel(hendelse.deltaker.id, type).getOrNull()
        if (forrigeVarsel?.erAktiv == true) {
            // Nå kan det hende at en beskjed har blitt inaktivert av innbygger uten at vi har fått vite om det.
            // For å vite dette kan vi lytte på aapen-varsel-hendelse-v1 for å få oppdateringer om status på varseler.
            log.info(
                "Forrige varsel for deltaker ${hendelse.deltaker.id} av type $type er fortsatt aktivt. " +
                    "Oppretter ikke nytt varsel.",
            )
            return
        }

        val varsel = Varsel(
            id = UUID.randomUUID(),
            type = type,
            aktivFra = nowUTC(),
            aktivTil = aktivTil,
            deltakerId = hendelse.deltaker.id,
            personident = hendelse.deltaker.personident,
            tekst = tekst,
            skalVarsleEksternt = skalVarsleEksternt,
        )

        repository.upsert(varsel)

        log.info("Opprettet varsel for deltaker ${hendelse.deltaker.id} av type $type")

        when (type) {
            Varsel.Type.OPPGAVE -> producer.opprettOppgave(varsel)
            Varsel.Type.BESKJED -> producer.opprettBeskjed(varsel, skalVarsleEksternt)
        }
    }

    fun inaktiverVarsel(deltaker: HendelseDeltaker, type: Varsel.Type) {
        repository.getSisteVarsel(deltaker.id, type).onSuccess { varsel ->
            if (varsel.erAktiv) {
                repository.upsert(varsel.copy(aktivTil = nowUTC()))
                producer.inaktiver(varsel)
            }
        }
    }
}

fun nowUTC(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("Z"))
