package no.nav.amt.distribusjon.journalforing.pdf

import io.kotest.matchers.shouldBe
import no.nav.amt.distribusjon.hendelse.model.Aarsak
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.Innhold
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import no.nav.amt.distribusjon.utils.data.Persondata
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PdfUtilsTest {
    @Test
    fun `lagEndringsvedtakPdfDto - to endringer av samme type - bruker nyeste endring`() {
        val deltaker = Hendelsesdata.deltaker()
        val navBruker = Persondata.lagNavBruker()
        val ansvarligNavVeileder = Hendelsesdata.ansvarligNavVeileder()
        val hendelser: List<Hendelse> = listOf(
            Hendelsesdata.hendelse(
                HendelseTypeData.forlengDeltakelse(sluttdato = LocalDate.now().plusWeeks(3)),
                deltaker = deltaker,
                ansvarlig = ansvarligNavVeileder,
                opprettet = LocalDateTime.now().minusMinutes(20),
            ),
            Hendelsesdata.hendelse(
                HendelseTypeData.forlengDeltakelse(sluttdato = LocalDate.now().plusWeeks(4)),
                deltaker = deltaker,
                ansvarlig = ansvarligNavVeileder,
                opprettet = LocalDateTime.now(),
            ),
        )

        val pdfDto = lagEndringsvedtakPdfDto(deltaker, navBruker, ansvarligNavVeileder, hendelser)

        pdfDto.endringer.size shouldBe 1
        (pdfDto.endringer.first() as EndringDto.ForlengDeltakelse).sluttdato shouldBe LocalDate.now().plusWeeks(4)
    }

    @Test
    fun `lagEndringsvedtakPdfDto - IkkeAktuell - inneholder arsak som string`() {
        val deltaker = Hendelsesdata.deltaker()
        val navBruker = Persondata.lagNavBruker()
        val ansvarligNavVeileder = Hendelsesdata.ansvarligNavVeileder()
        val arsak = Aarsak(Aarsak.Type.IKKE_MOTT)
        val hendelser: List<Hendelse> = listOf(
            Hendelsesdata.hendelse(
                HendelseTypeData.ikkeAktuell(arsak),
                deltaker = deltaker,
                ansvarlig = ansvarligNavVeileder,
                opprettet = LocalDateTime.now().minusMinutes(20),
            ),
        )

        val pdfDto = lagEndringsvedtakPdfDto(deltaker, navBruker, ansvarligNavVeileder, hendelser)

        pdfDto.endringer.size shouldBe 1
        (pdfDto.endringer.first() as EndringDto.IkkeAktuell).aarsak shouldBe arsak.visningsnavn()
    }

    @Test
    fun `lagEndringsvedtakPdfDto - EndreInnhold - inneholder innhold som string`() {
        val deltaker = Hendelsesdata.deltaker()
        val navBruker = Persondata.lagNavBruker()
        val ansvarligNavVeileder = Hendelsesdata.ansvarligNavVeileder()
        val innhold = listOf(
            Innhold("tekst 1", "kode 1", null),
            Innhold("tekst 2", "kode 2", null),
        )
        val hendelser: List<Hendelse> = listOf(
            Hendelsesdata.hendelse(
                HendelseTypeData.endreInnhold(innhold),
                deltaker = deltaker,
                ansvarlig = ansvarligNavVeileder,
                opprettet = LocalDateTime.now().minusMinutes(20),
            ),
        )

        val pdfDto = lagEndringsvedtakPdfDto(deltaker, navBruker, ansvarligNavVeileder, hendelser)

        pdfDto.endringer.size shouldBe 1
        (pdfDto.endringer.first() as EndringDto.EndreInnhold).innhold shouldBe listOf("tekst 1", "tekst 2")
    }
}
