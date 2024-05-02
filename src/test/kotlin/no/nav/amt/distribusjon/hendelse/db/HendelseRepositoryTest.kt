package no.nav.amt.distribusjon.hendelse.db

import io.kotest.matchers.shouldBe
import no.nav.amt.distribusjon.journalforing.JournalforingstatusRepository
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.distribusjon.utils.SingletonPostgresContainer
import no.nav.amt.distribusjon.utils.TestRepository
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime

class HendelseRepositoryTest {
    companion object {
        lateinit var hendelseRepository: HendelseRepository
        lateinit var journalforingstatusRepository: JournalforingstatusRepository

        @JvmStatic
        @BeforeClass
        fun setup() {
            SingletonPostgresContainer.start()
            hendelseRepository = HendelseRepository()
            journalforingstatusRepository = JournalforingstatusRepository()
        }
    }

    @Before
    fun cleanDatabase() {
        TestRepository.cleanDatabase()
    }

    @Test
    fun `getIkkeJournalforteHendelser - hendelse er ikke journalfort - returnerer hendelse`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse())
        TestRepository.insert(
            HendelseDbo(
                hendelse.id,
                hendelse.deltaker.id,
                hendelse.deltaker,
                hendelse.ansvarlig,
                hendelse.payload,
                LocalDateTime.now().minusHours(1),
            ),
        )

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now())

        ikkeJournalforteHendelser.size shouldBe 1
        ikkeJournalforteHendelser.first().id shouldBe hendelse.id
    }

    @Test
    fun `getIkkeJournalforteHendelser - hendelse er ikke journalfort, tidspunkt ikke passert - returnerer tom liste`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse())
        TestRepository.insert(
            HendelseDbo(
                hendelse.id,
                hendelse.deltaker.id,
                hendelse.deltaker,
                hendelse.ansvarlig,
                hendelse.payload,
                LocalDateTime.now(),
            ),
        )

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now().minusHours(1))

        ikkeJournalforteHendelser.size shouldBe 0
    }

    @Test
    fun `getIkkeJournalforteHendelser - hendelse er journalfort - returnerer tom liste`() {
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse())
        TestRepository.insert(
            HendelseDbo(
                hendelse.id,
                hendelse.deltaker.id,
                hendelse.deltaker,
                hendelse.ansvarlig,
                hendelse.payload,
                LocalDateTime.now().minusHours(1),
            ),
        )
        journalforingstatusRepository.insert(
            Journalforingstatus(
                hendelse.id,
                "12345",
            ),
        )

        val ikkeJournalforteHendelser = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now())

        ikkeJournalforteHendelser.size shouldBe 0
    }
}
