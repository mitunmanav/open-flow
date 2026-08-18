package app.openflow.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AudioFileManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var baseDir: File
    private lateinit var manager: AudioFileManager

    @Before
    fun setUp() {
        baseDir = tempFolder.newFolder("audio_test")
        manager = AudioFileManager(baseDir)
    }

    @Test
    fun resolves_audio_file_for_session_id() {
        val file = manager.getAudioFile("session-123")
        assertThat(file.name).isEqualTo("session-123.wav")
        assertThat(file.parentFile?.absolutePath).isEqualTo(baseDir.absolutePath)
    }

    @Test
    fun checks_audio_existence() {
        assertThat(manager.hasAudio("session-456")).isFalse()
        val file = manager.getAudioFile("session-456")
        file.writeBytes(ByteArray(64) { 1 })
        assertThat(manager.hasAudio("session-456")).isTrue()
    }

    @Test
    fun deletes_audio_file() {
        val file = manager.getAudioFile("session-789")
        file.writeBytes(ByteArray(64) { 5 })
        assertThat(manager.hasAudio("session-789")).isTrue()

        val deleted = manager.deleteAudio("session-789")
        assertThat(deleted).isTrue()
        assertThat(manager.hasAudio("session-789")).isFalse()
    }

    @Test
    fun purge_all_deletes_all_audio_files() {
        manager.getAudioFile("s1").writeBytes(ByteArray(64) { 1 })
        manager.getAudioFile("s2").writeBytes(ByteArray(64) { 2 })
        manager.getAudioFile("s3").writeBytes(ByteArray(64) { 3 })

        assertThat(manager.listAudioSessions()).hasSize(3)
        manager.purgeAll()
        assertThat(manager.listAudioSessions()).isEmpty()
    }
}
