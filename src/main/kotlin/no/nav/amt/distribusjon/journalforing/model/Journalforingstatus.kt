package no.nav.amt.distribusjon.journalforing.model

import java.util.UUID

data class Journalforingstatus(
    val hendelseId: UUID,
    val journalpostId: String?,
    val skalSendeBrev: Boolean,
    val bestillingsId: UUID?,
) {
    fun erJournalfort(): Boolean {
        return journalpostId != null
    }

    fun erDistribuert(): Boolean {
        return if (skalSendeBrev) {
            bestillingsId != null
        } else {
            true
        }
    }
}
