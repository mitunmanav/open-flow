package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContinuousPolicyTest {

    private val p = ContinuousPolicy()

    @Test
    fun not_listening_never_restarts() {
        assertThat(p.shouldRestart(listening = false, errorCode = 6, hadResult = false))
            .isFalse()
    }

    @Test
    fun timeout_while_listening_restarts() {
        // ERROR_SPEECH_TIMEOUT = 6
        assertThat(p.shouldRestart(listening = true, errorCode = 6, hadResult = false))
            .isTrue()
    }

    @Test
    fun no_match_while_listening_restarts() {
        // ERROR_NO_MATCH = 7
        assertThat(p.shouldRestart(listening = true, errorCode = 7, hadResult = false))
            .isTrue()
    }

    @Test
    fun client_error_restarts() {
        // ERROR_CLIENT = 5
        assertThat(p.shouldRestart(listening = true, errorCode = 5, hadResult = false))
            .isTrue()
    }

    @Test
    fun busy_restarts_after_backoff() {
        // ERROR_RECOGNIZER_BUSY = 8
        assertThat(p.shouldRestart(listening = true, errorCode = 8, hadResult = false))
            .isTrue()
        assertThat(p.restartDelayMs(errorCode = 8)).isAtLeast(250L)
    }

    @Test
    fun after_final_result_restarts_if_listening() {
        assertThat(p.shouldRestart(listening = true, errorCode = null, hadResult = true))
            .isTrue()
        assertThat(p.restartDelayMs(errorCode = null)).isAtMost(100L)
    }

    @Test
    fun insufficient_permissions_does_not_restart() {
        // ERROR_INSUFFICIENT_PERMISSIONS = 9
        assertThat(p.shouldRestart(listening = true, errorCode = 9, hadResult = false))
            .isFalse()
    }

    @Test
    fun recreate_recognizer_every_n_sessions() {
        assertThat(p.shouldRecreateRecognizer(sessionCount = 0)).isFalse()
        assertThat(p.shouldRecreateRecognizer(sessionCount = 12)).isTrue()
    }

    @Test
    fun server_disconnected_restarts_and_recreates() {
        // ERROR_SERVER_DISCONNECTED = 11 (API 31)
        assertThat(p.shouldRestart(listening = true, errorCode = 11, hadResult = false))
            .isTrue()
        assertThat(p.shouldRecreateOnError(11)).isTrue()
        assertThat(p.restartDelayMs(errorCode = 11)).isEqualTo(p.normalRestartDelayMs)
    }

    @Test
    fun busy_recreates() {
        assertThat(p.shouldRecreateOnError(8)).isTrue()
    }

    @Test
    fun client_restarts_but_does_not_recreate() {
        assertThat(p.shouldRestart(listening = true, errorCode = 5, hadResult = false))
            .isTrue()
        assertThat(p.shouldRecreateOnError(5)).isFalse()
    }

    @Test
    fun result_restart_stays_fast() {
        assertThat(p.restartDelayMs(errorCode = null)).isEqualTo(60L)
        assertThat(p.normalRestartDelayMs).isEqualTo(60L)
    }

    @Test
    fun language_not_supported_restarts_and_recreates() {
        // ERROR_LANGUAGE_NOT_SUPPORTED = 12 (API 31)
        assertThat(p.shouldRestart(listening = true, errorCode = 12, hadResult = false))
            .isTrue()
        assertThat(p.shouldRecreateOnError(12)).isTrue()
    }

    @Test
    fun language_unavailable_restarts_and_recreates() {
        // ERROR_LANGUAGE_UNAVAILABLE = 13 (API 31)
        assertThat(p.shouldRestart(listening = true, errorCode = 13, hadResult = false))
            .isTrue()
        assertThat(p.shouldRecreateOnError(13)).isTrue()
    }
}
