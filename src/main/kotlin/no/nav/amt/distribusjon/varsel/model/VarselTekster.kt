package no.nav.amt.distribusjon.varsel.model

import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.journalforing.pdf.visningsnavn

const val OPPGAVE_TEKST = "Du har mottatt et utkast til påmelding på arbeidsmarkedstiltaket: %s hos %s. Svar på spørsmålet her."

const val BESKJED_TEKST = "Ny endring på arbeidsmarkedstiltaket: %s hos %s."

const val MELDT_PA_DIREKTE_TEKST = "Du er meldt på arbeidsmarkedstiltaket: %s hos %s."

fun oppgaveTekst(hendelse: Hendelse) = String.format(
    OPPGAVE_TEKST,
    hendelse.deltaker.deltakerliste.tiltak.navn,
    hendelse.deltaker.deltakerliste.arrangor.visningsnavn(),
)

fun beskjedTekst(hendelse: Hendelse): String {
    val tekst = if (hendelse.payload is HendelseType.NavGodkjennUtkast) {
        MELDT_PA_DIREKTE_TEKST
    } else {
        BESKJED_TEKST
    }

    return String.format(
        tekst,
        hendelse.deltaker.deltakerliste.tiltak.navn,
        hendelse.deltaker.deltakerliste.arrangor.visningsnavn(),
    )
}
