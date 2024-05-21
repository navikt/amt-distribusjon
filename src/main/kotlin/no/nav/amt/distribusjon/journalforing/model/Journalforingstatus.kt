package no.nav.amt.distribusjon.journalforing.model

import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.distribusjonskanal.skalDistribueresDigitalt
import java.util.UUID

data class Journalforingstatus(
    val hendelseId: UUID,
    val journalpostId: String?,
    val bestillingsId: UUID?,
) {
    fun erJournalfort(): Boolean {
        return journalpostId != null
    }

    fun erDistribuert(distribusjonskanal: Distribusjonskanal): Boolean {
        return if (distribusjonskanal.skalDistribueresDigitalt()) {
            true
        } else {
            bestillingsId != null
        }
    }
}
