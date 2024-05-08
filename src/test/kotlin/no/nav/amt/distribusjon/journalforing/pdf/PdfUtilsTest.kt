package no.nav.amt.distribusjon.journalforing.pdf

import io.kotest.matchers.shouldBe
import no.nav.amt.distribusjon.hendelse.model.Hendelse
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
}
