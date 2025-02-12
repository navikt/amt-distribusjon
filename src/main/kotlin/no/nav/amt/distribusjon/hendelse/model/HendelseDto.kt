package no.nav.amt.distribusjon.hendelse.model

import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal

typealias HendelseDto = no.nav.amt.lib.models.hendelse.Hendelse

fun HendelseDto.toModel(distribusjonskanal: Distribusjonskanal, manuellOppfolging: Boolean) = Hendelse(
    id,
    opprettet,
    deltaker,
    ansvarlig,
    payload,
    distribusjonskanal,
    manuellOppfolging,
)
