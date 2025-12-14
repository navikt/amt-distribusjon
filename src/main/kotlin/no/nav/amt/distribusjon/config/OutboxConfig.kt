package no.nav.amt.distribusjon.config

import no.nav.amt.distribusjon.job.LeaderElection
import no.nav.amt.lib.utils.job.JobManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class OutboxConfig {
    @Bean
    fun jobManager(leaderElection: LeaderElection) = JobManager(
        isLeader = leaderElection::isLeader,
        applicationIsReady = { true }, // TODO
    )
}
