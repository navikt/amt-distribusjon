package no.nav.amt.distribusjon.amtdeltaker

import no.nav.amt.distribusjon.hendelse.model.ArenaTiltakTypeKode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AmtDeltakerResponse(
    val id: UUID,
    val navBruker: NavBruker,
    val deltakerliste: Deltakerliste,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val deltakelsesinnhold: Any?,
    val status: Any,
    val vedtaksinformasjon: Any?,
    val sistEndret: LocalDateTime,
)

data class NavBruker(
    val personId: UUID,
    val personident: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val navVeilederId: UUID?,
    val navEnhetId: UUID?,
    val telefon: String?,
    val epost: String?,
    val erSkjermet: Boolean,
    val adresse: Any?,
    val adressebeskyttelse: Adressebeskyttelse?,
    val oppfolgingsperioder: List<Any>,
    val innsatsgruppe: Any?,
)

enum class Adressebeskyttelse {
    STRENGT_FORTROLIG,
    FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
}

data class Deltakerliste(
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    val status: Status,
    val startDato: LocalDate,
    val sluttDato: LocalDate? = null,
    val oppstart: Oppstartstype?,
    val arrangor: Any,
) {
    enum class Oppstartstype {
        LOPENDE,
        FELLES,
    }

    enum class Status {
        GJENNOMFORES,
        AVBRUTT,
        AVLYST,
        AVSLUTTET,
        PLANLAGT,
    }
}

data class Tiltakstype(
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    val arenaKode: ArenaTiltakTypeKode,
    val innsatsgrupper: Set<Any>,
    val innhold: Any?,
) {
    enum class Tiltakskode {
        ARBEIDSFORBEREDENDE_TRENING,
        ARBEIDSRETTET_REHABILITERING,
        AVKLARING,
        DIGITALT_OPPFOLGINGSTILTAK,
        GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        GRUPPE_FAG_OG_YRKESOPPLAERING,
        JOBBKLUBB,
        OPPFOLGING,
        VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    }
}
