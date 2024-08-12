package net.rsprox.proxy.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.michaelbull.logging.InlineLogger
import net.rsprox.cache.Js5MasterIndex
import net.rsprox.cache.resolver.HistoricCacheResolver
import net.rsprox.proxy.binary.BinaryBlob
import net.rsprox.proxy.cache.StatefulCacheProvider
import net.rsprox.proxy.config.BINARY_PATH
import net.rsprox.proxy.config.FILTERS_DIRECTORY
import net.rsprox.proxy.filters.DefaultPropertyFilterSetStore
import net.rsprox.proxy.huffman.HuffmanProvider
import net.rsprox.proxy.plugin.DecodingSession
import net.rsprox.proxy.plugin.PluginLoader
import net.rsprox.proxy.util.NopSessionMonitor
import net.rsprox.shared.StreamDirection
import net.rsprox.shared.indexing.MultiMapBinaryIndex
import net.rsprox.transcriber.BaseMessageConsumerContainer
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.bufferedWriter
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.time.measureTime

@Suppress("DuplicatedCode")
public class IndexerCommand : CliktCommand(name = "index") {
    private val name by option("-name")

    override fun run() {
        Locale.setDefault(Locale.US)
        val pluginLoader = PluginLoader()
        HuffmanProvider.load()
        val provider = StatefulCacheProvider(HistoricCacheResolver())
        pluginLoader.loadTranscriberPlugins("osrs", provider)
        val fileName = this.name
        if (fileName != null) {
            val binaryName = if (fileName.endsWith(".bin")) fileName else "$fileName.bin"
            val file = BINARY_PATH.resolve(binaryName)
            if (!file.exists()) {
                echo("Unable to locate file $fileName in $BINARY_PATH")
                return
            }
            val time =
                measureTime {
                    stdoutTranscribe(file, pluginLoader, provider)
                }
            logger.debug { "$file took $time to index." }
        } else {
            val fileTreeWalk =
                BINARY_PATH
                    .toFile()
                    .walkTopDown()
                    .filter { it.extension == "bin" }
            for (bin in fileTreeWalk) {
                val time =
                    measureTime {
                        stdoutTranscribe(bin.toPath(), pluginLoader, provider)
                    }
                logger.debug { "${bin.name} took $time to index." }
            }
        }
    }

    private fun stdoutTranscribe(
        binaryPath: Path,
        pluginLoader: PluginLoader,
        statefulCacheProvider: StatefulCacheProvider,
    ) {
        val filters = DefaultPropertyFilterSetStore.load(FILTERS_DIRECTORY)
        val binary = BinaryBlob.decode(binaryPath, filters)
        statefulCacheProvider.update(
            Js5MasterIndex.trimmed(
                binary.header.revision,
                binary.header.js5MasterIndex,
            ),
        )
        val latestPlugin = pluginLoader.getPlugin(binary.header.revision)
        val transcriberProvider = pluginLoader.getIndexerProvider(binary.header.revision)
        val session = DecodingSession(binary, latestPlugin)
        val folder = binaryPath.parent.resolve("indexed")
        Files.createDirectories(folder)
        val consumers = BaseMessageConsumerContainer(emptyList())
        val index = MultiMapBinaryIndex()
        val runner =
            transcriberProvider.provide(
                consumers,
                statefulCacheProvider,
                NopSessionMonitor,
                filters,
                index,
            )

        folder.resolve(binaryPath.nameWithoutExtension + ".txt").bufferedWriter().use { writer ->
            writer.appendLine("------------------")
            writer.appendLine("Header information")
            writer.appendLine("version: ${binary.header.revision}.${binary.header.subRevision}")
            writer.appendLine("client type: ${binary.header.clientType}")
            writer.appendLine("platform type: ${binary.header.platformType}")
            writer.appendLine(
                "world: ${binary.header.worldId}, host: ${binary.header.worldHost}, " +
                    "flags: ${binary.header.worldFlags}, location: ${binary.header.worldLocation}, " +
                    "activity: ${binary.header.worldActivity}",
            )
            writer.appendLine("local player index: ${binary.header.localPlayerIndex}")
            writer.appendLine("-------------------")

            for ((direction, prot, packet) in session.sequence()) {
                try {
                    when (direction) {
                        StreamDirection.CLIENT_TO_SERVER -> {
                            runner.onClientProt(prot, packet)
                        }
                        StreamDirection.SERVER_TO_CLIENT -> {
                            runner.onServerPacket(prot, packet)
                        }
                    }
                } catch (t: NotImplementedError) {
                    continue
                }
            }
            consumers.close()

            val results = index.results()
            for ((type, counts) in results) {
                writer.appendLine("Indexing results for $type:")
                for ((key, count) in counts) {
                    writer.appendLine("    $key = $count")
                }
            }
        }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}

public fun main(args: Array<String>) {
    IndexerCommand().main(args)
}