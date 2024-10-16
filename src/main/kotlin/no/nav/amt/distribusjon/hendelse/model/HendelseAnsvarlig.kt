package no.nav.amt.distribusjon.hendelse.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface HendelseAnsvarlig {
    data class NavVeileder(
        val id: UUID,
        val navn: String,
        val navIdent: String,
        val enhet: Enhet,
    ) : HendelseAnsvarlig {
        data class Enhet(
            val id: UUID,
            val enhetsnummer: String,
        )
    }

    data class Deltaker(
        val id: UUID,
        val navn: String,
    ) : HendelseAnsvarlig

    data class Arrangor(
        val enhet: Enhet,
    ) : HendelseAnsvarlig {
        data class Enhet(
            val id: UUID,
            val enhetsnummer: String,
        )
    }
}
