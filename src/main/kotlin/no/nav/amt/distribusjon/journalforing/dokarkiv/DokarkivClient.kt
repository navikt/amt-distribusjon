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
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.veilarboppfolging.Sak
import java.util.UUID

class DokarkivClient(
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
    environment: Environment,
) {
    private val scope = environment.dokarkivScope
    private val url = environment.dokarkivUrl

    suspend fun opprettJournalpost(
        hendelseId: UUID,
        fnr: String,
        sak: Sak,
        pdf: ByteArray,
        journalforendeEnhet: String,
        tiltakstype: HendelseDeltaker.Deltakerliste.Tiltak,
        endring: Boolean,
    ): String {
        val request = getJournalpostRequest(
            hendelseId = hendelseId,
            fnr = fnr,
            sak = sak,
            pdf = pdf,
            journalforendeEnhet = journalforendeEnhet,
            tiltakstype = tiltakstype,
            endring = endring,
        )
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$url/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(request))
        }
        if (!response.status.isSuccess()) {
            error("Kunne ikke opprette journalpost for hendelseId $hendelseId")
        }
        return response.body<OpprettJournalpostResponse>().journalpostId
    }

    private fun getJournalpostRequest(
        hendelseId: UUID,
        fnr: String,
        sak: Sak,
        pdf: ByteArray,
        journalforendeEnhet: String,
        tiltakstype: HendelseDeltaker.Deltakerliste.Tiltak,
        endring: Boolean,
    ): OpprettJournalpostRequest {
        val tittel = getTittel(tiltakstype, endring)
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
                    tittel = tittel,
                ),
            ),
            journalfoerendeEnhet = journalforendeEnhet,
            sak = Sak(
                fagsakId = sak.sakId.toString(),
                fagsaksystem = sak.fagsaksystem,
            ),
            tema = sak.tema,
            tittel = tittel,
            eksternReferanseId = hendelseId.toString(),
        )
    }

    private fun getTittel(tiltakstype: HendelseDeltaker.Deltakerliste.Tiltak, endring: Boolean): String {
        return if (endring) {
            "Endringsvedtak - ${tiltakstype.navn}"
        } else {
            "Vedtak - ${tiltakstype.navn}"
        }
    }

    private fun getBrevkode(tiltakstype: HendelseDeltaker.Deltakerliste.Tiltak): String {
        return "tiltak-vedtak-${tiltakstype.type.name}"
    }
}
