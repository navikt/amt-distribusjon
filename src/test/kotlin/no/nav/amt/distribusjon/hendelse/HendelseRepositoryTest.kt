package no.nav.amt.distribusjon.hendelse

import io.kotest.matchers.shouldBe
import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.journalforing.JournalforingstatusRepository
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.distribusjon.utils.TestRepository
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class HendelseRepositoryTest {
    companion object {
        private val hendelseRepository = HendelseRepository()
        private val journalforingstatusRepository = JournalforingstatusRepository()

        @JvmStatic
        @BeforeAll
        fun setup() {
            @Suppress("UnusedExpression")
            SingletonPostgres16Container
        }
    }

    @BeforeEach
    fun cleanDatabase() = TestRepository.cleanDatabase()

    @Test
    fun `getIkkeJournalforteHendelser - hendelse er ikke journalfort - returnerer hendelse`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse(), opprettet = LocalDateTime.now().minusHours(1))
        TestRepository.insert(hendelse)
        journalforingstatusRepository.upsert(
            Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = null,
                bestillingsId = null,
                kanIkkeDistribueres = null,
                kanIkkeJournalfores = null,
            ),
        )

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now())

        ikkeJournalforteHendelser.size shouldBe 1
        ikkeJournalforteHendelser.first().hendelse.id shouldBe hendelse.id
    }

    @Test
    fun `getIkkeJournalforteHendelser - hendelse kan ikke journalfores - returnerer tom liste`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse(), opprettet = LocalDateTime.now().minusHours(1))
        TestRepository.insert(hendelse)
        journalforingstatusRepository.upsert(
            Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = null,
                bestillingsId = null,
                kanIkkeDistribueres = null,
                kanIkkeJournalfores = true,
            ),
        )

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now())

        ikkeJournalforteHendelser.size shouldBe 0
    }

    @Test
    fun `getIkkeJournalforteHendelser - hendelse er ikke journalfort, tidspunkt ikke passert - returnerer tom liste`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse(), opprettet = LocalDateTime.now())
        TestRepository.insert(hendelse)
        journalforingstatusRepository.upsert(
            Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = null,
                bestillingsId = null,
                kanIkkeDistribueres = null,
                kanIkkeJournalfores = null,
            ),
        )

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now().minusHours(1))

        ikkeJournalforteHendelser.size shouldBe 0
    }

    @Test
    fun `getIkkeJournalforteHendelser - hendelse er journalfort og skal ikke sendes brev - returnerer tom liste`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse(), opprettet = LocalDateTime.now().minusHours(1))
        TestRepository.insert(hendelse)
        journalforingstatusRepository.upsert(
            Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = "12345",
                bestillingsId = null,
                kanIkkeDistribueres = null,
                kanIkkeJournalfores = false,
            ),
        )

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now())

        ikkeJournalforteHendelser.size shouldBe 0
    }

    @Test
    fun `getIkkeJournalforteHendelser - hendelse er journalfort, kan ikke distribueres - returnerer tom liste`() {
        val hendelse = Hendelsesdata.hendelse(
            HendelseTypeData.forlengDeltakelse(),
            opprettet = LocalDateTime.now().minusHours(1),
            distribusjonskanal = Distribusjonskanal.PRINT,
        )
        TestRepository.insert(hendelse)
        journalforingstatusRepository.upsert(
            Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = "12345",
                bestillingsId = null,
                kanIkkeDistribueres = true,
                kanIkkeJournalfores = false,
            ),
        )

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now())

        ikkeJournalforteHendelser.size shouldBe 0
    }

    @Test
    fun `getIkkeJournalforteHendelser - hendelse er journalfort, brev skal sendes, er ikke sendt - returnerer hendelse`() {
        val hendelse = Hendelsesdata.hendelse(
            HendelseTypeData.forlengDeltakelse(),
            opprettet = LocalDateTime.now().minusHours(1),
            distribusjonskanal = Distribusjonskanal.PRINT,
        )
        TestRepository.insert(hendelse)
        journalforingstatusRepository.upsert(
            Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = "12345",
                bestillingsId = null,
                kanIkkeDistribueres = null,
                kanIkkeJournalfores = false,
            ),
        )

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now())

        ikkeJournalforteHendelser.size shouldBe 1
        ikkeJournalforteHendelser.first().hendelse.id shouldBe hendelse.id
    }

    @Test
    fun `getIkkeJournalforteHendelser - hendelse er journalfort og brev er sendt - returnerer tom liste`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse(), opprettet = LocalDateTime.now().minusHours(1))
        TestRepository.insert(hendelse)
        journalforingstatusRepository.upsert(
            Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = "12345",
                bestillingsId = UUID.randomUUID(),
                kanIkkeDistribueres = false,
                kanIkkeJournalfores = false,
            ),
        )

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now())

        ikkeJournalforteHendelser.size shouldBe 0
    }

    @Test
    fun `getIkkeJournalforteHendelser - journalforingstatus finnes ikke - returnerer tom liste`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse(), opprettet = LocalDateTime.now().minusHours(1))
        TestRepository.insert(hendelse)

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now())

        ikkeJournalforteHendelser.size shouldBe 0
    }

    @Test
    fun `getHendelser - skal returnere hendelser`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.opprettUtkast())
        TestRepository.insert(hendelse)

        val hendelser = hendelseRepository.getHendelser(listOf(hendelse.id))

        hendelser.size shouldBe 1
    }
}
