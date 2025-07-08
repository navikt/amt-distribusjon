package no.nav.amt.distribusjon

import no.nav.amt.distribusjon.utils.mockAzureAdClient

abstract class ClientTestBase {
    protected val mockAzureAdTokenClient = mockAzureAdClient(testEnvironment)
}
