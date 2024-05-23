package no.nav.amt.distribusjon.digitalbruker

import no.nav.amt.distribusjon.distribusjonskanal.DokdistkanalClient
import no.nav.amt.distribusjon.distribusjonskanal.skalDistribueresDigitalt
import no.nav.amt.distribusjon.veilarboppfolging.VeilarboppfolgingClient

class DigitalBrukerService(
    private val dokdistkanalClient: DokdistkanalClient,
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
) {
    suspend fun erDigital(personident: String): Boolean {
        val erUnderManuellOppfolging = veilarboppfolgingClient.erUnderManuellOppfolging(personident)
        if (erUnderManuellOppfolging) {
            return false
        }
        val distribusjonskanal = dokdistkanalClient.bestemDistribusjonskanal(personident)
        return distribusjonskanal.skalDistribueresDigitalt()
    }
}
