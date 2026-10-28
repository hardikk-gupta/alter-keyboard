package com.aistudio.altermind.voice

import kotlinx.coroutines.test.runTest
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class ParakeetArchiveTest {
    private val model = "model.int8.onnx"
    private val tokens = "tokens.txt"
    private val bytes = "fixture".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private val components get() = listOf(model, tokens).associateWith { ParakeetComponent(bytes.size.toLong(), hash) }
    private fun archive(root: File, entries: List<String>): File = File(root, "fixture.tar.bz2").also { archive ->
        TarArchiveOutputStream(BZip2CompressorOutputStream(archive.outputStream())).use { tar ->
            entries.forEach { name ->
                val entry = TarArchiveEntry(name).apply { size = bytes.size.toLong() }
                tar.putArchiveEntry(entry); tar.write(bytes); tar.closeArchiveEntry()
            }
        }
    }
    private suspend fun fixture(block: suspend (File) -> Unit) {
        val root = Files.createTempDirectory("parakeet-test").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }
    @Test fun extractsOnlyVerifiedComponentsAndNeverArchivePaths() = runTest {
        fixture { root ->
            val out = File(root, "out")
            extractParakeet(archive(root, listOf("./$PARAKEET_PACKAGE/$model", "$PARAKEET_PACKAGE/$tokens",
                "../../escape", "$PARAKEET_PACKAGE/test_wavs/0.wav")), out, components)
            assertEquals(setOf(model, tokens), out.list()!!.toSet())
            assertArrayEquals(bytes, File(out, model).readBytes())
            assertFalse(File(root, "escape").exists())
        }
    }
    @Test fun rejectsDuplicateComponents() = runTest {
        fixture { root ->
            assertTrue(runCatching { extractParakeet(archive(root, listOf(model, model, tokens)), File(root, "out"), components) }.isFailure)
        }
    }
    @Test fun rejectsIncompleteArchives() = runTest {
        fixture { root ->
            assertTrue(runCatching { extractParakeet(archive(root, listOf(model)), File(root, "out"), components) }.isFailure)
        }
    }
    @Test fun rejectsAComponentWithAnIncorrectChecksum() = runTest {
        fixture { root ->
            val wrong = components + (model to ParakeetComponent(bytes.size.toLong(), "0".repeat(64)))
            assertTrue(runCatching { extractParakeet(archive(root, listOf(model, tokens)), File(root, "out"), wrong) }.isFailure)
        }
    }
    @Test fun rejectsAComponentWithAnIncorrectLength() = runTest {
        fixture { root ->
            val wrong = components + (model to ParakeetComponent(1, hash))
            assertTrue(runCatching { extractParakeet(archive(root, listOf(model, tokens)), File(root, "out"), wrong) }.isFailure)
        }
    }

    @Test fun rejectsSymbolicLinksEvenForWhitelistedNames() = runTest {
        fixture { root ->
            val input = File(root, "link.tar.bz2")
            TarArchiveOutputStream(BZip2CompressorOutputStream(input.outputStream())).use { tar ->
                val entry = TarArchiveEntry(model, '2'.code.toByte()).apply { linkName = "../escape" }
                tar.putArchiveEntry(entry); tar.closeArchiveEntry()
            }
            assertTrue(runCatching { extractParakeet(input, File(root, "out"), components) }.isFailure)
        }
    }
}
