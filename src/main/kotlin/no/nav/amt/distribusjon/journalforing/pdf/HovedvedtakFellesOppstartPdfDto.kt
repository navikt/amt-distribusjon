package no.nav.amt.distribusjon.journalforing.pdf

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import java.time.LocalDate

data class HovedvedtakFellesOppstartPdfDto(
    val deltaker: DeltakerDto,
    val deltakerliste: DeltakerlisteDto,
    val avsender: AvsenderDto,
    val opprettetDato: LocalDate,
) {
    data class DeltakerDto(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val personident: String,
    )

    data class DeltakerlisteDto(
        val tiltakskode: Tiltakstype.Tiltakskode,
        val tittelNavn: String,
        val ingressNavn: String,
        val ledetekst: String?,
        val startdato: String,
        val sluttdato: String?,
        val forskriftskapittel: Int,
        val arrangor: ArrangorDto,
    )

    data class AvsenderDto(
        val navn: String,
        val enhet: String,
    )
}
