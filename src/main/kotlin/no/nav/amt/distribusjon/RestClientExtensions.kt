package no.nav.amt.distribusjon

import org.springframework.web.client.RestClient

inline fun <reified T : Any> RestClient.RequestHeadersSpec<*>.exchangeWithLogging(errorMessage: String): T =
    this.exchange { request, response ->
        if (response.statusCode.is2xxSuccessful) {
            response.bodyTo(T::class.java) ?: error("Tom respons")
        } else {
            val bodyText = response.bodyTo(String::class.java)
            error(
                "$errorMessage. Metode: ${request.method}, status: ${response.statusCode}, error=$bodyText",
            )
        }
    }
