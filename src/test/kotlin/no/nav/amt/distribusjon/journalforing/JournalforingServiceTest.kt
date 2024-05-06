package no.nav.amt.distribusjon.journalforing

import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.journalforing.dokarkiv.DokarkivClient
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.distribusjon.journalforing.pdf.PdfgenClient
import no.nav.amt.distribusjon.journalforing.person.AmtPersonClient
import no.nav.amt.distribusjon.journalforing.sak.SakClient
import no.nav.amt.distribusjon.utils.SingletonPostgresContainer
import no.nav.amt.distribusjon.utils.TestRepository
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import no.nav.amt.distribusjon.utils.data.Journalforingdata
import no.nav.amt.distribusjon.utils.data.Persondata
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class JournalforingServiceTest {
    companion object {
        lateinit var journalforingstatusRepository: JournalforingstatusRepository
        lateinit var journalforingService: JournalforingService
        private val amtPersonClient = mockk<AmtPersonClient>()
        private val pdfgenClient = mockk<PdfgenClient>()
        private val sakClient = mockk<SakClient>()
        private val dokarkivClient = mockk<DokarkivClient>()

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            journalforingstatusRepository = JournalforingstatusRepository()
            journalforingService =
                JournalforingService(journalforingstatusRepository, amtPersonClient, pdfgenClient, sakClient, dokarkivClient)
        }
    }

    @Before
    fun cleanDatabaseAndMocks() {
        TestRepository.cleanDatabase()
        clearMocks(amtPersonClient, pdfgenClient, sakClient, dokarkivClient)
    }

    @Test
    fun `handleHendelse - InnbyggerGodkjennUtkast - journalforer hovedvedtak`() {
        val sak = Journalforingdata.lagSak()
        val ansvarligNavVeileder = Hendelsesdata.ansvarligNavVeileder()
        coEvery { amtPersonClient.hentNavBruker(any()) } returns Persondata.lagNavBruker()
        coEvery { sakClient.opprettEllerHentSak(any()) } returns sak
        coEvery { pdfgenClient.hovedvedtak(any()) } returns "test".toByteArray()
        coEvery { dokarkivClient.opprettJournalpost(any(), any(), any(), any(), any(), any(), any()) } returns "12345"

        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.innbyggerGodkjennUtkast(), ansvarlig = ansvarligNavVeileder)

        runBlocking {
            journalforingService.handleHendelse(hendelse)

            journalforingstatusRepository.get(hendelse.id) shouldBe Journalforingstatus(hendelse.id, "12345", true)

            coVerify {
                dokarkivClient.opprettJournalpost(
                    hendelse.id,
                    hendelse.deltaker.personident,
                    sak,
                    any(),
                    ansvarligNavVeileder.enhet.enhetsnummer,
                    hendelse.deltaker.deltakerliste.tiltak,
                    false,
                )
            }
        }
    }

    @Test
    fun `handleHendelse - InnbyggerGodkjennUtkast, er allerede journalfort - ignorerer hendelse`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.innbyggerGodkjennUtkast())
        journalforingstatusRepository.upsert(Journalforingstatus(hendelse.id, "12345", true))

        runBlocking {
            journalforingService.handleHendelse(hendelse)

            coVerify(exactly = 0) { dokarkivClient.opprettJournalpost(any(), any(), any(), any(), any(), any(), any()) }
        }
    }

    @Test
    fun `handleHendelse - InnbyggerGodkjennUtkast, har ikke aktiv oppfolgingsperiode - feiler`() {
        coEvery { amtPersonClient.hentNavBruker(any()) } returns Persondata.lagNavBruker(
            oppfolgingsperioder = listOf(
                Persondata.lagOppfolgingsperiode(
                    startdato = LocalDateTime.now().minusYears(2),
                    sluttdato = LocalDateTime.now().minusMonths(4),
                ),
            ),
        )

        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.innbyggerGodkjennUtkast())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                journalforingService.handleHendelse(hendelse)
            }
        }
    }

    @Test
    fun `handleHendelse - EndreBakgrunnsinformasjon - journalforer ikke`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.endreBakgrunnsinformasjon())

        runBlocking {
            journalforingService.handleHendelse(hendelse)

            journalforingstatusRepository.get(hendelse.id) shouldBe Journalforingstatus(hendelse.id, null, false)

            coVerify(exactly = 0) { dokarkivClient.opprettJournalpost(any(), any(), any(), any(), any(), any(), any()) }
        }
    }

    @Test
    fun `journalforEndringsvedtak - deltakelsesmengde og forleng - journalforer endringsvedtak`() {
        val sak = Journalforingdata.lagSak()
        val ansvarligNavVeileder = Hendelsesdata.ansvarligNavVeileder()
        coEvery { amtPersonClient.hentNavBruker(any()) } returns Persondata.lagNavBruker()
        coEvery { sakClient.opprettEllerHentSak(any()) } returns sak
        coEvery { pdfgenClient.endringsvedtak(any()) } returns "test".toByteArray()
        coEvery { dokarkivClient.opprettJournalpost(any(), any(), any(), any(), any(), any(), any()) } returns "12345"

        val deltaker = Hendelsesdata.deltaker()
        val hendelseDeltakelsesmengde = Hendelsesdata.hendelse(
            HendelseTypeData.endreDeltakelsesmengde(),
            deltaker = deltaker,
            opprettet = LocalDateTime.now().minusMinutes(20),
        )
        journalforingstatusRepository.upsert(Journalforingstatus(hendelseDeltakelsesmengde.id, null, true))
        val hendelseForleng = Hendelsesdata.hendelse(
            HendelseTypeData.forlengDeltakelse(),
            deltaker = deltaker,
            ansvarlig = ansvarligNavVeileder,
            opprettet = LocalDateTime.now(),
        )
        journalforingstatusRepository.upsert(Journalforingstatus(hendelseForleng.id, null, true))

        runBlocking {
            journalforingService.journalforEndringsvedtak(listOf(hendelseForleng, hendelseDeltakelsesmengde))

            journalforingstatusRepository.get(
                hendelseDeltakelsesmengde.id,
            ) shouldBe Journalforingstatus(hendelseDeltakelsesmengde.id, "12345", true)
            journalforingstatusRepository.get(hendelseForleng.id) shouldBe Journalforingstatus(hendelseForleng.id, "12345", true)

            coVerify { pdfgenClient.endringsvedtak(match { it.endringer.size == 2 }) }
            coVerify {
                dokarkivClient.opprettJournalpost(
                    hendelseForleng.id,
                    deltaker.personident,
                    sak,
                    any(),
                    ansvarligNavVeileder.enhet.enhetsnummer,
                    deltaker.deltakerliste.tiltak,
                    true,
                )
            }
        }
    }

    @Test
    fun `journalforEndringsvedtak - to endringer av samme type - bruker nyeste endring`() {
        val sak = Journalforingdata.lagSak()
        val ansvarligNavVeileder = Hendelsesdata.ansvarligNavVeileder()
        coEvery { amtPersonClient.hentNavBruker(any()) } returns Persondata.lagNavBruker()
        coEvery { sakClient.opprettEllerHentSak(any()) } returns sak
        coEvery { pdfgenClient.endringsvedtak(any()) } returns "test".toByteArray()
        coEvery { dokarkivClient.opprettJournalpost(any(), any(), any(), any(), any(), any(), any()) } returns "12345"

        val deltaker = Hendelsesdata.deltaker()
        val hendelse1 = Hendelsesdata.hendelse(
            HendelseTypeData.forlengDeltakelse(sluttdato = LocalDate.now().plusWeeks(3)),
            deltaker = deltaker,
            ansvarlig = ansvarligNavVeileder,
            opprettet = LocalDateTime.now().minusMinutes(20),
        )
        journalforingstatusRepository.upsert(Journalforingstatus(hendelse1.id, null, true))
        val hendelse2 = Hendelsesdata.hendelse(
            HendelseTypeData.forlengDeltakelse(sluttdato = LocalDate.now().plusWeeks(4)),
            deltaker = deltaker,
            ansvarlig = ansvarligNavVeileder,
            opprettet = LocalDateTime.now(),
        )
        journalforingstatusRepository.upsert(Journalforingstatus(hendelse2.id, null, true))

        runBlocking {
            journalforingService.journalforEndringsvedtak(listOf(hendelse1, hendelse2))

            journalforingstatusRepository.get(hendelse1.id) shouldBe Journalforingstatus(hendelse1.id, "12345", true)
            journalforingstatusRepository.get(hendelse2.id) shouldBe Journalforingstatus(hendelse2.id, "12345", true)

            coVerify {
                pdfgenClient.endringsvedtak(
                    match {
                        it.endringer.size == 1 &&
                            (it.endringer.first().hendelseType as HendelseType.ForlengDeltakelse).sluttdato == LocalDate.now().plusWeeks(4)
                    },
                )
            }
            coVerify {
                dokarkivClient.opprettJournalpost(
                    hendelse2.id,
                    deltaker.personident,
                    sak,
                    any(),
                    ansvarligNavVeileder.enhet.enhetsnummer,
                    deltaker.deltakerliste.tiltak,
                    true,
                )
            }
        }
    }

    @Test
    fun `journalforEndringsvedtak - ulik deltakerid - feiler`() {
        val hendelseDeltakelsesmengde = Hendelsesdata.hendelse(
            HendelseTypeData.endreDeltakelsesmengde(),
            opprettet = LocalDateTime.now().minusMinutes(20),
        )
        val hendelseForleng = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse(), opprettet = LocalDateTime.now())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                journalforingService.journalforEndringsvedtak(listOf(hendelseForleng, hendelseDeltakelsesmengde))
            }
        }
    }
}
