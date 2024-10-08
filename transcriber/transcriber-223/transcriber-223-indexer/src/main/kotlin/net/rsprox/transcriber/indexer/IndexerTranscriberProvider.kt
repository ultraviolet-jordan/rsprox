package net.rsprox.transcriber.indexer

import net.rsprox.cache.api.CacheProvider
import net.rsprox.shared.SessionMonitor
import net.rsprox.shared.filters.PropertyFilterSetStore
import net.rsprox.shared.indexing.BinaryIndex
import net.rsprox.transcriber.BaseMessageConsumerContainer
import net.rsprox.transcriber.TranscriberPlugin
import net.rsprox.transcriber.TranscriberProvider
import net.rsprox.transcriber.TranscriberRunner
import net.rsprox.transcriber.state.StateTracker

public class IndexerTranscriberProvider : TranscriberProvider {
    override fun provide(
        container: BaseMessageConsumerContainer,
        cacheProvider: CacheProvider,
        monitor: SessionMonitor<*>,
        filters: PropertyFilterSetStore,
        binaryIndex: BinaryIndex,
    ): TranscriberRunner {
        val stateTracker = StateTracker()
        return TranscriberPlugin(
            IndexerTranscriber(
                cacheProvider,
                monitor,
                stateTracker,
                binaryIndex,
            ),
        )
    }
}
