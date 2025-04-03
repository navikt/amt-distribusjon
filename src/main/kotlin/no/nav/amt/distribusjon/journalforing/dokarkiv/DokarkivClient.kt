package no.nav.amt.distribusjon.journalforing.dokarkiv

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.auth.AzureAdTokenClient
import no.nav.amt.distribusjon.veilarboppfolging.Sak
import no.nav.amt.lib.models.hendelse.HendelseDeltaker
import org.slf4j.LoggerFactory
import java.util.UUID

class DokarkivClient(
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
    environment: Environment,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = environment.dokarkivScope
    private val url = environment.dokarkivUrl

    /*
    https://confluence.adeo.no/display/BOA/opprettJournalpost
    Oppretter en journalpost i Joark/dokarkiv
     */
    suspend fun opprettJournalpost(
        hendelseId: UUID,
        fnr: String,
        sak: Sak,
        pdf: ByteArray,
        journalforendeEnhet: String,
        tiltakstype: HendelseDeltaker.Deltakerliste.Tiltak,
        journalpostNavn: String,
    ): String {
        val request = lagJournalpostRequest(
            hendelseId = hendelseId,
            fnr = fnr,
            sak = sak,
            pdf = pdf,
            journalforendeEnhet = journalforendeEnhet,
            tiltakstype = tiltakstype,
            journalpostNavn = journalpostNavn,
        )
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$url/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(request))
        }
        if (response.status.value == 409) {
            log.warn("Journalpost for hendelseId $hendelseId er allerede opprettet")
        }
        if (!response.status.isSuccess() && response.status.value != 409) {
            error("Kunne ikke opprette journalpost for hendelseId $hendelseId")
        }
        return response.body<OpprettJournalpostResponse>().journalpostId
    }

    private fun lagJournalpostRequest(
        hendelseId: UUID,
        fnr: String,
        sak: Sak,
        pdf: ByteArray,
        journalforendeEnhet: String,
        tiltakstype: HendelseDeltaker.Deltakerliste.Tiltak,
        journalpostNavn: String,
    ): OpprettJournalpostRequest {
        return OpprettJournalpostRequest(
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
            tema = sak.tema,
            tittel = journalpostNavn,
            eksternReferanseId = hendelseId.toString(),
        )
    }

    private fun getBrevkode(tiltakstype: HendelseDeltaker.Deltakerliste.Tiltak): String = "tiltak-vedtak-${tiltakstype.type.name}"
}
