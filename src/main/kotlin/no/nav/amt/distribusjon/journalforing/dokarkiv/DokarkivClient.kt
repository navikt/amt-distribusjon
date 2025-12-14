package no.nav.amt.distribusjon.journalforing.dokarkiv

import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.veilarboppfolging.Sak
import no.nav.amt.lib.models.hendelse.HendelseDeltaker
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.requiredBody
import java.util.UUID

@Service
class DokarkivClient(
    private val dokArkivHttpClient: RestClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /*
    https://confluence.adeo.no/display/BOA/opprettJournalpost
    Oppretter en journalpost i Joark/dokarkiv
     */
    fun opprettJournalpost(
        hendelseId: UUID,
        fnr: String,
        sak: Sak,
        pdf: ByteArray,
        journalforendeEnhet: String,
        tiltakstype: HendelseDeltaker.Deltakerliste.Tiltak,
        journalpostNavn: String,
    ): String {
        val requestBody = lagJournalpostRequest(
            hendelseId = hendelseId,
            fnr = fnr,
            sak = sak,
            pdf = pdf,
            journalforendeEnhet = journalforendeEnhet,
            tiltakstype = tiltakstype,
            journalpostNavn = journalpostNavn,
        )

        return dokArkivHttpClient
            .post()
            .uri("/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true")
            .body(requestBody)
            .retrieve()
            .onStatus({ it.value() == HttpStatus.CONFLICT.value() }) { _, _ ->
                log.warn("Journalpost for hendelseId $hendelseId er allerede opprettet")
            }.onStatus({ !it.is2xxSuccessful }) { _, response ->
                val bodyText = response.body.reader().use { it.readText() }
                throw IllegalStateException(
                    "Kunne ikke opprette journalpost for hendelseId $hendelseId, " +
                        "Status=${response.statusCode} error=$bodyText",
                )
            }.requiredBody<OpprettJournalpostResponse>()
            .journalpostId
    }

    companion object {
        private fun lagJournalpostRequest(
            hendelseId: UUID,
            fnr: String,
            sak: Sak,
            pdf: ByteArray,
            journalforendeEnhet: String,
            tiltakstype: HendelseDeltaker.Deltakerliste.Tiltak,
            journalpostNavn: String,
        ): OpprettJournalpostRequest = OpprettJournalpostRequest(
            avsenderMottaker = AvsenderMottaker(
                id = fnr,
            ),
            bruker = Bruker(
                id = fnr,
            ),
            dokumenter = listOf(
                Dokument(
                    brevkode = getBrevkode(tiltakstype),
                    dokumentvarianter = listOf(
                        DokumentVariant(
                            fysiskDokument = pdf,
                        ),
                    ),
                    tittel = journalpostNavn,
                ),
            ),
            journalfoerendeEnhet = journalforendeEnhet,
            sak = Sak(
                fagsakId = sak.sakId.toString(),
                fagsaksystem = sak.fagsaksystem,
            ),
            tema = Environment.SAF_TEMA,
            tittel = journalpostNavn,
            eksternReferanseId = hendelseId.toString(),
        )

        // må benytte Arena-kode her for at ting ikke skal knekke
        private fun getBrevkode(tiltakstype: HendelseDeltaker.Deltakerliste.Tiltak): String =
            "tiltak-vedtak-${tiltakstype.tiltakskode.toArenaKode().name}"
    }
}
