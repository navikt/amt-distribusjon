package no.nav.amt.distribusjon.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun LocalDate.toStringDate(): String = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(this)
