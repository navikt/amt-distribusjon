package no.nav.amt.distribusjon.tiltakshendelse

import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.amtdeltaker.AmtDeltakerClient
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.tiltakshendelse.TiltakshendelseService.Companion.UTKAST_TIL_PAMELDING_TEKST
import no.nav.amt.distribusjon.tiltakshendelse.model.Tiltakshendelse
import no.nav.amt.distribusjon.tiltakshendelse.model.toDto
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import no.nav.amt.lib.models.hendelse.HendelseType
import no.nav.amt.lib.outbox.OutboxService
import no.nav.amt.lib.utils.database.Database
import org.slf4j.LoggerFactory
import java.util.UUID

class TiltakshendelseService(
    private val repository: TiltakshendelseRepository,
    private val producer: TiltakshendelseProducer,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val outboxService: OutboxService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val UTKAST_TIL_PAMELDING_TEKST = "Utkast til påmelding"
    }

    suspend fun handleHendelse(hendelse: Hendelse) {
        if (repository.getByHendelseId(hendelse.id).isSuccess) {
            log.info("Tiltakshendelse for hendelse ${hendelse.id} er allerede håndtert.")
            return
        }

        when (hendelse.payload) {
            is HendelseType.OpprettUtkast -> opprettStartHendelse(hendelse)
            is HendelseType.AvbrytUtkast,
            is HendelseType.InnbyggerGodkjennUtkast,
            is HendelseType.NavGodkjennUtkast,
            -> stoppUtkastHendelse(hendelse)

            else -> {}
        }
    }

    suspend fun handleForslag(forslag: Forslag) {
        when (forslag.status) {
            is Forslag.Status.VenterPaSvar -> opprettStartHendelse(forslag)
            is Forslag.Status.Godkjent,
            is Forslag.Status.Avvist,
            is Forslag.Status.Tilbakekalt,
            is Forslag.Status.Erstattet,
            -> stoppForslagHendelse(forslag.id)
        }
    }

    suspend fun opprettStartHendelse(hendelse: Hendelse) {
        lagreOgDistribuer(hendelse.toTiltakshendelse())
    }

    suspend fun opprettStartHendelse(forslag: Forslag) {
        val deltaker = amtDeltakerClient.getDeltaker(forslag.deltakerId)

        lagreOgDistribuer(
            forslag.toHendelse(
                personIdent = deltaker.navBruker.personident,
                arenaKode = deltaker.deltakerliste.tiltakstype.arenaKode,
                aktiv = true,
            ),
        )
    }

    private suspend fun stoppUtkastHendelse(hendelse: Hendelse) {
        repository.getHendelse(hendelse.deltaker.id, Tiltakshendelse.Type.UTKAST).onSuccess {
            val inaktivertHendelse = it.copy(
                aktiv = false,
                hendelser = it.hendelser.plus(hendelse.id),
            )
            lagreOgDistribuer(inaktivertHendelse)
        }
    }

    suspend fun stoppForslagHendelse(forslagId: UUID) {
        repository.getForslagHendelse(forslagId).onSuccess {
            val inaktivertHendelse = it.copy(
                aktiv = false,
            )
            lagreOgDistribuer(inaktivertHendelse)
        }
    }

    private suspend fun lagreOgDistribuer(tiltakshendelse: Tiltakshendelse) {
        Database.transaction {
            repository.upsert(tiltakshendelse)
            outboxService.insertRecord(
                key = tiltakshendelse.id,
                value = tiltakshendelse.toDto(),
                topic = Environment.TILTAKSHENDELSE_TOPIC,
            )
        }
        log.info("Upsertet tiltakshendelse ${tiltakshendelse.id}")
    }

    fun reproduser(id: UUID) {
        val tiltakshendelse = repository.get(id).getOrThrow()
        producer.produce(tiltakshendelse)
        log.info("Reproduserte tiltakshendelse $id")
    }
}

fun Forslag.toHendelse(
    personIdent: String,
    arenaKode: ArenaKode,
    aktiv: Boolean,
) = Tiltakshendelse(
    id = UUID.randomUUID(),
    type = Tiltakshendelse.Type.FORSLAG,
    deltakerId = deltakerId,
    forslagId = id,
    hendelser = emptyList(),
    personident = personIdent,
    aktiv = aktiv,
    tekst = getForslagHendelseTekst(this),
    tiltakstype = arenaKode,
    opprettet = opprettet,
)

fun Hendelse.toTiltakshendelse() = when (this.payload) {
    is HendelseType.OpprettUtkast -> Tiltakshendelse(
        id = UUID.randomUUID(),
        type = Tiltakshendelse.Type.UTKAST,
        deltakerId = this.deltaker.id,
        forslagId = null,
        hendelser = listOf(this.id),
        personident = this.deltaker.personident,
        aktiv = true,
        tekst = UTKAST_TIL_PAMELDING_TEKST,
        tiltakstype = this.deltaker.deltakerliste.tiltak.type,
        opprettet = this.opprettet,
    )

    else -> throw IllegalArgumentException(
        "Kan ikke lage tiltakshendelse for hendelse ${this.id} av type ${this.payload.javaClass.simpleName}",
    )
}

fun getForslagHendelseTekst(forslag: Forslag): String {
    val forslagtekst = "Forslag:"
    return when (forslag.endring) {
        is Forslag.ForlengDeltakelse -> "$forslagtekst Forleng deltakelse"
        is Forslag.AvsluttDeltakelse -> "$forslagtekst Avslutt deltakelse"
        is Forslag.IkkeAktuell -> "$forslagtekst Er ikke aktuell"
        is Forslag.Deltakelsesmengde -> "$forslagtekst Deltakelsesmengde"
        is Forslag.Startdato -> "$forslagtekst Oppstartsdato"
        is Forslag.Sluttdato -> "$forslagtekst Sluttdato"
        is Forslag.Sluttarsak -> "$forslagtekst Sluttårsak"
        is Forslag.FjernOppstartsdato -> "$forslagtekst Fjern oppstartsdato"
        is Forslag.EndreAvslutning -> "$forslagtekst Endre avslutning"
    }
}
