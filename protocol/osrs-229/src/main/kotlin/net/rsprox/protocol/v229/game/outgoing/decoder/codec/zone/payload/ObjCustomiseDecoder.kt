package net.rsprox.protocol.v229.game.outgoing.decoder.codec.zone.payload

import net.rsprot.buffer.JagByteBuf
import net.rsprot.protocol.ClientProt
import net.rsprox.protocol.ProxyMessageDecoder
import net.rsprox.protocol.game.outgoing.model.zone.payload.ObjCustomise
import net.rsprox.protocol.game.outgoing.model.zone.payload.util.CoordInZone
import net.rsprox.protocol.session.Session
import net.rsprox.protocol.v229.game.outgoing.decoder.prot.GameServerProt

internal class ObjCustomiseDecoder : ProxyMessageDecoder<ObjCustomise> {
    override val prot: ClientProt = GameServerProt.OBJ_CUSTOMISE

    override fun decode(
        buffer: JagByteBuf,
        session: Session,
    ): ObjCustomise {
        val retexIndex = buffer.g2Alt1()
        val coordInZone = CoordInZone(buffer.g1Alt2())
        val recol = buffer.g2()
        val recolIndex = buffer.g2Alt3()
        val model = buffer.g2Alt2()
        val retex = buffer.g2Alt3()
        val quantity = buffer.g4Alt3()
        val id = buffer.g2Alt3()
        return ObjCustomise(
            id,
            quantity,
            model,
            recolIndex,
            recol,
            retexIndex,
            retex,
            coordInZone.xInZone,
            coordInZone.zInZone,
        )
    }
}
