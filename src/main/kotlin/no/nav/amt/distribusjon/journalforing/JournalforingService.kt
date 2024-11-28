package no.nav.amt.distribusjon.journalforing

import no.nav.amt.distribusjon.digitalbruker.DigitalBrukerService
import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseAnsvarlig
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.hendelse.model.Utkast
import no.nav.amt.distribusjon.journalforing.dokarkiv.DokarkivClient
import no.nav.amt.distribusjon.journalforing.dokdistfordeling.DokdistfordelingClient
import no.nav.amt.distribusjon.journalforing.model.HendelseMedJournalforingstatus
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.distribusjon.journalforing.pdf.PdfgenClient
import no.nav.amt.distribusjon.journalforing.pdf.lagEndringsvedtakPdfDto
import no.nav.amt.distribusjon.journalforing.pdf.lagHovedvedtakPdfDto
import no.nav.amt.distribusjon.journalforing.person.AmtPersonClient
import no.nav.amt.distribusjon.journalforing.person.model.NavBruker
import no.nav.amt.distribusjon.veilarboppfolging.VeilarboppfolgingClient
import org.slf4j.LoggerFactory

class JournalforingService(
    private val journalforingstatusRepository: JournalforingstatusRepository,
    private val amtPersonClient: AmtPersonClient,
    private val pdfgenClient: PdfgenClient,
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val dokarkivClient: DokarkivClient,
    private val dokdistfordelingClient: DokdistfordelingClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun handleHendelse(hendelse: Hendelse) {
        val journalforingstatus = journalforingstatusRepository.get(hendelse.id)
        if (hendelseErBehandlet(journalforingstatus, hendelse.distribusjonskanal, hendelse.manuellOppfolging)) {
            log.info("Hendelse med id ${hendelse.id} for deltaker ${hendelse.deltaker.id} er allerede behandlet")
            return
        }
        when (hendelse.payload) {
            is HendelseType.InnbyggerGodkjennUtkast -> journalforHovedvedtak(
                hendelse,
                hendelse.payload.utkast,
                journalforingstatus,
            )
            is HendelseType.ReaktiverDeltakelse -> journalforHovedvedtak(
                hendelse,
                hendelse.payload.utkast,
                journalforingstatus,
            )
            is HendelseType.NavGodkjennUtkast -> journalforHovedvedtak(
                hendelse,
                hendelse.payload.utkast,
                journalforingstatus,
            )
            is HendelseType.AvsluttDeltakelse,
            is HendelseType.EndreDeltakelsesmengde,
            is HendelseType.EndreSluttdato,
            is HendelseType.EndreStartdato,
            is HendelseType.ForlengDeltakelse,
            is HendelseType.IkkeAktuell,
            is HendelseType.EndreInnhold,
            is HendelseType.EndreBakgrunnsinformasjon,
            is HendelseType.LeggTilOppstartsdato,
            -> handleEndringsvedtak(hendelse, journalforingstatus)

            is HendelseType.EndreSluttarsak,
            is HendelseType.EndreUtkast,
            is HendelseType.OpprettUtkast,
            is HendelseType.AvbrytUtkast,
            is HendelseType.DeltakerSistBesokt,
            -> {
            }
        }
    }

    private suspend fun journalforHovedvedtak(
        hendelse: Hendelse,
        utkast: Utkast,
        journalforingstatus: Journalforingstatus?,
    ) {
        val navBruker = amtPersonClient.hentNavBruker(hendelse.deltaker.personident)
        if (journalforingstatus == null || !journalforingstatus.erJournalfort()) {
            val veileder = when (hendelse.ansvarlig) {
                is HendelseAnsvarlig.NavVeileder -> hendelse.ansvarlig
                is HendelseAnsvarlig.Deltaker,
                is HendelseAnsvarlig.Arrangor,
                -> throw IllegalArgumentException(
                    "Deltaker eller arrangør kan ikke være ansvarlig for vedtaket",
                )
            }
            val aktivOppfolgingsperiode = navBruker.getAktivOppfolgingsperiode()
                ?: throw IllegalArgumentException(
                    "Kan ikke endre på deltaker ${hendelse.deltaker.id} som ikke har aktiv oppfølgingsperiode",
                )
            val sak = veilarboppfolgingClient.opprettEllerHentSak(aktivOppfolgingsperiode.id)
            val pdf = pdfgenClient.genererHovedvedtak(
                lagHovedvedtakPdfDto(
                    deltaker = hendelse.deltaker,
                    navBruker = navBruker,
                    utkast = utkast,
                    veileder = veileder,
                    vedtaksdato = hendelse.opprettet.toLocalDate(),
                    begrunnelseFraNav = hendelse.getBegrunnelseForHovedvedtak(),
                ),
            )

            val journalpostId = dokarkivClient.opprettJournalpost(
                hendelseId = hendelse.id,
                fnr = hendelse.deltaker.personident,
                sak = sak,
                pdf = pdf,
                journalforendeEnhet = veileder.enhet.enhetsnummer,
                tiltakstype = hendelse.deltaker.deltakerliste.tiltak,
                endring = false,
            )

            val nyJournalforingstatus = Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = journalpostId,
                bestillingsId = null,
                kanIkkeDistribueres = null,
                kanIkkeJournalfores = false,
            )
            journalforingstatusRepository.upsert(nyJournalforingstatus)

            distribuer(listOf(hendelse), journalpostId, navBruker.harAdresse())
        } else {
            distribuer(listOf(hendelse), journalforingstatus.journalpostId!!, navBruker.harAdresse())
        }

        log.info("Journalførte hovedvedtak for deltaker ${hendelse.deltaker.id}")
    }

    private fun handleEndringsvedtak(hendelse: Hendelse, journalforingstatus: Journalforingstatus?) {
        if (hendelse.deltaker.forsteVedtakFattet == null) {
            log.error("Deltaker med id ${hendelse.deltaker.id} mangler fattet-dato for første vedtak")
            throw IllegalStateException("Kan ikke journalføre endringsvedtak hvis opprinnelig vedtak ikke er fattet")
        }
        journalforingstatusRepository.upsert(
            Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = journalforingstatus?.journalpostId,
                bestillingsId = journalforingstatus?.bestillingsId,
                kanIkkeDistribueres = journalforingstatus?.kanIkkeDistribueres,
                kanIkkeJournalfores = journalforingstatus?.kanIkkeJournalfores,
            ),
        )
        log.info("Endringsvedtak for hendelse ${hendelse.id} er lagret og plukkes opp av asynkron jobb")
    }

    suspend fun journalforOgDistribuerEndringsvedtak(hendelseMedJournalforingstatuser: List<HendelseMedJournalforingstatus>) {
        if (hendelseMedJournalforingstatuser.isEmpty()) {
            return
        }

        val sisteHendelse = hendelseMedJournalforingstatuser.maxBy { it.hendelse.opprettet }
        if (hendelseMedJournalforingstatuser.find { it.hendelse.deltaker.id != sisteHendelse.hendelse.deltaker.id } != null) {
            throw IllegalArgumentException("Alle hendelser må tilhøre samme deltaker!")
        }

        val journalforteHendelser = hendelseMedJournalforingstatuser.filter { it.journalforingstatus.erJournalfort() }
        val ikkeJournalforteHendelser = hendelseMedJournalforingstatuser.filter { !it.journalforingstatus.erJournalfort() }
            .map { it.hendelse }

        val navBruker = amtPersonClient.hentNavBruker(sisteHendelse.hendelse.deltaker.personident)
        if (ikkeJournalforteHendelser.isNotEmpty()) {
            val journalpostId = journalforEndringsvedtak(ikkeJournalforteHendelser, navBruker) ?: return
            distribuer(ikkeJournalforteHendelser, journalpostId, navBruker.harAdresse())
        }

        if (journalforteHendelser.isNotEmpty()) {
            val unikeJournalpostIder = journalforteHendelser.distinctBy { it.journalforingstatus.journalpostId }
                .mapNotNull { it.journalforingstatus.journalpostId }
            val journalpostHendelseMap =
                unikeJournalpostIder.associateWith {
                        journalpostid ->
                    journalforteHendelser.filter { it.journalforingstatus.journalpostId == journalpostid }
                }
            journalpostHendelseMap.entries.forEach { entry ->
                distribuer(journalpostId = entry.key, hendelser = entry.value.map { it.hendelse }, harAdresse = navBruker.harAdresse())
            }
        }

        log.info(
            "Journalførte og distribuerte endringsvedtak for deltaker ${hendelseMedJournalforingstatuser.first().hendelse.deltaker.id}, " +
                "hendelser ${hendelseMedJournalforingstatuser.map { it.hendelse.id }.joinToString()}",
        )
    }

    private suspend fun journalforEndringsvedtak(ikkeJournalforteHendelser: List<Hendelse>, navBruker: NavBruker): String? {
        val nyesteHendelse = ikkeJournalforteHendelser.maxBy { it.opprettet }
        val ansvarlig = getAnsvarlig(nyesteHendelse, ikkeJournalforteHendelser)
        val aktivOppfolgingsperiode = navBruker.getAktivOppfolgingsperiode()

        if (aktivOppfolgingsperiode == null) {
            val kanIkkeJournalfores = ikkeJournalforteHendelser.filter { it.tillattEndringUtenAktivOppfolgingsperiode() }
            kanIkkeJournalfores.forEach {
                journalforingstatusRepository.upsert(
                    Journalforingstatus(
                        hendelseId = it.id,
                        journalpostId = null,
                        bestillingsId = null,
                        kanIkkeDistribueres = null,
                        kanIkkeJournalfores = true,
                    ),
                )
                log.warn("Lar være å journalføre endring som kan gjøres uten oppfølging, hendelseId ${it.id}")
            }
            if (kanIkkeJournalfores.size != ikkeJournalforteHendelser.size) {
                throw IllegalArgumentException(
                    "Kan ikke endre på deltaker ${nyesteHendelse.deltaker.id} som ikke har aktiv oppfølgingsperiode",
                )
            }
            return null
        }
        val sak = veilarboppfolgingClient.opprettEllerHentSak(aktivOppfolgingsperiode.id)
        val pdf = pdfgenClient.endringsvedtak(
            lagEndringsvedtakPdfDto(
                nyesteHendelse.deltaker,
                navBruker,
                ansvarlig,
                ikkeJournalforteHendelser,
                nyesteHendelse.opprettet.toLocalDate(),
            ),
        )

        val journalpostId = dokarkivClient.opprettJournalpost(
            hendelseId = nyesteHendelse.id,
            fnr = nyesteHendelse.deltaker.personident,
            sak = sak,
            pdf = pdf,
            journalforendeEnhet = getJournalforendeEnhet(ansvarlig),
            tiltakstype = nyesteHendelse.deltaker.deltakerliste.tiltak,
            endring = true,
        )

        ikkeJournalforteHendelser.forEach {
            journalforingstatusRepository.upsert(
                Journalforingstatus(
                    hendelseId = it.id,
                    journalpostId = journalpostId,
                    bestillingsId = null,
                    kanIkkeDistribueres = null,
                    kanIkkeJournalfores = false,
                ),
            )
        }
        log.info(
            "Journalførte endringsvedtak for deltaker ${ikkeJournalforteHendelser.first().deltaker.id}, " +
                "hendelser ${ikkeJournalforteHendelser.map { it.id }.joinToString()}",
        )
        return journalpostId
    }

    private suspend fun distribuer(
        hendelser: List<Hendelse>,
        journalpostId: String,
        harAdresse: Boolean,
    ) {
        if (hendelser.isEmpty()) {
            return
        }
        val nyesteHendelse = hendelser.maxBy { it.opprettet }
        if (!DigitalBrukerService.skalDistribueresDigitalt(nyesteHendelse.distribusjonskanal, nyesteHendelse.manuellOppfolging)) {
            if (!harAdresse) {
                log.warn("Kan ikke distribuere journalpost $journalpostId fordi bruker mangler adresse i PDL")
                hendelser.forEach {
                    journalforingstatusRepository.upsert(
                        Journalforingstatus(
                            hendelseId = it.id,
                            journalpostId = journalpostId,
                            bestillingsId = null,
                            kanIkkeDistribueres = true,
                            kanIkkeJournalfores = false,
                        ),
                    )
                }
            } else {
                val bestillingsId = dokdistfordelingClient.distribuerJournalpost(journalpostId)
                hendelser.forEach {
                    journalforingstatusRepository.upsert(
                        Journalforingstatus(
                            hendelseId = it.id,
                            journalpostId = journalpostId,
                            bestillingsId = bestillingsId,
                            kanIkkeDistribueres = bestillingsId == null,
                            kanIkkeJournalfores = false,
                        ),
                    )
                }
            }
        }
    }

    private fun hendelseErBehandlet(
        journalforingstatus: Journalforingstatus?,
        distribusjonskanal: Distribusjonskanal,
        manuellOppfolging: Boolean,
    ): Boolean {
        return journalforingstatus != null && journalforingstatus.erJournalfort() && journalforingstatus.erDistribuert(
            distribusjonskanal,
            manuellOppfolging,
        )
    }

    private fun getAnsvarlig(nyesteHendelse: Hendelse, ikkeJournalforteHendelser: List<Hendelse>): HendelseAnsvarlig {
        if (nyesteHendelse.ansvarlig is HendelseAnsvarlig.NavVeileder) {
            return nyesteHendelse.ansvarlig
        }
        ikkeJournalforteHendelser.firstOrNull { it.ansvarlig is HendelseAnsvarlig.NavVeileder }?.let { return it.ansvarlig }
        ikkeJournalforteHendelser.firstOrNull { it.ansvarlig is HendelseAnsvarlig.Arrangor }?.let { return it.ansvarlig }

        throw IllegalArgumentException("Må ha en ansvarlig som er enten veileder eller arrangør")
    }

    private fun getJournalforendeEnhet(ansvarlig: HendelseAnsvarlig): String = when (ansvarlig) {
        is HendelseAnsvarlig.NavVeileder -> ansvarlig.enhet.enhetsnummer
        is HendelseAnsvarlig.Arrangor -> ansvarlig.enhet.enhetsnummer
        is HendelseAnsvarlig.Deltaker -> throw IllegalArgumentException("Kan ikke journalføre endringsvedtak fra deltaker")
    }
}
