package net.rsprox.proxy.client

import com.github.michaelbull.logging.InlineLogger
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.rsprot.buffer.extensions.p2
import net.rsprot.buffer.extensions.toJagByteBuf
import net.rsprot.crypto.rsa.decipherRsa
import net.rsprox.proxy.channel.remove
import net.rsprox.proxy.channel.replace
import net.rsprox.proxy.rsa.Rsa
import net.rsprox.proxy.rsa.rsa
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import java.math.BigInteger

public class ClientLoginHandler(
    private val serverChannel: Channel,
    private val rsa: RSAPrivateCrtKeyParameters,
    private val originalModulus: BigInteger,
) : SimpleChannelInboundHandler<WrappedIncomingMessage>(WrappedIncomingMessage::class.java) {
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: WrappedIncomingMessage,
    ) {
        when (msg.prot) {
            LoginClientProt.INIT_GAME_CONNECTION -> {
                logger.debug {
                    "Init game connection"
                }
            }
            LoginClientProt.INIT_JS5REMOTE_CONNECTION -> {
                logger.debug {
                    "Init JS5 remote connection"
                }
                switchToRelay(ctx)
            }
            LoginClientProt.GAMELOGIN -> {
                logger.debug {
                    "Game login received, re-encrypting RSA"
                }
                handleLogin(ctx, msg)
            }
            LoginClientProt.GAMERECONNECT -> {
                logger.debug {
                    "Game reconnect received, re-encrypting RSA"
                }
                handleLogin(ctx, msg)
            }
            LoginClientProt.POW_REPLY -> {
                logger.debug {
                    "Proof of Work reply received"
                }
            }
            LoginClientProt.UNKNOWN -> {
                logger.debug {
                    "Unknown login prot received"
                }
            }
            LoginClientProt.REMAINING_BETA_ARCHIVE_CRCS -> {
                logger.debug {
                    "Remaining beta archive CRCs received"
                }
            }
            LoginClientProt.SSL_WEB_CONNECTION -> {
                logger.debug { "SSL Web connection received, switching to relay" }
                switchToRelay(ctx)
            }
        }
        serverChannel.writeAndFlush(msg.encode(ctx.alloc()))
    }

    private fun handleLogin(
        ctx: ChannelHandlerContext,
        msg: WrappedIncomingMessage,
    ) {
        val buffer = msg.payload.toJagByteBuf()
        val version = buffer.g4()
        val subVersion = buffer.g4()
        val clientType = buffer.g1()
        val platformType = buffer.g1()
        buffer.skipRead(1)

        // The header^ will just naively be copied over
        val headerSize = 4 + 4 + 1 + 1 + 1

        val originalRsaSize = buffer.g2()
        val rsaSlice = buffer.buffer.readSlice(originalRsaSize)
        val xteaBlock = buffer.buffer.copy()
        val decryptedRsaBuffer = rsaSlice.rsa(rsa).toJagByteBuf()
        val rsaStart = decryptedRsaBuffer.readerIndex()
        decryptedRsaBuffer.skipRead(1)
        val isaacSeed =
            IntArray(4) {
                decryptedRsaBuffer.g4()
            }
        logger.debug {
            "Login info: $version, $subVersion, $clientType, $platformType, ${isaacSeed.contentToString()}"
        }
        val encoded = ctx.alloc().buffer(msg.payload.readableBytes())
        encoded.writeBytes(msg.payload, msg.start, headerSize)
        decryptedRsaBuffer.readerIndex(rsaStart)
        val encrypted =
            decryptedRsaBuffer.buffer.decipherRsa(
                Rsa.PUBLIC_EXPONENT,
                originalModulus,
                decryptedRsaBuffer.readableBytes(),
            )
        encoded.p2(encrypted.readableBytes())
        encoded.writeBytes(encrypted)
        encoded.writeBytes(xteaBlock)
        // Swap out the original login packet with the new one
        msg.replacePayload(encoded)
        // For now just switch back to relay
        switchToRelay(ctx)
    }

    private fun switchToRelay(ctx: ChannelHandlerContext) {
        val clientPipeline = ctx.channel().pipeline()
        clientPipeline.remove<ClientLoginDecoder>()
        clientPipeline.replace<ClientLoginHandler>(ClientRelayHandler(serverChannel))
    }

    private companion object {
        private val logger = InlineLogger()
    }
}