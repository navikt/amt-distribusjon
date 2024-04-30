package no.nav.amt.distribusjon.journalforing.model

import java.util.UUID

data class Journalforingstatus(
    val hendelseId: UUID,
    val journalpostId: String,
)
