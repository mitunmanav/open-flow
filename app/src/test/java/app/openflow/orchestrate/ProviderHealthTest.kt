package app.openflow.orchestrate

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProviderHealthTest {

    @Test
    fun initialState_isHealthy() {
        val health = ProviderHealth()
        assertThat(health.state("whisper")).isEqualTo(HealthState.HEALTHY)
        assertThat(health.isAvailable("whisper")).isTrue()
    }

    @Test
    fun oneFailure_isDegraded() {
        val health = ProviderHealth(failThreshold = 3)
        health.recordFailure("whisper")
        assertThat(health.state("whisper")).isEqualTo(HealthState.DEGRADED)
        assertThat(health.isAvailable("whisper")).isTrue()
    }

    @Test
    fun belowThresholdFailures_staysDegraded() {
        val health = ProviderHealth(failThreshold = 3)
        health.recordFailure("whisper")
        health.recordFailure("whisper")
        assertThat(health.state("whisper")).isEqualTo(HealthState.DEGRADED)
        assertThat(health.isAvailable("whisper")).isTrue()
    }

    @Test
    fun thresholdFailures_becomesUnavailable() {
        val health = ProviderHealth(failThreshold = 3)
        repeat(3) { health.recordFailure("whisper") }
        assertThat(health.state("whisper")).isEqualTo(HealthState.UNAVAILABLE)
        assertThat(health.isAvailable("whisper")).isFalse()
    }

    @Test
    fun successResets_toHealthy() {
        val health = ProviderHealth(failThreshold = 3)
        health.recordFailure("whisper")
        health.recordFailure("whisper")
        health.recordSuccess("whisper")
        assertThat(health.state("whisper")).isEqualTo(HealthState.HEALTHY)
        assertThat(health.isAvailable("whisper")).isTrue()
    }

    @Test
    fun successAfterUnavailable_resetsToHealthy() {
        val health = ProviderHealth(failThreshold = 3)
        repeat(3) { health.recordFailure("whisper") }
        health.recordSuccess("whisper")
        assertThat(health.state("whisper")).isEqualTo(HealthState.HEALTHY)
        assertThat(health.isAvailable("whisper")).isTrue()
    }

    @Test
    fun recoversAfterCooldown() {
        var now = 0L
        val health = ProviderHealth(failThreshold = 3, cooldownMs = 60_000L) { now }
        repeat(3) { health.recordFailure("whisper") }
        assertThat(health.state("whisper")).isEqualTo(HealthState.UNAVAILABLE)

        now = 60_000L
        assertThat(health.state("whisper")).isEqualTo(HealthState.HEALTHY)
        assertThat(health.isAvailable("whisper")).isTrue()
    }

    @Test
    fun stillUnavailable_beforeCooldownExpires() {
        var now = 0L
        val health = ProviderHealth(failThreshold = 3, cooldownMs = 60_000L) { now }
        repeat(3) { health.recordFailure("whisper") }

        now = 59_999L
        assertThat(health.state("whisper")).isEqualTo(HealthState.UNAVAILABLE)
        assertThat(health.isAvailable("whisper")).isFalse()
    }

    @Test
    fun providersAreIndependent() {
        val health = ProviderHealth(failThreshold = 3)
        repeat(3) { health.recordFailure("a") }
        assertThat(health.state("a")).isEqualTo(HealthState.UNAVAILABLE)
        assertThat(health.state("b")).isEqualTo(HealthState.HEALTHY)
    }
}
