package no.nav.amt.distribusjon.tiltakshendelse

import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.tiltakshendelse.TiltakshendelseService.Companion.UTKAST_TIL_PAMELDING_TEKST
import no.nav.amt.distribusjon.tiltakshendelse.model.Tiltakshendelse
import org.slf4j.LoggerFactory
import java.util.UUID

class TiltakshendelseService(
    private val repository: TiltakshendelseRepository,
    private val producer: TiltakshendelseProducer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val UTKAST_TIL_PAMELDING_TEKST = "Utkast til påmelding"
    }

    fun handleHendelse(hendelse: Hendelse) {
        if (repository.getByHendelseId(hendelse.id).isSuccess) {
            log.info("Tiltakshendelse for hendelse ${hendelse.id} er allerede håndtert.")
            return
        }

        when (hendelse.payload) {
            is HendelseType.OpprettUtkast -> opprettUtkastHendelse(hendelse)
            is HendelseType.AvbrytUtkast,
            is HendelseType.InnbyggerGodkjennUtkast,
            is HendelseType.NavGodkjennUtkast,
            -> inaktiverUtkastHendelse(hendelse)

            else -> {}
        }
    }

    private fun upsert(tiltakshendelse: Tiltakshendelse) {
        repository.upsert(tiltakshendelse)
        producer.produce(tiltakshendelse)
        log.info("Upsertet tiltakshendelse ${tiltakshendelse.id}")
    }

    private fun opprettUtkastHendelse(hendelse: Hendelse) {
        upsert(hendelse.tilTiltakshendelse())
    }

    private fun inaktiverUtkastHendelse(hendelse: Hendelse) {
        repository.getUtkastHendelse(hendelse.deltaker.id).onSuccess {
            val inaktivertHendelse = it.copy(
                aktiv = false,
                hendelser = it.hendelser.plus(hendelse.id),
            )
            upsert(inaktivertHendelse)
        }
    }
}

fun Hendelse.tilTiltakshendelse() = when (this.payload) {
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
