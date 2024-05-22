package no.nav.amt.distribusjon.varsel.model

import io.kotest.matchers.shouldBe
import no.nav.amt.distribusjon.journalforing.pdf.visningsnavn
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import org.junit.Test

class VarselTeksterTest {
    @Test
    fun `oppgaveTekst - bruker riktig tekst, tiltaksnavn og arrangornavn`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.opprettUtkast())

        oppgaveTekst(hendelse) shouldBe String.format(
            OPPGAVE_TEKST,
            hendelse.deltaker.deltakerliste.tiltak.navn,
            hendelse.deltaker.deltakerliste.arrangor.visningsnavn(),
        )
    }

    @Test
    fun `beskjedTekst - bruker riktig tekst, tiltaksnavn og arrangornavn`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.opprettUtkast())

        beskjedTekst(hendelse) shouldBe String.format(
            BESKJED_TEKST,
            hendelse.deltaker.deltakerliste.tiltak.navn,
            hendelse.deltaker.deltakerliste.arrangor.visningsnavn(),
        )
    }
}
