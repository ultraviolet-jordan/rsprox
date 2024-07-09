package net.rsprox.protocol.game.incoming.decoder.codec.social

import net.rsprot.buffer.JagByteBuf
import net.rsprot.protocol.ClientProt
import net.rsprot.protocol.message.codec.MessageDecoder
import net.rsprot.protocol.metadata.Consistent
import net.rsprot.protocol.tools.MessageDecodingTools
import net.rsprox.protocol.game.incoming.decoder.prot.GameClientProt
import net.rsprox.protocol.game.incoming.model.social.IgnoreListDel

@Consistent
public class IgnoreListDelDecoder : MessageDecoder<IgnoreListDel> {
    override val prot: ClientProt = GameClientProt.IGNORELIST_DEL

    override fun decode(
        buffer: JagByteBuf,
        tools: MessageDecodingTools,
    ): IgnoreListDel {
        val name = buffer.gjstr()
        return IgnoreListDel(name)
    }
}
