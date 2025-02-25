package net.rsprox.transcriber

import net.rsprot.protocol.Prot
import net.rsprot.protocol.message.IncomingMessage

public interface TranscriberRunner {
    public fun onServerPacket(
        prot: Prot,
        message: IncomingMessage,
        revision: Int,
    )

    public fun onClientProt(
        prot: Prot,
        message: IncomingMessage,
        revision: Int,
    )

    public fun preprocess(packets: List<Packet>): List<Packet> {
        return packets
    }
}
