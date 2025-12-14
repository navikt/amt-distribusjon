package no.nav.amt.distribusjon.hendelse

import io.kotest.matchers.shouldBe
import no.nav.amt.distribusjon.RepositoryTestBase
import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.journalforing.JournalforingstatusRepository
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime
import java.util.UUID

@JdbcTest
@Import(HendelseRepository::class, JournalforingstatusRepository::class)
class HendelseRepositoryTest(
    private val hendelseRepository: HendelseRepository,
    private val journalforingstatusRepository: JournalforingstatusRepository,
) : RepositoryTestBase() {
    @BeforeEach
    fun cleanDatabase() {
        testRepository.cleanDatabase()
    }

    @Test
    fun `getIkkeJournalforteHendelser - hendelse er ikke journalfort - returnerer hendelse`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse(), opprettet = LocalDateTime.now().minusHours(1))

        testRepository.insert(hendelse)

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
        testRepository.insert(hendelse)

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
        testRepository.insert(hendelse)
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
        testRepository.insert(hendelse)
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
        testRepository.insert(hendelse)
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
        testRepository.insert(hendelse)
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
        testRepository.insert(hendelse)
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
        testRepository.insert(hendelse)

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now())

        ikkeJournalforteHendelser.size shouldBe 0
    }
}
