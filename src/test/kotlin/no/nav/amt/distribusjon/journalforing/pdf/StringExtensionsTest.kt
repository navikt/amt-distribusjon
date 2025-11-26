package no.nav.amt.distribusjon.journalforing.pdf

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class StringExtensionsTest {
    @ParameterizedTest
    @CsvSource(
        // Ingen punktum
        "'Tekst', 'Tekst'",
        "' Tekst ', 'Tekst'",
        "'Tekst ', 'Tekst'",
        // Ett avsluttende punktum
        "'Tekst.', 'Tekst'",
        "'Tekst. ', 'Tekst'",
        "' Tekst. ', 'Tekst'",
        // Flere avsluttende punktum
        "'Tekst..', 'Tekst'",
        "' Tekst... ', 'Tekst'",
        // Punktum inne i teksten skal ikke berøres
        "'En setning. Med mer.', 'En setning. Med mer'",
        "' En setning. Med mer. ', 'En setning. Med mer'",
        // Bare punktum
        "'.', ''",
        "'..', ''",
        "' ... ', ''",
        // Tomme strenger
        "'', ''",
        "'   ', ''",
    )
    fun `trimOgFjernAvsluttendePunktum fjerner avsluttende punktum og trimmer whitespace`(input: String, expected: String) {
        input.trimOgFjernAvsluttendePunktum() shouldBe expected
    }
}
