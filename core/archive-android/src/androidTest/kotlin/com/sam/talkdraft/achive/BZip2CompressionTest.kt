package com.sam.talkdraft.achive

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import com.sam.talkdraft.archive_android.NativeBzip2Compressor
import com.sam.talkdraft.archive_android.exceptions.NativeCompressionException
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import okio.Path.Companion.toOkioPath
import org.junit.Rule
import org.junit.rules.TemporaryFolder

@RunWithPlatform
class BZip2CompressionTest {

    private lateinit var compressor: NativeBzip2Compressor

    @get:Rule
    val tempDirectory = TemporaryFolder()

    @BeforeTest
    fun setup() {
        compressor = NativeBzip2Compressor()
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.delete()
    }

    @Test
    fun compress_and_decompress_text_file() {
        val input = tempDirectory.newFile("input.txt")
        val compressed = tempDirectory.root.resolve("input.txt.bz2")
        val output = tempDirectory.root.resolve("output.txt")

        val content = """
            Hello from BZip2.
            This is a compression test.
            Native compression should preserve this content exactly.
        """.trimIndent()

        input.writeText(content)

        compressor.compress(input.toOkioPath(), compressed.toOkioPath())
        compressor.decompress(compressed.toOkioPath(), output.toOkioPath())

        assertThat(output.readText()).isEqualTo(content)
    }

    @Test
    fun compress_and_decompress_binary_data() {
        val input = tempDirectory.newFile("input.bin")
        val compressed = tempDirectory.root.resolve("input.bin.bz2")
        val output = tempDirectory.root.resolve("output.bin")

        val data = ByteArray(10_000) { index -> (index % 256).toByte() }

        input.writeBytes(data)

        compressor.compress(input.toOkioPath(), compressed.toOkioPath())
        compressor.decompress(compressed.toOkioPath(), output.toOkioPath())

        assertThat(output.readBytes())
            .isEqualTo(data)
    }

    @Test
    fun compress_and_decompress_empty_file() {
        val input = tempDirectory.newFile("empty.txt")
        val compressed = tempDirectory.root.resolve("empty.txt.bz2")
        val output = tempDirectory.root.resolve("output.txt")

        input.writeBytes(ByteArray(0))

        compressor.compress(input.toOkioPath(), compressed.toOkioPath())
        compressor.decompress(compressed.toOkioPath(), output.toOkioPath())

        assertThat(output.length())
            .isEqualTo(0)
    }

    @Test
    fun compress_and_decompress_large_file() {
        val input = tempDirectory.newFile("large.bin")
        val compressed = tempDirectory.root.resolve("large.bin.bz2")
        val output = tempDirectory.root.resolve("output.bin")

        // 100 Mb file
        val data = ByteArray(5 * 1024 * 1024) { index ->
            when (index % 4) {
                0 -> 0
                1 -> 1
                2 -> 2
                else -> 3
            }.toByte()
        }

        input.writeBytes(data)

        compressor.compress(input.toOkioPath(), compressed.toOkioPath())

        compressor.decompress(compressed.toOkioPath(), output.toOkioPath())

        assertThat(output.readBytes())
            .isEqualTo(data)
    }

    @Test
    fun compressed_file_is_smaller_for_highly_repetitive_data() {
        val input = tempDirectory.newFile("repetitive.txt")
        val compressed = tempDirectory.root.resolve("repetitive.txt.bz2")

        val content = "The quick brown fox jumps over the lazy dog.\n"
            .repeat(100_000)

        input.writeText(content)

        compressor.compress(
            input.toOkioPath(),
            compressed.toOkioPath(),
        )

        assertThat(compressed.length())
            .isLessThan(input.length())
    }

