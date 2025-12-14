package no.nav.amt.distribusjon.digitalbruker

import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.distribusjonskanal.DokdistkanalClient
import no.nav.amt.distribusjon.distribusjonskanal.skalDistribueresDigitalt
import no.nav.amt.distribusjon.veilarboppfolging.VeilarboppfolgingClient
import org.springframework.stereotype.Service

@Service
class DigitalBrukerService(
    private val dokdistkanalClient: DokdistkanalClient,
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
) {
    fun erDigital(personident: String): Boolean {
        if (veilarboppfolgingClient.erUnderManuellOppfolging(personident)) {
            return false
        }
        val distribusjonskanal = dokdistkanalClient.bestemDistribusjonskanal(personident)
        return distribusjonskanal.skalDistribueresDigitalt()
    }

    companion object {
        fun skalDistribueresDigitalt(distribusjonskanal: Distribusjonskanal, erUnderManuellOppfolging: Boolean): Boolean =
            if (erUnderManuellOppfolging) {
                false
            } else {
                distribusjonskanal.skalDistribueresDigitalt()
            }
    }
}
