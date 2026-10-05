package com.sam.talkdraft.achive

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import com.sam.talkdraft.archive_android.NativeTarArchiver
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import okio.Path.Companion.toOkioPath
import org.junit.Rule
import org.junit.rules.TemporaryFolder

@RunWithPlatform
class TarArchiverTest {

    private lateinit var archiver: NativeTarArchiver

    @get:Rule
    val tempDirectory = TemporaryFolder()

    @BeforeTest
    fun setup() {
        archiver = NativeTarArchiver()
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.delete()
    }

    @Test
    fun create_tar_from_directory_and_extract() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")
        val destination = tempDirectory.root.resolve("destination")

        source.mkdirs()

        source.resolve("hello.txt").writeText("Hello from TAR")
        val create = archiver.createTar(source.toOkioPath(), archive.toOkioPath())
        assertThat(create.isSuccess).isTrue()

        assertThat(archive.exists()).isTrue()
        assertThat(archive.length()).isGreaterThan(0)

        val extract = archiver.extractTar(archive.toOkioPath(), destination.toOkioPath())
        assertThat(extract.isSuccess).isTrue()

        println(destination.listFiles()?.filterNotNull()?.map { it.name })

        val extracted = destination.resolve("hello.txt")

        assertThat(extracted.exists()).isTrue()
        assertThat(extracted.readText()).isEqualTo("Hello from TAR")
    }

    @Test
    fun create_tar_with_multiple_files() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")
        val destination = tempDirectory.root.resolve("destination")

        source.mkdirs()

        source.resolve("one.txt").writeText("one")
        source.resolve("two.txt").writeText("two")
        source.resolve("three.txt").writeText("three")

        assertThat(
            archiver.createTar(
                source.toOkioPath(),
                archive.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(
            archiver.extractTar(
                archive.toOkioPath(),
                destination.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(
            destination.resolve("one.txt").readText(),
        ).isEqualTo("one")

        assertThat(
            destination.resolve("two.txt").readText(),
        ).isEqualTo("two")

        assertThat(
            destination.resolve("three.txt").readText(),
        ).isEqualTo("three")
    }

    @Test
    fun create_tar_with_nested_directories() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")
        val destination = tempDirectory.root.resolve("destination")

        val nested = source
            .resolve("level1")
            .resolve("level2")

        nested.mkdirs()

        nested.resolve("nested.txt").writeText(
            "Nested file content",
        )

        assertThat(
            archiver.createTar(
                source.toOkioPath(),
                archive.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(
            archiver.extractTar(
                archive.toOkioPath(),
                destination.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        val extracted = destination
            .resolve("level1")
            .resolve("level2")
            .resolve("nested.txt")

        assertThat(extracted.exists()).isTrue()
        assertThat(extracted.readText())
            .isEqualTo("Nested file content")
    }

    @Test
    fun create_tar_preserves_empty_file() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")
        val destination = tempDirectory.root.resolve("destination")

        source.mkdirs()

        source.resolve("empty.txt").writeBytes(
            ByteArray(0),
        )

        assertThat(
            archiver.createTar(
                source.toOkioPath(),
                archive.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(
            archiver.extractTar(
                archive.toOkioPath(),
                destination.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        val extracted = destination.resolve("empty.txt")

        assertThat(extracted.exists()).isTrue()
        assertThat(extracted.length()).isEqualTo(0)
    }

    @Test
    fun create_tar_preserves_empty_directory() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")
        val destination = tempDirectory.root.resolve("destination")

        source.mkdirs()

        source.resolve("empty-directory").mkdirs()

        assertThat(
            archiver.createTar(
                source.toOkioPath(),
                archive.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(
            archiver.extractTar(
                archive.toOkioPath(),
                destination.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        val extracted = destination.resolve("empty-directory")

        assertThat(extracted.exists()).isTrue()
        assertThat(extracted.isDirectory).isTrue()
    }

    @Test
    fun create_tar_preserves_binary_data() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")
        val destination = tempDirectory.root.resolve("destination")

        source.mkdirs()

        val original = ByteArray(100_000) { index ->
            ((index * 31 + 17) and 0xFF).toByte()
        }

        source.resolve("data.bin").writeBytes(original)

        assertThat(
            archiver.createTar(
                source.toOkioPath(),
                archive.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(
            archiver.extractTar(
                archive.toOkioPath(),
                destination.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        val extracted = destination.resolve("data.bin")

        assertThat(extracted.readBytes())
            .isEqualTo(original)
    }

    @Test
    fun create_tar_preserves_unicode_content() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")
        val destination = tempDirectory.root.resolve("destination")

        source.mkdirs()

        val content = """
            Hello
            नमस्ते
            বাংলা
            日本語
            한국어
            Привет
            مرحبا
        """.trimIndent()

        source.resolve("unicode.txt").writeText(content)

        assertThat(
            archiver.createTar(
                source.toOkioPath(),
                archive.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(
            archiver.extractTar(
                archive.toOkioPath(),
                destination.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        val extracted = destination.resolve("unicode.txt")

        assertThat(extracted.readText())
            .isEqualTo(content)
    }

    @Test
    fun create_tar_preserves_unicode_filename() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")
        val destination = tempDirectory.root.resolve("destination")

        source.mkdirs()

        val filename = "বাংলা-日本語-नमस्ते.txt"

        source.resolve(filename).writeText(
            "Unicode filename test",
        )

        assertThat(
            archiver.createTar(
                source.toOkioPath(),
                archive.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(
            archiver.extractTar(
                archive.toOkioPath(),
                destination.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        val extracted = destination.resolve(filename)

        assertThat(extracted.exists()).isTrue()
        assertThat(extracted.readText())
            .isEqualTo("Unicode filename test")
    }

    @Test
    fun create_tar_creates_missing_output_parent_directories() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root
            .resolve("archives")
            .resolve("nested")
            .resolve("archive.tar")

        source.mkdirs()

        source.resolve("test.txt").writeText("test")

        assertThat(
            archiver.createTar(
                source.toOkioPath(),
                archive.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(archive.exists()).isTrue()
    }

    @Test
    fun extract_tar_creates_missing_destination_directory() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")
        val destination = tempDirectory.root
            .resolve("output")
            .resolve("nested")

        source.mkdirs()

        source.resolve("test.txt").writeText("test")

        assertThat(
            archiver.createTar(
                source.toOkioPath(),
                archive.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(destination.exists()).isFalse()

        assertThat(
            archiver.extractTar(
                archive.toOkioPath(),
                destination.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(destination.exists()).isTrue()
        assertThat(destination.resolve("test.txt").readText())
            .isEqualTo("test")
    }

    @Test
    fun create_tar_fails_when_source_is_not_directory() {
        val source = tempDirectory.newFile("file.txt")
        val archive = tempDirectory.root.resolve("archive.tar")

        source.writeText("not a directory")

        val result = archiver.createTar(
            source.toOkioPath(),
            archive.toOkioPath(),
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun create_tar_fails_when_source_does_not_exist() {
        val source = tempDirectory.root.resolve("missing")
        val archive = tempDirectory.root.resolve("archive.tar")

        val result = archiver.createTar(
            source.toOkioPath(),
            archive.toOkioPath(),
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun extract_tar_fails_when_archive_does_not_exist() {
        val archive = tempDirectory.root.resolve("missing.tar")
        val destination = tempDirectory.root.resolve("destination")

        val result = archiver.extractTar(
            archive.toOkioPath(),
            destination.toOkioPath(),
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun extract_tar_fails_for_invalid_tar_file() {
        val archive = tempDirectory.newFile("invalid.tar")
        val destination = tempDirectory.root.resolve("destination")

        archive.writeText(
            "This is not a valid TAR archive",
        )

        val result = archiver.extractTar(
            archive.toOkioPath(),
            destination.toOkioPath(),
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun round_trip_preserves_complete_directory_structure() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")
        val destination = tempDirectory.root.resolve("destination")

        source.resolve("documents").mkdirs()
        source.resolve("images").mkdirs()
        source.resolve("empty").mkdirs()

        source.resolve("root.txt").writeText("root")

        source.resolve("documents")
            .resolve("document.txt")
            .writeText("document")

        source.resolve("images")
            .resolve("data.bin")
            .writeBytes(
                ByteArray(50_000) { index ->
                    (index % 256).toByte()
                },
            )

        assertThat(
            archiver.createTar(
                source.toOkioPath(),
                archive.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(
            archiver.extractTar(
                archive.toOkioPath(),
                destination.toOkioPath(),
            ).isSuccess,
        ).isTrue()

        assertThat(
            destination.resolve("root.txt").readText(),
        ).isEqualTo("root")

        assertThat(
            destination
                .resolve("documents")
                .resolve("document.txt")
                .readText(),
        ).isEqualTo("document")

        assertThat(
            destination
                .resolve("images")
                .resolve("data.bin")
                .readBytes(),
        ).isEqualTo(
            ByteArray(50_000) { index ->
                (index % 256).toByte()
            },
        )

        assertThat(
            destination.resolve("empty").isDirectory,
        ).isTrue()
    }

    @Test
    fun create_tar_reports_progress_monotonically() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")

        source.mkdirs()
        source.resolve("large.bin").writeBytes(
            ByteArray(200_000) { index ->
                (index % 256).toByte()
            },
        )

        val progressValues = mutableListOf<Float>()
        val result = archiver.createTar(
            source.toOkioPath(),
            archive.toOkioPath(),
            onProgress = { progress ->
                progressValues.add(progress)
            },
        )

        assertThat(progressValues).isNotEmpty()
        assertThat(progressValues.last()).isEqualTo(100.0f)
        for (i in 1 until progressValues.size) {
            assertThat(progressValues[i]).isGreaterThanOrEqualTo(progressValues[i - 1])
        }
    }

    @Test
    fun extract_tar_reports_progress_monotonically() {
        val source = tempDirectory.root.resolve("source")
        val archive = tempDirectory.root.resolve("archive.tar")
        val destination = tempDirectory.root.resolve("destination")

        source.mkdirs()
        source.resolve("large.bin").writeBytes(
            ByteArray(200_000) { index ->
                (index % 256).toByte()
            },
        )

        archiver.createTar(source.toOkioPath(), archive.toOkioPath()).getOrThrow()

        val progressValues = mutableListOf<Float>()
        val result = archiver.extractTar(
            archive.toOkioPath(),
            destination.toOkioPath(),
            onProgress = { progress ->
                progressValues.add(progress)
            },
        )

        assertThat(progressValues).isNotEmpty()
        assertThat(progressValues.last()).isEqualTo(100.0f)
        for (i in 1 until progressValues.size) {
            assertThat(progressValues[i]).isGreaterThanOrEqualTo(progressValues[i - 1])
        }
    }
}
