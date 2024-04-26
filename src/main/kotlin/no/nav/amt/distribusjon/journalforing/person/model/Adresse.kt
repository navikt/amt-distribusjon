package no.nav.amt.distribusjon.journalforing.person.model

data class Adresse(
    val bostedsadresse: Bostedsadresse?,
    val oppholdsadresse: Oppholdsadresse?,
    val kontaktadresse: Kontaktadresse?,
)

fun Adresse.toAdresselinjer(): List<String> {
    val forenkletAdresse = kontaktadresse?.toForenkletAdresse()
        ?: oppholdsadresse?.toForenkletAdresse()
        ?: bostedsadresse?.toForenkletAdresse()
    return forenkletAdresse.toList()
}

data class ForenkletAdresse(
    val postnummer: String,
    val poststed: String,
    val tilleggsnavn: String?,
    val adressenavn: String?,
)

fun ForenkletAdresse?.toList(): List<String> {
    if (this == null) return emptyList()

    return listOfNotNull(
        tilleggsnavn,
        adressenavn,
        "$postnummer $poststed",
    )
}

data class Bostedsadresse(
    val coAdressenavn: String?,
    val vegadresse: Vegadresse?,
    val matrikkeladresse: Matrikkeladresse?,
) {
    fun toForenkletAdresse() = vegadresse?.toAdresse(coAdressenavn)
        ?: matrikkeladresse?.toAdresse(coAdressenavn)
        ?: error("Bostedsadresse må ha enten veiadresse eller matrikkeladresse")
}

data class Oppholdsadresse(
    val coAdressenavn: String?,
    val vegadresse: Vegadresse?,
    val matrikkeladresse: Matrikkeladresse?,
) {
    fun toForenkletAdresse() = vegadresse?.toAdresse(coAdressenavn)
        ?: matrikkeladresse?.toAdresse(coAdressenavn)
        ?: error("Oppholdsadresse må ha enten veiadresse eller matrikkeladresse")
}

data class Kontaktadresse(
    val coAdressenavn: String?,
    val vegadresse: Vegadresse?,
    val postboksadresse: Postboksadresse?,
) {
    fun toForenkletAdresse() = vegadresse?.toAdresse(coAdressenavn)
        ?: postboksadresse?.toAdresse(coAdressenavn)
        ?: error("Kontaktadresse må ha enten veiadresse eller postboksadresse")
}

data class Vegadresse(
    val husnummer: String?,
    val husbokstav: String?,
    val adressenavn: String?,
    val tilleggsnavn: String?,
    val postnummer: String,
    val poststed: String,
) {
    fun toAdresse(coAdressenavn: String?) = ForenkletAdresse(
        postnummer = postnummer,
        poststed = poststed,
        tilleggsnavn = tilleggsnavn,
        adressenavn = tilAdressenavn(coAdressenavn),
    )

    private fun tilAdressenavn(coAdressenavn: String?): String {
        val adressenavn = "${(adressenavn ?: "")} ${(husnummer ?: "")}${(husbokstav ?: "")}"
        if (coAdressenavn.isNullOrEmpty()) {
            return adressenavn
        }
        return "$coAdressenavn, $adressenavn"
    }
}

data class Matrikkeladresse(
    val tilleggsnavn: String?,
    val postnummer: String,
    val poststed: String,
) {
    fun toAdresse(coAdressenavn: String?) = ForenkletAdresse(
        postnummer = postnummer,
        poststed = poststed,
        tilleggsnavn = tilleggsnavn,
        adressenavn = coAdressenavn,
    )
}

data class Postboksadresse(
    val postboks: String,
    val postnummer: String,
    val poststed: String,
) {
    fun toAdresse(coAdressenavn: String?) = ForenkletAdresse(
        postnummer = postnummer,
        poststed = poststed,
        tilleggsnavn = null,
        adressenavn = tilAdressenavn(coAdressenavn),
    )

    private fun tilAdressenavn(coAdressenavn: String?): String {
        if (coAdressenavn.isNullOrEmpty()) {
            return "Postboks $postboks"
        }
        return "$coAdressenavn, postboks $postboks"
    }
}
