package no.nav.amt.distribusjon.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@EnableCaching
@Configuration(proxyBeanMethods = false)
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager = CaffeineCacheManager().apply {
        registerCustomCache(
            DISTRIBUSJONSKANAL_CACHE,
            Caffeine
                .newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .build(),
        )
        registerCustomCache(
            MANUELL_OPPFOLGING_CACHE,
            Caffeine
                .newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .build(),
        )
    }

    companion object {
        const val DISTRIBUSJONSKANAL_CACHE = "distribusjonskanalCache"
        const val MANUELL_OPPFOLGING_CACHE = "manuellOppfolgingCache"
    }
}
