package no.nav.amt.distribusjon

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.amt.distribusjon.auth.AzureAdTokenClient
import org.junit.Before

abstract class ClientTestBase {
    protected val mockAzureAdTokenClient: AzureAdTokenClient = mockk(relaxed = true)

    @Before
    fun setup() {
        coEvery { mockAzureAdTokenClient.getMachineToMachineToken(any()) } returns "~token~"
    }
}
