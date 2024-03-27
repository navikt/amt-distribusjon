package no.nav.amt.distribusjon.varsel

import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.varsel.model.PAMELDING_TEKST
import no.nav.amt.distribusjon.varsel.model.PLACEHOLDER_BESKJED_TEKST
import no.nav.amt.distribusjon.varsel.model.Varsel
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class VarselService(
    private val repository: VarselRepository,
    private val producer: VarselProducer,
) {
    private val varselUtsettelse = Duration.ofHours(1)
    private val beskjedAktivLengde = Duration.ofDays(14)

    fun handleHendelse(hendelse: Hendelse) {
        when (hendelse.payload) {
            is HendelseType.OpprettUtkast -> opprettOppgave(hendelse, Varsel.Type.PAMELDING)
            is HendelseType.AvbrytUtkast -> inaktiverVarsel(hendelse.deltaker, Varsel.Type.PAMELDING)
            is HendelseType.InnbyggerGodkjennUtkast -> inaktiverVarsel(hendelse.deltaker, Varsel.Type.PAMELDING)
            is HendelseType.NavGodkjennUtkast -> {
                inaktiverVarsel(hendelse.deltaker, Varsel.Type.PAMELDING)
                opprettBeskjed(hendelse, Varsel.Type.PAMELDING)
            }

            is HendelseType.EndreSluttdato,
            is HendelseType.ForlengDeltakelse,
            is HendelseType.AvsluttDeltakelse,
            is HendelseType.IkkeAktuell,
            -> opprettBeskjed(hendelse, Varsel.Type.AVSLUTTNING)

            is HendelseType.EndreStartdato,
            -> opprettBeskjed(hendelse, Varsel.Type.OPPSTART)

            is HendelseType.EndreDeltakelsesmengde,
            is HendelseType.EndreBakgrunnsinformasjon,
            is HendelseType.EndreInnhold,
            is HendelseType.EndreSluttarsak,
            -> {
            }
        }
    }

    fun opprettBeskjed(hendelse: Hendelse, type: Varsel.Type) {
        val forrigeVarsel = repository.getSisteVarsel(hendelse.deltaker.id, type).getOrNull()
        repository.upsert(
            nyttVarsel(
                forrigeVarsel = forrigeVarsel,
                type = type,
                aktivTil = nowUTC().plus(beskjedAktivLengde),
                tekst = PLACEHOLDER_BESKJED_TEKST,
                hendelse = hendelse,
            ),
        )
    }

    fun opprettOppgave(hendelse: Hendelse, type: Varsel.Type) {
        val forrigeVarsel = repository.getSisteVarsel(hendelse.deltaker.id, type).getOrNull()
        repository.upsert(
            nyttVarsel(
                forrigeVarsel = forrigeVarsel,
                type = type,
                aktivTil = null,
                tekst = PAMELDING_TEKST,
                hendelse = hendelse,
            ),
        )
    }

    fun inaktiverVarsel(deltaker: HendelseDeltaker, type: Varsel.Type) {
        repository.getSisteVarsel(deltaker.id, type).onSuccess { varsel ->
            if (varsel.aktivTil != null && varsel.aktivTil > nowUTC()) {
                producer.inaktiver(varsel)
                repository.upsert(varsel.copy(aktivTil = nowUTC()))
            }
        }
    }

    private fun nyttVarsel(
        forrigeVarsel: Varsel?,
        type: Varsel.Type,
        aktivTil: ZonedDateTime?,
        tekst: String,
        hendelse: Hendelse,
    ): Varsel {
        return if (forrigeVarsel != null && forrigeVarsel.aktivFra > nowUTC()) {
            forrigeVarsel.copy(
                aktivFra = nowUTC().plus(varselUtsettelse),
                aktivTil = aktivTil,
            )
        } else {
            Varsel(
                id = UUID.randomUUID(),
                type = type,
                aktivFra = nowUTC().plus(varselUtsettelse),
                aktivTil = aktivTil,
                deltakerId = hendelse.deltaker.id,
                personident = hendelse.deltaker.personident,
                tekst = tekst,
            )
        }
    }
}

fun nowUTC() = ZonedDateTime.now(ZoneId.of("Z"))