    @Test
    fun compress_and_decompress_unicode_text() {
        val input = tempDirectory.newFile("unicode.txt")
        val compressed = tempDirectory.root.resolve("unicode.txt.bz2")
        val output = tempDirectory.root.resolve("output.txt")

        val content = """
            Hello
            नमस्ते
            বাংলা
            日本語
            한국어
            Привет
            مرحبا
        """.trimIndent()

        input.writeText(
            content,
            Charsets.UTF_8,
        )

        compressor.compress(
            input.toOkioPath(),
            compressed.toOkioPath(),
        )

        compressor.decompress(
            compressed.toOkioPath(),
            output.toOkioPath(),
        )

        assertThat(output.readBytes())
            .isEqualTo(input.readBytes())
    }

    @Test
    fun compress_throws_when_input_does_not_exist() {
        val input = tempDirectory.root
            .resolve("does-not-exist.txt")

        val output = tempDirectory.root
            .resolve("output.bz2")

        val exception = assertFailsWith<NativeCompressionException> {
            compressor.compress(
                input.toOkioPath(),
                output.toOkioPath(),
            )
        }

        assertThat(exception.message)
            .isNotNull()
            .isNotEmpty()
    }

    @Test
    fun decompress_throws_for_invalid_bzip2_file() {
        val input = tempDirectory.newFile("invalid.bz2")
        val output = tempDirectory.root.resolve("output.txt")

        input.writeText("This is not a valid BZip2 stream")

        val exception = assertFailsWith<NativeCompressionException> {
            compressor.decompress(
                input.toOkioPath(),
                output.toOkioPath(),
            )
        }

        assertThat(exception.message)
            .isNotNull()
            .isNotEmpty()
    }

    @Test
    fun decompress_throws_for_corrupted_bzip2() {
        val input = tempDirectory.newFile("input.txt")
        val compressed = tempDirectory.root.resolve("input.bz2")
        val corrupted = tempDirectory.root.resolve("corrupted.bz2")
        val output = tempDirectory.root.resolve("output.txt")

        input.writeText(
            "This content will be corrupted."
                .repeat(1000),
        )

        compressor.compress(
            input.toOkioPath(),
            compressed.toOkioPath(),
        )

        val bytes = compressed.readBytes()

        // Let's do some mischief.
        val middle = bytes.size / 2

        bytes[middle] = (
            bytes[middle].toInt() xor 0xFF
            ).toByte()

        corrupted.writeBytes(bytes)

        assertFailsWith<NativeCompressionException> {
            compressor.decompress(
                corrupted.toOkioPath(),
                output.toOkioPath(),
            )
        }
    }

    @Test
    fun round_trip_preserves_exact_bytes() {
        val input = tempDirectory.newFile("input.bin")
        val compressed = tempDirectory.root.resolve("input.bz2")
        val output = tempDirectory.root.resolve("output.bin")

        val original = ByteArray(1_000_000) { index ->
            ((index * 31 + 17) and 0xFF).toByte()
        }

        input.writeBytes(original)

        compressor.compress(
            input.toOkioPath(),
            compressed.toOkioPath(),
        )

        compressor.decompress(
            compressed.toOkioPath(),
            output.toOkioPath(),
        )

        assertThat(output.readBytes()).isEqualTo(original)
    }

    @Test
    fun decompress_reports_progress_monotonically() {
        val input = tempDirectory.newFile("input.bin")
        val compressed = tempDirectory.root.resolve("input.bz2")
        val output = tempDirectory.root.resolve("output.bin")

        val original = ByteArray(500_000) { index ->
            ((index * 17 + 5) and 0xFF).toByte()
        }

        input.writeBytes(original)
        compressor.compress(input.toOkioPath(), compressed.toOkioPath())

        val progressValues = mutableListOf<Float>()
        compressor.decompress(
            compressed.toOkioPath(),
            output.toOkioPath(),
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
    fun compress_reports_progress_monotonically() {
        val input = tempDirectory.newFile("input.bin")
        val compressed = tempDirectory.root.resolve("input.bz2")

        val original = ByteArray(500_000) { index ->
            ((index * 17 + 5) and 0xFF).toByte()
        }

        input.writeBytes(original)

        val progressValues = mutableListOf<Float>()
        compressor.compress(
            input.toOkioPath(),
            compressed.toOkioPath(),
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
